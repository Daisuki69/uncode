package com.uncode.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.HashSet

object ScheduleManager {

    private const val TAG = "ScheduleManager"
    const val PREFS_NAME = "uncode_lock"

    @JvmStatic
    fun syncSchedules(context: Context, schedulesJson: String?, whitelist: Set<String>?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        if (schedulesJson != null) {
            editor.putString("schedules_json", schedulesJson)
        }
        if (whitelist != null) {
            editor.putStringSet("whitelist", whitelist)
        }
        editor.apply()

        rescheduleAll(context)
    }

    @JvmStatic
    fun rescheduleAll(context: Context) {
        cancelAllScheduledAlarms(context)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timeOffset = prefs.getLong("time_offset", 0L)
        val effectiveNow = System.currentTimeMillis() + timeOffset
        val isLockActive = prefs.getBoolean("lockdown_active", false)
        val currentEnd = prefs.getLong("lock_end_time", 0L)

        // If lockdown was active but effective clock is now at or beyond lockEndTime, cleanly end the session and transition to consequence!
        if (isLockActive && currentEnd > 0 && effectiveNow >= currentEnd) {
            Log.i(TAG, "rescheduleAll: effectiveNow ($effectiveNow) >= currentEnd ($currentEnd) — ending expired lockdown")
            prefs.edit()
                .putBoolean("lockdown_active", false)
                .putBoolean("consequence_active", true)
                .remove("lock_end_time")
                .remove("active_schedule_id")
                .apply()
            AlarmReceiver.cancelLockEndAlarm(context)
            FloatingOverlayService.stopService(context)
        }

        val json = prefs.getString("schedules_json", null)
        if (json.isNullOrBlank()) return

        try {
            val arr = JSONArray(json)
            val activeAlarmCodes = HashSet<String>()

            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                if (!s.optBoolean("isActive", true)) continue

                scheduleAlarmsForSingle(context, s, activeAlarmCodes)
            }

            prefs.edit().putStringSet("scheduled_alarm_codes", activeAlarmCodes).apply()
            Log.i(TAG, "Rescheduled ${activeAlarmCodes.size} alarm triggers")
        } catch (e: Exception) {
            Log.e(TAG, "Error in rescheduleAll: ${e.message}")
        }
    }

    private fun scheduleAlarmsForSingle(context: Context, s: JSONObject, activeAlarmCodes: MutableSet<String>) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val id = s.optString("id", "default")
            val title = s.optString("title", "Study Session")
            val timeStr = s.optString("activationTime", "")
            val durationMinutes = s.optInt("durationMinutes", 25)

            if (!timeStr.contains(":")) return
            val parts = timeStr.split(":")
            val hour = parts[0].trim().toInt()
            val min = parts[1].trim().toInt()

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val timeOffset = prefs.getLong("time_offset", 0L)
            val realNow = System.currentTimeMillis()
            val effectiveNow = realNow + timeOffset

            val target = Calendar.getInstance().apply {
                timeInMillis = effectiveNow
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val durationMs = durationMinutes * 60L * 1000L
            var targetStartSim = target.timeInMillis
            val targetEndSim = targetStartSim + durationMs

            // Check if currently inside today's active window!
            if (effectiveNow >= targetStartSim && effectiveNow < targetEndSim) {
                val lastCompletedEnd = prefs.getLong("last_completed_window_end_$id", 0L)
                if (targetEndSim <= lastCompletedEnd) {
                    Log.i(TAG, "Schedule $id window already completed for today. Scheduling for tomorrow.")
                    target.add(Calendar.DAY_OF_YEAR, 1)
                    targetStartSim = target.timeInMillis
                    val realTriggerStartMs = targetStartSim - timeOffset
                    val baseCode = Math.abs(id.hashCode()) % 100000
                    val startCode = baseCode * 10 + 0
                    scheduleExact(context, am, realTriggerStartMs, startCode, AlarmReceiver.ACTION_SCHEDULE_START, id, title, durationMinutes, null)
                    activeAlarmCodes.add(startCode.toString())
                    prefs.edit()
                        .putLong("armed_window_start_$id", targetStartSim)
                        .putLong("armed_window_end_$id", targetStartSim + durationMs)
                        .apply()
                    return
                }
                val safeEnd = Math.min(targetEndSim, effectiveNow + durationMs)
                Log.i(TAG, "Schedule $id is active RIGHT NOW! Triggering lockdown immediately (safeEnd=$safeEnd).")
                AlarmReceiver.triggerScheduleStartDirectly(context, s, safeEnd)
                return
            }

            // If the window has already passed for today, schedule for tomorrow
            if (effectiveNow >= targetEndSim) {
                target.add(Calendar.DAY_OF_YEAR, 1)
                targetStartSim = target.timeInMillis
            }

            // Real wall-clock trigger time for AlarmManager
            val realTriggerStartMs = targetStartSim - timeOffset
            val baseCode = Math.abs(id.hashCode()) % 100000

            // 1. Exact Start Alarm
            val startCode = baseCode * 10 + 0
            scheduleExact(context, am, realTriggerStartMs, startCode, AlarmReceiver.ACTION_SCHEDULE_START, id, title, durationMinutes, null)
            activeAlarmCodes.add(startCode.toString())
            prefs.edit()
                .putLong("armed_window_start_$id", targetStartSim)
                .putLong("armed_window_end_$id", targetStartSim + durationMs)
                .apply()
            Log.i(TAG, "Scheduled start alarm for $id at real timestamp $realTriggerStartMs (armed window: $targetStartSim - ${targetStartSim + durationMs})")

            // 2. Warning Alarms (30 min, 15 min, 5 min, 1 min, 30 sec, 10 sec)
            val warningOffsets = longArrayOf(
                30L * 60L * 1000L,  // 30 min
                15L * 60L * 1000L,  // 15 min
                5L * 60L * 1000L,   // 5 min
                1L * 60L * 1000L,   // 1 min
                30L * 1000L,        // 30 sec
                10L * 1000L         // 10 sec
            )

            val warningTexts = arrayOf(
                "⏰ Upcoming study session in 30 minutes! Wrap up your tasks.",
                "⏰ Study session starts in 15 minutes! Prepare your materials.",
                "⚠️ Lockdown in 5 minutes! Save your work on other apps.",
                "⚠️ Lockdown in 1 minute! QIEZKA is preparing to lock.",
                "🚨 Lockdown starting in 30 seconds! QIEZKA is engaging.",
                "🚨 Lockdown starting in 10 seconds!"
            )

            for (w in warningOffsets.indices) {
                val warnSimTime = targetStartSim - warningOffsets[w]
                if (warnSimTime > effectiveNow) {
                    val realWarnTime = warnSimTime - timeOffset
                    val warnCode = baseCode * 10 + (w + 1)
                    scheduleExact(context, am, realWarnTime, warnCode, AlarmReceiver.ACTION_SCHEDULE_WARNING, id, title, durationMinutes, warningTexts[w])
                    activeAlarmCodes.add(warnCode.toString())
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling alarms for " + s.optString("id") + ": " + e.message)
        }
    }

    private fun scheduleExact(
        context: Context,
        am: AlarmManager,
        triggerAtMs: Long,
        requestCode: Int,
        action: String,
        scheduleId: String,
        title: String,
        durationMinutes: Int,
        warningText: String?
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            setAction(action)
            putExtra(AlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(AlarmReceiver.EXTRA_SCHEDULE_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_DURATION_MINUTES, durationMinutes.toLong())
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            if (warningText != null) {
                putExtra(AlarmReceiver.EXTRA_WARNING_TEXT, warningText)
            }
        }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }

        val pi = PendingIntent.getBroadcast(context, requestCode, intent, flags)
        AlarmReceiver.scheduleExactAlarmCompat(context, am, triggerAtMs, pi)
    }

    @JvmStatic
    fun cancelAllScheduledAlarms(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val codes = prefs.getStringSet("scheduled_alarm_codes", null)
            if (codes != null) {
                val actions = arrayOf<String?>(
                    AlarmReceiver.ACTION_SCHEDULE_START,
                    AlarmReceiver.ACTION_SCHEDULE_WARNING,
                    null
                )

                for (codeStr in codes) {
                    try {
                        val code = codeStr.toInt()
                        for (action in actions) {
                            val intent = Intent(context, AlarmReceiver::class.java)
                            if (action != null) {
                                intent.action = action
                            }
                            var pFlags = PendingIntent.FLAG_UPDATE_CURRENT
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                pFlags = pFlags or PendingIntent.FLAG_IMMUTABLE
                            }
                            val pi = PendingIntent.getBroadcast(context, code, intent, pFlags)
                            if (pi != null) {
                                am.cancel(pi)
                                pi.cancel()
                            }
                        }
                    } catch (ignore: Exception) {}
                }
            }
            prefs.edit().remove("scheduled_alarm_codes").apply()
        } catch (e: Exception) {
            Log.e(TAG, "cancelAllScheduledAlarms error: ${e.message}")
        }
    }
}
