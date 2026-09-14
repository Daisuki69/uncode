package com.uncode.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.Notification;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import org.json.JSONObject;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    public static final String PREFS_NAME = "uncode_lock";

    public static final String CHANNEL_ID_ALERTS = "qiezka_alerts_v2";
    public static final String CHANNEL_ID_STATUS = "qiezka_status_v2";

    public static final String ACTION_SCHEDULE_START = "com.uncode.app.ACTION_SCHEDULE_START";
    public static final String ACTION_LOCK_END = "com.uncode.app.ACTION_LOCK_END";
    public static final String ACTION_SCHEDULE_WARNING = "com.uncode.app.ACTION_SCHEDULE_WARNING";

    public static final String EXTRA_SCHEDULE_ID = "schedule_id";
    public static final String EXTRA_SCHEDULE_TITLE = "schedule_title";
    public static final String EXTRA_DURATION_MINUTES = "duration_minutes";
    public static final String EXTRA_LOCK_END_TIME = "lock_end_time";
    public static final String EXTRA_WARNING_TEXT = "warning_text";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    public static final int NOTIF_ID_STATUS = 1001;
    public static final int NOTIF_ID_COMPLETED = 1002;
    public static final int NOTIF_ID_WARNING = 1003;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        Log.i(TAG, "onReceive triggered with action: " + action);

        createNotificationChannels(context);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (ACTION_SCHEDULE_START.equals(action)) {
            handleScheduleStart(context, intent, prefs);
        } else if (ACTION_LOCK_END.equals(action)) {
            handleLockEnd(context, intent, prefs);
        } else if (ACTION_SCHEDULE_WARNING.equals(action)) {
            handleScheduleWarning(context, intent);
        }
    }

    private void handleScheduleStart(Context context, Intent intent, SharedPreferences prefs) {
        String scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID);
        String title = intent.getStringExtra(EXTRA_SCHEDULE_TITLE);
        long durationMinutes = intent.getLongExtra(EXTRA_DURATION_MINUTES, 25L);

        // Verify that this schedule actually exists and is active in schedules_json
        String schedulesJson = prefs.getString("schedules_json", null);
        boolean scheduleExistsAndActive = false;
        if (schedulesJson != null && scheduleId != null) {
            try {
                org.json.JSONArray arr = new org.json.JSONArray(schedulesJson);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject s = arr.getJSONObject(i);
                    if (scheduleId.equals(s.optString("id", ""))) {
                        if (s.optBoolean("isActive", true)) {
                            scheduleExistsAndActive = true;
                            durationMinutes = s.optLong("durationMinutes", durationMinutes);
                            title = s.optString("title", title);
                        }
                        break;
                    }
                }
            } catch (Exception ignore) {}
        }

        if (!scheduleExistsAndActive) {
            Log.w(TAG, "handleScheduleStart: schedule " + scheduleId + " does not exist in schedules_json or is inactive — discarding orphaned alarm!");
            return;
        }

        long timeOffset = prefs.getLong("time_offset", 0L);
        long effectiveNow = System.currentTimeMillis() + timeOffset;
        long lockEndTime = effectiveNow + (durationMinutes * 60L * 1000L);

        Log.i(TAG, "Starting scheduled lockdown for schedule: " + scheduleId + ", duration=" + durationMinutes + "m");

        prefs.edit()
                .putBoolean("lockdown_active", true)
                .putLong("lock_end_time", lockEndTime)
                .putString("active_schedule_id", scheduleId != null ? scheduleId : "")
                .apply();

        // Schedule exact auto-release alarm
        scheduleLockEndAlarm(context, lockEndTime, scheduleId);

        // Send high-priority notification with fullScreenIntent
        String sessionTitle = (title != null && !title.trim().isEmpty()) ? title : "Study Session";
        showNotification(
                context,
                NOTIF_ID_STATUS,
                CHANNEL_ID_STATUS,
                "🔒 QIEZKA Lockdown Engaged",
                sessionTitle + " is active (" + durationMinutes + " min). Distracting apps are blocked.",
                Notification.PRIORITY_HIGH,
                true
        );

        // Bring up MainActivity if overlay permission is permitted
        try {
            if (Settings.canDrawOverlays(context)) {
                Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                if (launch == null) {
                    launch = new Intent().setClassName(context.getPackageName(), "com.uncode.app.MainActivity");
                }
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                launch.putExtra("route", "locked");
                launch.putExtra("schedule_id", scheduleId);
                context.startActivity(launch);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch MainActivity on schedule start: " + e.getMessage());
        }

        // Start Floating Assistive Timer Ball Overlay
        FloatingOverlayService.startService(context, lockEndTime, sessionTitle);

        // Instantly enforce blocking if user is currently inside a non-whitelisted app
        if (LockAccessibilityService.instance != null) {
            LockAccessibilityService.instance.onScheduleStartTriggered();
        }
    }

    public static void triggerScheduleStartDirectly(Context context, JSONObject s, long lockEndTime) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String scheduleId = s.optString("id", "default");
            String title = s.optString("title", "Study Session");
            long durationMinutes = s.optLong("durationMinutes", 25L);

            long timeOffset = prefs.getLong("time_offset", 0L);
            long effectiveNow = System.currentTimeMillis() + timeOffset;
            long maxLockEnd = effectiveNow + (durationMinutes * 60L * 1000L);
            long safeLockEndTime = Math.min(lockEndTime, maxLockEnd);

            boolean alreadyActive = prefs.getBoolean("lockdown_active", false);

            prefs.edit()
                    .putBoolean("lockdown_active", true)
                    .putLong("lock_end_time", safeLockEndTime)
                    .putString("active_schedule_id", scheduleId)
                    .apply();

            scheduleLockEndAlarm(context, safeLockEndTime, scheduleId);

            // Only show notification and bring activity to front if this is a fresh transition into lockdown
            if (!alreadyActive) {
                createNotificationChannels(context);
                showNotificationStatic(
                        context,
                        NOTIF_ID_STATUS,
                        CHANNEL_ID_STATUS,
                        "🔒 QIEZKA Lockdown Active",
                        title + " is currently active (" + durationMinutes + " min). Distracting apps are blocked.",
                        Notification.PRIORITY_HIGH,
                        true
                );

                Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    context.startActivity(launch);
                }
            }

            // Start Floating Assistive Timer Ball Overlay
            FloatingOverlayService.startService(context, safeLockEndTime, title);

            if (!alreadyActive && LockAccessibilityService.instance != null) {
                LockAccessibilityService.instance.onScheduleStartTriggered();
            }
        } catch (Exception e) {
            Log.e(TAG, "triggerScheduleStartDirectly error: " + e.getMessage());
        }
    }

    private void handleLockEnd(Context context, Intent intent, SharedPreferences prefs) {
        Log.i(TAG, "Lockdown session timer expired by alarm — activating Consequence Mode");

        prefs.edit()
                .putBoolean("consequence_active", true)
                .remove("lock_end_time")
                .apply();

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIF_ID_STATUS);
        }

        showNotification(
                context,
                NOTIF_ID_COMPLETED,
                CHANNEL_ID_ALERTS,
                "⚠️ Homework Expired — Consequence Active",
                "Study session expired without passing. Distracting apps remain restricted during operating hours (7 PM – 3 AM) until rescheduled and passed.",
                Notification.PRIORITY_HIGH,
                false
        );
    }

    private void handleScheduleWarning(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isLockdownActive = prefs.getBoolean("lockdown_active", false);
        if (isLockdownActive) {
            Log.i(TAG, "handleScheduleWarning: lockdown already active, suppressing advance warning");
            return;
        }

        String scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID);
        String schedulesJson = prefs.getString("schedules_json", null);
        boolean scheduleExistsAndActive = false;
        if (schedulesJson != null && scheduleId != null) {
            try {
                org.json.JSONArray arr = new org.json.JSONArray(schedulesJson);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject s = arr.getJSONObject(i);
                    if (scheduleId.equals(s.optString("id", ""))) {
                        if (s.optBoolean("isActive", true)) {
                            scheduleExistsAndActive = true;
                        }
                        break;
                    }
                }
            } catch (Exception ignore) {}
        }

        if (!scheduleExistsAndActive) {
            Log.w(TAG, "handleScheduleWarning: schedule " + scheduleId + " does not exist in schedules_json or is inactive — discarding orphaned warning!");
            return;
        }

        String warningText = intent.getStringExtra(EXTRA_WARNING_TEXT);
        int notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, NOTIF_ID_WARNING);
        if (warningText == null || warningText.trim().isEmpty()) {
            warningText = "Upcoming study session starting soon. Prepare your homework materials.";
        }

        Log.i(TAG, "Posting schedule advance warning: " + warningText);

        showNotification(
                context,
                notifId,
                CHANNEL_ID_ALERTS,
                "⏰ QIEZKA Upcoming Lockdown",
                warningText,
                Notification.PRIORITY_HIGH,
                false
        );
    }

    public static void scheduleLockEndAlarm(Context context, long lockEndTimeMs, String scheduleId) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            long timeOffset = prefs.getLong("time_offset", 0L);

            // Convert simulated lockEndTimeMs back to real wall-clock timestamp
            long realTriggerAtMs = lockEndTimeMs - timeOffset;
            long realNow = System.currentTimeMillis();

            if (realTriggerAtMs <= realNow) {
                Log.i(TAG, "scheduleLockEndAlarm: already expired, releasing immediately");
                prefs.edit()
                        .putBoolean("lockdown_active", false)
                        .remove("lock_end_time")
                        .remove("active_schedule_id")
                        .apply();
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.cancel(NOTIF_ID_STATUS);
                return;
            }

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction(ACTION_LOCK_END);
            intent.putExtra(EXTRA_SCHEDULE_ID, scheduleId);
            intent.putExtra(EXTRA_LOCK_END_TIME, lockEndTimeMs);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pi = PendingIntent.getBroadcast(context, 99999, intent, flags);
            scheduleExactAlarmCompat(context, am, realTriggerAtMs, pi);
            Log.i(TAG, "Scheduled exact lock end alarm for real timestamp: " + realTriggerAtMs + " (effective: " + lockEndTimeMs + ")");
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule lock end alarm: " + e.getMessage());
        }
    }

    public static void cancelLockEndAlarm(Context context) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction(ACTION_LOCK_END);

            int flags = PendingIntent.FLAG_NO_CREATE;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pi = PendingIntent.getBroadcast(context, 99999, intent, flags);
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel lock end alarm: " + e.getMessage());
        }
    }

    public static void scheduleExactAlarmCompat(Context context, AlarmManager am, long triggerAtMs, PendingIntent pi) {
        // Priority 1: Check canScheduleExactAlarms (Android 12+) and use setExactAndAllowWhileIdle
        boolean canExact = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canExact = am.canScheduleExactAlarms();
        }

        if (canExact) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi);
                Log.i(TAG, "Scheduled exact alarm with setExactAndAllowWhileIdle for timestamp: " + triggerAtMs);
                return;
            } catch (Exception e) {
                Log.w(TAG, "setExactAndAllowWhileIdle failed (" + e.getMessage() + "), falling back to setAlarmClock...");
            }
        }

        // Priority 2: setAlarmClock with ActivityOptions background start mode (bypasses BAL restrictions on Android 14/15/16)
        try {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            int pFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pFlags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent showPi = PendingIntent.getActivity(context, 0, launchIntent, pFlags);

            AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(triggerAtMs, showPi);
            am.setAlarmClock(clockInfo, pi);
            Log.i(TAG, "Successfully scheduled with setAlarmClock for timestamp: " + triggerAtMs);
            return;
        } catch (Exception e) {
            Log.w(TAG, "setAlarmClock failed (" + e.getMessage() + "), falling back to standard alarm...");
        }

        // Priority 3: Fallback alarm
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pi);
            }
            Log.i(TAG, "Scheduled with fallback alarm for timestamp: " + triggerAtMs);
        } catch (Exception e) {
            Log.e(TAG, "All alarm scheduling attempts failed: " + e.getMessage());
        }
    }

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            android.net.Uri soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
            android.media.AudioAttributes audioAttributes = new android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            long[] vibrationPattern = new long[]{0, 300, 200, 300};

            NotificationChannel alertsChannel = new NotificationChannel(
                    CHANNEL_ID_ALERTS,
                    "Schedule Warnings & Completion",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertsChannel.setDescription("Advance warnings before schedules start and unlock completion notifications.");
            alertsChannel.enableVibration(true);
            alertsChannel.setVibrationPattern(vibrationPattern);
            alertsChannel.enableLights(true);
            alertsChannel.setSound(soundUri, audioAttributes);
            alertsChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationChannel statusChannel = new NotificationChannel(
                    CHANNEL_ID_STATUS,
                    "Lockdown Status",
                    NotificationManager.IMPORTANCE_HIGH
            );
            statusChannel.setDescription("Alerts and persistent status during active lockdown.");
            statusChannel.enableVibration(true);
            statusChannel.setVibrationPattern(vibrationPattern);
            statusChannel.enableLights(true);
            statusChannel.setSound(soundUri, audioAttributes);
            statusChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            nm.createNotificationChannel(alertsChannel);
            nm.createNotificationChannel(statusChannel);
        }
    }

    private void showNotification(Context context, int id, String channelId, String title, String body, int priority, boolean ongoing) {
        showNotificationStatic(context, id, channelId, title, body, priority, ongoing);
    }

    public static void showNotificationStatic(Context context, int id, String channelId, String title, String body, int priority, boolean ongoing) {
        try {
            createNotificationChannels(context);

            boolean notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();
            if (!notificationsEnabled) {
                Log.w(TAG, "Cannot show notification: POST_NOTIFICATIONS is disabled in OS settings for " + context.getPackageName());
            }

            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (launchIntent == null) {
                launchIntent = new Intent().setClassName(context.getPackageName(), "com.uncode.app.MainActivity");
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            int pFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pFlags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent contentIntent = PendingIntent.getActivity(context, id, launchIntent, pFlags);

            android.net.Uri soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
            long[] vibrationPattern = new long[]{0, 300, 200, 300};

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_qiezka_notif)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(!ongoing)
                    .setOngoing(ongoing)
                    .setContentIntent(contentIntent)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(ongoing ? NotificationCompat.CATEGORY_STATUS : NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSound(soundUri)
                    .setVibrate(vibrationPattern)
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            if (Build.VERSION.SDK_INT >= 34) {
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null && nm.canUseFullScreenIntent()) {
                    builder.setFullScreenIntent(contentIntent, true);
                }
            } else {
                builder.setFullScreenIntent(contentIntent, true);
            }

            NotificationManagerCompat.from(context).notify(id, builder.build());
            Log.i(TAG, "Notification displayed successfully: " + title + " (id=" + id + ")");
        } catch (Exception e) {
            Log.e(TAG, "Error displaying notification (" + title + "): " + e.getMessage(), e);
        }
    }
}
