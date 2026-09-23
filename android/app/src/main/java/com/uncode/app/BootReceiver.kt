package com.uncode.app

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED != intent.action) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timeOffset = prefs.getLong("time_offset", 0L)
        val effectiveNow = System.currentTimeMillis() + timeOffset

        // 1. Snapshot lock state BEFORE reschedule
        val lockdownWasActive = prefs.getBoolean("lockdown_active", false)
        val lockEndTime = prefs.getLong("lock_end_time", 0L)
        val activeScheduleId = prefs.getString("active_schedule_id", "") ?: ""

        // 2. Reschedule all background schedule alarms & warning triggers
        ScheduleManager.rescheduleAll(context)

        // 3. CHECK_WAS_LOCKED Gate
        if (lockdownWasActive) {
            if (lockEndTime > 0 && effectiveNow >= lockEndTime) {
                // EXPIRE_TO_CONSEQUENCE: Timer expired while device was powered off
                Log.i(TAG, "Boot detected: lockdown already expired while powered off — transitioning to consequence")
                prefs.edit()
                    .putBoolean("lockdown_active", false)
                    .putBoolean("consequence_active", true)
                    .remove("lock_end_time")
                    .remove("active_schedule_id")
                    .apply()
                AlarmReceiver.cancelLockEndAlarm(context)
                FloatingOverlayService.stopService(context)
            } else {
                // RESCHED_ALARM: Lockdown still active
                Log.i(TAG, "Boot detected with active lockdown — rescheduling lock end and relaunching QIEZKA")
                if (lockEndTime > 0) {
                    AlarmReceiver.scheduleLockEndAlarm(context, lockEndTime, activeScheduleId)
                }
                relaunchQiezka(context)
            }
        } else {
            // CHECK_BOOT_SCHED: Lockdown was NOT active before restart
            checkBootScheduleStatus(context, prefs, effectiveNow)
        }
    }

    private fun checkBootScheduleStatus(context: Context, prefs: SharedPreferences, effectiveNow: Long) {
        val schedulesJson = prefs.getString("schedules_json", null)
        if (schedulesJson.isNullOrBlank()) {
            Log.i(TAG, "Boot check: no schedules exist -> Total System Idle")
            return
        }

        try {
            val arr = JSONArray(schedulesJson)
            var missedAnyWindow = false
            var activeScheduleToStart: JSONObject? = null
            var safeEndToStart: Long = 0L

            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                if (!s.optBoolean("isActive", true)) continue

                val id = s.optString("id", "")
                if (id.isEmpty()) continue

                val armedStart = prefs.getLong("armed_window_start_$id", 0L)
                val armedEnd = prefs.getLong("armed_window_end_$id", 0L)
                val lastCompletedEnd = prefs.getLong("last_completed_window_end_$id", 0L)

                // Check Case 1: Was an armed scheduled session missed while the phone was turned off?
                if (armedStart > 0 && armedEnd > 0 && armedEnd > lastCompletedEnd) {
                    if (effectiveNow >= armedEnd) {
                        Log.w(TAG, "Boot check: schedule $id window ($armedStart - $armedEnd) was missed while off! Transitioning to consequence.")
                        missedAnyWindow = true
                        // Clear armed window to prevent re-triggering on future boots
                        prefs.edit()
                            .remove("armed_window_start_$id")
                            .remove("armed_window_end_$id")
                            .apply()
                    }
                }

                // Check Case 2: Is effectiveNow currently inside an active schedule window?
                val timeStr = s.optString("activationTime", "")
                val durationMinutes = s.optInt("durationMinutes", 25)
                if (timeStr.contains(":")) {
                    val parts = timeStr.split(":")
                    val hour = parts[0].trim().toIntOrNull() ?: -1
                    val min = parts[1].trim().toIntOrNull() ?: -1
                    if (hour in 0..23 && min in 0..59) {
                        val cal = java.util.Calendar.getInstance()
                        cal.timeInMillis = effectiveNow
                        cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
                        cal.set(java.util.Calendar.MINUTE, min)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        val windowStart = cal.timeInMillis
                        val windowEnd = windowStart + (durationMinutes * 60L * 1000L)

                        if (effectiveNow in windowStart until windowEnd && windowEnd > lastCompletedEnd) {
                            activeScheduleToStart = s
                            safeEndToStart = Math.min(windowEnd, effectiveNow + (durationMinutes * 60L * 1000L))
                        }
                    }
                }
            }

            if (missedAnyWindow) {
                // MISSED_CONSEQUENCE: User evaded lock by turning off device
                prefs.edit()
                    .putBoolean("lockdown_active", false)
                    .putBoolean("consequence_active", true)
                    .remove("lock_end_time")
                    .remove("active_schedule_id")
                    .apply()
                AlarmReceiver.createNotificationChannels(context)
                AlarmReceiver.showNotificationStatic(
                    context,
                    AlarmReceiver.NOTIF_ID_COMPLETED,
                    AlarmReceiver.CHANNEL_ID_ALERTS,
                    "⚠️ Scheduled Lockdown Missed",
                    "A scheduled study session was missed while device was powered off. Distracting apps are restricted in Consequence Mode.",
                    Notification.PRIORITY_HIGH,
                    false
                )
                Log.i(TAG, "Missed scheduled session while powered off -> consequence mode activated")
                return
            }

            if (activeScheduleToStart != null) {
                // BOOT_START_LOCK: Device powered on while inside a lock window
                Log.i(TAG, "Boot occurred inside active schedule window -> starting lockdown immediately")
                AlarmReceiver.triggerScheduleStartDirectly(context, activeScheduleToStart, safeEndToStart)
                relaunchQiezka(context)
                return
            }

            // Otherwise: Upcoming Schedule or Zero Schedules.
            // Alarms have already been rescheduled by ScheduleManager.rescheduleAll().
            // System enters Standby or Idle.
        } catch (e: Exception) {
            Log.e(TAG, "Error checking boot schedule status: ${e.message}")
        }
    }

    private fun relaunchQiezka(context: Context) {
        var launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launch == null) {
            launch = Intent().setClassName(context.packageName, "com.uncode.app.MainActivity")
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(launch)
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val PREFS_NAME = "uncode_lock"
    }
}
