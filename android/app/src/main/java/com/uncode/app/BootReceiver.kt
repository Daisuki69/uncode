package com.uncode.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED != intent.action) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Reschedule all background schedule alarms & warning triggers
        ScheduleManager.rescheduleAll(context)

        // 2. Check lockdown status
        val lockdownWasActive = prefs.getBoolean("lockdown_active", false)
        val lockEndTime = prefs.getLong("lock_end_time", 0L)

        if (lockdownWasActive) {
            val now = System.currentTimeMillis()
            if (lockEndTime > 0 && now >= lockEndTime) {
                // Lock expired while device was powered off
                Log.i(TAG, "Boot detected: lockdown already expired while powered off — releasing")
                prefs.edit()
                    .putBoolean("lockdown_active", false)
                    .remove("lock_end_time")
                    .remove("active_schedule_id")
                    .apply()
            } else {
                Log.i(TAG, "Boot detected with active lockdown — rescheduling lock end and relaunching QIEZKA")
                if (lockEndTime > 0) {
                    AlarmReceiver.scheduleLockEndAlarm(context, lockEndTime, prefs.getString("active_schedule_id", "") ?: "")
                }
                var launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (launch == null) {
                    launch = Intent().setClassName(context.packageName, "com.uncode.app.MainActivity")
                }
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(launch)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val PREFS_NAME = "uncode_lock"
    }
}
