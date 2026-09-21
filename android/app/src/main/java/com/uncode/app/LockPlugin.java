package com.uncode.app;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONException;

import android.content.Intent;
import android.net.Uri;
import android.net.VpnService;
import android.app.Activity;
import android.app.Notification;
import android.os.Handler;
import android.os.Looper;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.annotation.ActivityCallback;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.InputStream;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.pm.ResolveInfo;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodInfo;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.getcapacitor.PermissionState;

@CapacitorPlugin(
    name = "LockPlugin",
    permissions = {
        @Permission(
            alias = "notifications",
            strings = { android.Manifest.permission.POST_NOTIFICATIONS }
        )
    }
)
public class LockPlugin extends Plugin {

    private static final String TAG = "LockPlugin";
    private static final String PREFS_NAME = "uncode_lock";
    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private SharedPreferences prefs;

    @Override
    public void load() {
        dpm = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(getActivity(), AdminReceiver.class);
        prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private Long getLongFromCall(PluginCall call, String key) {
        if (call == null || call.getData() == null) return null;
        Object val = call.getData().opt(key);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val instanceof String) {
            try {
                return Long.parseLong((String) val);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    @PluginMethod
    public void startLockdown(PluginCall call) {
        try {
            JSArray allowedAppIds = call.getArray("allowedAppIds");
            long durationMinutes = Math.min(90, Math.max(1, call.getInt("durationMinutes", 25)));
            Long customEndTime = getLongFromCall(call, "lockEndTime");
            String scheduleId = call.getString("scheduleId", "");

            long timeOffset = prefs.getLong("time_offset", 0L);
            long effectiveNow = System.currentTimeMillis() + timeOffset;

            boolean currentlyActive = prefs.getBoolean("lockdown_active", false);
            long existingEndTime = prefs.getLong("lock_end_time", 0L);

            long lockEndTime;
            if (customEndTime != null && customEndTime > 0) {
                lockEndTime = customEndTime;
            } else if (currentlyActive && existingEndTime > effectiveNow) {
                // If lockdown is already active and still has time remaining, preserve ongoing timer!
                lockEndTime = existingEndTime;
                Log.i(TAG, "startLockdown: Preserving active ongoing lockEndTime=" + lockEndTime);
            } else {
                lockEndTime = effectiveNow + (durationMinutes * 60L * 1000L);
            }

            long maxAllowedEnd = effectiveNow + (durationMinutes * 60L * 1000L);
            if (lockEndTime > maxAllowedEnd) {
                Log.w(TAG, "startLockdown: Clamping lockEndTime (" + lockEndTime + ") to maxAllowedEnd (" + maxAllowedEnd + ")");
                lockEndTime = maxAllowedEnd;
            }

            Set<String> whitelist = new HashSet<>();
            whitelist.add(getActivity().getPackageName()); // Always allow QIEZKA itself

            // Always silently whitelist all system and third-party keyboards so typing never bricks lockdown
            try {
                String defaultIme = Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
                if (defaultIme != null && defaultIme.contains("/")) {
                    whitelist.add(defaultIme.split("/")[0]);
                }
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    List<InputMethodInfo> imis = imm.getInputMethodList();
                    if (imis != null) {
                        for (InputMethodInfo imi : imis) {
                            if (imi != null && imi.getPackageName() != null) {
                                whitelist.add(imi.getPackageName());
                            }
                        }
                    }
                    List<InputMethodInfo> enabledImis = imm.getEnabledInputMethodList();
                    if (enabledImis != null) {
                        for (InputMethodInfo imi : enabledImis) {
                            if (imi != null && imi.getPackageName() != null) {
                                whitelist.add(imi.getPackageName());
                            }
                        }
                    }
                }
            } catch (Exception ignore) {}

            if (allowedAppIds != null) {
                for (int i = 0; i < allowedAppIds.length(); i++) {
                    String appId = allowedAppIds.getString(i);
                    if (appId != null && !AppClassifier.isSettingsOrDeviceManager(appId, null)) {
                        whitelist.add(appId);
                    }
                }
            }

            // Always exempt document pickers and media providers
            whitelist.addAll(LockAccessibilityService.MEDIA_AND_FILE_EXEMPT);
            whitelist.addAll(LockAccessibilityService.ALWAYS_EXEMPT);

            // Save whitelist and timestamp for AccessibilityService
            prefs.edit()
                    .putStringSet("whitelist", whitelist)
                    .putBoolean("lockdown_active", true)
                    .putLong("lock_end_time", lockEndTime)
                    .putString("active_schedule_id", scheduleId)
                    .apply();

            AppClassifier.clearCache();

            // Schedule exact lock end auto-release alarm
            AlarmReceiver.scheduleLockEndAlarm(getActivity(), lockEndTime, scheduleId);

            // Start Floating Assistive Timer Ball Overlay
            FloatingOverlayService.startService(getActivity(), lockEndTime, scheduleId);

            // Start Local DNS Sinkhole if web protection mode is dns_vpn or dual_hybrid
            String webMode = prefs.getString("web_protection_mode", "accessibility");
            if ("dns_vpn".equalsIgnoreCase(webMode) || "dual_hybrid".equalsIgnoreCase(webMode)) {
                LocalDnsVpnService.startVpn(getActivity());
            }



            JSObject ret = new JSObject();
            ret.put("lockEndTime", lockEndTime);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "startLockdown failed", e);
            call.reject("startLockdown failed: " + e.getMessage());
        }
    }

    public static void clearLockdownState(Context context) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String activeScheduleId = prefs.getString("active_schedule_id", "");
            long timeOffset = prefs.getLong("time_offset", 0L);
            long effectiveNow = System.currentTimeMillis() + timeOffset;
            long currentLockEnd = prefs.getLong("lock_end_time", effectiveNow);

            SharedPreferences.Editor editor = prefs.edit()
                    .putBoolean("lockdown_active", false)
                    .putBoolean("consequence_active", false)
                    .remove("consequence_schedule_id")
                    .remove("lock_end_time")
                    .remove("active_schedule_id")
                    .putStringSet("whitelist", new HashSet<>());

            if (activeScheduleId != null && !activeScheduleId.trim().isEmpty()) {
                editor.putLong("last_completed_window_end_" + activeScheduleId, currentLockEnd);
                Log.i(TAG, "clearLockdownState: recorded completed window for " + activeScheduleId + " until " + currentLockEnd);
            }
            editor.apply();
            AppClassifier.clearCache();

            // 1. Cancel Alarms
            AlarmReceiver.cancelLockEndAlarm(context);

            // 2. Stop Floating Overlay Service
            FloatingOverlayService.stopService(context);

            // 3. Stop Local DNS Sinkhole VPN
            LocalDnsVpnService.stopVpn(context);

            // 4. Cancel Notifications
            android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(AlarmReceiver.NOTIF_ID_STATUS);
                nm.cancel(AlarmReceiver.NOTIF_ID_COMPLETED);
            }



            Log.i(TAG, "clearLockdownState: complete and authoritative unlock executed successfully");
        } catch (Exception e) {
            Log.e(TAG, "clearLockdownState error", e);
        }
    }

    public static void startConsequenceState(Context context, String scheduleId, Set<String> whitelist) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit()
                    .putBoolean("consequence_active", true)
                    .putBoolean("lockdown_active", true)
                    .remove("lock_end_time");

            if (scheduleId != null && !scheduleId.trim().isEmpty()) {
                editor.putString("consequence_schedule_id", scheduleId);
            }
            if (whitelist != null && !whitelist.isEmpty()) {
                whitelist.addAll(LockAccessibilityService.ALWAYS_EXEMPT);
                editor.putStringSet("whitelist", whitelist);
            }
            editor.apply();

            // 1. Clear classification cache so whitelist takes immediate effect
            AppClassifier.clearCache();



            // 3. Start Local DNS Sinkhole VPN if configured
            String webMode = prefs.getString("web_protection_mode", "accessibility");
            if ("dns_vpn".equalsIgnoreCase(webMode) || "dual_hybrid".equalsIgnoreCase(webMode)) {
                LocalDnsVpnService.startVpn(context);
            }

            // 4. Start Floating Overlay Service with Consequence Mode indicator
            FloatingOverlayService.startService(context, 0L, "Consequence Mode");

            // 5. Post Status Notification
            long timeOffset = prefs.getLong("time_offset", 0L);
            long effectiveNow = System.currentTimeMillis() + timeOffset;
            boolean inOperatingHours = LockAccessibilityService.isInOperatingHours(context, effectiveNow);
            boolean isHardcore = "hardcore".equalsIgnoreCase(prefs.getString("operating_mode", "safemode"));
            String notifMsg;
            if (isHardcore) {
                notifMsg = "Distracting apps remain restricted 24/7 (Hardcore Mode) until homework is rescheduled and passed.";
            } else {
                notifMsg = inOperatingHours
                        ? "Distracting apps are restricted until homework is rescheduled and passed."
                        : "Consequence active: Enforcement paused during daytime (resumes at 7:00 PM).";
            }

            AlarmReceiver.createNotificationChannels(context);
            AlarmReceiver.showNotificationStatic(
                    context,
                    AlarmReceiver.NOTIF_ID_STATUS,
                    AlarmReceiver.CHANNEL_ID_STATUS,
                    "⚠️ QIEZKA Consequence Mode",
                    notifMsg,
                    Notification.PRIORITY_HIGH,
                    true
            );

            // 6. Immediately kick user out if currently inside a blocked app (if in operating hours)
            if (inOperatingHours) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        if (LockAccessibilityService.getInstance() != null) {
                            String activePkg = LockAccessibilityService.getInstance().detectCurrentForegroundPackage();
                            if (activePkg != null && LockAccessibilityService.getInstance().isPackageBlocked(activePkg)) {
                                LockAccessibilityService.getInstance().enforceBlock(activePkg);
                            }
                        }
                    } catch (Exception ignored) {}
                }, 300L);
            }

            Log.i(TAG, "startConsequenceState: complete consequence enforcement engaged (inOperatingHours=" + inOperatingHours + ")");
        } catch (Exception e) {
            Log.e(TAG, "startConsequenceState error", e);
        }
    }

    @PluginMethod
    public void endLockdown(PluginCall call) {
        try {
            clearLockdownState(getActivity());
            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "endLockdown failed", e);
            call.reject("endLockdown failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void setConsequenceActive(PluginCall call) {
        try {
            boolean active = call.getBoolean("active", false);
            String scheduleId = call.getString("scheduleId", "");

            if (active) {
                JSArray rawWhitelist = call.getArray("whitelist");
                Set<String> whitelist = new HashSet<>();
                if (rawWhitelist != null) {
                    for (int i = 0; i < rawWhitelist.length(); i++) {
                        whitelist.add(rawWhitelist.getString(i));
                    }
                }
                startConsequenceState(getActivity(), scheduleId, whitelist);
            } else {
                clearLockdownState(getActivity());
            }

            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "setConsequenceActive failed", e);
            call.reject("setConsequenceActive failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void setOperatingMode(PluginCall call) {
        try {
            String mode = call.getString("mode", "safemode");
            prefs.edit().putString("operating_mode", mode).apply();
            Log.i(TAG, "Operating mode set to: " + mode);
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "setOperatingMode failed", e);
            call.reject("setOperatingMode failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void setWebProtectionMode(PluginCall call) {
        try {
            String mode = call.getString("mode", "accessibility");
            if ("off".equalsIgnoreCase(mode)) {
                mode = "accessibility"; // Milestone 18: No unrestricted mode; baseline protection mandatory
            }
            prefs.edit().putString("web_protection_mode", mode).apply();
            Log.i(TAG, "Web protection mode set to: " + mode);

            boolean isLockActive = prefs.getBoolean("lockdown_active", false) || prefs.getBoolean("consequence_active", false);
            if (isLockActive) {
                if ("dns_vpn".equalsIgnoreCase(mode) || "dual_hybrid".equalsIgnoreCase(mode)) {
                    LocalDnsVpnService.startVpn(getActivity());
                } else {
                    LocalDnsVpnService.stopVpn(getActivity());
                }
            }

            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "setWebProtectionMode failed", e);
            call.reject("setWebProtectionMode failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void setAllowYoutube(PluginCall call) {
        try {
            boolean allow = Boolean.TRUE.equals(call.getBoolean("allow", false));
            prefs.edit().putBoolean("allow_youtube", allow).apply();
            Log.i(TAG, "Allow YouTube set to: " + allow);
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "setAllowYoutube failed", e);
            call.reject("setAllowYoutube failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void setBlockWebGames(PluginCall call) {
        try {
            // Milestone 18: Web games blocking is permanently active and cannot be turned off
            prefs.edit().putBoolean("block_web_games", true).apply();
            Log.i(TAG, "Block web games locked to: true");
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "setBlockWebGames failed", e);
            call.reject("setBlockWebGames failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void setDnsFilterProfile(PluginCall call) {
        try {
            // Upstream DNS is handled on-device with WebClassifier
            prefs.edit().putString("dns_filter_profile", "webclassifier").apply();
            Log.i(TAG, "DNS filter profile set to WebClassifier");
            LocalDnsVpnService.updateNotification(getActivity());
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "setDnsFilterProfile failed", e);
            call.reject("setDnsFilterProfile failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void setEnforceSafeSearch(PluginCall call) {
        try {
            boolean enforce = Boolean.TRUE.equals(call.getBoolean("enforce", true));
            prefs.edit().putBoolean("enforce_safesearch", enforce).apply();
            Log.i(TAG, "Enforce SafeSearch set to: " + enforce);
            LocalDnsVpnService.updateNotification(getActivity());
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "setEnforceSafeSearch failed", e);
            call.reject("setEnforceSafeSearch failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void setYoutubePolicy(PluginCall call) {
        try {
            String policy = call.getString("policy", "academic");
            boolean allow = "academic".equalsIgnoreCase(policy) || "unrestricted".equalsIgnoreCase(policy);
            prefs.edit()
                .putString("youtube_policy", policy)
                .putBoolean("allow_youtube", allow)
                .apply();
            Log.i(TAG, "YouTube policy set to: " + policy + " (allow_youtube=" + allow + ")");
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "setYoutubePolicy failed", e);
            call.reject("setYoutubePolicy failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void requestVpnPermission(PluginCall call) {
        try {
            Intent vpnIntent = VpnService.prepare(getActivity());
            if (vpnIntent == null) {
                JSObject ret = new JSObject();
                ret.put("granted", true);
                call.resolve(ret);
            } else {
                startActivityForResult(call, vpnIntent, "vpnPermissionCallback");
            }
        } catch (Exception e) {
            Log.e(TAG, "requestVpnPermission failed", e);
            JSObject ret = new JSObject();
            ret.put("granted", false);
            call.resolve(ret);
        }
    }

    @ActivityCallback
    private void vpnPermissionCallback(PluginCall call, ActivityResult result) {
        JSObject ret = new JSObject();
        boolean granted = (result != null && result.getResultCode() == Activity.RESULT_OK);
        ret.put("granted", granted);
        call.resolve(ret);
    }

    @PluginMethod
    public void getLockStatus(PluginCall call) {
        try {
            boolean isActive = prefs.getBoolean("lockdown_active", false);
            boolean isConsequence = prefs.getBoolean("consequence_active", false);
            long lockEndTime = prefs.getLong("lock_end_time", 0L);
            String scheduleId = prefs.getString("active_schedule_id", "");

            long timeOffset = prefs.getLong("time_offset", 0L);
            long effectiveNow = System.currentTimeMillis() + timeOffset;

            // Native timestamp auto-expire only when NOT in consequence mode
            if (isActive && !isConsequence && lockEndTime > 0 && effectiveNow >= lockEndTime) {
                isActive = false;
                clearLockdownState(getActivity());
            }

            JSObject ret = new JSObject();
            ret.put("isLockActive", isActive);
            ret.put("isConsequenceActive", isConsequence);
            ret.put("lockEndTime", lockEndTime);
            ret.put("activeScheduleId", scheduleId);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "getLockStatus failed", e);
            call.reject("getLockStatus failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void syncTimeOffset(PluginCall call) {
        try {
            Long timeOffset = getLongFromCall(call, "timeOffset");
            if (timeOffset == null) timeOffset = 0L;
            prefs.edit().putLong("time_offset", timeOffset).apply();
            Log.i(TAG, "Synchronized timeOffset to native: " + timeOffset + "ms");

            ScheduleManager.rescheduleAll(getActivity());

            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "syncTimeOffset failed", e);
            call.reject("syncTimeOffset failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void syncSchedules(PluginCall call) {
        try {
            JSArray schedulesArr = call.getArray("schedules");
            JSArray allowedAppIds = call.getArray("allowedAppIds");

            String schedulesJson = schedulesArr != null ? schedulesArr.toString() : "[]";

            Set<String> whitelist = new HashSet<>();
            whitelist.add(getActivity().getPackageName());
            if (allowedAppIds != null) {
                for (int i = 0; i < allowedAppIds.length(); i++) {
                    String appId = allowedAppIds.getString(i);
                    if (appId != null && !AppClassifier.isSettingsOrDeviceManager(appId, null)) {
                        whitelist.add(appId);
                    }
                }
            }

            ScheduleManager.syncSchedules(getActivity(), schedulesJson, whitelist);
            AppClassifier.clearCache();

            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "syncSchedules failed", e);
            call.reject("syncSchedules failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void getInstalledApps(PluginCall call) {
        Runnable task = () -> {
            try {
                PackageManager pm = getActivity().getPackageManager();
                String myPkg = getActivity().getPackageName();

                // 1. Single-pass pre-queries (run once, not inside any package loop)
                Set<String> browserPackages = new HashSet<>();
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
                    browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
                    List<ResolveInfo> bList = pm.queryIntentActivities(browserIntent, 0);
                    for (ResolveInfo r : bList) {
                        if (r.activityInfo != null && r.activityInfo.packageName != null) {
                            browserPackages.add(r.activityInfo.packageName);
                        }
                    }
                } catch (Exception ignore) {}

                Set<String> musicPackages = new HashSet<>();
                try {
                    Intent musicIntent = new Intent(Intent.ACTION_MAIN);
                    musicIntent.addCategory(Intent.CATEGORY_APP_MUSIC);
                    List<ResolveInfo> mList = pm.queryIntentActivities(musicIntent, 0);
                    for (ResolveInfo r : mList) {
                        if (r.activityInfo != null && r.activityInfo.packageName != null) {
                            musicPackages.add(r.activityInfo.packageName);
                        }
                    }
                    Intent audioIntent = new Intent(Intent.ACTION_VIEW);
                    audioIntent.setDataAndType(Uri.parse("file://test.mp3"), "audio/*");
                    List<ResolveInfo> aList = pm.queryIntentActivities(audioIntent, 0);
                    for (ResolveInfo r : aList) {
                        if (r.activityInfo != null && r.activityInfo.packageName != null) {
                            musicPackages.add(r.activityInfo.packageName);
                        }
                    }
                } catch (Exception ignore) {}

                Set<String> cameraPackages = new HashSet<>();
                try {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    List<ResolveInfo> cList = pm.queryIntentActivities(cameraIntent, 0);
                    for (ResolveInfo r : cList) {
                        if (r.activityInfo != null && r.activityInfo.packageName != null) {
                            cameraPackages.add(r.activityInfo.packageName);
                        }
                    }
                } catch (Exception ignore) {}

                Set<String> keyboardPackages = new HashSet<>();
                try {
                    String defaultIme = Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
                    if (defaultIme != null && defaultIme.contains("/")) {
                        keyboardPackages.add(defaultIme.split("/")[0]);
                    }
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        List<InputMethodInfo> imis = imm.getInputMethodList();
                        if (imis != null) {
                            for (InputMethodInfo imi : imis) {
                                if (imi != null && imi.getPackageName() != null) {
                                    keyboardPackages.add(imi.getPackageName());
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {}

                // 2. Query user-launchable apps directly to avoid iterating hundreds of hidden system daemons
                Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
                launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> launcherList = pm.queryIntentActivities(launcherIntent, 0);

                Set<String> candidatePackages = new LinkedHashSet<>();
                for (ResolveInfo r : launcherList) {
                    if (r.activityInfo != null && r.activityInfo.packageName != null) {
                        candidatePackages.add(r.activityInfo.packageName);
                    }
                }
                candidatePackages.addAll(browserPackages);
                candidatePackages.addAll(musicPackages);
                candidatePackages.addAll(cameraPackages);

                JSArray apps = new JSArray();
                Set<String> addedPackages = new HashSet<>();

                for (String pkg : candidatePackages) {
                    if (pkg == null || pkg.equals(myPkg) || addedPackages.contains(pkg)) {
                        continue;
                    }
                    if (AppClassifier.isSettingsOrDeviceManager(pkg, null) || AppClassifier.isStage1Bloat(pkg, null) || AppClassifier.isForbiddenDistraction(getActivity(), pkg)) {
                        continue; // Strictly omit anti-tamper, bloatware, games, and social media from candidate selection
                    }
                    if (keyboardPackages.contains(pkg) || isKeyboardAppKeywords(pkg)) {
                        continue; // Keyboards are silently exempted in lockdown, hidden from whitelist UI
                    }

                    try {
                        ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                        String appLabel = pm.getApplicationLabel(appInfo).toString();

                        if (isHiddenInfrastructureApp(pkg, appLabel)) {
                            continue; // Hide camera extension proxies, aperture lens launchers, etc.
                        }

                        boolean isBrowser = browserPackages.contains(pkg) || isBrowserAppKeywords(pkg);
                        boolean isMusic = musicPackages.contains(pkg) || isMusicAppKeywords(pkg);
                        boolean isCamera = cameraPackages.contains(pkg) || isCameraAppKeywords(pkg);
                        boolean isAuthenticator = isAuthenticatorAppKeywords(pkg, appLabel);
                        boolean isNotes = isNotesAppKeywords(pkg, appLabel);
                        boolean isStudentApp = isStudentAppKeywords(pkg, appLabel);
                        boolean isAi = isAiAppKeywords(pkg, appLabel);
                        boolean isMessaging = AppClassifier.isMessagingApp(pkg, appLabel);
                        boolean isHardcoded = isBrowser || isMusic || isCamera || isAuthenticator || isNotes || isStudentApp || isAi;

                        addedPackages.add(pkg);

                        JSObject app = new JSObject();
                        app.put("id", pkg);
                        app.put("name", appLabel);

                        boolean isSimOrCarrier = LockAccessibilityService.isSimOrCarrierService(pkg, appLabel);

                        String iconName = "LayoutGrid";
                        if (isBrowser) iconName = "Globe";
                        else if (isMusic) iconName = "Music";
                        else if (isCamera) iconName = "Camera";
                        else if (isAuthenticator) iconName = "ShieldCheck";
                        else if (isAi) iconName = "Sparkles";
                        else if (isNotes) iconName = "FileText";
                        else if (isStudentApp) iconName = "BookOpen";
                        else if (isMessaging || isSimOrCarrier) iconName = "MessageSquare";

                        boolean isAutoAllowed = isHardcoded || isMessaging || isSimOrCarrier || !AppClassifier.isPackageBlocked(getActivity(), pkg, null);
                        if (!isHardcoded && !isSimOrCarrier && isAutoAllowed && "LayoutGrid".equals(iconName)) {
                            iconName = "BookOpen";
                        }

                        app.put("iconName", iconName);
                        app.put("isHardcoded", isHardcoded);
                        app.put("isAutoAllowed", isAutoAllowed);
                        app.put("isMessaging", isMessaging);
                        app.put("isBrowser", isBrowser);
                        app.put("isMusic", isMusic);
                        app.put("isCamera", isCamera);
                        app.put("isAuthenticator", isAuthenticator);
                        app.put("isNotes", isNotes);
                        app.put("isStudentApp", isStudentApp);
                        app.put("isAi", isAi);

                        try {
                            Drawable icon = pm.getApplicationIcon(appInfo);
                            String base64Icon = getBase64Icon(icon);
                            if (base64Icon != null) {
                                app.put("iconBase64", base64Icon);
                            }
                        } catch (Exception ignore) {}

                        apps.put(app);
                    } catch (PackageManager.NameNotFoundException ignore) {}
                }

                JSObject result = new JSObject();
                result.put("apps", apps);
                call.resolve(result);
            } catch (Exception e) {
                Log.e(TAG, "getInstalledApps failed", e);
                call.reject("getInstalledApps failed: " + e.getMessage());
            }
        };

        if (getBridge() != null) {
            getBridge().execute(task);
        } else {
            new Thread(task).start();
        }
    }

    private boolean isAuthenticatorAppKeywords(String packageName, String label) {
        if (packageName != null) {
            String lower = packageName.toLowerCase();
            if (lower.contains("authenticator") || lower.contains("twofas") || lower.contains("duomobile") || lower.contains("yubioath")) {
                return true;
            }
        }
        if (label != null) {
            String lowerLabel = label.toLowerCase();
            if (lowerLabel.contains("authenticator") || lowerLabel.contains("2fa") || lowerLabel.contains("otp")) {
                return true;
            }
        }
        return false;
    }

    private boolean isNotesAppKeywords(String packageName, String label) {
        if (packageName != null) {
            String lower = packageName.toLowerCase();
            if (lower.contains("keep") || lower.contains("onenote") || lower.contains("obsidian") ||
                lower.contains("notion") || lower.contains("notepad") || lower.contains(".notes") ||
                lower.contains("memo") || lower.contains("simplenote") || lower.contains("colornote")) {
                return true;
            }
        }
        if (label != null) {
            String lowerLabel = label.toLowerCase();
            if (lowerLabel.contains("notes") || lowerLabel.contains("notepad") || lowerLabel.contains("memo") ||
                lowerLabel.contains("keep") || lowerLabel.contains("onenote") || lowerLabel.contains("notion") ||
                lowerLabel.contains("obsidian") || lowerLabel.contains("journal")) {
                return true;
            }
        }
        return false;
    }

    private boolean isStudentAppKeywords(String packageName, String label) {
        if (packageName != null) {
            String lower = packageName.toLowerCase();
            if (lower.contains("classroom") || lower.contains("canvas") || lower.contains("blackboard") ||
                lower.contains("schoology") || lower.contains("quizlet") || lower.contains("anki") ||
                lower.contains("desmos") || lower.contains("geogebra") || lower.contains("calculator") ||
                lower.contains("docs.editors") || (lower.contains("google") && lower.contains("docs")) ||
                lower.contains("photomath") || lower.contains("wolfram") || lower.contains("adobe.reader") ||
                lower.contains("camscanner") || lower.contains("translate") || lower.contains("deepl") ||
                lower.contains("duolingo") || lower.contains("khanacademy") || lower.contains("symbolab") ||
                lower.contains("mathway") || lower.contains("chegg") || lower.contains("termux") ||
                lower.contains("pydroid") || lower.contains("skydrive") || lower.contains("dropbox") ||
                lower.contains("readera") || lower.contains("wps")) {
                return true;
            }
        }
        if (label != null) {
            String lowerLabel = label.toLowerCase();
            if (lowerLabel.contains("classroom") || lowerLabel.contains("canvas") || lowerLabel.contains("blackboard") ||
                lowerLabel.contains("schoology") || lowerLabel.contains("quizlet") || lowerLabel.contains("anki") ||
                lowerLabel.contains("desmos") || lowerLabel.contains("geogebra") || lowerLabel.contains("calculator") ||
                lowerLabel.contains("photomath") || lowerLabel.contains("docs") || lowerLabel.contains("sheets") ||
                lowerLabel.contains("slides") || lowerLabel.contains("drive") || lowerLabel.contains("student") ||
                lowerLabel.contains("acrobat") || lowerLabel.contains("scanner") || lowerLabel.contains("translate") ||
                lowerLabel.contains("dictionary") || lowerLabel.contains("duolingo") || lowerLabel.contains("khan academy") ||
                lowerLabel.contains("symbolab") || lowerLabel.contains("mathway") || lowerLabel.contains("chegg") ||
                lowerLabel.contains("termux") || lowerLabel.contains("onedrive") || lowerLabel.contains("dropbox") ||
                lowerLabel.contains("readera") || lowerLabel.contains("wps office")) {
                return true;
            }
        }
        return false;
    }

    private boolean isAiAppKeywords(String packageName, String label) {
        if (packageName != null) {
            String lower = packageName.toLowerCase();
            if (lower.contains("chatgpt") || lower.contains("bard") || lower.contains("gemini") ||
                lower.contains("claude") || lower.contains("copilot") || lower.contains("perplexity") ||
                lower.contains("deepseek") || lower.contains(".poe")) {
                return true;
            }
        }
        if (label != null) {
            String lowerLabel = label.toLowerCase();
            if (lowerLabel.contains("chatgpt") || lowerLabel.contains("gemini") || lowerLabel.contains("claude") ||
                lowerLabel.contains("copilot") || lowerLabel.contains("perplexity") || lowerLabel.contains("deepseek") ||
                lowerLabel.contains("ai assistant") || lowerLabel.contains("poe")) {
                return true;
            }
        }
        return false;
    }

    private boolean isBrowserAppKeywords(String packageName) {
        if (packageName == null) return false;
        String lower = packageName.toLowerCase();
        return lower.contains("chrome") || lower.contains("browser") || lower.contains("firefox") || lower.contains("opera") || lower.contains("brave") || lower.contains("duckduckgo");
    }

    private boolean isMusicAppKeywords(String packageName) {
        if (packageName == null) return false;
        String lower = packageName.toLowerCase();
        return lower.contains("music") || lower.contains("spotify") || lower.contains("tidal") || lower.contains("deezer") || lower.contains("soundcloud") || lower.contains("aspiro");
    }

    private boolean isCameraAppKeywords(String packageName) {
        if (packageName == null) return false;
        String lower = packageName.toLowerCase();
        return lower.contains("camera");
    }

    private boolean isKeyboardAppKeywords(String packageName) {
        if (packageName == null) return false;
        String lower = packageName.toLowerCase();
        return lower.contains("inputmethod") || 
            lower.contains("honeyboard") || 
            lower.contains("keyboard") || 
            lower.contains("gboard") || 
            lower.contains("swiftkey") || 
            lower.contains(".ime");
    }

    private boolean isHiddenInfrastructureApp(String packageName, String label) {
        if (packageName != null) {
            String lower = packageName.toLowerCase();
            if (lower.contains("cameraextension") ||
                lower.contains("extensionproxy") ||
                lower.contains("lenslauncher") ||
                lower.contains("aperturelenslauncher") ||
                lower.contains("opensourcemusicplayer") ||
                lower.contains("androidopensourcemusicplayer") ||
                lower.contains("packageinstaller") ||
                lower.contains(".installer") ||
                lower.equals("com.android.vending") ||
                lower.equals("com.google.android.feedback") ||
                lower.equals("com.google.android.gms")) {
                return true;
            }
        }
        if (label != null) {
            String lowerLabel = label.toLowerCase().replace(" ", "");
            if (lowerLabel.contains("cameraextensionproxy") ||
                lowerLabel.contains("cameraextension") ||
                lowerLabel.contains("lenslauncher") ||
                lowerLabel.contains("aperturelenslauncher") ||
                lowerLabel.contains("aperaturelenslauncher") ||
                lowerLabel.contains("androidopensourcemusicplayer") ||
                lowerLabel.contains("opensourcemusicplayer") ||
                lowerLabel.contains("packageinstaller") ||
                lowerLabel.contains("googleplaystore") ||
                lowerLabel.contains("playstore")) {
                return true;
            }
        }
        return false;
    }

    private String getBase64Icon(Drawable icon) {
        if (icon == null) return null;
        try {
            int targetDim = 96;
            Bitmap bitmap = Bitmap.createBitmap(targetDim, targetDim, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            icon.setBounds(0, 0, targetDim, targetDim);
            icon.draw(canvas);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 75, outputStream);
            return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }
    @PluginMethod
    public void checkPermissions(PluginCall call) {
        JSObject result = new JSObject();

        result.put("isAdminActive", dpm.isAdminActive(adminComponent));
        
        boolean accessibilityEnabled = false;

        // 1. Primary Check: Query active AccessibilityManager services
        try {
            AccessibilityManager am = (AccessibilityManager) getActivity().getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am != null) {
                List<AccessibilityServiceInfo> runningServices = 
                    am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
                if (runningServices != null) {
                    String pkg = getActivity().getPackageName();
                    for (AccessibilityServiceInfo s : runningServices) {
                        if (s.getId() != null && s.getId().contains(pkg)) {
                            accessibilityEnabled = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "AccessibilityManager query failed", e);
        }

        // 2. Secondary Check: Fallback to Settings.Secure
        if (!accessibilityEnabled) {
            try {
                int enabled = android.provider.Settings.Secure.getInt(
                    getActivity().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED, 0);
                if (enabled == 1) {
                    String services = android.provider.Settings.Secure.getString(
                        getActivity().getContentResolver(),
                        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                    if (services != null) {
                        String pkg = getActivity().getPackageName();
                        if (services.contains(pkg + "/") || services.contains("LockAccessibilityService")) {
                            accessibilityEnabled = true;
                        }
                    }
                }
            } catch (Exception e) {}
        }
        
        result.put("isAccessibilityEnabled", accessibilityEnabled);

        // 3. Detect Installation Source (ADB vs On-Device Package Installer)
        String installSource = "On-Device Package Installer";
        boolean isAdbInstall = false;
        try {
            PackageManager pm = getActivity().getPackageManager();
            String installer = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                android.content.pm.InstallSourceInfo info = pm.getInstallSourceInfo(getActivity().getPackageName());
                if (info != null) {
                    installer = info.getInstallingPackageName();
                    if (installer == null) {
                        installer = info.getInitiatingPackageName();
                    }
                }
            } else {
                installer = pm.getInstallerPackageName(getActivity().getPackageName());
            }

            // Sideload via ADB has null or "com.android.shell" installer
            if (installer == null || "com.android.shell".equals(installer)) {
                isAdbInstall = true;
                installSource = "ADB (PC Script / USB)";
            } else if (installer.contains("vending")) {
                installSource = "Google Play Store";
            } else if (installer.contains("packageinstaller")) {
                installSource = "On-Device Package Installer";
            } else {
                installSource = "Installer: " + installer;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to query installer info", e);
        }

        // Check intent extra or prefs from qiezka.bat
        try {
            Intent launchIntent = getActivity().getIntent();
            if (launchIntent != null && "adb".equals(launchIntent.getStringExtra("setup_source"))) {
                isAdbInstall = true;
                installSource = "ADB (PC Script / USB)";
                prefs.edit().putBoolean("configured_via_adb", true).apply();
            } else if (prefs.getBoolean("configured_via_adb", false)) {
                isAdbInstall = true;
                installSource = "ADB (PC Script / USB)";
            }
        } catch (Exception e) {}

        result.put("isAdbInstall", isAdbInstall);
        result.put("installSource", installSource);

        boolean isBatteryIgnored = false;
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                isBatteryIgnored = pm.isIgnoringBatteryOptimizations(getActivity().getPackageName());
            }
        } catch (Exception e) {}
        result.put("isBatteryOptimizationIgnored", isBatteryIgnored);

        boolean isNotificationGranted = true;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                isNotificationGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                    getActivity(), android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            } else {
                isNotificationGranted = androidx.core.app.NotificationManagerCompat.from(getActivity()).areNotificationsEnabled();
            }
        } catch (Exception e) {}
        result.put("isNotificationGranted", isNotificationGranted);

        boolean isExactAlarmGranted = true;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.app.AlarmManager am = (android.app.AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
                if (am != null) {
                    isExactAlarmGranted = am.canScheduleExactAlarms();
                }
            }
        } catch (Exception e) {}
        result.put("isExactAlarmGranted", isExactAlarmGranted);

        call.resolve(result);
    }

    @PluginMethod
    public void requestBatteryOptimization(PluginCall call) {
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getActivity().getPackageName())) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
            }
            call.resolve();
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(fallback);
                call.resolve();
            } catch (Exception ex) {
                Log.e(TAG, "Failed to request battery optimization", ex);
                call.reject("Failed to request battery optimization: " + ex.getMessage());
            }
        }
    }

    @PluginMethod
    public void requestNotificationPermission(PluginCall call) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                if (getPermissionState("notifications") != PermissionState.GRANTED) {
                    requestPermissionForAlias("notifications", call, "notificationPermCallback");
                    return;
                }
            }
            JSObject ret = new JSObject();
            ret.put("granted", true);
            call.resolve(ret);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
                call.resolve();
            } catch (Exception ex) {
                Log.e(TAG, "Failed to request notification permission", ex);
                call.reject("Failed to request notification permission: " + ex.getMessage());
            }
        }
    }

    @PermissionCallback
    private void notificationPermCallback(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("granted", getPermissionState("notifications") == PermissionState.GRANTED);
        call.resolve(ret);
    }

    @PluginMethod
    public void openNotificationSettings(PluginCall call) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(intent);
            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "Failed to open notification settings", e);
            call.reject("Failed to open notification settings: " + e.getMessage());
        }
    }
    
    @PluginMethod
    public void openAccessibilitySettings(PluginCall call) {
        android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        getActivity().startActivity(intent);
        call.resolve();
    }

    @PluginMethod
    public void openAppInfo(PluginCall call) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getActivity().getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(intent);
            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "Failed to open app info", e);
            call.reject("Failed to open app info: " + e.getMessage());
        }
    }

    @PluginMethod
    public void openDeviceAdminSettings(PluginCall call) {
        try {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Activate QIEZKA as Device Administrator to prevent uninstallation during lockdown.");
            startActivityForResult(call, intent, "deviceAdminResult");
        } catch (Exception e) {
            Log.e(TAG, "Direct ADD_DEVICE_ADMIN failed, opening settings list", e);
            openDeviceAdminListFallback(call);
        }
    }

    @PluginMethod
    public void openDeviceAdminList(PluginCall call) {
        openDeviceAdminListFallback(call);
    }

    private void openDeviceAdminListFallback(PluginCall call) {
        try {
            Intent intent = new Intent("android.settings.DEVICE_ADMIN_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(intent);
            call.resolve();
        } catch (Exception e1) {
            try {
                Intent fallback = new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(fallback);
                call.resolve();
            } catch (Exception ex) {
                Log.e(TAG, "Failed to open device admin settings", ex);
                call.reject("Failed to open device admin settings: " + ex.getMessage());
            }
        }
    }

    @ActivityCallback
    private void deviceAdminResult(PluginCall call, ActivityResult result) {
        if (call == null) return;
        boolean isAdmin = dpm.isAdminActive(adminComponent);
        JSObject ret = new JSObject();
        ret.put("isAdminActive", isAdmin);
        call.resolve(ret);
    }

    @PluginMethod
    public void exportBackup(PluginCall call) {
        String tempFileName = call.getString("tempFileName");
        String defaultName = call.getString("defaultName", "backup.json");

        if (tempFileName == null) {
            call.reject("Must provide tempFileName");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, defaultName);

        startActivityForResult(call, intent, "exportBackupResult");
    }

    @ActivityCallback
    private void exportBackupResult(PluginCall call, ActivityResult result) {
        if (result.getResultCode() == android.app.Activity.RESULT_OK) {
            Intent data = result.getData();
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                String tempFileName = call.getString("tempFileName");
                
                try {
                    File cacheDir = getContext().getCacheDir();
                    File tempFile = new File(cacheDir, tempFileName);
                    
                    if (!tempFile.exists()) {
                        call.reject("Temp file not found");
                        return;
                    }
                    
                    InputStream in = new FileInputStream(tempFile);
                    OutputStream out = getContext().getContentResolver().openOutputStream(uri);
                    
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    
                    in.close();
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                    
                    tempFile.delete();
                    
                    call.resolve();
                } catch (Exception e) {
                    call.reject("Failed to copy file", e);
                }
            } else {
                call.reject("No URI returned");
            }
        } else {
            call.reject("User canceled");
        }
    }

    public boolean hasBackListeners() {
        return hasListeners("backPressed");
    }

    public void triggerBackPressed() {
        notifyListeners("backPressed", new JSObject());
    }

    @PluginMethod
    public void exitToHome(PluginCall call) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                getActivity().moveTaskToBack(true);
                call.resolve();
            });
        } else {
            call.resolve();
        }
    }

    @PluginMethod
    public void showToast(PluginCall call) {
        String message = call.getString("message", "");
        if (message != null && !message.trim().isEmpty() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                android.widget.Toast.makeText(getActivity(), message, android.widget.Toast.LENGTH_SHORT).show();
                call.resolve();
            });
        } else {
            call.resolve();
        }
    }
}
