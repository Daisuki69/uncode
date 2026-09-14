package com.uncode.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public final class ScheduleManager {

    private static final String TAG = "ScheduleManager";
    public static final String PREFS_NAME = "uncode_lock";

    private ScheduleManager() {}

    public static void syncSchedules(Context context, String schedulesJson, Set<String> whitelist) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (schedulesJson != null) {
            editor.putString("schedules_json", schedulesJson);
        }
        if (whitelist != null) {
            editor.putStringSet("whitelist", whitelist);
        }
        editor.apply();

        rescheduleAll(context);
    }

    public static void rescheduleAll(Context context) {
        cancelAllScheduledAlarms(context);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long timeOffset = prefs.getLong("time_offset", 0L);
        long effectiveNow = System.currentTimeMillis() + timeOffset;
        boolean isLockActive = prefs.getBoolean("lockdown_active", false);
        long currentEnd = prefs.getLong("lock_end_time", 0L);

        // If lockdown was active but effective clock is now at or beyond lockEndTime, cleanly end the session!
        if (isLockActive && currentEnd > 0 && effectiveNow >= currentEnd) {
            Log.i(TAG, "rescheduleAll: effectiveNow (" + effectiveNow + ") >= currentEnd (" + currentEnd + ") — ending expired lockdown");
            prefs.edit()
                    .putBoolean("lockdown_active", false)
                    .remove("lock_end_time")
                    .remove("active_schedule_id")
                    .apply();
            AlarmReceiver.cancelLockEndAlarm(context);
            FloatingOverlayService.stopService(context);
        }

        String json = prefs.getString("schedules_json", null);
        if (json == null || json.trim().isEmpty()) return;

        try {
            JSONArray arr = new JSONArray(json);
            Set<String> activeAlarmCodes = new HashSet<>();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject s = arr.getJSONObject(i);
                if (!s.optBoolean("isActive", true)) continue;

                scheduleAlarmsForSingle(context, s, activeAlarmCodes);
            }

            prefs.edit().putStringSet("scheduled_alarm_codes", activeAlarmCodes).apply();
            Log.i(TAG, "Rescheduled " + activeAlarmCodes.size() + " alarm triggers");
        } catch (Exception e) {
            Log.e(TAG, "Error in rescheduleAll: " + e.getMessage());
        }
    }

    private static void scheduleAlarmsForSingle(Context context, JSONObject s, Set<String> activeAlarmCodes) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            String id = s.optString("id", "default");
            String title = s.optString("title", "Study Session");
            String timeStr = s.optString("activationTime", "");
            int durationMinutes = s.optInt("durationMinutes", 25);

            if (!timeStr.contains(":")) return;
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0].trim());
            int min = Integer.parseInt(parts[1].trim());

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            long timeOffset = prefs.getLong("time_offset", 0L);
            long realNow = System.currentTimeMillis();
            long effectiveNow = realNow + timeOffset;

            Calendar target = Calendar.getInstance();
            target.setTimeInMillis(effectiveNow);
            target.set(Calendar.HOUR_OF_DAY, hour);
            target.set(Calendar.MINUTE, min);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);

            long durationMs = durationMinutes * 60L * 1000L;
            long targetStartSim = target.getTimeInMillis();
            long targetEndSim = targetStartSim + durationMs;

            // Check if currently inside today's active window!
            if (effectiveNow >= targetStartSim && effectiveNow < targetEndSim) {
                long lastCompletedEnd = prefs.getLong("last_completed_window_end_" + id, 0L);
                if (targetEndSim <= lastCompletedEnd) {
                    Log.i(TAG, "Schedule " + id + " window already completed for today. Scheduling for tomorrow.");
                    target.add(Calendar.DAY_OF_YEAR, 1);
                    targetStartSim = target.getTimeInMillis();
                    long realTriggerStartMs = targetStartSim - timeOffset;
                    int baseCode = Math.abs(id.hashCode()) % 100000;
                    int startCode = baseCode * 10 + 0;
                    scheduleExact(context, am, realTriggerStartMs, startCode, AlarmReceiver.ACTION_SCHEDULE_START, id, title, durationMinutes, null);
                    activeAlarmCodes.add(String.valueOf(startCode));
                    return;
                }
                long safeEnd = Math.min(targetEndSim, effectiveNow + durationMs);
                Log.i(TAG, "Schedule " + id + " is active RIGHT NOW! Triggering lockdown immediately (safeEnd=" + safeEnd + ").");
                AlarmReceiver.triggerScheduleStartDirectly(context, s, safeEnd);
                return;
            }

            // If the window has already passed for today, schedule for tomorrow
            if (effectiveNow >= targetEndSim) {
                target.add(Calendar.DAY_OF_YEAR, 1);
                targetStartSim = target.getTimeInMillis();
            }

            // Real wall-clock trigger time for AlarmManager
            long realTriggerStartMs = targetStartSim - timeOffset;
            int baseCode = Math.abs(id.hashCode()) % 100000;

            // 1. Exact Start Alarm
            int startCode = baseCode * 10 + 0;
            scheduleExact(context, am, realTriggerStartMs, startCode, AlarmReceiver.ACTION_SCHEDULE_START, id, title, durationMinutes, null);
            activeAlarmCodes.add(String.valueOf(startCode));
            Log.i(TAG, "Scheduled start alarm for " + id + " at real timestamp " + realTriggerStartMs);

            // 2. Warning Alarms (30 min, 15 min, 5 min, 1 min, 30 sec, 10 sec)
            long[] warningOffsets = new long[]{
                30L * 60L * 1000L,  // 30 min
                15L * 60L * 1000L,  // 15 min
                5L * 60L * 1000L,   // 5 min
                1L * 60L * 1000L,   // 1 min
                30L * 1000L,        // 30 sec
                10L * 1000L         // 10 sec
            };

            String[] warningTexts = new String[]{
                "⏰ Upcoming study session in 30 minutes! Wrap up your tasks.",
                "⏰ Study session starts in 15 minutes! Prepare your materials.",
                "⚠️ Lockdown in 5 minutes! Save your work on other apps.",
                "⚠️ Lockdown in 1 minute! QIEZKA is preparing to lock.",
                "🚨 Lockdown starting in 30 seconds! QIEZKA is engaging.",
                "🚨 Lockdown starting in 10 seconds!"
            };

            for (int w = 0; w < warningOffsets.length; w++) {
                long warnSimTime = targetStartSim - warningOffsets[w];
                if (warnSimTime > effectiveNow) {
                    long realWarnTime = warnSimTime - timeOffset;
                    int warnCode = baseCode * 10 + (w + 1);
                    scheduleExact(context, am, realWarnTime, warnCode, AlarmReceiver.ACTION_SCHEDULE_WARNING, id, title, durationMinutes, warningTexts[w]);
                    activeAlarmCodes.add(String.valueOf(warnCode));
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed scheduling alarms for " + s.optString("id") + ": " + e.getMessage());
        }
    }

    private static void scheduleExact(Context context, AlarmManager am, long triggerAtMs, int requestCode, String action, String scheduleId, String title, int durationMinutes, String warningText) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(action);
        intent.putExtra(AlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId);
        intent.putExtra(AlarmReceiver.EXTRA_SCHEDULE_TITLE, title);
        intent.putExtra(AlarmReceiver.EXTRA_DURATION_MINUTES, (long) durationMinutes);
        intent.putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode);
        if (warningText != null) {
            intent.putExtra(AlarmReceiver.EXTRA_WARNING_TEXT, warningText);
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, flags);
        AlarmReceiver.scheduleExactAlarmCompat(context, am, triggerAtMs, pi);
    }

    public static void cancelAllScheduledAlarms(Context context) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Set<String> codes = prefs.getStringSet("scheduled_alarm_codes", null);
            if (codes != null) {
                String[] actions = new String[]{
                    AlarmReceiver.ACTION_SCHEDULE_START,
                    AlarmReceiver.ACTION_SCHEDULE_WARNING,
                    null
                };

                for (String codeStr : codes) {
                    try {
                        int code = Integer.parseInt(codeStr);
                        for (String action : actions) {
                            Intent intent = new Intent(context, AlarmReceiver.class);
                            if (action != null) {
                                intent.setAction(action);
                            }
                            int pFlags = PendingIntent.FLAG_UPDATE_CURRENT;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                pFlags |= PendingIntent.FLAG_IMMUTABLE;
                            }
                            PendingIntent pi = PendingIntent.getBroadcast(context, code, intent, pFlags);
                            if (pi != null) {
                                am.cancel(pi);
                                pi.cancel();
                            }
                        }
                    } catch (Exception ignore) {}
                }
            }
            prefs.edit().remove("scheduled_alarm_codes").apply();
        } catch (Exception e) {
            Log.e(TAG, "cancelAllScheduledAlarms error: " + e.getMessage());
        }
    }
}
