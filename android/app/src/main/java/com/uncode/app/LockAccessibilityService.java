package com.uncode.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Browser;
import android.net.Uri;
import android.app.Notification;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import android.widget.Toast;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class LockAccessibilityService extends AccessibilityService {

    private static final String TAG = "LockAccessService";
    private static final String PREFS_NAME = "uncode_lock";

    /**
     * Active browser tracking to provide Same-Page and Long-Press immunity.
     * In accordance with flowchart: As long as the user is on the same page, browsing
     * including long-press or context menu interactions continues uninterrupted.
     */
    private String lastBrowserEventClass = null;









    public static boolean isSimOrCarrierService(String pkg, String appLabel) {
        return AppClassifier.isSimOrCarrierService(pkg, appLabel);
    }

    public static boolean isSimOrCarrierService(String pkg) {
        return AppClassifier.isSimOrCarrierService(pkg);
    }

    public static volatile LockAccessibilityService instance = null;

    public static LockAccessibilityService getInstance() {
        return instance;
    }

    public static final Set<String> dynamicLauncherPackages = KnownSafe.dynamicLauncherPackages;
    public static final Set<String> dynamicLauncherLabels = KnownSafe.dynamicLauncherLabels;
    private SharedPreferences prefs;

    public static boolean isLauncherApp(Context context, String pkg) {
        return KnownSafe.isLauncherApp(context, pkg);
    }

    private String lastForegroundPackage = null;
    private String lastBlockedPackage = null;
    private final Set<String> firedWarningKeys = new HashSet<>();
    private final Handler tickerHandler = new Handler(Looper.getMainLooper());
    private final Runnable tickerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                onTickerTick();
            } catch (Exception e) {
                Log.e(TAG, "Ticker error: " + e.getMessage());
            } finally {
                tickerHandler.postDelayed(this, 300L);
            }
        }
    };

    public void startTicker() {
        tickerHandler.removeCallbacks(tickerRunnable);
        tickerHandler.post(tickerRunnable);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        refreshDynamicExemptPackages();
        AppClassifier.refreshBrowserCache(this);
        startTicker();
        Log.i(TAG, "LockAccessibilityService onCreate — ticker started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTicker();
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i(TAG, "onTaskRemoved: app task cleared from recents — keeping ticker alive");
        startTicker();
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        AppClassifier.refreshBrowserCache(this);

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.packageNames = null; // Watch all packages
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                          AccessibilityEvent.TYPE_WINDOWS_CHANGED |
                          AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                          AccessibilityEvent.TYPE_VIEW_CLICKED |
                          AccessibilityEvent.TYPE_VIEW_FOCUSED |
                          AccessibilityEvent.TYPE_VIEW_SCROLLED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                     AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                     AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        info.notificationTimeout = 0;
        setServiceInfo(info);

        refreshDynamicExemptPackages();
        startTicker();

        // Immediate evaluation on reconnect
        try {
            boolean active = prefs.getBoolean("lockdown_active", false);
            if (!active) {
                active = checkAndAutoStartScheduledLock(prefs);
            }
            if (active) {
                long lockEnd = prefs.getLong("lock_end_time", 0L);
                FloatingOverlayService.startService(this, lockEnd, "Study Session");
                String activePkg = detectCurrentForegroundPackage();
                if (activePkg != null && isPackageBlocked(activePkg)) {
                    enforceBlock(activePkg);
                }
            }
        } catch (Exception ignore) {}

        Log.i(TAG, "LockAccessibilityService connected and ticker running");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        tickerHandler.removeCallbacks(tickerRunnable);
    }

    /**
     * Dynamically discovers all installed Home Launchers (CATEGORY_HOME)
     * on this specific device.
     */
    private void refreshDynamicExemptPackages() {
        try {
            PackageManager pm = getPackageManager();
            if (pm == null) return;

            // Dynamically discover all installed Home Launchers (CATEGORY_HOME)
            try {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                List<ResolveInfo> homeApps = pm.queryIntentActivities(homeIntent, 0);
                if (homeApps != null) {
                    for (ResolveInfo info : homeApps) {
                        if (InstalledLauncherDetector.isRealLauncher(info, getPackageName())) {
                            dynamicLauncherPackages.add(info.activityInfo.packageName);
                            CharSequence lbl = info.loadLabel(pm);
                            if (lbl != null) {
                                dynamicLauncherLabels.add(lbl.toString().trim().toLowerCase(Locale.US));
                            }
                        }
                    }
                }
            } catch (Exception ignore) {}

            Log.d(TAG, "Discovered dynamic launchers: " + dynamicLauncherPackages.size());
        } catch (Exception e) {
            Log.w(TAG, "Error resolving dynamic launchers: " + e.getMessage());
        }
    }



    public static boolean isKeyboardPackage(Context context, String pkg) {
        return KnownSafe.isKeyboardPackage(context, pkg);
    }

    private boolean isKeyboardApp(String pkg) {
        if (pkg == null) return false;
        return KnownSafe.isKeyboardPackage(this, pkg);
    }



    public static boolean isInOperatingHours(Context context, long effectiveNow) {
        if (context != null) {
            SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String mode = p.getString("operating_mode", "safemode");
            if ("hardcore".equalsIgnoreCase(mode)) {
                return true;
            }
        }
        return isClockInOperatingHours(effectiveNow);
    }

    public static boolean isInOperatingHours(long effectiveNow) {
        if (instance != null && instance.prefs != null) {
            String mode = instance.prefs.getString("operating_mode", "safemode");
            if ("hardcore".equalsIgnoreCase(mode)) {
                return true;
            }
        }
        return isClockInOperatingHours(effectiveNow);
    }

    private static boolean isClockInOperatingHours(long effectiveNow) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(effectiveNow);
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = cal.get(java.util.Calendar.MINUTE);
        int totalMins = hour * 60 + minute;
        // Operating window: 19:00 (1140 mins) to 23:59 (1439 mins) OR 00:00 (0 mins) to 02:59 (179 mins)
        return (totalMins >= 1140 || totalMins < 180);
    }

    public boolean hasActiveSchedules() {
        if (prefs == null) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        return hasActiveSchedules(prefs);
    }

    public static boolean hasActiveSchedules(SharedPreferences preferences) {
        if (preferences == null) return false;
        String schedulesJson = preferences.getString("schedules_json", null);
        if (schedulesJson == null || schedulesJson.trim().isEmpty()) return false;
        if (schedulesJson.contains("\"isActive\":true") || schedulesJson.contains("\"isActive\": true")) {
            return true;
        }
        try {
            org.json.JSONArray arr = new org.json.JSONArray(schedulesJson);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject s = arr.getJSONObject(i);
                if (s.optBoolean("isActive", true)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static long sLastSirenCheckTime = 0L;
    private static boolean sCachedIsSirenDefault = false;

    public static boolean isSirenDefaultLauncher(Context context) {
        long now = System.currentTimeMillis();
        if (sLastSirenCheckTime > now + 60000L) {
            return sCachedIsSirenDefault;
        }
        if (context == null) return false;
        if (now - sLastSirenCheckTime < 1000L) {
            return sCachedIsSirenDefault;
        }
        try {
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                ResolveInfo defaultHome = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
                if (defaultHome != null && defaultHome.activityInfo != null) {
                    sCachedIsSirenDefault = LauncherStateManager.SIREN_PACKAGE.equals(defaultHome.activityInfo.packageName);
                    sLastSirenCheckTime = now;
                    return sCachedIsSirenDefault;
                }
            }
        } catch (Exception ignore) {}
        sCachedIsSirenDefault = false;
        sLastSirenCheckTime = now;
        return false;
    }

    public static void setCachedIsSirenDefaultForTesting(Boolean value) {
        if (value == null) {
            sLastSirenCheckTime = 0L;
            sCachedIsSirenDefault = false;
        } else {
            sCachedIsSirenDefault = value;
            sLastSirenCheckTime = System.currentTimeMillis() + 3600000L;
        }
    }

    public boolean isHomeChangeInterceptorActive() {
        if (prefs == null) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        return isHomeChangeInterceptorActive(this, prefs);
    }

    public static boolean isHomeChangeInterceptorActive(Context context, SharedPreferences preferences) {
        // Condition 1: SIREN must be detected as the OS default home launcher
        if (!isSirenDefaultLauncher(context)) {
            return false;
        }

        // Condition 2: Active schedule, lockdown, or consequence
        if (preferences == null) return false;

        boolean isLockdownActive = preferences.getBoolean("lockdown_active", false);
        boolean isConsequenceActive = preferences.getBoolean("consequence_active", false);

        long timeOffset = preferences.getLong("simulated_time_offset", 0L);
        long effectiveNow = System.currentTimeMillis() + timeOffset;
        boolean inOperatingHours = isClockInOperatingHours(effectiveNow);
        boolean isHardcore = "hardcore".equalsIgnoreCase(preferences.getString("operating_mode", "safemode"));
        boolean isConsequenceEnforcing = isConsequenceActive && (isHardcore || inOperatingHours);

        boolean hasSchedules = hasActiveSchedules(preferences);

        return isLockdownActive || isConsequenceEnforcing || hasSchedules;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        // Ultra-Fast Path: Master Veto Default Home Launcher role & switch attempt (ONLY if SIREN is default AND session active)
        if (isHomeChangeInterceptorActive() && isFastHomeRoleEvent(event)) {
            AccessibilityNodeInfo source = event.getSource();
            evictHomeLauncherChange(source);
            if (source != null) {
                try { source.recycle(); } catch (Exception ignore) {}
            }
            return;
        }

        if (prefs == null) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }

        // Track foreground app switches
        int eventType = event.getEventType();

        // Flowchart Immunity: Long press gestures and in-page selections must NEVER trigger app blocks or evictions
        if (eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
            return;
        }

        if (event.getClassName() != null) {
            lastBrowserEventClass = event.getClassName().toString();
        }

        CharSequence pkgChar = event.getPackageName();
        if (pkgChar != null) {
            String pkgStr = pkgChar.toString();
            if ((eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                 eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                 eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED)) {
                if (isGeminiActive(pkgStr, event.getClassName() != null ? event.getClassName().toString() : null, event.getSource()) && !isGeminiAllowed()) {
                    lastForegroundPackage = "com.google.android.apps.bard";
                } else if (!isSystemOrLauncher(pkgStr)) {
                    lastForegroundPackage = pkgStr;
                }
            }
        }

        boolean isLockdownActive = prefs.getBoolean("lockdown_active", false);
        boolean isConsequenceActive = prefs.getBoolean("consequence_active", false);
        long lockEndTime = prefs.getLong("lock_end_time", 0L);
        long timeOffset = prefs.getLong("time_offset", 0L);
        long effectiveNow = System.currentTimeMillis() + timeOffset;

        boolean inOperatingHours = isInOperatingHours(effectiveNow);

        // ── Native Timestamp Handling on Timer Expiration ──
        if (isLockdownActive && !isConsequenceActive && lockEndTime > 0 && effectiveNow >= lockEndTime) {
            isConsequenceActive = true;
            prefs.edit()
                    .putBoolean("consequence_active", true)
                    .remove("lock_end_time")
                    .apply();
            AlarmReceiver.cancelLockEndAlarm(this);
            AlarmReceiver.createNotificationChannels(this);
            boolean isHardcore = "hardcore".equalsIgnoreCase(prefs.getString("operating_mode", "safemode"));
            String notifTitle = isHardcore
                    ? "⚠️ Homework Expired — Consequence Active (Hardcore)"
                    : "⚠️ Homework Expired — Consequence Active";
            String notifMsg = isHardcore
                    ? "Study timer expired without passing. Distracting apps remain restricted 24/7 (Hardcore Mode) until rescheduled and passed."
                    : "Study timer expired without passing. Distracting apps remain restricted during operating hours (7 PM – 3 AM) until rescheduled and passed.";
            AlarmReceiver.showNotificationStatic(
                    this,
                    AlarmReceiver.NOTIF_ID_COMPLETED,
                    AlarmReceiver.CHANNEL_ID_ALERTS,
                    notifTitle,
                    notifMsg,
                    Notification.PRIORITY_HIGH,
                    false
            );
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(AlarmReceiver.NOTIF_ID_STATUS);
            }
            Log.i(TAG, "Lockdown expired — entered Consequence Mode");
        }

        // ── State Evaluation: Lockdown, Consequence, Standby, or Total Idle ──
        boolean isHardcore = "hardcore".equalsIgnoreCase(prefs.getString("operating_mode", "safemode"));
        boolean isConsequenceEnforcing = isConsequenceActive && (isHardcore || inOperatingHours);

        if (!isLockdownActive && !isConsequenceActive) {
            isLockdownActive = checkAndAutoStartScheduledLock(prefs);
        }

        boolean isEnforcing = isLockdownActive || isConsequenceEnforcing;
        boolean hasSchedules = hasActiveSchedules();

        // Flowchart: If Consequence Active in Safemode outside Operating Hours (Daytime 3am-7pm) -> ALLOW_IDLE
        if (isConsequenceActive && !isHardcore && !inOperatingHours) {
            return;
        }

        // Flowchart: If neither Lockdown nor Consequence is Active, check Active Schedules
        if (!isEnforcing && !hasSchedules) {
            // Total System Idle: Zero schedules -> ALLOW_IDLE (Settings, launcher, and uninstall allowed)
            return;
        }

        // ── STAGE 1: MASTER VETO GATE (Active Lockdown, Active Consequence, or Standby Protection) ──
        if (interceptHomeLauncherChangeAttempt(event, null, pkgChar != null ? pkgChar.toString() : null)) {
            return;
        }

        if (pkgChar != null) {
            String pkgStr = pkgChar.toString();
            if (!pkgStr.equals(getPackageName()) && !isSystemOrLauncher(pkgStr)) {

                String appLabel = null;
                try {
                    PackageManager pm = getPackageManager();
                    if (pm != null) {
                        android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(pkgStr, 0);
                        CharSequence lbl = pm.getApplicationLabel(ai);
                        if (lbl != null) appLabel = lbl.toString();
                    }
                } catch (Exception ignore) {}

                if (AppClassifier.isSettingsOrDeviceManager(pkgStr, appLabel) || AppClassifier.isStage1Bloat(pkgStr, appLabel)) {
                    Log.w(TAG, "Stage 1 Master Veto Gate matched: " + pkgStr + " (" + appLabel + ")");
                    enforceBlock(pkgStr);
                    return;
                }
            }
        }

        // Milestone 20: Immediate evaluation on window stack reordering (such as Recents task switch)
        // In Android, TYPE_WINDOWS_CHANGED events dispatched by WindowManager have event.getPackageName() == null!
        // It MUST be evaluated here before any null package check.
        if (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            String currentFg = detectCurrentForegroundPackage();
            if (currentFg != null && !currentFg.equals(getPackageName()) && !isSystemOrLauncher(currentFg)) {
                String fgLabel = null;
                try {
                    PackageManager pm = getPackageManager();
                    if (pm != null) {
                        android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(currentFg, 0);
                        CharSequence lbl = pm.getApplicationLabel(ai);
                        if (lbl != null) fgLabel = lbl.toString();
                    }
                } catch (Exception ignore) {}

                if (AppClassifier.isSettingsOrDeviceManager(currentFg, fgLabel) || AppClassifier.isStage1Bloat(currentFg, fgLabel)) {
                    Log.w(TAG, "Stage 1 Master Veto caught window stack change: " + currentFg);
                    enforceBlock(currentFg);
                    return;
                }

                // If not enforcing (Standby), normal apps are allowed
                if (!isEnforcing) {
                    return;
                }

                if (isBrowserPackage(currentFg)) {
                    if (AppClassifier.isPortalSuperApp(currentFg)) {
                        String subCls = lastBrowserEventClass;
                        if (AppClassifier.isDistractingSubActivity(currentFg, subCls)) {
                            Log.w(TAG, "Caught distracting sub-activity on window change in portal app " + currentFg + " (" + subCls + ") -> Auto-Back");
                            remediateDistractingSubActivity(currentFg, subCls);
                            return;
                        }
                    }
                    AccessibilityNodeInfo root = getRootInActiveWindow();
                    if (root != null) {
                        try {
                            inspectBrowserWindow(root, currentFg, false);
                        } finally {
                            root.recycle();
                        }
                    }
                    return;
                } else if (isPackageBlocked(currentFg)) {
                    enforceBlock(currentFg);
                    return;
                }
            }
        }

        // ── STAGE 1 PASSED: ROUTE BY STATE ──
        if (!isEnforcing) {
            // Active Schedule Standby: Normal apps (TikTok, YouTube, games, browsers) are allowed until schedule lock starts
            return;
        }

        if (pkgChar == null) return;
        String pkg = pkgChar.toString();

        // Never inspect or block QIEZKA itself
        if (pkg.equals(getPackageName())) return;

        // Gemini interception inside com.google.android.googlequicksearchbox or entry stub
        String eventCls = event.getClassName() != null ? event.getClassName().toString() : null;
        if (isGeminiActive(pkg, eventCls, event.getSource())) {
            if (!isGeminiAllowed()) {
                Log.w(TAG, "Blocked Gemini Robin UI inside " + pkg + " (" + eventCls + ") — AI policy disabled during lockdown/consequence");
                enforceBlock("com.google.android.apps.bard");
                return;
            }
        }

        // ── SystemUI handling ──
        if (pkg.equals("com.android.systemui")) {
            handleSystemUiEvent(event);
            return;
        }

        // Milestone 19/20: Drop pure background notifications from non-active apps
        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            return;
        }

        // STAGE 2 — Branch 1: Web Browser Gate (URL / DOM Classifier & In-Browser Auto-Back)
        if (isBrowserPackage(pkg)) {
            // Typing Immunity: Ignore keystrokes and text selection events while user is editing in address bars
            if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
                eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                return;
            }

            // Sub-Function Remediation for Portal Super-Apps (Baidu Reels, Phoenix, UC)
            if (AppClassifier.isPortalSuperApp(pkg)) {
                String subCls = event.getClassName() != null ? event.getClassName().toString() : lastBrowserEventClass;
                if (AppClassifier.isDistractingSubActivity(pkg, subCls)) {
                    Log.w(TAG, "Caught distracting sub-activity in portal app " + pkg + " (" + subCls + ") -> Auto-Back");
                    remediateDistractingSubActivity(pkg, subCls);
                    return;
                }
            }
            boolean isTransition = (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                                    eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED);
            boolean isInteraction = (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED || 
                                     eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                                     eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED);

            boolean isVisibleOnScreen = false;
            AccessibilityNodeInfo source = event.getSource();
            if (source != null) {
                try {
                    isVisibleOnScreen = source.isVisibleToUser();
                } finally {
                    source.recycle();
                }
            }

            String currentForeground = detectCurrentForegroundPackage();
            boolean isForegroundApp = pkg.equals(currentForeground);
            if (isTransition || isInteraction || isVisibleOnScreen || isForegroundApp) {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null) {
                    try {
                        inspectBrowserWindow(root, pkg, false);
                    } finally {
                        root.recycle();
                    }
                }
            }
            return;
        }

        // STAGE 2 — Branch 2: App Classifier Gate (isPackageBlocked)
        if (isPackageBlocked(pkg)) {
            // Milestone 20: PWA & Recents Task-Switch Interceptor
            boolean isTransition = (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                                    eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED);
            boolean isInteraction = (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED || 
                                     eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                                     eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED);

            // Direct inspection of event source node visibility on screen:
            boolean isVisibleOnScreen = false;
            AccessibilityNodeInfo source = event.getSource();
            if (source != null) {
                try {
                    isVisibleOnScreen = source.isVisibleToUser();
                } finally {
                    source.recycle();
                }
            }

            String currentForeground = detectCurrentForegroundPackage();
            boolean isForegroundApp = pkg.equals(currentForeground);

            if (isTransition || isInteraction || isVisibleOnScreen || isForegroundApp) {
                enforceBlock(pkg);
            } else {
                Log.d(TAG, "Ignored background event from blocked package: " + pkg + " (event=" + eventType + ")");
            }
            return;
        }
    }

    private static final long BROWSER_REMEDIATION_COOLDOWN_MS = 2200L;
    private static final long BROWSER_REMEDIATION_TIMEOUT_MS = 6500L;
    private static final long BROWSER_TOAST_COOLDOWN_MS = 3000L;

    private long lastBrowserToastTime = 0L;
    private long lastBrowserRemediationTime = 0L;
    private String lastRemediatedUrl = null;

    public static boolean isBrowserPackage(String pkg) {
        return AppClassifier.isBrowserPackage(pkg);
    }

    public boolean isBrowserPackage(String pkg, String appLabel) {
        return AppClassifier.isBrowserPackage(this, pkg, appLabel);
    }

    public boolean isGeminiAllowed() {
        return AppClassifier.isPackageAllowedByUnifiedPolicy(this, "com.google.android.apps.gemini");
    }

    public boolean isGeminiActive(String pkg, String className, AccessibilityNodeInfo root) {
        if (pkg == null) return false;
        String lowerPkg = pkg.toLowerCase(Locale.ROOT);
        if (lowerPkg.equals("com.google.android.apps.bard") || lowerPkg.equals("com.google.android.apps.gemini")) {
            return true;
        }
        if (lowerPkg.equals("com.google.android.googlequicksearchbox")) {
            if (className != null) {
                String lowerCls = className.toLowerCase(Locale.ROOT);
                if (lowerCls.contains(".robin.") || lowerCls.contains(".gemini.") ||
                    lowerCls.contains("geminigatewayactivity") || lowerCls.contains("robinentrypointactivity")) {
                    return true;
                }
            }
            if (lastBrowserEventClass != null) {
                String lowerLast = lastBrowserEventClass.toLowerCase(Locale.ROOT);
                if (lowerLast.contains(".robin.") || lowerLast.contains(".gemini.")) {
                    return true;
                }
            }
            AccessibilityNodeInfo activeRoot = root;
            boolean shouldRecycle = false;
            try {
                if (activeRoot == null) {
                    activeRoot = getRootInActiveWindow();
                    shouldRecycle = (activeRoot != null);
                }
                if (activeRoot != null) {
                    List<AccessibilityNodeInfo> nodes = activeRoot.findAccessibilityNodeInfosByViewId("com.google.android.googlequicksearchbox:id/assistant_robin_chat_header_container");
                    if (nodes != null && !nodes.isEmpty()) return true;
                    nodes = activeRoot.findAccessibilityNodeInfosByViewId("com.google.android.googlequicksearchbox:id/assistant_robin_chat_compose_toolbar");
                    if (nodes != null && !nodes.isEmpty()) return true;
                    List<AccessibilityNodeInfo> textNodes = activeRoot.findAccessibilityNodeInfosByText("Open Gemini Live");
                    if (textNodes != null && !textNodes.isEmpty()) return true;
                }
            } catch (Exception ignore) {
            } finally {
                if (shouldRecycle && activeRoot != null) {
                    try { activeRoot.recycle(); } catch (Exception ignore) {}
                }
            }
        }
        return false;
    }

    private boolean isSystemOrLauncher(String pkg) {
        if (pkg == null) return false;
        if (pkg.contains("permissioncontroller")) {
            return false;
        }
        if (pkg.equals("com.google.android.googlequicksearchbox") && isGeminiActive(pkg, lastBrowserEventClass, null)) {
            if (!isGeminiAllowed()) {
                return false;
            }
        }
        // Stage 2: Verified Home Launchers (OEM, third-party, and SIREN proxy)
        if (isLauncherApp(this, pkg)) {
            return true;
        }
        // Universal System Partition Gateway for non-browser, non-settings system overlays
        try {
            PackageManager pm = getPackageManager();
            if (pm != null) {
                android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                if ((ai.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (ai.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    if (!isBrowserPackage(pkg) && !AppClassifier.isSettingsOrDeviceManager(pkg, null) && !AppClassifier.isStage1Bloat(pkg, null)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignore) {}
        return false;
    }

    /**
     * Resolves the title of the active or topmost application window from WindowManager.
     */
    private String resolveActiveWindowTitle(AccessibilityNodeInfo root) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && root != null) {
                AccessibilityWindowInfo win = root.getWindow();
                if (win != null) {
                    CharSequence t = win.getTitle();
                    if (t != null && t.length() > 0) return t.toString();
                }
            }
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null && !windows.isEmpty()) {
                for (AccessibilityWindowInfo w : windows) {
                    if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION && (w.isActive() || w.isFocused())) {
                        CharSequence t = w.getTitle();
                        if (t != null && t.length() > 0) return t.toString();
                    }
                }
                for (AccessibilityWindowInfo w : windows) {
                    if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        CharSequence t = w.getTitle();
                        if (t != null && t.length() > 0) return t.toString();
                    }
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    /**
     * Inspects whether an active window or event in a browser package represents a standalone
     * Progressive Web App (PWA) / WebAPK / TWA / CustomTab rather than normal browser tab navigation.
     */
    private boolean isStandalonePwa(String pkg, String eventClass, String windowTitle, AccessibilityNodeInfo root) {
        if (pkg == null) return false;
        // Known general web browsers are NEVER standalone PWAs during standard web browsing
        if (isBrowserPackage(pkg)) {
            if (eventClass != null) {
                String lowerClass = eventClass.toLowerCase(Locale.US);
                if (lowerClass.contains("webappactivity") || 
                    lowerClass.contains("sametaskwebapkactivity") || 
                    lowerClass.contains("webapplauncheractivity")) {
                    return true;
                }
            }
            return false;
        }
        // Non-browser packages running custom tabs or WebAPKs
        if (eventClass != null) {
            String lowerClass = eventClass.toLowerCase(Locale.US);
            if (lowerClass.contains("customtab") || lowerClass.contains("webapk") || lowerClass.contains("webapp")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifies if the active browser window/tab represents a Search Engine Results Page (SERP),
     * a search query, or active typing in the search box.
     * Search queries are research and must NEVER be blocked or inspected for blacklisted search terms.
     */
    /**
     * Verifies if the active browser window/tab represents a Search Engine Results Page (SERP),
     * a search query, or active typing in the search box.
     * Research is 100% immune; browsing continues uninterrupted.
     */
    private static boolean isSearchEngineHost(String host) {
        if (host == null || host.isEmpty()) return false;
        if (host.equals("gemini.google.com") || host.equals("bard.google.com")) {
            return false; // Gemini and Bard are conversational AI assistants, NOT search engines!
        }
        if (host.equals("google.com") || host.equals("www.google.com")) {
            return true;
        }
        if ((host.startsWith("google.") || host.startsWith("www.google.")) && !host.contains("gemini") && !host.contains("bard")) {
            return true;
        }
        return host.equals("bing.com") || host.equals("www.bing.com") ||
               host.equals("duckduckgo.com") || host.equals("www.duckduckgo.com") ||
               host.equals("search.yahoo.com") || host.equals("ecosia.org") || host.equals("www.ecosia.org") ||
               host.equals("qwant.com") || host.equals("www.qwant.com") ||
               host.equals("baidu.com") || host.equals("www.baidu.com") || host.equals("m.baidu.com") || host.endsWith(".baidu.com") ||
               host.equals("yandex.com") || host.equals("www.yandex.com") ||
               host.equals("startpage.com") || host.equals("www.startpage.com");
    }

    public static boolean isAddressBarNode(AccessibilityNodeInfo node) {
        if (node == null) return false;
        String resId = node.getViewIdResourceName();
        if (resId != null) {
            String lower = resId.toLowerCase(Locale.US);
            return lower.contains("url") || lower.contains("location") ||
                   lower.contains("search") || lower.contains("toolbar") ||
                   lower.contains("address") || lower.contains("omnibox") ||
                   lower.contains("line_1");
        }
        return false;
    }

    private boolean isBrowserSearch(String url, AccessibilityNodeInfo root) {
        // 1. Flowchart: Active search input box / Query typing in progress -> ALLOW_SEARCH (100% immune)
        if (root != null) {
            try {
                AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                if (focused != null) {
                    try {
                        if (focused.isEditable() || focused.isFocused()) {
                            // If the focused input is the native browser address bar / search box, typing is 100% immune
                            if (isAddressBarNode(focused)) {
                                return true; // Active Omnibox / address bar typing or autocomplete in progress
                            }
                            // If on a search engine host, an in-page search input (e.g. Google/Bing search box) is also immune
                            if (url != null && !url.trim().isEmpty()) {
                                String host = WebBlocklistConstants.extractHost(url.trim().toLowerCase(Locale.US));
                                if (isSearchEngineHost(host)) {
                                    return true;
                                }
                            } else {
                                // URL is null (e.g. Chrome New Tab Page search input)
                                return true;
                            }
                        }
                    } finally {
                        focused.recycle();
                    }
                }
            } catch (Exception ignore) {}
        }

        // 2. Search Engine Results Page (SERP) or Search Engine URLs
        if (url != null && !url.trim().isEmpty()) {
            String lowerUrl = url.trim().toLowerCase(Locale.US);

            // Google redirect URLs (google.com/url?q=...) are transitions to destination pages, not search results
            if (lowerUrl.contains("google.") && lowerUrl.contains("/url?")) {
                return false;
            }

            // Check if it is a search engine URL with query parameters
            String host = WebBlocklistConstants.extractHost(lowerUrl);
            if (isSearchEngineHost(host) && (lowerUrl.contains("/search") || lowerUrl.contains("?q=") || lowerUrl.contains("&q="))) {
                return true;
            }
            if (lowerUrl.contains("bing.com/search") || lowerUrl.contains("duckduckgo.com") ||
                lowerUrl.contains("search.yahoo.com") || lowerUrl.contains("ecosia.org/search") ||
                lowerUrl.contains("qwant.com") || lowerUrl.contains("baidu.com") || lowerUrl.contains("m.baidu.com") ||
                lowerUrl.contains("yandex.com/search") || lowerUrl.contains("startpage.com")) {
                return true;
            }

            // If it is a destination URL that is NOT a search engine host, it is NEVER a search results page!
            if (WebClassifier.isDestinationUrl(url)) {
                if (!isSearchEngineHost(host)) {
                    return false; // Destination page! E.g. gemini.google.com, chatgpt.com, y8.com, etc.
                }
            }
        }

        return false;
    }

    /**
     * Unified Web Browser & PWA Inspection Pipeline (Flowchart Aligned).
     *
     * Flow:
     * 1. VERIFICATION: Is it a Browser Search?
     *    - Search Engine Results Page (SERP), query typing, or active search input -> ALLOW_SEARCH.
     *      Research is 100% immune; browsing including long press continues uninterrupted.
     * 2. Extract URL (extractUrlFromBrowser):
     *    - URL Found (Address bar visible - Standard Browser Tab):
     *        a. Academic Safe-List check -> ALLOW_BROWSER
     *        b. Evaluate WebClassifier.classify(url, root, allowYoutube)
     *        c. Allowed -> ALLOW_BROWSER (normal browsing including long press continues uninterrupted)
     *        d. Blocked (e.g. y8.com, TikTok) -> WEB_REMEDIATE (In-Browser Auto-Back / Home button).
     *    - URL is Null (No address bar - Scrolled Tab / Standalone PWA / TWA):
     *        a. Known general browser in standard browsing (scrolled tab, in-page popup, context menu) ->
     *           ALLOW_BROWSER (never treated as PWA)
     *        b. Actual standalone PWA / TWA / WebAPK ->
     *           Walk Chromium Accessibility View Tree via WebClassifier.classifyStandalonePwa
     *           If blocked -> enforceBlock(pkg) directly to QIEZKA Lock (no Back key)
     */
    private void inspectBrowserWindow(AccessibilityNodeInfo root, String pkg, boolean fromTicker) {
        if (root == null || pkg == null) return;

        try {
            String url = extractUrlFromBrowser(root, pkg);

            // ── VERIFICATION: Is it a Browser Search? ──
            if (isBrowserSearch(url, root)) {
                resetBrowserRemediationState();
                return; // ALLOW_SEARCH: Research is 100% immune, browsing continues uninterrupted
            }

            boolean allowYoutube = prefs != null && prefs.getBoolean("allow_youtube", false);

            Set<String> activeServices = prefs != null ? prefs.getStringSet("active_unified_services", new HashSet<>()) : new HashSet<>();
            Set<String> allowedDomains = new HashSet<>(UnifiedPolicyRegistry.getDomainsForServices(activeServices));
            if (prefs != null) {
                Set<String> customDomains = prefs.getStringSet("allowed_domains", null);
                if (customDomains != null) allowedDomains.addAll(customDomains);
            }
            if (allowYoutube) {
                UnifiedService yt = UnifiedPolicyRegistry.SERVICES.get("youtube");
                if (yt != null) allowedDomains.addAll(yt.getDomains());
            }

            if (url != null && !url.trim().isEmpty()) {
                // ── BRANCH A: URL Found (Standard Browser Tab) ──
                if (WebBlocklistConstants.isAcademicExempt(url)) {
                    resetBrowserRemediationState();
                    return; // Academic safe-list immunity
                }
                WebClassifier.ClassificationResult res = WebClassifier.classify(url, root, allowYoutube, allowedDomains);
                if (res.isBlocked) {
                    Log.w(TAG, "inspectBrowserWindow: blocked browser URL: " + url + " (" + res.reason + ")");
                    remediateBlockedBrowserTab(pkg, url, res.reason, root);
                } else {
                    resetBrowserRemediationState();
                }
            } else {
                // ── BRANCH B: URL is Null (Scrolled Tab / In-Page Context Menu / PWA / TWA) ──
                // Known general web browsers in standard browsing (scrolling down, in-page popups, context menus)
                // must NEVER be treated as Standalone PWAs and must NEVER be evicted to QIEZKA Lock.
                boolean isPwa = isStandalonePwa(pkg, lastBrowserEventClass, null, root);
                if (!isPwa && isBrowserPackage(pkg)) {
                    // Standard browser in-page interaction or scrolled state -> ALLOW_BROWSER
                    return;
                }

                // Walk Chromium Accessibility View Tree for genuine standalone PWAs / WebAPKs
                WebClassifier.ClassificationResult pwaRes = WebClassifier.classifyStandalonePwa(null, root, allowYoutube);
                if (pwaRes.isBlocked) {
                    Log.w(TAG, "inspectBrowserWindow: caught blocked standalone PWA: " + pkg + " (" + pwaRes.reason + ")");
                    // Flowchart: Block and Evict to QIEZKA (no Back key)
                    enforceBlock(pkg);
                } else {
                    resetBrowserRemediationState();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in inspectBrowserWindow: " + e.getMessage());
        }
    }

    private int consecutiveBlockedUrlHits = 0;

    private static String extractHost(String rawUrl) {
        if (rawUrl == null) return "";
        String clean = rawUrl.trim().toLowerCase(Locale.US);
        if (clean.startsWith("https://")) clean = clean.substring(8);
        else if (clean.startsWith("http://")) clean = clean.substring(7);
        int slash = clean.indexOf('/');
        if (slash != -1) clean = clean.substring(0, slash);
        int colon = clean.indexOf(':');
        if (colon != -1) clean = clean.substring(0, colon);
        if (clean.startsWith("www.")) clean = clean.substring(4);
        return clean;
    }

    private boolean isSameWebTarget(String url1, String url2) {
        if (url1 == null || url2 == null) return false;
        if (url1.equals(url2)) return true;
        String host1 = extractHost(url1);
        String host2 = extractHost(url2);
        return !host1.isEmpty() && host1.equals(host2);
    }

    private void resetBrowserRemediationState() {
        if (consecutiveBlockedUrlHits > 0 || lastRemediatedUrl != null) {
            consecutiveBlockedUrlHits = 0;
            lastRemediatedUrl = null;
        }
    }

    /**
     * Remediates a blocked web page inside a general web browser without locking the user out indefinitely
     * and without tab accumulation ("blowing up tabs").
     * Strategy (Smart Back with Safety Bailout):
     * 1. Primary (Auto-Back, Hits 1-2): Invoke performGlobalAction(GLOBAL_ACTION_BACK) with a 2.2s cooldown.
     *    If student navigated to the blocked URL from an academic/safe page (e.g. GitHub or Wikipedia) or a chain
     *    of pages, the browser smoothly reverses through history to that safe page in the same tab without stutter.
     * 2. Secondary (Home Button Reset, Hit 3): If the blocked URL persists after 2 back attempts (e.g. fresh tab
     *    with 0 history stack, or JS back-button traps), click the native browser Home button (id/home_button)
     *    to cleanly reset the tab in-place to the New Tab Page without tab accumulation or closing Chrome.
     * 3. Tertiary (Safe Tab Intent Fallback): If accessibility toolbar nodes cannot be found, dispatch explicit Intent
     *    with EXTRA_APPLICATION_ID = getPackageName() ("com.uncode.app") and EXTRA_CREATE_NEW_TAB = false to reuse at most 1 tab.
     */
    private void remediateBlockedBrowserTab(String pkg, String url, String reason, AccessibilityNodeInfo root) {
        long now = System.currentTimeMillis();

        // 1. Debounce: If a remediation action (Auto-Back or Home button) was triggered recently (< 2200ms)
        // for this exact same target origin, allow the browser transition to finish smoothly without interruption.
        if (now - lastBrowserRemediationTime < BROWSER_REMEDIATION_COOLDOWN_MS && isSameWebTarget(url, lastRemediatedUrl)) {
            return;
        }

        // 2. Escalation counter: If the browser is STILL on the same blocked origin after the cooldown window
        // (between 2200ms and 6500ms), advance to next hit. Otherwise reset to 1.
        if (isSameWebTarget(url, lastRemediatedUrl) && (now - lastBrowserRemediationTime < BROWSER_REMEDIATION_TIMEOUT_MS)) {
            consecutiveBlockedUrlHits++;
        } else {
            consecutiveBlockedUrlHits = 1;
        }

        lastBrowserRemediationTime = now;
        lastRemediatedUrl = url;

        Log.w(TAG, "remediateBlockedBrowserTab on " + pkg + " for " + url + " (" + reason + ") hits=" + consecutiveBlockedUrlHits);

        // 1. Display educational Toast notification informing student
        if (now - lastBrowserToastTime > BROWSER_TOAST_COOLDOWN_MS) {
            lastBrowserToastTime = now;
            final String finalReason = reason != null ? reason : "Distracting website";
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(getApplicationContext(), "⚠️ " + finalReason + ". Returning to safe page.", Toast.LENGTH_SHORT).show();
            });
        }

        // 2. Custom Tab Remediation: If opened inside a Chrome Custom Tab, close it directly via close_button
        if (closeCustomTab(root, pkg)) {
            Log.i(TAG, "remediateBlockedBrowserTab: closed CustomTab via close_button");
            return;
        }

        // 3. Primary Remediation: Auto-Back (Hits 1 and 2)
        // Reverses browser history to the prior safe page (e.g. GitHub, Wikipedia, Google, New Tab Page).
        // With 2200ms cooldown, Chrome has sufficient time to complete the back navigation without stutter.
        if (consecutiveBlockedUrlHits <= 2) {
            boolean backed = performGlobalAction(GLOBAL_ACTION_BACK);
            Log.i(TAG, "remediateBlockedBrowserTab: executed auto-back (hit " + consecutiveBlockedUrlHits + ") -> " + backed);
            return;
        }

        // 4. Secondary Remediation: Repeat hit after multiple backs means no back history in this tab or JS trap — reset tab in-place via Home button
        Log.i(TAG, "remediateBlockedBrowserTab: repeat hit detected after auto-back, resetting tab in-place via home button");
        boolean homeClicked = clickBrowserHomeButton(root, pkg);
        if (homeClicked) {
            Log.i(TAG, "remediateBlockedBrowserTab: active tab neutralized in-place via home button (0 new tabs created)");
            return;
        }

        // 5. Tertiary Remediation: Reusable safe tab via Intent (Redirect to clean Google Search page)
        Log.w(TAG, "remediateBlockedBrowserTab: home button unavailable, falling back to reused safe tab intent");
        navigateBrowserToSafeBlank(pkg);
        consecutiveBlockedUrlHits = 0;
    }

    private long lastSubActivityRemediationTime = 0L;
    private int consecutiveSubActivityHits = 0;
    private String lastRemediatedSubActivity = null;

    private void remediateDistractingSubActivity(String pkg, String activityCls) {
        long now = System.currentTimeMillis();
        if (now - lastSubActivityRemediationTime < 600L && activityCls != null && activityCls.equals(lastRemediatedSubActivity)) {
            return; // Debounce rapid accessibility events while back transition is in flight
        }

        if (activityCls != null && activityCls.equals(lastRemediatedSubActivity) && (now - lastSubActivityRemediationTime < 3500L)) {
            consecutiveSubActivityHits++;
        } else {
            consecutiveSubActivityHits = 1;
        }

        lastSubActivityRemediationTime = now;
        lastRemediatedSubActivity = activityCls;

        Log.w(TAG, "remediateDistractingSubActivity on " + pkg + " (" + activityCls + ") hits=" + consecutiveSubActivityHits);

        // Educational Toast notice informing student
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(getApplicationContext(), "⚠️ Short videos / reels are restricted during focus lockdown.", Toast.LENGTH_SHORT).show();
        });

        // If repeated persistent attempts (> 3 rapid clicks into reels), bring up lock overlay
        if (consecutiveSubActivityHits > 3) {
            Log.w(TAG, "Persistent reels bypass attempt detected in " + pkg + " -> asserting QIEZKA lock");
            enforceBlock(pkg);
            return;
        }

        // Auto-Back: Instantly closes VideoTabActivity / feed flow and returns to search interface
        boolean backed = performGlobalAction(GLOBAL_ACTION_BACK);
        Log.i(TAG, "remediateDistractingSubActivity: executed Auto-Back -> " + backed);
    }

    private boolean closeCustomTab(AccessibilityNodeInfo passedRoot, String pkg) {
        AccessibilityNodeInfo root = passedRoot;
        boolean shouldRecycleRoot = false;
        if (root == null) {
            try {
                root = getRootInActiveWindow();
                shouldRecycleRoot = true;
            } catch (Exception ignore) {}
        }
        if (root == null) return false;

        try {
            String[] closeButtonIds = {
                "com.android.chrome:id/close_button",
                "org.chromium.chrome:id/close_button",
                "com.sec.android.app.sbrowser:id/close_button"
            };
            for (String closeId : closeButtonIds) {
                List<AccessibilityNodeInfo> closeNodes = root.findAccessibilityNodeInfosByViewId(closeId);
                if (closeNodes != null && !closeNodes.isEmpty()) {
                    for (AccessibilityNodeInfo node : closeNodes) {
                        if (node != null) {
                            try {
                                if (node.isClickable()) {
                                    boolean clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    Log.i(TAG, "closeCustomTab: ACTION_CLICK on " + closeId + " -> " + clicked);
                                    if (clicked) return true;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed close button click on " + closeId + ": " + e.getMessage());
                            } finally {
                                node.recycle();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "closeCustomTab error: " + e.getMessage());
        } finally {
            if (shouldRecycleRoot && root != null) {
                root.recycle();
            }
        }
        return false;
    }

    private boolean clickBrowserHomeButton(AccessibilityNodeInfo passedRoot, String pkg) {
        AccessibilityNodeInfo root = passedRoot;
        boolean shouldRecycleRoot = false;
        if (root == null) {
            try {
                root = getRootInActiveWindow();
                shouldRecycleRoot = true;
            } catch (Exception ignore) {}
        }
        if (root == null) return false;

        try {
            String[] homeButtonIds = {
                "com.android.chrome:id/home_button",
                "com.sec.android.app.sbrowser:id/location_bar_home_button",
                "org.chromium.chrome:id/home_button",
                "com.microsoft.emmx:id/home_button"
            };

            for (String homeId : homeButtonIds) {
                List<AccessibilityNodeInfo> homeNodes = root.findAccessibilityNodeInfosByViewId(homeId);
                if (homeNodes != null && !homeNodes.isEmpty()) {
                    for (AccessibilityNodeInfo homeNode : homeNodes) {
                        if (homeNode != null) {
                            try {
                                if (homeNode.isClickable()) {
                                    boolean clicked = homeNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    Log.i(TAG, "clickBrowserHomeButton: ACTION_CLICK on " + homeId + " -> " + clicked);
                                    if (clicked) {
                                        return true;
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed home button click on " + homeId + ": " + e.getMessage());
                            } finally {
                                homeNode.recycle();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "clickBrowserHomeButton error: " + e.getMessage());
        } finally {
            if (shouldRecycleRoot && root != null) {
                root.recycle();
            }
        }
        return false;
    }

    private void navigateBrowserToSafeBlank(String pkg) {
        try {
            Intent safeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
            safeIntent.setPackage(pkg);
            // CRITICAL: DO NOT pass 'pkg' ("com.android.chrome") as EXTRA_APPLICATION_ID!
            // Passing Chrome's own package name triggers Chromium's DONT_CLOBBER_TABS_WITH_CHROME_APP_ID,
            // which returns TabOpenType.OPEN_NEW_TAB on every call and blows up open tabs.
            // Instead, passing getPackageName() ("com.uncode.app") triggers TabOpenType.REUSE_APP_ID_MATCHING_TAB_ELSE_NEW_TAB,
            // ensuring Chrome reuses at most ONE single safety tab rather than accumulating tabs.
            safeIntent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
            safeIntent.putExtra("com.android.browser.application_id", getPackageName());
            safeIntent.putExtra(Browser.EXTRA_CREATE_NEW_TAB, false);
            safeIntent.putExtra("create_new_tab", false);
            safeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(safeIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate browser to safe tab: " + e.getMessage());
        }
    }

    private String extractUrlFromBrowser(AccessibilityNodeInfo root, String pkg) {
        if (root == null) return null;

        // Layer 1: Input Focus Guard. If the address bar or search input is actively focused by the user,
        // typing or autocomplete is in progress. Flowchart: ALLOW_SEARCH (100% immune, uninterrupted).
        AccessibilityNodeInfo focusNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focusNode != null) {
            try {
                if (focusNode.isFocused() || focusNode.isEditable()) {
                    if (isAddressBarNode(focusNode)) {
                        return null; // User is actively searching or typing in the browser's address bar
                    }
                }
            } finally {
                focusNode.recycle();
            }
        }

        // 1. Chrome / Chromium family: Query multiple known address bar & Custom Tab view IDs
        String[] chromeViewIds = {
            "com.android.chrome:id/url_bar",
            "com.android.chrome:id/title_url",
            "com.android.chrome:id/search_box_text",
            "com.android.chrome:id/location_bar",
            "com.android.chrome:id/toolbar",
            "com.android.chrome:id/omnibox_text_field",
            "org.chromium.chrome:id/url_bar",
            "org.chromium.chrome:id/title_url"
        };
        for (String id : chromeViewIds) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node != null) {
                        if (node.isFocused()) {
                            continue; // Active typing or autocomplete in progress
                        }
                        CharSequence text = node.getText();
                        CharSequence desc = node.getContentDescription();
                        String val = text != null && text.length() > 0 ? text.toString().trim() :
                                    (desc != null && desc.length() > 0 ? desc.toString().trim() : null);
                        if (isValidUrlCandidate(val)) return val;
                    }
                }
            }
        }

        // 2. Samsung Internet
        String[] sbrowserViewIds = {
            "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "com.sec.android.app.sbrowser:id/url_bar",
            "com.sec.android.app.sbrowser:id/location_bar"
        };
        for (String id : sbrowserViewIds) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node != null) {
                        if (node.isFocused()) {
                            continue; // Active typing or autocomplete in progress
                        }
                        CharSequence text = node.getText();
                        CharSequence desc = node.getContentDescription();
                        String val = text != null && text.length() > 0 ? text.toString().trim() :
                                    (desc != null && desc.length() > 0 ? desc.toString().trim() : null);
                        if (isValidUrlCandidate(val)) return val;
                    }
                }
            }
        }

        // 3. Firefox
        String[] firefoxViewIds = {
            "org.mozilla.firefox:id/url_bar_title",
            "org.mozilla.firefox:id/toolbar",
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view"
        };
        for (String id : firefoxViewIds) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node != null) {
                        if (node.isFocused()) {
                            continue; // Active typing or autocomplete in progress
                        }
                        CharSequence text = node.getText();
                        String val = text != null && text.length() > 0 ? text.toString().trim() : null;
                        if (isValidUrlCandidate(val)) return val;
                    }
                }
            }
        }

        // 4. Edge
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId("com.microsoft.emmx:id/url_bar");
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null) {
                    if (node.isFocused()) {
                        continue; // Active typing or autocomplete in progress
                    }
                    CharSequence text = node.getText();
                    CharSequence desc = node.getContentDescription();
                    String val = text != null && text.length() > 0 ? text.toString().trim() :
                                (desc != null && desc.length() > 0 ? desc.toString().trim() : null);
                    if (isValidUrlCandidate(val)) return val;
                }
            }
        }

        // 5. Brave
        nodes = root.findAccessibilityNodeInfosByViewId("com.brave.browser:id/url_bar");
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null) {
                    if (node.isFocused()) {
                        continue; // Active typing or autocomplete in progress
                    }
                    CharSequence text = node.getText();
                    CharSequence desc = node.getContentDescription();
                    String val = text != null && text.length() > 0 ? text.toString().trim() :
                                (desc != null && desc.length() > 0 ? desc.toString().trim() : null);
                    if (isValidUrlCandidate(val)) return val;
                }
            }
        }

        // 6. 1DM & 1DM+ Browser (idm.internet.download.manager / idm.internet.download.manager.plus)
        String[] idmViewIds = {
            pkg + ":id/search",
            pkg + ":id/search_container",
            "idm.internet.download.manager:id/search",
            "idm.internet.download.manager.plus:id/search"
        };
        for (String id : idmViewIds) {
            nodes = root.findAccessibilityNodeInfosByViewId(id);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node != null) {
                        if (node.isFocused()) {
                            continue; // Active typing or autocomplete in progress
                        }
                        CharSequence text = node.getText();
                        CharSequence desc = node.getContentDescription();
                        String val = text != null && text.length() > 0 ? text.toString().trim() :
                                    (desc != null && desc.length() > 0 ? desc.toString().trim() : null);
                        if (isValidUrlCandidate(val)) return val;
                    }
                }
            }
        }

        // 7. Generic Heuristic Fallback through hierarchy (Targeting address bar & input nodes only)
        return findUrlInHierarchy(root, 0);
    }

    /**
     * Validates whether an extracted string from a browser address bar node qualifies as a plausible
     * web URL or domain navigation target.
     * Prevents empty tab hint placeholders (e.g. "Search or type web address", "Search", "about:blank")
     * from being passed into WebClassifier as unclassified destination domains.
     */
    public static boolean isValidUrlCandidate(String val) {
        if (val == null) return false;
        String clean = val.trim();
        if (clean.isEmpty()) return false;
        // Fast reject: Unencoded spaces indicate placeholder hint text or typed search queries
        if (clean.contains(" ")) return false;
        // Fast reject: Common browser empty tab placeholder words without spaces
        String lower = clean.toLowerCase(Locale.US);
        if (lower.equals("search") || lower.equals("search...") ||
            lower.equals("about:blank") || lower.startsWith("chrome://newtab")) {
            return false;
        }
        // Must look like a real host or URL: contains dot, or protocol, or path slash
        return clean.contains(".") || lower.startsWith("http://") || lower.startsWith("https://") || clean.contains("/");
    }

    private String findUrlInHierarchy(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 8) return null;

        String resId = node.getViewIdResourceName();
        boolean isAddressOrInput = false;
        if (resId != null) {
            String lowerResId = resId.toLowerCase(Locale.US);
            if (lowerResId.contains("url_bar") || lowerResId.contains("location_bar") || 
                lowerResId.contains("address_bar") || lowerResId.contains("omnibar") ||
                lowerResId.contains("url_field") || lowerResId.contains("title_url") ||
                lowerResId.contains("custom_tab_toolbar") || lowerResId.endsWith(":id/search")) {
                isAddressOrInput = true;
            }
        }

        // Native address bars must have a valid identifier.
        // We explicitly do NOT match anonymous EditText or editable nodes without a toolbar/url resource ID,
        // because those represent in-page HTML form fields (such as Wikipedia search, login forms, etc.).
        if (isAddressOrInput) {
            CharSequence text = node.getText();
            CharSequence desc = node.getContentDescription();
            String val = text != null && text.length() > 0 ? text.toString().trim() :
                        (desc != null && desc.length() > 0 ? desc.toString().trim() : null);
            if (node.isFocused()) {
                return null; // Active typing or autocomplete in progress
            }
            if (isValidUrlCandidate(val)) {
                return val;
            }
        }

        // Do not descend into web page render views (WebView / WebContents) looking for address bars.
        // The address bar is native browser chrome and will never be inside web content.
        CharSequence className = node.getClassName();
        if (className != null) {
            String lowerClass = className.toString().toLowerCase(Locale.US);
            if (lowerClass.contains("webview") || lowerClass.contains("render") || lowerClass.contains("contentview")) {
                return null;
            }
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                String found = findUrlInHierarchy(child, depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    public boolean isPackageBlocked(String pkg) {
        if (pkg == null) return false;
        if (pkg.equals(getPackageName())) return false;

        // Query app label for fast-path blacklist matching
        String appLabel = null;
        try {
            PackageManager pm = getPackageManager();
            if (pm != null) {
                android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                CharSequence lbl = pm.getApplicationLabel(ai);
                if (lbl != null) appLabel = lbl.toString();
            }
        } catch (Exception ignore) {}

        // STAGE 1 — MASTER VETO GATE: Strict Anti-Tamper, Bloatware & Evasion Shield
        if (AppClassifier.isStage1Vetoed(pkg, appLabel)) return true;

        // STAGE 2 — Branch 1: Web Browsers (Passed Stage 1, allowed at package level for WebClassifier inspection)
        if (isBrowserPackage(pkg, appLabel)) return false;
        if (isKeyboardApp(pkg)) return false;
        if (isLauncherApp(this, pkg)) return false;
        if (KnownSafe.isCameraApp(this, pkg, appLabel)) return false;

        // Gemini / Robin active inside Google App
        if (pkg.equals("com.google.android.googlequicksearchbox") && isGeminiActive(pkg, lastBrowserEventClass, null)) {
            return !isGeminiAllowed();
        }

        Set<String> whitelist = prefs != null ? prefs.getStringSet("whitelist", new HashSet<>()) : new HashSet<>();

        // ── STAGE 2 & STAGE 3: On-Device Local App Classifier (Initial Policy, Heuristics & Universal System Gateway) ──
        return AppClassifier.isPackageBlocked(this, pkg, whitelist);
    }

    private void onTickerTick() {
        if (prefs == null) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }

        long timeOffset = prefs.getLong("time_offset", 0L);
        long effectiveNow = System.currentTimeMillis() + timeOffset;
        boolean inOperatingHours = isInOperatingHours(effectiveNow);

        boolean isLockdownActive = prefs.getBoolean("lockdown_active", false);
        boolean isConsequenceActive = prefs.getBoolean("consequence_active", false);
        long lockEndTime = prefs.getLong("lock_end_time", 0L);
        boolean isHardcore = "hardcore".equalsIgnoreCase(prefs.getString("operating_mode", "safemode"));

        // 1. Check if lockdown timer has expired
        if (isLockdownActive && !isConsequenceActive && lockEndTime > 0 && effectiveNow >= lockEndTime) {
            Log.i(TAG, "Ticker: lockdown timer expired without passing (effectiveNow=" + effectiveNow + " >= " + lockEndTime + ") -> consequence active");
            prefs.edit()
                    .putBoolean("consequence_active", true)
                    .remove("lock_end_time")
                    .apply();
            AlarmReceiver.cancelLockEndAlarm(this);
            AlarmReceiver.createNotificationChannels(this);
            String notifTitle = isHardcore
                    ? "⚠️ Homework Expired — Consequence Active (Hardcore)"
                    : "⚠️ Homework Expired — Consequence Active";
            String notifMsg = isHardcore
                    ? "Study timer expired without passing. Distracting apps remain restricted 24/7 (Hardcore Mode) until rescheduled and passed."
                    : "Study timer expired without passing. Distracting apps remain restricted during operating hours (7 PM – 3 AM) until rescheduled and passed.";
            AlarmReceiver.showNotificationStatic(
                    this,
                    AlarmReceiver.NOTIF_ID_COMPLETED,
                    AlarmReceiver.CHANNEL_ID_ALERTS,
                    notifTitle,
                    notifMsg,
                    Notification.PRIORITY_HIGH,
                    false
            );
            isConsequenceActive = true;
        }

        // 1b. Check active lockdown countdown warnings
        if (isLockdownActive && lockEndTime > 0) {
            long remainingSec = (lockEndTime - effectiveNow) / 1000L;
            if (remainingSec > 0) {
                checkAndFireLockdownTimerWarning(remainingSec);
            }
        }

        // 1c. Synchronize LocalDnsVpnService strictly with active session enforcement (Option B: Session-Only Web Guard)
        boolean isConsequenceEnforcing = isConsequenceActive && (isHardcore || inOperatingHours);
        boolean isEnforcing = isLockdownActive || isConsequenceEnforcing;

        String webMode = prefs.getString("web_protection_mode", "accessibility");
        boolean isVpnMode = "dns_vpn".equalsIgnoreCase(webMode) || "dual_hybrid".equalsIgnoreCase(webMode);
        boolean shouldVpnRun = isVpnMode && isEnforcing;

        if (shouldVpnRun) {
            if (!LocalDnsVpnService.isRunning) {
                if (android.net.VpnService.prepare(this) == null) {
                    LocalDnsVpnService.startVpn(this);
                }
            }
        } else {
            if (LocalDnsVpnService.isRunning) {
                LocalDnsVpnService.stopVpn(this);
            }
        }

        // 2. Check schedules from schedules_json
        String schedulesJson = prefs.getString("schedules_json", null);
        if (schedulesJson != null && !schedulesJson.trim().isEmpty()) {
            try {
                JSONArray arr = new JSONArray(schedulesJson);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject s = arr.getJSONObject(i);
                    if (!s.optBoolean("isActive", true)) continue;

                    String timeStr = s.optString("activationTime", "");
                    if (!timeStr.contains(":")) continue;

                    String[] parts = timeStr.split(":");
                    int schedHour = Integer.parseInt(parts[0].trim());
                    int schedMin = Integer.parseInt(parts[1].trim());
                    int durationMinutes = s.optInt("durationMinutes", 25);
                    long durationMs = durationMinutes * 60L * 1000L;

                    Calendar schedCal = Calendar.getInstance();
                    schedCal.setTimeInMillis(effectiveNow);
                    schedCal.set(Calendar.HOUR_OF_DAY, schedHour);
                    schedCal.set(Calendar.MINUTE, schedMin);
                    schedCal.set(Calendar.SECOND, 0);
                    schedCal.set(Calendar.MILLISECOND, 0);

                    long schedStartTime = schedCal.getTimeInMillis();
                    long schedEndTime = schedStartTime + durationMs;

                    // A) Check if schedule should start RIGHT NOW (today's window or midnight cross)
                    boolean inWindow = false;
                    long windowEnd = 0;
                    if (effectiveNow >= schedStartTime && effectiveNow < schedEndTime) {
                        inWindow = true;
                        windowEnd = schedEndTime;
                    } else {
                        long yesterdayStart = schedStartTime - (24L * 3600L * 1000L);
                        long yesterdayEnd = yesterdayStart + durationMs;
                        if (effectiveNow >= yesterdayStart && effectiveNow < yesterdayEnd) {
                            inWindow = true;
                            windowEnd = yesterdayEnd;
                        }
                    }

                    if (inWindow && !isLockdownActive) {
                        long lastCompletedEnd = prefs.getLong("last_completed_window_end_" + s.optString("id", ""), 0L);
                        if (windowEnd <= lastCompletedEnd) {
                            // This schedule session was already completed or ended! Do NOT restart lockdown!
                            continue;
                        }

                        Log.i(TAG, "Ticker: schedule " + s.optString("id") + " starting NOW! (windowEnd=" + windowEnd + ")");
                        prefs.edit()
                                .putBoolean("lockdown_active", true)
                                .putLong("lock_end_time", windowEnd)
                                .putString("active_schedule_id", s.optString("id", ""))
                                .apply();
                        AlarmReceiver.scheduleLockEndAlarm(this, windowEnd, s.optString("id", ""));
                        AlarmReceiver.showNotificationStatic(
                                this,
                                AlarmReceiver.NOTIF_ID_STATUS,
                                AlarmReceiver.CHANNEL_ID_STATUS,
                                "🔒 QIEZKA Lockdown Engaged",
                                s.optString("title", "Study Session") + " is active (" + durationMinutes + " min). Distracting apps are blocked.",
                                Notification.PRIORITY_HIGH,
                                true
                        );
                        isLockdownActive = true;
                        FloatingOverlayService.startService(this, windowEnd, s.optString("title", "Study Session"));

                        // Immediately kick out if user is currently inside a blocked app
                        String activePkg = detectCurrentForegroundPackage();
                        if (activePkg != null && isPackageBlocked(activePkg)) {
                            enforceBlock(activePkg);
                        }
                    }

                    // B) Check advance countdown warnings before schedule start
                    long diffSec = (schedStartTime - effectiveNow) / 1000L;
                    if (diffSec > 0 && diffSec <= 1800) {
                        checkAndFireTickerWarning(s.optString("id", ""), diffSec);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Ticker schedule check error: " + e.getMessage());
            }
        }

        // 3. State Evaluation Gate
        isHardcore = "hardcore".equalsIgnoreCase(prefs.getString("operating_mode", "safemode"));
        isConsequenceEnforcing = isConsequenceActive && (isHardcore || inOperatingHours);
        isEnforcing = isLockdownActive || isConsequenceEnforcing;
        boolean hasSchedules = hasActiveSchedules();

        if (isConsequenceActive && !isHardcore && !inOperatingHours) {
            return; // Safemode Daytime (3am - 7pm): ALLOW_IDLE
        }

        if (!isEnforcing && !hasSchedules) {
            return; // Total System Idle: ALLOW_IDLE
        }

        // ── STAGE 1: Anti-Tamper & Task Killer Shield (Active Lockdown, Consequence, or Standby) ──
        if (interceptHomeLauncherChangeAttempt(null, null, null)) {
            return;
        }

        AccessibilityNodeInfo activeRoot = getRootInActiveWindow();
        if (activeRoot != null) {
            try {
                CharSequence p = activeRoot.getPackageName();
                if (p != null) {
                    String rootPkg = p.toString();
                    if (!rootPkg.equals(getPackageName())) {
                        if (interceptHomeLauncherChangeAttempt(null, activeRoot, rootPkg)) {
                            return;
                        }
                        String appLabel = null;
                        try {
                            PackageManager pm = getPackageManager();
                            if (pm != null) {
                                android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(rootPkg, 0);
                                CharSequence lbl = pm.getApplicationLabel(ai);
                                if (lbl != null) appLabel = lbl.toString();
                            }
                        } catch (Exception ignore) {}

                        if (AppClassifier.isSettingsOrDeviceManager(rootPkg, appLabel) || AppClassifier.isStage1Bloat(rootPkg, appLabel)) {
                            enforceBlock(rootPkg);
                            return;
                        }

                        // STAGE 2 & 3: Only when Lockdown or Consequence is Active
                        if (isEnforcing) {
                            if (isGeminiActive(rootPkg, null, activeRoot) && !isGeminiAllowed()) {
                                Log.w(TAG, "Ticker: detected Gemini active in foreground while AI policy is disabled");
                                enforceBlock("com.google.android.apps.bard");
                                return;
                            }
                            if (!isSystemOrLauncher(rootPkg)) {
                                if (isBrowserPackage(rootPkg)) {
                                    if (AppClassifier.isPortalSuperApp(rootPkg)) {
                                        String subCls = lastBrowserEventClass;
                                        if (AppClassifier.isDistractingSubActivity(rootPkg, subCls)) {
                                            remediateDistractingSubActivity(rootPkg, subCls);
                                            return;
                                        }
                                    }
                                    inspectBrowserWindow(activeRoot, rootPkg, true);
                                } else if (isPackageBlocked(rootPkg)) {
                                    enforceBlock(rootPkg);
                                    return;
                                }
                            }
                        }
                    }
                }
            } finally {
                activeRoot.recycle();
            }
        } else {
            // Fallback: When getRootInActiveWindow() is null (e.g. system transitions / secure surfaces)
            if (!isEnforcing) {
                return; // Standby: normal apps allowed
            }

            String currentForegroundPkg = detectCurrentForegroundPackage();
            if (currentForegroundPkg != null && !isSystemOrLauncher(currentForegroundPkg) && !currentForegroundPkg.equals(getPackageName())) {
                if (isBrowserPackage(currentForegroundPkg)) {
                    if (AppClassifier.isPortalSuperApp(currentForegroundPkg)) {
                        String subCls = lastBrowserEventClass;
                        if (AppClassifier.isDistractingSubActivity(currentForegroundPkg, subCls)) {
                            remediateDistractingSubActivity(currentForegroundPkg, subCls);
                            return;
                        }
                    }
                    AccessibilityNodeInfo browserRoot = getRootInActiveWindow();
                    if (browserRoot != null) {
                        try {
                            inspectBrowserWindow(browserRoot, currentForegroundPkg, true);
                        } finally {
                            browserRoot.recycle();
                        }
                    }
                } else if (isPackageBlocked(currentForegroundPkg)) {
                    enforceBlock(currentForegroundPkg);
                }
            }
        }
    }

    private void checkAndFireTickerWarning(String schedId, long diffSec) {
        String warnKey = null;
        String warnText = null;

        if (diffSec <= 1800 && diffSec >= 1795) {
            warnKey = schedId + "_30m";
            warnText = "⏰ Upcoming study session in 30 minutes! Wrap up your tasks.";
        } else if (diffSec <= 900 && diffSec >= 895) {
            warnKey = schedId + "_15m";
            warnText = "⏰ Study session starts in 15 minutes! Prepare your materials.";
        } else if (diffSec <= 300 && diffSec >= 295) {
            warnKey = schedId + "_5m";
            warnText = "⚠️ Lockdown in 5 minutes! Save your work on other apps.";
        } else if (diffSec <= 60 && diffSec >= 55) {
            warnKey = schedId + "_1m";
            warnText = "⚠️ Lockdown in 1 minute! QIEZKA is preparing to lock.";
        } else if (diffSec <= 30 && diffSec >= 25) {
            warnKey = schedId + "_30s";
            warnText = "🚨 Lockdown starting in 30 seconds! QIEZKA is engaging.";
        } else if (diffSec <= 10 && diffSec >= 5) {
            warnKey = schedId + "_10s";
            warnText = "🚨 Lockdown starting in 10 seconds!";
        }

        if (warnKey != null && !firedWarningKeys.contains(warnKey)) {
            firedWarningKeys.add(warnKey);
            Log.i(TAG, "Firing ticker advance warning: " + warnText);
            AlarmReceiver.showNotificationStatic(
                    this,
                    AlarmReceiver.NOTIF_ID_WARNING,
                    AlarmReceiver.CHANNEL_ID_ALERTS,
                    "⏰ QIEZKA Upcoming Lockdown",
                    warnText,
                    Notification.PRIORITY_HIGH,
                    false
            );
        }
    }

    private void checkAndFireLockdownTimerWarning(long remSec) {
        String warnKey = null;
        String warnText = null;

        if (remSec <= 1800 && remSec >= 1795) {
            warnKey = "lock_timer_30m";
            warnText = "⏳ 30 minutes remaining in active lockdown.";
        } else if (remSec <= 900 && remSec >= 895) {
            warnKey = "lock_timer_15m";
            warnText = "⏳ 15 minutes remaining in active lockdown.";
        } else if (remSec <= 300 && remSec >= 295) {
            warnKey = "lock_timer_5m";
            warnText = "⏳ 5 minutes remaining! Finish your homework submission.";
        } else if (remSec <= 60 && remSec >= 55) {
            warnKey = "lock_timer_1m";
            warnText = "⏳ 1 minute remaining before lockdown auto-unlocks.";
        } else if (remSec <= 30 && remSec >= 25) {
            warnKey = "lock_timer_30s";
            warnText = "⏳ 30 seconds remaining!";
        }

        if (warnKey != null && !firedWarningKeys.contains(warnKey)) {
            firedWarningKeys.add(warnKey);
            Log.i(TAG, "Firing lockdown timer warning: " + warnText);
            AlarmReceiver.showNotificationStatic(
                    this,
                    AlarmReceiver.NOTIF_ID_WARNING,
                    AlarmReceiver.CHANNEL_ID_ALERTS,
                    "⏳ QIEZKA Lock Timer",
                    warnText,
                    Notification.PRIORITY_HIGH,
                    false
            );
        }
    }

    private long lastBlockTimestamp = 0L;

    public void enforceBlock(String pkg) {
        if (pkg == null || pkg.equals(getPackageName()) || isSystemOrLauncher(pkg)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBlockTimestamp < 350L && pkg.equals(lastBlockedPackage)) {
            return; // Debounce rapid accessibility events from the same launch attempt
        }
        lastBlockTimestamp = now;
        lastBlockedPackage = pkg;

        Log.w(TAG, "enforceBlock on distracting app: " + pkg + " — directly bringing QIEZKA lock to front (no home redirection)");
        
        // Directly bring QIEZKA lock screen to front immediately
        launchLockOverlay();

        // Follow-up check to re-assert QIEZKA Lock overlay if another activity transitions in
        tickerHandler.postDelayed(() -> {
            String fg = detectCurrentForegroundPackage();
            if (fg != null && !fg.equals(getPackageName()) && !isSystemOrLauncher(fg)) {
                if (isPackageBlocked(fg)) {
                    launchLockOverlay();
                }
            }
        }, 250L);
    }

    public void onScheduleStartTriggered() {
        try {
            String activePkg = detectCurrentForegroundPackage();
            if (activePkg != null && !activePkg.equals(getPackageName()) && isPackageBlocked(activePkg)) {
                enforceBlock(activePkg);
            }
        } catch (Exception e) {
            Log.e(TAG, "onScheduleStartTriggered error: " + e.getMessage());
        }
    }

    public String detectCurrentForegroundPackage() {
        // 1. Z-Order Window Audit: Check if any active or visible application window belongs to a blocked package.
        // This defeats TWA/CustomTab host wrapping (e.g. id.kisskh.twa wrapped in com.android.chrome),
        // split-screen multi-window bypasses, and floating windows.
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null && !windows.isEmpty()) {
                for (AccessibilityWindowInfo w : windows) {
                    if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        AccessibilityNodeInfo root = w.getRoot();
                        if (root != null) {
                            try {
                                CharSequence p = root.getPackageName();
                                if (p != null) {
                                    String pkg = p.toString();
                                    if (isSystemOrLauncher(pkg) || pkg.equals(getPackageName()) || "com.android.systemui".equals(pkg)) {
                                        continue;
                                    }
                                    if ("com.google.android.googlequicksearchbox".equals(pkg) && isGeminiActive(pkg, null, root) && !isGeminiAllowed()) {
                                        lastForegroundPackage = "com.google.android.apps.bard";
                                        return "com.google.android.apps.bard";
                                    }
                                    if (!isSystemOrLauncher(pkg) && isPackageBlocked(pkg)) {
                                        lastForegroundPackage = pkg;
                                        return pkg;
                                    }
                                }
                            } finally {
                                root.recycle();
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            CharSequence wt = w.getTitle();
                            if (wt != null) {
                                String titleStr = wt.toString();
                                if (isHomeChangeInterceptorActive() && isHomeAppSelectionTitle(titleStr)) {
                                    Log.w(TAG, "🛡️ Sandboxed window matched Home App Selection title: " + titleStr);
                                    lastForegroundPackage = "com.android.permissioncontroller";
                                    return "com.android.permissioncontroller";
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {}

        // 2. Primary: Inspect active root window directly
        try {
            AccessibilityNodeInfo activeRoot = getRootInActiveWindow();
            if (activeRoot != null) {
                CharSequence p = activeRoot.getPackageName();
                activeRoot.recycle();
                if (p != null) {
                    String rootPkg = p.toString();
                    if ("com.google.android.googlequicksearchbox".equals(rootPkg) && isGeminiActive(rootPkg, null, null) && !isGeminiAllowed()) {
                        lastForegroundPackage = "com.google.android.apps.bard";
                        return "com.google.android.apps.bard";
                    }
                    if (!isSystemOrLauncher(rootPkg)) {
                        lastForegroundPackage = rootPkg;
                        return rootPkg;
                    }
                }
            }
        } catch (Exception ignore) {}

        // 3. Secondary: Inspect interactive application windows in Z-order
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null && !windows.isEmpty()) {
                // First pass: Active or Focused application window
                for (AccessibilityWindowInfo w : windows) {
                    if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION && (w.isActive() || w.isFocused())) {
                        AccessibilityNodeInfo root = w.getRoot();
                        if (root != null) {
                            try {
                                CharSequence p = root.getPackageName();
                                if (p != null) {
                                    String pkg = p.toString();
                                    if (isSystemOrLauncher(pkg) || pkg.equals(getPackageName()) || "com.android.systemui".equals(pkg)) {
                                        continue;
                                    }
                                    if ("com.google.android.googlequicksearchbox".equals(pkg) && isGeminiActive(pkg, null, root) && !isGeminiAllowed()) {
                                        lastForegroundPackage = "com.google.android.apps.bard";
                                        return "com.google.android.apps.bard";
                                    }
                                    if (!isSystemOrLauncher(pkg)) {
                                        lastForegroundPackage = pkg;
                                        return pkg;
                                    }
                                }
                            } finally {
                                root.recycle();
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            CharSequence wt = w.getTitle();
                            if (wt != null) {
                                String titleStr = wt.toString();
                                if (isHomeChangeInterceptorActive() && isHomeAppSelectionTitle(titleStr)) {
                                    Log.w(TAG, "🛡️ Sandboxed window matched Home App Selection title: " + titleStr);
                                    lastForegroundPackage = "com.android.permissioncontroller";
                                    return "com.android.permissioncontroller";
                                }
                            }
                        }
                    }
                }
                // Second pass: Top visible application window
                for (AccessibilityWindowInfo w : windows) {
                    if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        AccessibilityNodeInfo root = w.getRoot();
                        if (root != null) {
                            try {
                                CharSequence p = root.getPackageName();
                                if (p != null) {
                                    String pkg = p.toString();
                                    if ("com.google.android.googlequicksearchbox".equals(pkg) && isGeminiActive(pkg, null, root) && !isGeminiAllowed()) {
                                        lastForegroundPackage = "com.google.android.apps.bard";
                                        return "com.google.android.apps.bard";
                                    }
                                    if (!isSystemOrLauncher(pkg)) {
                                        lastForegroundPackage = pkg;
                                        return pkg;
                                    }
                                }
                            } finally {
                                root.recycle();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {}

        // 4. Fallback: last recorded non-system/non-launcher foreground package from events
        if (lastForegroundPackage != null && !isSystemOrLauncher(lastForegroundPackage)) {
            return lastForegroundPackage;
        }

        return lastForegroundPackage;
    }

    private void launchLockOverlay() {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                boolean isConsequence = prefs != null && prefs.getBoolean("consequence_active", false);
                launch.putExtra("route", isConsequence ? "logs" : "locked");
                startActivity(launch);
            }
        } catch (Exception e) {
            Log.e(TAG, "launchLockOverlay failed: " + e.getMessage());
        }
    }


    /**
     * Handles SystemUI events.
     * - Allows the notification shade to be pulled down.
     * - Collapses the Quick Settings (QS) tile panel immediately when expanded.
     */
    private void handleSystemUiEvent(AccessibilityEvent event) {
        CharSequence classNameChar = event.getClassName();
        if (classNameChar == null) return;
        String cls = classNameChar.toString().toLowerCase();

        // Collapse QS tile panel (but allow the plain notification shade)
        if (cls.contains("qs")
                || cls.contains("quicksetting")
                || cls.contains("brightnesscontroller")
                || cls.contains("tilerecord")
                || cls.contains("quickstatusbar")) {
            collapseQsPanel();
        }
    }

    private void collapseQsPanel() {
        try {
            Object statusBarService = getSystemService("statusbar");
            if (statusBarService != null) {
                Method collapseMethod = statusBarService.getClass().getMethod("collapsePanels");
                collapseMethod.setAccessible(true);
                collapseMethod.invoke(statusBarService);
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, "StatusBarManager reflection failed: " + e.getMessage());
        }
        // Fallback
        try {
            performGlobalAction(GLOBAL_ACTION_BACK);
        } catch (Exception e) {
            Log.e(TAG, "Fallback collapse failed: " + e.getMessage());
        }
    }

    private boolean checkAndAutoStartScheduledLock(SharedPreferences prefs) {
        String schedulesJson = prefs.getString("schedules_json", null);
        if (schedulesJson == null || schedulesJson.trim().isEmpty()) return false;

        try {
            JSONArray arr = new JSONArray(schedulesJson);
            long timeOffset = prefs.getLong("time_offset", 0L);
            long effectiveNow = System.currentTimeMillis() + timeOffset;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject s = arr.getJSONObject(i);
                if (!s.optBoolean("isActive", true)) continue;

                String timeStr = s.optString("activationTime", "");
                if (!timeStr.contains(":")) continue;

                String[] parts = timeStr.split(":");
                int schedHour = Integer.parseInt(parts[0].trim());
                int schedMin = Integer.parseInt(parts[1].trim());
                int durationMinutes = s.optInt("durationMinutes", 25);
                long durationMs = durationMinutes * 60L * 1000L;

                Calendar schedCal = Calendar.getInstance();
                schedCal.setTimeInMillis(effectiveNow);
                schedCal.set(Calendar.HOUR_OF_DAY, schedHour);
                schedCal.set(Calendar.MINUTE, schedMin);
                schedCal.set(Calendar.SECOND, 0);
                schedCal.set(Calendar.MILLISECOND, 0);

                long schedStartTime = schedCal.getTimeInMillis();
                long schedEndTime = schedStartTime + durationMs;

                // Check today's window
                if (effectiveNow >= schedStartTime && effectiveNow < schedEndTime) {
                    long lastCompletedEnd = prefs.getLong("last_completed_window_end_" + s.optString("id", ""), 0L);
                    if (schedEndTime <= lastCompletedEnd) {
                        continue;
                    }
                    prefs.edit()
                            .putBoolean("lockdown_active", true)
                            .putLong("lock_end_time", schedEndTime)
                            .putString("active_schedule_id", s.optString("id", ""))
                            .apply();
                    AlarmReceiver.scheduleLockEndAlarm(this, schedEndTime, s.optString("id", ""));
                    AlarmReceiver.createNotificationChannels(this);
                    AlarmReceiver.showNotificationStatic(
                            this,
                            AlarmReceiver.NOTIF_ID_STATUS,
                            AlarmReceiver.CHANNEL_ID_STATUS,
                            "🔒 QIEZKA Lockdown Engaged",
                            s.optString("title", "Study Session") + " is active (" + durationMinutes + " min). Distracting apps are blocked.",
                            Notification.PRIORITY_HIGH,
                            true
                    );
                    FloatingOverlayService.startService(this, schedEndTime, s.optString("title", "Study Session"));
                    Log.i(TAG, "Native auto-start triggered for schedule: " + s.optString("id", "") + ", lockEnd=" + schedEndTime);
                    return true;
                }

                // Check yesterday's window (crossed midnight)
                long yesterdayStart = schedStartTime - (24L * 3600L * 1000L);
                long yesterdayEnd = yesterdayStart + durationMs;
                if (effectiveNow >= yesterdayStart && effectiveNow < yesterdayEnd) {
                    long lastCompletedEnd = prefs.getLong("last_completed_window_end_" + s.optString("id", ""), 0L);
                    if (yesterdayEnd <= lastCompletedEnd) {
                        continue;
                    }
                    prefs.edit()
                            .putBoolean("lockdown_active", true)
                            .putLong("lock_end_time", yesterdayEnd)
                            .putString("active_schedule_id", s.optString("id", ""))
                            .apply();
                    AlarmReceiver.scheduleLockEndAlarm(this, yesterdayEnd, s.optString("id", ""));
                    AlarmReceiver.createNotificationChannels(this);
                    AlarmReceiver.showNotificationStatic(
                            this,
                            AlarmReceiver.NOTIF_ID_STATUS,
                            AlarmReceiver.CHANNEL_ID_STATUS,
                            "🔒 QIEZKA Lockdown Engaged",
                            s.optString("title", "Study Session") + " is active (" + durationMinutes + " min). Distracting apps are blocked.",
                            Notification.PRIORITY_HIGH,
                            true
                    );
                    FloatingOverlayService.startService(this, yesterdayEnd, s.optString("title", "Study Session"));
                    Log.i(TAG, "Native auto-start (midnight-cross) triggered for schedule: " + s.optString("id", ""));
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "checkAndAutoStartScheduledLock error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Ultra-Fast Path: Evaluates whether an AccessibilityEvent represents an attempt to change
     * the default home launcher or interact with a launcher selection role dialog (0ms latency).
     */
    private boolean isFastHomeRoleEvent(AccessibilityEvent event) {
        if (event == null) return false;

        CharSequence pkgChar = event.getPackageName();
        String pkgStr = pkgChar != null ? pkgChar.toString().toLowerCase(Locale.US) : "";

        CharSequence clsChar = event.getClassName();
        String clsStr = clsChar != null ? clsChar.toString().toLowerCase(Locale.US) : "";

        // 1. Direct Activity class name match (sub-microsecond memory check, 100% language-independent)
        // These are exact system activity classes dedicated to Default Home App / Role selection across AOSP, Pixel, and OEM ROMs
        if (clsStr.contains("requestroleactivity") || clsStr.contains("defaultappactivity") ||
            clsStr.contains("homesettingsactivity") || clsStr.contains("defaulthomepicker") ||
            clsStr.contains("rolesearchactivity") || clsStr.contains("specialappaccessactivity")) {
            return true;
        }

        // 2. Fast-click defense: User taps any view inside permissioncontroller while role UI is active
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED && pkgStr.contains("permissioncontroller")) {
            return true;
        }

        // 3. Proactive tap interception: User taps "Not set as default" or "Nova is not default launcher" in launcher settings
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            List<CharSequence> texts = event.getText();
            if (texts != null) {
                for (CharSequence t : texts) {
                    if (t != null) {
                        String txt = t.toString().toLowerCase(Locale.US);
                        if (txt.contains("not set as default") || 
                            txt.contains("default launcher") ||
                            (txt.contains("set as default") && txt.contains("home"))) {
                            return true;
                        }
                    }
                }
            }
            CharSequence cd = event.getContentDescription();
            if (cd != null) {
                String cStr = cd.toString().toLowerCase(Locale.US);
                if (cStr.contains("not set as default") || cStr.contains("default launcher")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Stage 1 Master Veto Gate: Home Launcher Selection Dialog -> DISMISS & EVICT TO QIEZKA
     */
    private boolean interceptHomeLauncherChangeAttempt(AccessibilityEvent event, AccessibilityNodeInfo providedRoot, String pkg) {
        // Master Gate: ONLY activate interceptor if SIREN is default launcher AND a session/schedule is active
        if (!isHomeChangeInterceptorActive()) {
            return false;
        }

        String lowerPkg = pkg != null ? pkg.toLowerCase(Locale.US) : "";
        if (lowerPkg.isEmpty() && event != null && event.getPackageName() != null) {
            lowerPkg = event.getPackageName().toString().toLowerCase(Locale.US);
        }

        // PRE-FILTER: Never intercept our own app, SystemUI, or any installed launcher!
        if (isLauncherApp(this, lowerPkg) || lowerPkg.equals(getPackageName()) || lowerPkg.equals("com.android.systemui")) {
            return false;
        }

        String classStr = "";
        if (event != null && event.getClassName() != null) {
            classStr = event.getClassName().toString().toLowerCase(Locale.US);
        } else if (providedRoot != null && providedRoot.getClassName() != null) {
            classStr = providedRoot.getClassName().toString().toLowerCase(Locale.US);
        }

        // 1. Fast Event-level class checks (0ms - instant memory evaluation, 100% language-independent)
        if (classStr.contains("defaultappactivity") || classStr.contains("requestroleactivity") || 
            classStr.contains("homesettingsactivity") || classStr.contains("defaulthomepicker") ||
            classStr.contains("rolesearchactivity") || classStr.contains("specialappaccessactivity")) {
            AccessibilityNodeInfo rootToDismiss = providedRoot != null ? providedRoot : (event != null ? event.getSource() : null);
            evictHomeLauncherChange(rootToDismiss);
            return true;
        }

        // 2. Window Audit via getWindows() (Essential for Android 14/15/16 sandboxed permissioncontroller modal dialogs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                List<AccessibilityWindowInfo> windows = getWindows();
                if (windows != null) {
                    for (AccessibilityWindowInfo w : windows) {
                        if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION && (w.isActive() || w.isFocused())) {
                            AccessibilityNodeInfo wRoot = w.getRoot();
                            if (wRoot != null) {
                                try {
                                    CharSequence wp = wRoot.getPackageName();
                                    if (wp != null) {
                                        String wpStr = wp.toString().toLowerCase(Locale.US);
                                        if (isLauncherApp(this, wpStr) || wpStr.equals(getPackageName()) || wpStr.equals("com.android.systemui")) {
                                            continue;
                                        }
                                        if (isHomeAppSelectionPackageOrRoot(wpStr, wRoot)) {
                                            evictHomeLauncherChange(wRoot);
                                            return true;
                                        }
                                    }
                                    CharSequence wt = w.getTitle();
                                    if (wt != null && isHomeAppSelectionTitle(wt.toString())) {
                                        evictHomeLauncherChange(wRoot);
                                        return true;
                                    }
                                } finally {
                                    wRoot.recycle();
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignore) {}
        }

        AccessibilityNodeInfo root = providedRoot;
        boolean needsRecycle = false;
        try {
            if (root == null) {
                root = getRootInActiveWindow();
                if (root != null) needsRecycle = true;
            }
            if (root == null && event != null) {
                root = event.getSource();
            }

            if (root == null) return false;

            CharSequence rootPkg = root.getPackageName();
            String effectivePkg = rootPkg != null ? rootPkg.toString() : lowerPkg;
            if (isLauncherApp(this, effectivePkg) || effectivePkg.equals(getPackageName()) || effectivePkg.equals("com.android.systemui")) {
                return false;
            }

            if (isHomeAppSelectionPackageOrRoot(effectivePkg, root)) {
                evictHomeLauncherChange(root);
                return true;
            }

            // Third party launchers prompting to set default launcher
            if (effectivePkg.toLowerCase(Locale.US).contains("launcher") && !isLauncherApp(this, effectivePkg)) {
                List<AccessibilityNodeInfo> defaultHomeNodes = root.findAccessibilityNodeInfosByText("Default");
                if (defaultHomeNodes != null && !defaultHomeNodes.isEmpty()) {
                    List<AccessibilityNodeInfo> launcherNodes = root.findAccessibilityNodeInfosByText("Launcher");
                    if (launcherNodes != null && !launcherNodes.isEmpty()) {
                        evictHomeLauncherChange(root);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking home launcher change attempt: " + e.getMessage());
        } finally {
            if (root != null && needsRecycle) {
                try {
                    root.recycle();
                } catch (Exception ignore) {}
            }
        }
        return false;
    }

    private boolean isHomeAppSelectionPackageOrRoot(String pkg, AccessibilityNodeInfo root) {
        if (pkg == null || root == null) return false;
        String lower = pkg.toLowerCase(Locale.US);
        if (!lower.contains("permissioncontroller") && !lower.contains("settings")) {
            return false;
        }

        try {
            // 1. Check exact activity class name on root if available (language-independent)
            CharSequence rootClass = root.getClassName();
            if (rootClass != null) {
                String rc = rootClass.toString().toLowerCase(Locale.US);
                if (rc.contains("requestroleactivity") || rc.contains("defaultappactivity") ||
                    rc.contains("homesettingsactivity") || rc.contains("defaulthomepicker")) {
                    return true;
                }
            }

            // 2. Check title text in permissioncontroller role UI
            List<AccessibilityNodeInfo> titleNodes = root.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/title");
            if (titleNodes != null && !titleNodes.isEmpty()) {
                for (AccessibilityNodeInfo t : titleNodes) {
                    CharSequence txt = t.getText();
                    if (txt != null && isHomeAppSelectionTitle(txt.toString())) {
                        return true;
                    }
                }
            }

            // 3. Check candidate items in permissioncontroller list against installed Home Launchers
            // (100% language-independent: detects launcher packages and brand names in any locale)
            List<AccessibilityNodeInfo> listNodes = root.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/list");
            if (listNodes != null && !listNodes.isEmpty()) {
                List<AccessibilityNodeInfo> titles = root.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/title");
                if (titles != null && !titles.isEmpty()) {
                    for (AccessibilityNodeInfo t : titles) {
                        CharSequence txt = t.getText();
                        if (txt != null && isInstalledLauncherCandidate(txt.toString())) {
                            return true;
                        }
                    }
                }
                List<AccessibilityNodeInfo> btn1 = root.findAccessibilityNodeInfosByViewId("android:id/button1");
                if (btn1 != null && !btn1.isEmpty()) {
                    for (AccessibilityNodeInfo b : btn1) {
                        CharSequence bt = b.getText();
                        if (bt != null && bt.toString().toLowerCase(Locale.US).contains("default")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignore) {}
        return false;
    }

    private boolean clickCancelOnNode(AccessibilityNodeInfo root) {
        if (root == null) return false;
        try {
            List<AccessibilityNodeInfo> cancelButtons = root.findAccessibilityNodeInfosByViewId("android:id/button2");
            if (cancelButtons != null && !cancelButtons.isEmpty()) {
                for (AccessibilityNodeInfo btn : cancelButtons) {
                    if (btn != null && btn.isClickable() && btn.isEnabled()) {
                        btn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.i(TAG, "Dismissed home launcher dialog by clicking Cancel button (android:id/button2)");
                        return true;
                    }
                }
            }
            List<AccessibilityNodeInfo> textCancel = root.findAccessibilityNodeInfosByText("Cancel");
            if (textCancel != null && !textCancel.isEmpty()) {
                for (AccessibilityNodeInfo btn : textCancel) {
                    if (btn != null && btn.isClickable() && btn.isEnabled()) {
                        btn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.i(TAG, "Dismissed home launcher dialog by clicking text 'Cancel'");
                        return true;
                    }
                }
            }
        } catch (Exception ignore) {}
        return false;
    }

    private void evictHomeLauncherChange() {
        evictHomeLauncherChange(null);
    }

    private void evictHomeLauncherChange(AccessibilityNodeInfo dialogRoot) {
        Log.w(TAG, "🛡️ Intercepted Default Home App / Launcher change attempt! Dismissing dialog and evicting to QIEZKA.");
        
        // 1. Instant native Back action to dismiss dialog immediately (0ms)
        performGlobalAction(GLOBAL_ACTION_BACK);

        // 2. Programmatically click "Cancel" (android:id/button2) on dialogRoot
        clickCancelOnNode(dialogRoot);

        // 3. Fallback: query active window root if dialogRoot was null
        if (dialogRoot == null) {
            try {
                AccessibilityNodeInfo activeRoot = getRootInActiveWindow();
                if (activeRoot != null) {
                    clickCancelOnNode(activeRoot);
                    activeRoot.recycle();
                }
            } catch (Exception ignore) {}
        }

        // 4. Rapid-burst retries (25ms, 60ms, 120ms) to defeat fast clicks if window was inflating
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                AccessibilityNodeInfo r = getRootInActiveWindow();
                if (r != null) {
                    clickCancelOnNode(r);
                    r.recycle();
                }
                performGlobalAction(GLOBAL_ACTION_BACK);
            } catch (Exception ignore) {}
        }, 25L);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                performGlobalAction(GLOBAL_ACTION_BACK);
            } catch (Exception ignore) {}
        }, 60L);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                performGlobalAction(GLOBAL_ACTION_BACK);
            } catch (Exception ignore) {}
        }, 120L);

        // 5. Anti-Tamper Toast feedback
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(getApplicationContext(),
                "🛡️ Changing default home launcher is restricted by QIEZKA",
                Toast.LENGTH_SHORT).show();
        });

        // 6. Re-assert QIEZKA Lock overlay
        launchLockOverlay();
    }

    public static boolean isInstalledLauncherCandidate(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String lower = text.trim().toLowerCase(Locale.US);
        for (String label : dynamicLauncherLabels) {
            if (!label.isEmpty() && (lower.equals(label) || lower.contains(label) || label.contains(lower))) {
                return true;
            }
        }
        for (String pkg : dynamicLauncherPackages) {
            if (!pkg.isEmpty() && lower.contains(pkg.toLowerCase(Locale.US))) {
                return true;
            }
        }
        return lower.contains("launcher") || lower.contains("lawnchair") || lower.contains("trebuchet") ||
               lower.contains("pixel launcher") || lower.contains("one ui") || lower.contains("homeproxy");
    }

    public static boolean isHomeAppSelectionTitle(String title) {
        if (title == null) return false;
        String lower = title.trim().toLowerCase(Locale.US);
        // 1. Activity class name signatures (100% language-independent)
        if (lower.contains("requestroleactivity") || lower.contains("defaultappactivity") ||
            lower.contains("homesettingsactivity") || lower.contains("defaulthomepicker") ||
            lower.contains("rolesearchactivity") || lower.contains("specialappaccessactivity")) {
            return true;
        }
        // 2. Generic home role tokens
        return lower.contains("default home") || lower.contains("home app") ||
               (lower.contains("default") && lower.contains("home")) ||
               lower.equals("home launcher");
    }



    @Override
    public void onInterrupt() {
        // Required override
    }
}
