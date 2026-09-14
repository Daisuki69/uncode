package com.uncode.app;

import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.io.ByteArrayOutputStream;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class PunishmentManager {

    private static final String TAG = "PunishmentManager";
    public static final String PREFS_NAME = "uncode_lock";

    public static final String KEY_PUNISHMENT_ACTIVE = "punishment_active";
    public static final String KEY_PUNISHED_PACKAGES = "punished_packages";
    public static final String KEY_PUNISHMENT_SCHEDULE_ID = "punishment_schedule_id";
    public static final String KEY_PUNISHMENT_SCHEDULE_TITLE = "punishment_schedule_title";
    public static final String KEY_PUNISHMENT_TIMESTAMP = "punishment_timestamp";

    public static final int NOTIF_ID_PUNISHMENT = 1004;
    public static final String SETTINGS_PACKAGE = "com.android.settings";

    public static boolean isPunishmentActive(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_PUNISHMENT_ACTIVE, false);
    }

    public static Set<String> getPunishedPackages(Context context) {
        if (context == null) return Collections.emptySet();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getStringSet(KEY_PUNISHED_PACKAGES, Collections.emptySet());
    }

    public static boolean isPackagePunished(Context context, String packageName) {
        if (packageName == null || context == null) return false;
        if (!isPunishmentActive(context)) return false;
        Set<String> punished = getPunishedPackages(context);
        return punished.contains(packageName);
    }

    public static boolean hasUsageStatsPermission(Context context) {
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) return false;
            int mode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
            } else {
                mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
            }
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private static Set<String> getExemptPackages(Context context) {
        Set<String> exempt = new HashSet<>();
        exempt.add(context.getPackageName()); // UNCODE itself
        exempt.add("android");
        exempt.add("com.android.systemui");

        // Keyboards
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                for (InputMethodInfo imi : imm.getInputMethodList()) {
                    if (imi != null && imi.getPackageName() != null) {
                        exempt.add(imi.getPackageName());
                    }
                }
            }
        } catch (Exception ignore) {}

        // Launchers (Home screens)
        try {
            PackageManager pm = context.getPackageManager();
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> homes = pm.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo ri : homes) {
                if (ri.activityInfo != null && ri.activityInfo.packageName != null) {
                    exempt.add(ri.activityInfo.packageName);
                }
            }
        } catch (Exception ignore) {}

        // Camera handlers
        try {
            PackageManager pm = context.getPackageManager();
            Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            List<ResolveInfo> cams = pm.queryIntentActivities(camIntent, 0);
            for (ResolveInfo ri : cams) {
                if (ri.activityInfo != null && ri.activityInfo.packageName != null) {
                    exempt.add(ri.activityInfo.packageName);
                }
            }
        } catch (Exception ignore) {}

        // Telephony / Dialer
        try {
            PackageManager pm = context.getPackageManager();
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            List<ResolveInfo> dialers = pm.queryIntentActivities(dialIntent, 0);
            for (ResolveInfo ri : dialers) {
                if (ri.activityInfo != null && ri.activityInfo.packageName != null) {
                    exempt.add(ri.activityInfo.packageName);
                }
            }
        } catch (Exception ignore) {}

        return exempt;
    }

    public static List<String> selectTopDistractingPackages(Context context, int targetCount) {
        Set<String> exempt = getExemptPackages(context);
        Set<String> selected = new LinkedHashSet<>();
        PackageManager pm = context.getPackageManager();

        // ── Strategy A: UsageStatsManager (Real weekly screen time) ──
        if (hasUsageStatsPermission(context)) {
            try {
                UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                if (usm != null) {
                    long now = System.currentTimeMillis();
                    long start = now - (7L * 24 * 60 * 60 * 1000L); // Past 7 days
                    List<UsageStats> statsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, start, now);

                    if (statsList != null && !statsList.isEmpty()) {
                        Map<String, Long> aggregated = new HashMap<>();
                        for (UsageStats us : statsList) {
                            if (us == null || us.getPackageName() == null) continue;
                            String pkg = us.getPackageName();
                            if (exempt.contains(pkg)) continue;
                            if (SETTINGS_PACKAGE.equals(pkg)) continue; // Handled separately

                            long time = us.getTotalTimeInForeground();
                            if (time > 0) {
                                aggregated.put(pkg, aggregated.getOrDefault(pkg, 0L) + time);
                            }
                        }

                        List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(aggregated.entrySet());
                        sortedEntries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

                        for (Map.Entry<String, Long> entry : sortedEntries) {
                            String pkg = entry.getKey();
                            try {
                                pm.getApplicationInfo(pkg, 0);
                                selected.add(pkg);
                                if (selected.size() >= targetCount) break;
                            } catch (Exception ignore) {}
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error querying UsageStats: " + e.getMessage());
            }
        }

        // ── Strategy B: Fallback using Installed Launchable Applications ──
        if (selected.size() < targetCount) {
            try {
                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> launchables = pm.queryIntentActivities(mainIntent, 0);

                List<String> userApps = new ArrayList<>();
                List<String> blacklistApps = new ArrayList<>();

                for (ResolveInfo ri : launchables) {
                    if (ri.activityInfo == null || ri.activityInfo.packageName == null) continue;
                    String pkg = ri.activityInfo.packageName;
                    if (exempt.contains(pkg) || selected.contains(pkg) || SETTINGS_PACKAGE.equals(pkg)) continue;

                    if (BlacklistConstants.isBlacklisted(pkg)) {
                        blacklistApps.add(pkg);
                    } else {
                        try {
                            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                                userApps.add(pkg);
                            }
                        } catch (Exception ignore) {}
                    }
                }

                // 1. Add known popular entertainment / social blacklist apps first
                for (String bPkg : blacklistApps) {
                    selected.add(bPkg);
                    if (selected.size() >= targetCount) break;
                }

                // 2. Add non-system user-installed apps (shuffled to randomize if equal)
                if (selected.size() < targetCount) {
                    Collections.shuffle(userApps);
                    for (String uPkg : userApps) {
                        selected.add(uPkg);
                        if (selected.size() >= targetCount) break;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error selecting launchable apps: " + e.getMessage());
            }
        }

        return new ArrayList<>(selected);
    }

    public static Set<String> activatePunishment(Context context, String scheduleId, String scheduleTitle) {
        if (context == null) return Collections.emptySet();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Select 10 apps
        List<String> chosen10 = selectTopDistractingPackages(context, 10);
        Set<String> punishedSet = new HashSet<>(chosen10);

        // ALWAYS include Android Settings
        punishedSet.add(SETTINGS_PACKAGE);

        String title = (scheduleTitle != null && !scheduleTitle.trim().isEmpty())
                ? scheduleTitle
                : "Untitled Homework Session";

        prefs.edit()
                .putBoolean(KEY_PUNISHMENT_ACTIVE, true)
                .putStringSet(KEY_PUNISHED_PACKAGES, punishedSet)
                .putString(KEY_PUNISHMENT_SCHEDULE_ID, scheduleId != null ? scheduleId : "")
                .putString(KEY_PUNISHMENT_SCHEDULE_TITLE, title)
                .putLong(KEY_PUNISHMENT_TIMESTAMP, System.currentTimeMillis())
                .apply();

        Log.w(TAG, "🚨 Study Detention ACTIVATED! " + chosen10.size() + " apps + Settings locked for schedule: " + title);

        // Fire high-priority alert notification
        try {
            AlarmReceiver.createNotificationChannels(context);
            AlarmReceiver.showNotificationStatic(
                    context,
                    NOTIF_ID_PUNISHMENT,
                    AlarmReceiver.CHANNEL_ID_ALERTS,
                    "⚠️ Study Detention Active",
                    "10 of your apps and Settings are locked until you reschedule and pass \"" + title + "\" in UNCODE.",
                    Notification.PRIORITY_HIGH,
                    true
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to show detention notification: " + e.getMessage());
        }

        return punishedSet;
    }

    public static void clearPunishment(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String scheduleTitle = prefs.getString(KEY_PUNISHMENT_SCHEDULE_TITLE, "Study Session");

        prefs.edit()
                .putBoolean(KEY_PUNISHMENT_ACTIVE, false)
                .remove(KEY_PUNISHED_PACKAGES)
                .remove(KEY_PUNISHMENT_SCHEDULE_ID)
                .remove(KEY_PUNISHMENT_SCHEDULE_TITLE)
                .remove(KEY_PUNISHMENT_TIMESTAMP)
                .apply();

        Log.i(TAG, "🎉 Study Detention CLEARED natively for: " + scheduleTitle);

        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIF_ID_PUNISHMENT);
            }

            AlarmReceiver.createNotificationChannels(context);
            AlarmReceiver.showNotificationStatic(
                    context,
                    AlarmReceiver.NOTIF_ID_COMPLETED,
                    AlarmReceiver.CHANNEL_ID_ALERTS,
                    "🎉 Study Detention Cleared!",
                    "Awesome work passing your rescheduled homework! All 10 apps and Settings have been restored.",
                    Notification.PRIORITY_HIGH,
                    false
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to show detention cleared notification: " + e.getMessage());
        }
    }

    public static JSONObject getPunishmentDetails(Context context) {
        JSONObject obj = new JSONObject();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean isActive = prefs.getBoolean(KEY_PUNISHMENT_ACTIVE, false);
            String scheduleId = prefs.getString(KEY_PUNISHMENT_SCHEDULE_ID, "");
            String scheduleTitle = prefs.getString(KEY_PUNISHMENT_SCHEDULE_TITLE, "");
            long timestamp = prefs.getLong(KEY_PUNISHMENT_TIMESTAMP, 0L);
            Set<String> punishedSet = prefs.getStringSet(KEY_PUNISHED_PACKAGES, Collections.emptySet());

            obj.put("isActive", isActive);
            obj.put("scheduleId", scheduleId);
            obj.put("scheduleTitle", scheduleTitle);
            obj.put("timestamp", timestamp);

            JSONArray packagesArray = new JSONArray();
            JSONArray appsDetailsArray = new JSONArray();
            PackageManager pm = context.getPackageManager();

            for (String pkg : punishedSet) {
                packagesArray.put(pkg);

                JSONObject appObj = new JSONObject();
                appObj.put("id", pkg);

                if (SETTINGS_PACKAGE.equals(pkg)) {
                    appObj.put("name", "Android Settings");
                    appObj.put("iconName", "Settings");
                } else {
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                        String appName = pm.getApplicationLabel(ai).toString();
                        appObj.put("name", appName);

                        Drawable icon = pm.getApplicationIcon(ai);
                        String base64 = drawableToBase64(icon);
                        if (base64 != null) {
                            appObj.put("iconBase64", base64);
                        }
                    } catch (Exception e) {
                        appObj.put("name", pkg);
                    }
                }
                appsDetailsArray.put(appObj);
            }

            obj.put("punishedPackages", packagesArray);
            obj.put("punishedAppDetails", appsDetailsArray);

        } catch (Exception e) {
            Log.e(TAG, "Error building punishment details: " + e.getMessage());
        }
        return obj;
    }

    private static String drawableToBase64(Drawable drawable) {
        if (drawable == null) return null;
        try {
            int width = Math.min(drawable.getIntrinsicWidth(), 96);
            int height = Math.min(drawable.getIntrinsicHeight(), 96);
            if (width <= 0) width = 96;
            if (height <= 0) height = 96;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, outputStream);
            byte[] byteArray = outputStream.toByteArray();
            return "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }
}
