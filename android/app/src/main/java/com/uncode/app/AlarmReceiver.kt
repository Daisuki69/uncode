package com.uncode.app

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray
import org.json.JSONObject

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val PREFS_NAME = "uncode_lock"

        const val CHANNEL_ID_ALERTS = "qiezka_alerts_v2"
        const val CHANNEL_ID_STATUS = "qiezka_status_v2"

        const val ACTION_SCHEDULE_START = "com.uncode.app.ACTION_SCHEDULE_START"
        const val ACTION_LOCK_END = "com.uncode.app.ACTION_LOCK_END"
        const val ACTION_SCHEDULE_WARNING = "com.uncode.app.ACTION_SCHEDULE_WARNING"

        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_SCHEDULE_TITLE = "schedule_title"
        const val EXTRA_DURATION_MINUTES = "duration_minutes"
        const val EXTRA_LOCK_END_TIME = "lock_end_time"
        const val EXTRA_WARNING_TEXT = "warning_text"
        const val EXTRA_NOTIFICATION_ID = "notification_id"

        const val NOTIF_ID_STATUS = 1001
        const val NOTIF_ID_COMPLETED = 1002
        const val NOTIF_ID_WARNING = 1003

        @JvmStatic
        fun triggerScheduleStartDirectly(context: Context, s: JSONObject, lockEndTime: Long) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val scheduleId = s.optString("id", "default")
                val title = s.optString("title", "Study Session")
                val durationMinutes = s.optLong("durationMinutes", 25L)

                val timeOffset = prefs.getLong("time_offset", 0L)
                val effectiveNow = System.currentTimeMillis() + timeOffset
                val maxLockEnd = effectiveNow + (durationMinutes * 60L * 1000L)
                val safeLockEndTime = Math.min(lockEndTime, maxLockEnd)

                val alreadyActive = prefs.getBoolean("lockdown_active", false)

                prefs.edit()
                    .putBoolean("lockdown_active", true)
                    .putLong("lock_end_time", safeLockEndTime)
                    .putString("active_schedule_id", scheduleId)
                    .apply()

                scheduleLockEndAlarm(context, safeLockEndTime, scheduleId)

                // Only show notification and bring activity to front if this is a fresh transition into lockdown
                if (!alreadyActive) {
                    createNotificationChannels(context)
                    showNotificationStatic(
                        context,
                        NOTIF_ID_STATUS,
                        CHANNEL_ID_STATUS,
                        "🔒 QIEZKA Lockdown Active",
                        "$title is currently active ($durationMinutes min). Distracting apps are blocked.",
                        Notification.PRIORITY_HIGH,
                        true
                    )

                    EnforcementCoreService.ensureRunning(context)
                }

                // Start Floating Assistive Timer Ball Overlay
                FloatingOverlayService.startService(context, safeLockEndTime, title)

                if (!alreadyActive && LockAccessibilityService.instance != null) {
                    LockAccessibilityService.instance.onScheduleStartTriggered()
                }
            } catch (e: Exception) {
                Log.e(TAG, "triggerScheduleStartDirectly error: ${e.message}")
            }
        }

        @JvmStatic
        fun scheduleLockEndAlarm(context: Context, lockEndTimeMs: Long, scheduleId: String?) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val timeOffset = prefs.getLong("time_offset", 0L)

                // Convert simulated lockEndTimeMs back to real wall-clock timestamp
                val realTriggerAtMs = lockEndTimeMs - timeOffset
                val realNow = System.currentTimeMillis()

                if (realTriggerAtMs <= realNow) {
                    Log.i(TAG, "scheduleLockEndAlarm: already expired, releasing immediately")
                    prefs.edit()
                        .putBoolean("lockdown_active", false)
                        .remove("lock_end_time")
                        .remove("active_schedule_id")
                        .apply()
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    nm?.cancel(NOTIF_ID_STATUS)
                    return
                }

                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_LOCK_END
                    putExtra(EXTRA_SCHEDULE_ID, scheduleId)
                    putExtra(EXTRA_LOCK_END_TIME, lockEndTimeMs)
                }

                var flags = PendingIntent.FLAG_UPDATE_CURRENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags = flags or PendingIntent.FLAG_IMMUTABLE
                }

                val pi = PendingIntent.getBroadcast(context, 99999, intent, flags)
                scheduleExactAlarmCompat(context, am, realTriggerAtMs, pi)
                Log.i(TAG, "Scheduled exact lock end alarm for real timestamp: $realTriggerAtMs (effective: $lockEndTimeMs)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule lock end alarm: ${e.message}")
            }
        }

        @JvmStatic
        fun cancelLockEndAlarm(context: Context) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_LOCK_END
                }

                var flags = PendingIntent.FLAG_NO_CREATE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags = flags or PendingIntent.FLAG_IMMUTABLE
                }

                val pi = PendingIntent.getBroadcast(context, 99999, intent, flags)
                if (pi != null) {
                    am.cancel(pi)
                    pi.cancel()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel lock end alarm: ${e.message}")
            }
        }

        @JvmStatic
        fun scheduleExactAlarmCompat(context: Context, am: AlarmManager, triggerAtMs: Long, pi: PendingIntent) {
            // Priority 1: Check canScheduleExactAlarms (Android 12+) and use setExactAndAllowWhileIdle
            var canExact = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                canExact = am.canScheduleExactAlarms()
            }

            if (canExact) {
                try {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
                    Log.i(TAG, "Scheduled exact alarm with setExactAndAllowWhileIdle for timestamp: $triggerAtMs")
                    return
                } catch (e: Exception) {
                    Log.w(TAG, "setExactAndAllowWhileIdle failed (${e.message}), falling back to setAlarmClock...")
                }
            }

            // Priority 2: setAlarmClock with ActivityOptions background start mode (bypasses BAL restrictions on Android 14/15/16)
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                var pFlags = PendingIntent.FLAG_UPDATE_CURRENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pFlags = pFlags or PendingIntent.FLAG_IMMUTABLE
                }

                val showPi = PendingIntent.getActivity(context, 0, launchIntent, pFlags)

                val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMs, showPi)
                am.setAlarmClock(clockInfo, pi)
                Log.i(TAG, "Successfully scheduled with setAlarmClock for timestamp: $triggerAtMs")
                return
            } catch (e: Exception) {
                Log.w(TAG, "setAlarmClock failed (${e.message}), falling back to standard alarm...")
            }

            // Priority 3: Fallback alarm
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
                }
                Log.i(TAG, "Scheduled with fallback alarm for timestamp: $triggerAtMs")
            } catch (e: Exception) {
                Log.e(TAG, "All alarm scheduling attempts failed: ${e.message}")
            }
        }

        @JvmStatic
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()

                val vibrationPattern = longArrayOf(0, 350, 200, 350)

                val alertsChannel = NotificationChannel(
                    CHANNEL_ID_ALERTS,
                    "Schedule Warnings & Completion",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Advance warnings before schedules start and unlock completion notifications."
                    enableVibration(true)
                    setVibrationPattern(vibrationPattern)
                    enableLights(true)
                    setSound(soundUri, audioAttributes)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                val statusChannel = NotificationChannel(
                    CHANNEL_ID_STATUS,
                    "Lockdown Status",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts and persistent status during active lockdown."
                    enableVibration(true)
                    setVibrationPattern(vibrationPattern)
                    enableLights(true)
                    setSound(soundUri, audioAttributes)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                nm.createNotificationChannel(alertsChannel)
                nm.createNotificationChannel(statusChannel)
            }
        }

        @JvmStatic
        fun showNotificationStatic(context: Context, id: Int, channelId: String, title: String, body: String, priority: Int, ongoing: Boolean) {
            try {
                createNotificationChannels(context)

                val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                if (!notificationsEnabled) {
                    Log.w(TAG, "Cannot show notification: POST_NOTIFICATIONS is disabled in OS settings for ${context.packageName}")
                }

                var launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (launchIntent == null) {
                    launchIntent = Intent().setClassName(context.getPackageName(), "com.uncode.app.MainActivity")
                }
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

                var pFlags = PendingIntent.FLAG_UPDATE_CURRENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pFlags = pFlags or PendingIntent.FLAG_IMMUTABLE
                }

                val contentIntent = PendingIntent.getActivity(context, id, launchIntent, pFlags)

                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val vibrationPattern = longArrayOf(0, 350, 200, 350)

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_qiezka_notif)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(!ongoing)
                    .setOngoing(ongoing)
                    .setContentIntent(contentIntent)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(if (ongoing) NotificationCompat.CATEGORY_STATUS else NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSound(soundUri)
                    .setVibrate(vibrationPattern)
                    .setLights(0xFF4F46E5.toInt(), 500, 1000)

                if (Build.VERSION.SDK_INT >= 34) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    if (nm != null && nm.canUseFullScreenIntent()) {
                        builder.setFullScreenIntent(contentIntent, true)
                    }
                } else {
                    builder.setFullScreenIntent(contentIntent, true)
                }

                NotificationManagerCompat.from(context).notify(id, builder.build())

                // Active Hardware Vibration Trigger for instant tactile feedback
                try {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(vibrationPattern, -1)
                        }
                    }
                } catch (ex: Exception) {
                    Log.w(TAG, "Direct vibration trigger skipped: ${ex.message}")
                }

                Log.i(TAG, "Notification displayed successfully: $title (id=$id)")
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying notification ($title): ${e.message}", e)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null || intent.action == null) return
        val action = intent.action
        Log.i(TAG, "onReceive triggered with action: $action")

        createNotificationChannels(context)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        when (action) {
            ACTION_SCHEDULE_START -> handleScheduleStart(context, intent, prefs)
            ACTION_LOCK_END -> handleLockEnd(context, intent, prefs)
            ACTION_SCHEDULE_WARNING -> handleScheduleWarning(context, intent)
        }
    }

    private fun handleScheduleStart(context: Context, intent: Intent, prefs: SharedPreferences) {
        val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID)
        var title = intent.getStringExtra(EXTRA_SCHEDULE_TITLE)
        var durationMinutes = intent.getLongExtra(EXTRA_DURATION_MINUTES, 25L)

        // Verify that this schedule actually exists and is active in schedules_json
        val schedulesJson = prefs.getString("schedules_json", null)
        var scheduleExistsAndActive = false
        if (schedulesJson != null && scheduleId != null) {
            try {
                val arr = JSONArray(schedulesJson)
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    if (scheduleId == s.optString("id", "")) {
                        if (s.optBoolean("isActive", true)) {
                            scheduleExistsAndActive = true
                            durationMinutes = s.optLong("durationMinutes", durationMinutes)
                            title = s.optString("title", title) ?: title
                        }
                        break
                    }
                }
            } catch (ignore: Exception) {}
        }

        if (!scheduleExistsAndActive) {
            Log.w(TAG, "handleScheduleStart: schedule $scheduleId does not exist in schedules_json or is inactive — discarding orphaned alarm!")
            return
        }

        val timeOffset = prefs.getLong("time_offset", 0L)
        val effectiveNow = System.currentTimeMillis() + timeOffset
        val lockEndTime = effectiveNow + (durationMinutes * 60L * 1000L)

        Log.i(TAG, "Starting scheduled lockdown for schedule: $scheduleId, duration=${durationMinutes}m")

        prefs.edit()
            .putBoolean("lockdown_active", true)
            .putLong("lock_end_time", lockEndTime)
            .putString("active_schedule_id", scheduleId ?: "")
            .apply()

        // Schedule exact auto-release alarm
        scheduleLockEndAlarm(context, lockEndTime, scheduleId)

        // Send high-priority notification with fullScreenIntent
        val sessionTitle = if (!title.isNullOrBlank()) title else "Study Session"
        showNotification(
            context,
            NOTIF_ID_STATUS,
            CHANNEL_ID_STATUS,
            "🔒 QIEZKA Lockdown Engaged",
            "$sessionTitle is active ($durationMinutes min). Distracting apps are blocked.",
            Notification.PRIORITY_HIGH,
            true
        )

        // Bring up MainActivity if overlay permission is permitted
        try {
            EnforcementCoreService.ensureRunning(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure EnforcementCoreService on schedule start: ${e.message}")
        }

        // Start Floating Assistive Timer Ball Overlay
        FloatingOverlayService.startService(context, lockEndTime, sessionTitle)

        // Instantly enforce blocking if user is currently inside a non-whitelisted app
        if (LockAccessibilityService.instance != null) {
            LockAccessibilityService.instance.onScheduleStartTriggered()
        }
    }

    private fun handleLockEnd(context: Context, intent: Intent, prefs: SharedPreferences) {
        Log.i(TAG, "Lockdown session timer expired by alarm — activating Consequence Mode")

        prefs.edit()
            .putBoolean("consequence_active", true)
            .remove("lock_end_time")
            .apply()

        FloatingOverlayService.stopService(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(NOTIF_ID_STATUS)

        val isHardcore = "hardcore".equals(prefs.getString("operating_mode", "safemode"), ignoreCase = true)
        val notifTitle = if (isHardcore) {
            "⚠️ Homework Expired — Consequence Active (Hardcore)"
        } else {
            "⚠️ Homework Expired — Consequence Active"
        }
        val notifMsg = if (isHardcore) {
            "Study session expired without passing. Distracting apps remain restricted 24/7 (Hardcore Mode) until rescheduled and passed."
        } else {
            "Study session expired without passing. Distracting apps remain restricted during operating hours (7 PM – 3 AM) until rescheduled and passed."
        }

        showNotification(
            context,
            NOTIF_ID_COMPLETED,
            CHANNEL_ID_ALERTS,
            notifTitle,
            notifMsg,
            Notification.PRIORITY_HIGH,
            false
        )
    }

    private fun handleScheduleWarning(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLockdownActive = prefs.getBoolean("lockdown_active", false)
        if (isLockdownActive) {
            Log.i(TAG, "handleScheduleWarning: lockdown already active, suppressing advance warning")
            return
        }

        val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID)
        val schedulesJson = prefs.getString("schedules_json", null)
        var scheduleExistsAndActive = false
        if (schedulesJson != null && scheduleId != null) {
            try {
                val arr = JSONArray(schedulesJson)
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    if (scheduleId == s.optString("id", "")) {
                        if (s.optBoolean("isActive", true)) {
                            scheduleExistsAndActive = true
                        }
                        break
                    }
                }
            } catch (ignore: Exception) {}
        }

        if (!scheduleExistsAndActive) {
            Log.w(TAG, "handleScheduleWarning: schedule $scheduleId does not exist in schedules_json or is inactive — discarding orphaned warning!")
            return
        }

        var warningText = intent.getStringExtra(EXTRA_WARNING_TEXT)
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, NOTIF_ID_WARNING)
        if (warningText.isNullOrBlank()) {
            warningText = "Upcoming study session starting soon. Prepare your homework materials."
        }

        Log.i(TAG, "Posting schedule advance warning: $warningText")

        showNotification(
            context,
            notifId,
            CHANNEL_ID_ALERTS,
            "⏰ QIEZKA Upcoming Lockdown",
            warningText,
            Notification.PRIORITY_HIGH,
            false
        )
    }

    private fun showNotification(context: Context, id: Int, channelId: String, title: String, body: String, priority: Int, ongoing: Boolean) {
        showNotificationStatic(context, id, channelId, title, body, priority, ongoing)
    }
}
