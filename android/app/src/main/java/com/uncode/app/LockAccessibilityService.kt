package com.uncode.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.provider.Browser
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Method
import java.util.Calendar
import java.util.Collections
import java.util.HashSet
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

open class LockAccessibilityService : AccessibilityService() {

    private var lastBrowserEventClass: String? = null
    private var lastActiveActivityClass: String? = null
    private var prefs: SharedPreferences? = null

    private var lastForegroundPackage: String? = null
    private var lastBlockedPackage: String? = null
    private val firedWarningKeys: MutableSet<String> = Collections.synchronizedSet(HashSet<String>())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var auditThread: HandlerThread? = null
    private var auditHandler: Handler? = null
    private val tickerHandler = Handler(Looper.getMainLooper())

    private val tickerRunnable = object : Runnable {
        override fun run() {
            try {
                onTickerTick()
            } catch (e: Exception) {
                Log.e(TAG, "Ticker error: ${e.message}")
            } finally {
                tickerHandler.postDelayed(this, 1000L)
            }
        }
    }

    private val auditStep0Token = Any()

    private var lastBrowserToastTime: Long = 0L
    private var lastBrowserRemediationTime: Long = 0L
    private var lastRemediatedUrl: String? = null
    private var consecutiveBlockedUrlHits: Int = 0

    private var lastSubActivityRemediationTime: Long = 0L
    private var consecutiveSubActivityHits: Int = 0
    private var lastRemediatedSubActivity: String? = null

    private val packageManifestHostCache: MutableMap<String, String> = ConcurrentHashMap()
    private var lastBlockTimestamp: Long = 0L
    private var lastPipRemediationTime: Long = 0L

    companion object {
        private const val TAG = "LockAccessService"
        const val PREFS_NAME = "uncode_lock"
        private const val SERVICE_CHECK_INTERVAL_MS = 1000L

        private const val BROWSER_REMEDIATION_COOLDOWN_MS = 2200L
        private const val BROWSER_REMEDIATION_TIMEOUT_MS = 6500L
        private const val BROWSER_TOAST_COOLDOWN_MS = 3000L

        private val PROGRESSIVE_DOM_POLL_DELAYS = longArrayOf(300L, 700L, 1200L, 2000L)

        @JvmField
        @Volatile
        var instance: LockAccessibilityService? = null

        @JvmStatic
        fun getInstance(): LockAccessibilityService? = instance

        @JvmField
        val dynamicLauncherPackages: MutableSet<String> = KnownSafe.dynamicLauncherPackages
        @JvmField
        val dynamicLauncherLabels: MutableSet<String> = KnownSafe.dynamicLauncherLabels

        @JvmStatic
        fun isSimOrCarrierService(pkg: String?, appLabel: String?): Boolean {
            return AppClassifier.isSimOrCarrierService(pkg, appLabel)
        }

        @JvmStatic
        fun isSimOrCarrierService(pkg: String?): Boolean {
            return AppClassifier.isSimOrCarrierService(pkg)
        }

        @JvmStatic
        fun isLauncherApp(context: Context?, pkg: String?): Boolean {
            return KnownSafe.isLauncherApp(context, pkg)
        }

        @JvmStatic
        fun isKeyboardPackage(context: Context?, pkg: String?): Boolean {
            return KnownSafe.isKeyboardPackage(context, pkg)
        }

        @JvmStatic
        fun isInOperatingHours(context: Context?, effectiveNow: Long): Boolean {
            if (context != null) {
                val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val mode = p.getString("operating_mode", "safemode")
                if ("hardcore".equals(mode, ignoreCase = true)) {
                    return true
                }
            }
            return isClockInOperatingHours(effectiveNow)
        }

        @JvmStatic
        fun isInOperatingHours(effectiveNow: Long): Boolean {
            val inst = instance
            if (inst != null && inst.prefs != null) {
                val mode = inst.prefs?.getString("operating_mode", "safemode")
                if ("hardcore".equals(mode, ignoreCase = true)) {
                    return true
                }
            }
            return isClockInOperatingHours(effectiveNow)
        }

        @JvmStatic
        private fun isClockInOperatingHours(effectiveNow: Long): Boolean {
            val cal = Calendar.getInstance()
            cal.timeInMillis = effectiveNow
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val totalMins = hour * 60 + minute
            // Operating window: 19:00 (1140 mins) to 23:59 (1439 mins) OR 00:00 (0 mins) to 02:59 (179 mins)
            return (totalMins >= 1140 || totalMins < 180)
        }

        @JvmStatic
        fun hasActiveSchedules(preferences: SharedPreferences?): Boolean {
            if (preferences == null) return false
            val schedulesJson = preferences.getString("schedules_json", null)
            if (schedulesJson.isNullOrBlank()) return false
            if (schedulesJson.contains("\"isActive\":true") || schedulesJson.contains("\"isActive\": true")) {
                return true
            }
            try {
                val arr = JSONArray(schedulesJson)
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    if (s.optBoolean("isActive", true)) {
                        return true
                    }
                }
            } catch (ignored: Exception) {}
            return false
        }

        private var sLastSirenCheckTime = 0L
        private var sCachedIsSirenDefault = false

        @JvmStatic
        fun isSirenDefaultLauncher(context: Context?): Boolean {
            val now = System.currentTimeMillis()
            if (sLastSirenCheckTime > now + 60000L) {
                return sCachedIsSirenDefault
            }
            if (context == null) return false
            if (now - sLastSirenCheckTime < 1000L) {
                return sCachedIsSirenDefault
            }
            try {
                // 1. Android 10+ (API 29+) RoleManager authoritative check via reflection
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val roleManager = context.getSystemService(RoleManager::class.java)
                        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                            val method = roleManager.javaClass.getMethod("getRoleHolders", String::class.java)
                            @Suppress("UNCHECKED_CAST")
                            val holders = method.invoke(roleManager, RoleManager.ROLE_HOME) as? List<String>
                            if (!holders.isNullOrEmpty()) {
                                sCachedIsSirenDefault = holders.contains(LauncherStateManager.SIREN_PACKAGE)
                                sLastSirenCheckTime = now
                                return sCachedIsSirenDefault
                            }
                        }
                    } catch (ignore: Throwable) {}
                }

                // 2. Legacy PackageManager resolveActivity fallback
                val pm = context.packageManager
                if (pm != null) {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                    }
                    val defaultHome = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                    if (defaultHome?.activityInfo != null) {
                        val pkg = defaultHome.activityInfo.packageName
                        if (pkg != null && pkg != "android") {
                            sCachedIsSirenDefault = LauncherStateManager.SIREN_PACKAGE == pkg
                            sLastSirenCheckTime = now
                            return sCachedIsSirenDefault
                        }
                    }
                    val resolvedHome = pm.resolveActivity(homeIntent, 0)
                    if (resolvedHome?.activityInfo != null) {
                        val pkg = resolvedHome.activityInfo.packageName
                        if (pkg != null && pkg != "android") {
                            sCachedIsSirenDefault = LauncherStateManager.SIREN_PACKAGE == pkg
                            sLastSirenCheckTime = now
                            return sCachedIsSirenDefault
                        }
                    }
                }
            } catch (ignore: Exception) {}
            sCachedIsSirenDefault = false
            sLastSirenCheckTime = now
            return false
        }

        @JvmStatic
        fun setCachedIsSirenDefaultForTesting(value: Boolean?) {
            if (value == null) {
                sLastSirenCheckTime = 0L
                sCachedIsSirenDefault = false
            } else {
                sCachedIsSirenDefault = value
                sLastSirenCheckTime = System.currentTimeMillis() + 3600000L
            }
        }

        @JvmStatic
        fun isHomeChangeInterceptorActive(context: Context?, preferences: SharedPreferences?): Boolean {
            // Condition 1: SIREN must be detected as the OS default home launcher
            if (!isSirenDefaultLauncher(context)) {
                return false
            }

            // Condition 2: Active schedule, lockdown, or consequence
            if (preferences == null) return false

            val isLockdownActive = preferences.getBoolean("lockdown_active", false)
            val isConsequenceActive = preferences.getBoolean("consequence_active", false)

            val timeOffset = preferences.getLong("time_offset", preferences.getLong("simulated_time_offset", 0L))
            val effectiveNow = System.currentTimeMillis() + timeOffset
            val inOperatingHours = isClockInOperatingHours(effectiveNow)
            val isHardcore = "hardcore".equals(preferences.getString("operating_mode", "safemode"), ignoreCase = true)
            val isConsequenceEnforcing = isConsequenceActive && (isHardcore || inOperatingHours)

            val hasSchedules = hasActiveSchedules(preferences)

            return isLockdownActive || isConsequenceEnforcing || hasSchedules
        }

        @JvmStatic
        fun isBrowserPackage(pkg: String?): Boolean {
            return AppClassifier.isBrowserPackage(pkg)
        }

        @JvmStatic
        fun isAddressBarNode(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            val resId = node.viewIdResourceName
            if (resId != null) {
                val lower = resId.lowercase(Locale.US)
                return lower.contains("url") || lower.contains("location") ||
                       lower.contains("search") || lower.contains("toolbar") ||
                       lower.contains("address") || lower.contains("omnibox") ||
                       lower.contains("line_1")
            }
            return false
        }

        @JvmStatic
        fun isBrowserNewTabPage(root: AccessibilityNodeInfo?): Boolean {
            if (root == null) return false
            try {
                // 1. Chrome / Chromium family New Tab Page containers & elements
                val ntpViewIds = arrayOf(
                    "com.android.chrome:id/ntp_content",
                    "com.android.chrome:id/search_box_text",
                    "com.android.chrome:id/search_provider_logo",
                    "com.android.chrome:id/mv_tiles_layout",
                    "org.chromium.chrome:id/ntp_content",
                    "org.chromium.chrome:id/search_box_text",
                    "org.chromium.chrome:id/search_provider_logo",
                    "com.brave.browser:id/ntp_content",
                    "com.kiwibrowser.browser:id/ntp_content",
                    // Samsung Internet
                    "com.sec.android.app.sbrowser:id/quickaccess_layout",
                    "com.sec.android.app.sbrowser:id/new_tab_page",
                    // Firefox
                    "org.mozilla.firefox:id/home_layout",
                    "org.mozilla.firefox:id/top_sites_container",
                    // Microsoft Edge
                    "com.microsoft.emmx:id/ntp",
                    "com.microsoft.emmx:id/new_tab_page"
                )
                for (id in ntpViewIds) {
                    val nodes = root.findAccessibilityNodeInfosByViewId(id)
                    if (!nodes.isNullOrEmpty()) {
                        recycleNodes(nodes)
                        return true
                    }
                }
            } catch (ignore: Exception) {}
            return false
        }

        @JvmStatic
        fun isValidUrlCandidate(valStr: String?): Boolean {
            if (valStr == null) return false
            val clean = valStr.trim()
            if (clean.isEmpty()) return false
            // Fast reject: Unencoded spaces indicate placeholder hint text or typed search queries
            if (clean.contains(" ")) return false
            // Fast reject: Common browser empty tab placeholder words without spaces
            val lower = clean.lowercase(Locale.US)
            if (lower == "search" || lower == "search..." ||
                lower == "about:blank" || lower.startsWith("chrome://newtab")) {
                return false
            }
            // Must look like a real host or URL: contains dot, or protocol, or path slash
            return clean.contains(".") || lower.startsWith("http://") || lower.startsWith("https://") || clean.contains("/")
        }

        @JvmStatic
        fun isLinkContextMenu(root: AccessibilityNodeInfo?, pkg: String?): Boolean {
            if (root == null) return false
            try {
                val resId = root.viewIdResourceName
                if (resId != null) {
                    val idStr = resId.toString()
                    if (idStr.endsWith(":id/context_menu_layout") || idStr.endsWith(":id/context_menu_frame") ||
                        idStr.endsWith(":id/context_menu_list_view") || idStr.endsWith(":id/menu_header_url") ||
                        idStr.endsWith(":id/menu_header_alt_text") || idStr.endsWith(":id/title_and_url") ||
                        idStr.endsWith(":id/context_menu_dialog") || idStr.endsWith(":id/link_context_menu") ||
                        idStr.endsWith(":id/mozac_browser_menu_list")) {
                        return true
                    }
                }

                if (pkg != null) {
                    val pkgIds = arrayOf(
                        "$pkg:id/context_menu_layout",
                        "$pkg:id/menu_header_url",
                        "$pkg:id/context_menu_frame",
                        "$pkg:id/context_menu_dialog",
                        "$pkg:id/link_context_menu"
                    )
                    for (pId in pkgIds) {
                        val nodes = root.findAccessibilityNodeInfosByViewId(pId)
                        if (!nodes.isNullOrEmpty()) {
                            recycleNodes(nodes)
                            return true
                        }
                    }
                }

                val chromeIds = arrayOf(
                    "com.android.chrome:id/context_menu_layout",
                    "com.android.chrome:id/menu_header_url",
                    "org.chromium.chrome:id/context_menu_layout",
                    "org.chromium.chrome:id/menu_header_url"
                )
                for (cId in chromeIds) {
                    val nodes = root.findAccessibilityNodeInfosByViewId(cId)
                    if (!nodes.isNullOrEmpty()) {
                        recycleNodes(nodes)
                        return true
                    }
                }

                // Universal semantic action check for search and browser link menus
                var openNodes = root.findAccessibilityNodeInfosByText("Open in new tab")
                if (openNodes.isNullOrEmpty()) {
                    openNodes = root.findAccessibilityNodeInfosByText("Open in browser")
                }
                if (!openNodes.isNullOrEmpty()) {
                    recycleNodes(openNodes)
                    val actionTexts = arrayOf("Copy link address", "Copy link text", "Copy link", "Share link")
                    for (action in actionTexts) {
                        val actionNodes = root.findAccessibilityNodeInfosByText(action)
                        if (!actionNodes.isNullOrEmpty()) {
                            recycleNodes(actionNodes)
                            return true
                        }
                    }
                }
            } catch (ignore: Exception) {}
            return false
        }

        @JvmStatic
        fun isInstalledLauncherCandidate(text: String?): Boolean {
            if (text.isNullOrBlank()) return false
            if (dynamicLauncherLabels.isEmpty() && instance != null) {
                instance?.refreshDynamicExemptPackages()
            }
            val lower = text.trim().lowercase(Locale.US)
            for (label in dynamicLauncherLabels) {
                if (label.isNotEmpty() && (lower == label || lower.contains(label) || label.contains(lower))) {
                    return true
                }
            }
            for (pkg in dynamicLauncherPackages) {
                if (pkg.isNotEmpty() && lower.contains(pkg.lowercase(Locale.US))) {
                    return true
                }
            }
            return lower.contains("launcher") || lower.contains("lawnchair") || lower.contains("trebuchet") ||
                   lower.contains("pixel launcher") || lower.contains("one ui") || lower.contains("homeproxy")
        }

        @JvmStatic
        fun isHomeAppSelectionTitle(title: String?): Boolean {
            if (title == null) return false
            val lower = title.trim().lowercase(Locale.US)
            // 1. Activity class name signatures (100% language-independent)
            if (lower.contains("requestroleactivity") || lower.contains("defaultappactivity") ||
                lower.contains("homesettingsactivity") || lower.contains("defaulthomepicker") ||
                lower.contains("rolesearchactivity") || lower.contains("specialappaccessactivity")) {
                return true
            }
            // 2. Generic home role tokens
            return lower.contains("default home") || lower.contains("home app") ||
                   (lower.contains("default") && lower.contains("home")) ||
                   lower == "home launcher"
        }

        private fun recycleNodes(nodes: List<AccessibilityNodeInfo?>?) {
            if (nodes != null) {
                for (n in nodes) {
                    if (n != null) {
                        try { n.recycle() } catch (ignore: Exception) {}
                    }
                }
            }
        }

        private fun extractHost(rawUrl: String?): String {
            if (rawUrl == null) return ""
            var clean = rawUrl.trim().lowercase(Locale.US)
            if (clean.startsWith("https://")) clean = clean.substring(8)
            else if (clean.startsWith("http://")) clean = clean.substring(7)
            val slash = clean.indexOf('/')
            if (slash != -1) clean = clean.substring(0, slash)
            val colon = clean.indexOf(':')
            if (colon != -1) clean = clean.substring(0, colon)
            if (clean.startsWith("www.")) clean = clean.substring(4)
            return clean
        }

        private fun isSearchEngineHost(host: String?): Boolean {
            if (host.isNullOrEmpty()) return false
            if (host == "gemini.google.com" || host == "bard.google.com") {
                return false // Gemini and Bard are conversational AI assistants, NOT search engines!
            }
            if (host == "google.com" || host == "www.google.com") {
                return true
            }
            if ((host.startsWith("google.") || host.startsWith("www.google.")) && !host.contains("gemini") && !host.contains("bard")) {
                return true
            }
            return host == "bing.com" || host == "www.bing.com" ||
                   host == "duckduckgo.com" || host == "www.duckduckgo.com" ||
                   host == "search.yahoo.com" || host == "ecosia.org" || host == "www.ecosia.org" ||
                   host == "qwant.com" || host == "www.qwant.com" ||
                   host == "baidu.com" || host == "www.baidu.com" || host == "m.baidu.com" || host.endsWith(".baidu.com") ||
                   host == "naver.com" || host == "www.naver.com" || host == "m.naver.com" || host.endsWith(".naver.com") ||
                   host == "yandex.com" || host == "www.yandex.com" ||
                   host == "startpage.com" || host == "www.startpage.com"
        }
    }

    fun startTicker() {
        tickerHandler.removeCallbacks(tickerRunnable)
        tickerHandler.post(tickerRunnable)
    }

    private fun runOnMain(r: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run()
        } else {
            mainHandler.post(r)
        }
    }

    private fun postBrowserAudit(targetPkg: String?) {
        postBrowserAudit(targetPkg, 0)
    }

    private fun postBrowserAudit(targetPkg: String?, pollStep: Int) {
        val handler = auditHandler ?: return
        if (targetPkg == null) return
        if (pollStep == 0) {
            handler.removeCallbacksAndMessages(auditStep0Token)
            handler.postAtTime({ executeBrowserAudit(targetPkg, 0) }, auditStep0Token, SystemClock.uptimeMillis())
        } else {
            val delay = if (pollStep - 1 < PROGRESSIVE_DOM_POLL_DELAYS.size) {
                PROGRESSIVE_DOM_POLL_DELAYS[pollStep - 1]
            } else {
                1500L
            }
            handler.postDelayed({ executeBrowserAudit(targetPkg, pollStep) }, delay)
        }
    }

    private fun executeBrowserAudit(targetPkg: String, pollStep: Int) {
        try {
            val root = rootInActiveWindow
            if (root != null) {
                try {
                    val rPkg = root.packageName
                    if (rPkg != null && isBrowserPackage(rPkg.toString(), null) && !isSystemOrLauncher(rPkg.toString())) {
                        inspectBrowserWindow(root, targetPkg, false, pollStep)
                    }
                } finally {
                    root.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in background browser audit (step $pollStep): ${e.message}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        refreshDynamicExemptPackages()
        AppClassifier.refreshBrowserCache(this)

        val thread = HandlerThread("Qiezka-AuditWorker", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        auditThread = thread
        auditHandler = Handler(thread.looper)

        startTicker()
        Log.i(TAG, "LockAccessibilityService onCreate — ticker and audit worker started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startTicker()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "onTaskRemoved: app task cleared from recents — keeping ticker alive")
        startTicker()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        AppClassifier.refreshBrowserCache(this)

        var thread = auditThread
        if (thread == null || !thread.isAlive) {
            thread = HandlerThread("Qiezka-AuditWorker", Process.THREAD_PRIORITY_BACKGROUND)
            thread.start()
            auditThread = thread
            auditHandler = Handler(thread.looper)
        }

        val info = AccessibilityServiceInfo().apply {
            packageNames = null // Watch all packages
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 0
        }
        serviceInfo = info

        refreshDynamicExemptPackages()
        startTicker()

        // Immediate evaluation on reconnect
        try {
            val p = prefs
            var active = p?.getBoolean("lockdown_active", false) ?: false
            if (!active && p != null) {
                active = checkAndAutoStartScheduledLock(p)
            }
            if (active) {
                val lockEnd = p?.getLong("lock_end_time", 0L) ?: 0L
                FloatingOverlayService.startService(this, lockEnd, "Study Session")
                val activePkg = detectCurrentForegroundPackage()
                if (activePkg != null && isPackageBlocked(activePkg)) {
                    enforceBlock(activePkg)
                }
            }
        } catch (ignore: Exception) {}

        Log.i(TAG, "LockAccessibilityService connected and ticker running")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        auditThread?.let {
            it.quitSafely()
            auditThread = null
            auditHandler = null
        }
        tickerHandler.removeCallbacks(tickerRunnable)
        Log.i(TAG, "LockAccessibilityService onDestroy")
    }

    private fun refreshDynamicExemptPackages() {
        try {
            val pm = packageManager ?: return

            // Dynamically discover all installed Home Launchers (CATEGORY_HOME)
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                }
                val homeApps = pm.queryIntentActivities(homeIntent, 0)
                if (homeApps != null) {
                    for (info in homeApps) {
                        if (InstalledLauncherDetector.isRealLauncher(info, packageName)) {
                            info.activityInfo?.packageName?.let { dynamicLauncherPackages.add(it) }
                            val lbl = info.loadLabel(pm)
                            if (lbl != null) {
                                dynamicLauncherLabels.add(lbl.toString().trim().lowercase(Locale.US))
                            }
                        }
                    }
                }
            } catch (ignore: Exception) {}

            Log.d(TAG, "Discovered dynamic launchers: ${dynamicLauncherPackages.size}")
        } catch (e: Exception) {
            Log.w(TAG, "Error resolving dynamic launchers: ${e.message}")
        }
    }

    private fun isKeyboardApp(pkg: String?): Boolean {
        if (pkg == null) return false
        return KnownSafe.isKeyboardPackage(this, pkg)
    }

    fun hasActiveSchedules(): Boolean {
        if (prefs == null) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        return hasActiveSchedules(prefs)
    }

    fun isHomeChangeInterceptorActive(): Boolean {
        if (prefs == null) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        return isHomeChangeInterceptorActive(this, prefs)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Ultra-Fast Path: Master Veto Default Home Launcher role & switch attempt (ONLY if SIREN is default AND session active)
        if (isHomeChangeInterceptorActive()) {
            val src = event.source
            var root = if (src != null) findRootNode(src) else null
            var rootNeedsRecycle = (root != null && root != src)
            if (root == null) {
                root = rootInActiveWindow
                if (root != null) rootNeedsRecycle = true
            }
            try {
                if (isFastHomeRoleEvent(event, root)) {
                    evictHomeLauncherChange(root)
                    return
                }
            } finally {
                if (rootNeedsRecycle) {
                    try { root?.recycle() } catch (ignore: Exception) {}
                }
                try { src?.recycle() } catch (ignore: Exception) {}
            }
        }

        if (prefs == null) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        // Track foreground app switches
        val eventType = event.eventType

        // User Mandate: Scroll events must NEVER trigger inspections, DOM crawls, or app blocks
        if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            return
        }

        // Flowchart Immunity: Long press gestures and in-page selections must NEVER trigger app blocks or evictions
        if (eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
            return
        }

        if (event.className != null) {
            val clsStr = event.className.toString()
            lastBrowserEventClass = clsStr
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && isLikelyActivityClass(clsStr)) {
                lastActiveActivityClass = clsStr
            }
        }

        val pkgChar = event.packageName
        if (pkgChar != null) {
            val pkgStr = pkgChar.toString()
            val eventCls = event.className?.toString()
            var activeCls = if (isLikelyActivityClass(eventCls)) eventCls else lastActiveActivityClass
            activeCls = resolveCandidateClass(pkgStr, activeCls, event.source)
            if (AppClassifier.isSystemSettingsDialog(activeCls)) {
                lastActiveActivityClass = activeCls
            }
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                if (isGeminiActive(pkgStr, eventCls, event.source) && !isGeminiAllowed()) {
                    lastForegroundPackage = "com.google.android.apps.bard"
                } else if (isLauncherApp(this, pkgStr)) {
                    // Navigated to Home/Launcher: update lastForegroundPackage to launcher so background browsers are not remembered as active
                    lastForegroundPackage = pkgStr
                } else if (!isSystemOrLauncher(pkgStr, activeCls)) {
                    lastForegroundPackage = pkgStr
                }
            }
        }

        val currentPrefs = prefs ?: getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).also { prefs = it }
        var isLockdownActive = currentPrefs.getBoolean("lockdown_active", false)
        var isConsequenceActive = currentPrefs.getBoolean("consequence_active", false)
        val lockEndTime = currentPrefs.getLong("lock_end_time", 0L)
        val timeOffset = currentPrefs.getLong("time_offset", 0L)
        val effectiveNow = System.currentTimeMillis() + timeOffset

        val inOperatingHours = isInOperatingHours(effectiveNow)

        // ── Native Timestamp Handling on Timer Expiration ──
        if (isLockdownActive && !isConsequenceActive && lockEndTime > 0 && effectiveNow >= lockEndTime) {
            isConsequenceActive = true
            currentPrefs.edit()
                .putBoolean("consequence_active", true)
                .remove("lock_end_time")
                .apply()
            AlarmReceiver.cancelLockEndAlarm(this)
            AlarmReceiver.createNotificationChannels(this)
            val isHardcore = "hardcore".equals(currentPrefs.getString("operating_mode", "safemode"), ignoreCase = true)
            val notifTitle = if (isHardcore) {
                "⚠️ Homework Expired — Consequence Active (Hardcore)"
            } else {
                "⚠️ Homework Expired — Consequence Active"
            }
            val notifMsg = if (isHardcore) {
                "Study timer expired without passing. Distracting apps remain restricted 24/7 (Hardcore Mode) until rescheduled and passed."
            } else {
                "Study timer expired without passing. Distracting apps remain restricted during operating hours (7 PM – 3 AM) until rescheduled and passed."
            }
            AlarmReceiver.showNotificationStatic(
                this,
                AlarmReceiver.NOTIF_ID_COMPLETED,
                AlarmReceiver.CHANNEL_ID_ALERTS,
                notifTitle,
                notifMsg,
                Notification.PRIORITY_HIGH,
                false
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(AlarmReceiver.NOTIF_ID_STATUS)
            Log.i(TAG, "Lockdown expired — entered Consequence Mode")
        }

        // ── State Evaluation: Lockdown, Consequence, Standby, or Total Idle ──
        val isHardcore = "hardcore".equals(currentPrefs.getString("operating_mode", "safemode"), ignoreCase = true)
        val isConsequenceEnforcing = isConsequenceActive && (isHardcore || inOperatingHours)

        if (!isLockdownActive && !isConsequenceActive) {
            isLockdownActive = checkAndAutoStartScheduledLock(currentPrefs)
        }

        val isEnforcing = isLockdownActive || isConsequenceEnforcing
        val hasSchedules = hasActiveSchedules()

        // Flowchart: If Consequence Active in Safemode outside Operating Hours (Daytime 3am-7pm) -> ALLOW_IDLE
        if (isConsequenceActive && !isHardcore && !inOperatingHours) {
            return
        }

        // Flowchart: If neither Lockdown nor Consequence is Active, check Active Schedules
        if (!isEnforcing && !hasSchedules) {
            // Total System Idle: Zero schedules -> ALLOW_IDLE (Settings, launcher, and uninstall allowed)
            return
        }

        // ── STAGE 1: MASTER VETO GATE (Active Lockdown, Active Consequence, or Standby Protection) ──
        if (interceptHomeLauncherChangeAttempt(event, null, pkgChar?.toString())) {
            return
        }

        if (pkgChar != null) {
            val pkgStr = pkgChar.toString()
            val eventCls = event.className?.toString()
            var activeCls = if (isLikelyActivityClass(eventCls)) eventCls else lastActiveActivityClass
            activeCls = resolveCandidateClass(pkgStr, activeCls, event.source)
            if (AppClassifier.isSystemSettingsDialog(activeCls)) {
                lastActiveActivityClass = activeCls
            }

            if (pkgStr != packageName && !isSystemOrLauncher(pkgStr, activeCls)) {
                var appLabel: String? = null
                try {
                    val pm = packageManager
                    if (pm != null) {
                        val ai = pm.getApplicationInfo(pkgStr, 0)
                        val lbl = pm.getApplicationLabel(ai)
                        if (lbl != null) appLabel = lbl.toString()
                    }
                } catch (ignore: Exception) {}

                if (AppClassifier.isSettingsOrDeviceManager(pkgStr, appLabel, activeCls) || AppClassifier.isStage1Bloat(pkgStr, appLabel)) {
                    Log.w(TAG, "Stage 1 Master Veto Gate matched: $pkgStr ($appLabel, $activeCls)")
                    enforceBlock(pkgStr)
                    return
                }
            }
        }

        // Milestone 20: Immediate evaluation on window stack reordering (such as Recents task switch)
        // In Android, TYPE_WINDOWS_CHANGED events dispatched by WindowManager have event.getPackageName() == null!
        // It MUST be evaluated here before any null package check.
        if (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            val currentFg = detectCurrentForegroundPackage()
            var activeCls = lastActiveActivityClass
            if ("com.android.settings" == currentFg && !AppClassifier.isSystemSettingsDialog(activeCls)) {
                var r: AccessibilityNodeInfo? = null
                try {
                    r = rootInActiveWindow
                    activeCls = resolveCandidateClass(currentFg, activeCls, r)
                } finally {
                    r?.recycle()
                }
            }
            if (currentFg != null && currentFg != packageName && !isSystemOrLauncher(currentFg, activeCls)) {
                var fgLabel: String? = null
                try {
                    val pm = packageManager
                    if (pm != null) {
                        val ai = pm.getApplicationInfo(currentFg, 0)
                        val lbl = pm.getApplicationLabel(ai)
                        if (lbl != null) fgLabel = lbl.toString()
                    }
                } catch (ignore: Exception) {}

                if (AppClassifier.isSettingsOrDeviceManager(currentFg, fgLabel, activeCls) || AppClassifier.isStage1Bloat(currentFg, fgLabel)) {
                    Log.w(TAG, "Stage 1 Master Veto caught window stack change: $currentFg ($activeCls)")
                    enforceBlock(currentFg)
                    return
                }

                // If not enforcing (Standby), normal apps are allowed
                if (!isEnforcing) {
                    return
                }

                // Universal audit and remediation for unauthorized floating PiP / miniplayers
                auditAndRemediatePictureInPicture()

                if (isBrowserPackage(currentFg, null)) {
                    postBrowserAudit(currentFg)
                    return
                } else if (isPackageBlocked(currentFg)) {
                    enforceBlock(currentFg)
                    return
                }
            }
        }

        // ── STAGE 1 PASSED: ROUTE BY STATE ──
        if (!isEnforcing) {
            // Active Schedule Standby: Normal apps (TikTok, YouTube, games, browsers) are allowed until schedule lock starts
            return
        }

        if (pkgChar == null) return
        val pkg = pkgChar.toString()

        // Never inspect or block QIEZKA itself
        if (pkg == packageName) return

        // Gemini interception inside com.google.android.googlequicksearchbox or entry stub
        val eventCls = event.className?.toString()
        if (isGeminiActive(pkg, eventCls, event.source)) {
            if (!isGeminiAllowed()) {
                Log.w(TAG, "Blocked Gemini Robin UI inside $pkg ($eventCls) — AI policy disabled during lockdown/consequence")
                enforceBlock("com.google.android.apps.bard")
                return
            }
        }

        // ── SystemUI handling ──
        if (pkg == "com.android.systemui") {
            handleSystemUiEvent(event)
            return
        }

        // Milestone 19/20: Drop pure background notifications from non-active apps
        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            return
        }

        // STAGE 2 — Branch 1: Web Browser Gate (URL / DOM Classifier & In-Browser Auto-Back)
        if (isBrowserPackage(pkg, null)) {
            // Typing Immunity: Ignore keystrokes and text selection events while user is editing in address bars
            if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
                eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                return
            }

            // Sub-Function Remediation for Portal Super-Apps (Baidu Reels, Phoenix, UC, NAVER)
            if (AppClassifier.isPortalSuperApp(pkg)) {
                if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                    eventCls != null &&
                    !eventCls.startsWith("android.widget.") &&
                    !eventCls.startsWith("android.view.")) {
                    if (AppClassifier.isDistractingSubActivity(pkg, eventCls)) {
                        Log.w(TAG, "Caught distracting sub-activity in portal app $pkg ($eventCls) -> Auto-Back")
                        remediateDistractingSubActivity(pkg, eventCls)
                        return
                    }
                }
            }
            val isTransition = (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                                eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED)
            val isInteraction = (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
                                 eventType == AccessibilityEvent.TYPE_VIEW_CLICKED)

            var isVisibleOnScreen = false
            val source = event.source
            if (source != null) {
                try {
                    isVisibleOnScreen = source.isVisibleToUser
                } finally {
                    source.recycle()
                }
            }

            val currentForeground = detectCurrentForegroundPackage()
            val isForegroundApp = pkg == currentForeground
            if (isTransition || isInteraction || isVisibleOnScreen || isForegroundApp) {
                postBrowserAudit(pkg)
            }
            return
        }

        // STAGE 2 — Branch 2: App Classifier Gate (isPackageBlocked)
        if (isPackageBlocked(pkg)) {
            // Milestone 20: PWA & Recents Task-Switch Interceptor
            val isTransition = (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                                eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED)
            val isInteraction = (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
                                 eventType == AccessibilityEvent.TYPE_VIEW_CLICKED)

            // Direct inspection of event source node visibility on screen:
            var isVisibleOnScreen = false
            val source = event.source
            if (source != null) {
                try {
                    isVisibleOnScreen = source.isVisibleToUser
                } finally {
                    source.recycle()
                }
            }

            val currentForeground = detectCurrentForegroundPackage()
            val isForegroundApp = pkg == currentForeground

            if (isTransition || isInteraction || isVisibleOnScreen || isForegroundApp) {
                enforceBlock(pkg)
            } else {
                Log.d(TAG, "Ignored background event from blocked package: $pkg (event=$eventType)")
            }
            return
        }
    }

    fun isBrowserPackage(pkg: String?, appLabel: String?): Boolean {
        return AppClassifier.isBrowserPackage(this, pkg, appLabel)
    }

    fun isGeminiAllowed(): Boolean {
        return AppClassifier.isPackageAllowedByUnifiedPolicy(this, "com.google.android.apps.gemini")
    }

    fun isGeminiActive(pkg: String?, className: String?, root: AccessibilityNodeInfo?): Boolean {
        if (pkg == null) return false
        val lowerPkg = pkg.lowercase(Locale.ROOT)
        if (lowerPkg == "com.google.android.apps.bard" || lowerPkg == "com.google.android.apps.gemini") {
            return true
        }
        if (lowerPkg == "com.google.android.googlequicksearchbox") {
            if (className != null) {
                val lowerCls = className.lowercase(Locale.ROOT)
                if (lowerCls.contains(".robin.") || lowerCls.contains(".gemini.") ||
                    lowerCls.contains("geminigatewayactivity") || lowerCls.contains("robinentrypointactivity")) {
                    return true
                }
            }
            val lastEvent = lastBrowserEventClass
            if (lastEvent != null) {
                val lowerLast = lastEvent.lowercase(Locale.ROOT)
                if (lowerLast.contains(".robin.") || lowerLast.contains(".gemini.")) {
                    return true
                }
            }
            var activeRoot = root
            var shouldRecycle = false
            try {
                if (activeRoot == null) {
                    activeRoot = rootInActiveWindow
                    shouldRecycle = (activeRoot != null)
                }
                if (activeRoot != null) {
                    val nodes = activeRoot.findAccessibilityNodeInfosByViewId("com.google.android.googlequicksearchbox:id/assistant_robin_chat_header_container")
                    if (!nodes.isNullOrEmpty()) return true
                    val nodes2 = activeRoot.findAccessibilityNodeInfosByViewId("com.google.android.googlequicksearchbox:id/assistant_robin_chat_compose_toolbar")
                    if (!nodes2.isNullOrEmpty()) return true
                    val textNodes = activeRoot.findAccessibilityNodeInfosByText("Open Gemini Live")
                    if (!textNodes.isNullOrEmpty()) return true
                }
            } catch (ignore: Exception) {
            } finally {
                if (shouldRecycle && activeRoot != null) {
                    try { activeRoot.recycle() } catch (ignore: Exception) {}
                }
            }
        }
        return false
    }

    fun isVideoPolicyAllowed(): Boolean {
        val p = prefs
        if (p != null && p.getBoolean("allow_youtube", false)) {
            return true
        }
        return AppClassifier.getActiveServices(this).contains("youtube")
    }

    fun isPortalSuperAppVideoActive(root: AccessibilityNodeInfo?, pkg: String?): Boolean {
        if (root == null || pkg == null) return false
        if (!AppClassifier.isPortalSuperApp(pkg)) return false
        val rootPkgCs = root.packageName
        if (rootPkgCs != null && pkg != rootPkgCs.toString()) return false

        if ("com.baidu.searchbox" == pkg) {
            val baiduVideoIds = arrayOf(
                "com.baidu.searchbox:id/video_flow_cmp_player",
                "com.baidu.searchbox:id/video_flow_tab_component",
                "com.baidu.searchbox:id/video_item_portrait_root",
                "com.baidu.searchbox:id/first_init_video_item_container",
                "com.baidu.searchbox:id/video_flow_cmp_list",
                "com.baidu.searchbox:id/video_flow_next_big_card",
                "com.baidu.searchbox:id/video_flow_cmp_seek_bar"
            )
            for (vidId in baiduVideoIds) {
                try {
                    val nodes = root.findAccessibilityNodeInfosByViewId(vidId)
                    if (!nodes.isNullOrEmpty()) {
                        for (n in nodes) {
                            if (n != null) {
                                try {
                                    if (n.isVisibleToUser) return true
                                } catch (ignore: Exception) {
                                    return true
                                }
                            }
                        }
                    }
                } catch (ignore: Exception) {}
            }
        } else if ("com.nhn.android.search" == pkg) {
            // Fast-path: If NAVER is on SearchHomePage with searchBarRootView and Clip tab is not selected, skip scanning 14 video IDs
            try {
                val searchBar = root.findAccessibilityNodeInfosByViewId("com.nhn.android.search:id/searchBarRootView")
                if (!searchBar.isNullOrEmpty()) {
                    var clipTabSelected = false
                    val clipTabNodes = root.findAccessibilityNodeInfosByText("클립")
                    if (!clipTabNodes.isNullOrEmpty()) {
                        for (n in clipTabNodes) {
                            if (n != null) {
                                val desc = n.contentDescription
                                if (desc != null && desc.toString().contains("선택됨")) {
                                    clipTabSelected = true
                                    break
                                }
                            }
                        }
                    }
                    if (!clipTabSelected) {
                        return false // Safe Search Home Page!
                    }
                }
            } catch (ignore: Exception) {}

            val naverVideoIds = arrayOf(
                "com.nhn.android.search:id/container_clip_viewpager",
                "com.nhn.android.search:id/clip_follow_view_pager",
                "com.nhn.android.search:id/videoView",
                "com.nhn.android.search:id/videoGroup",
                "com.nhn.android.search:id/container_clip_nested_scrollable_host",
                "com.nhn.android.search:id/shortentsNowViewPager",
                "com.nhn.android.search:id/clipContentSoundToggle",
                "com.nhn.android.search:id/clip_root",
                "com.nhn.android.search:id/clip_player",
                "com.nhn.android.search:id/clip_player_view",
                "com.nhn.android.search:id/clip_view_pager",
                "com.nhn.android.search:id/clip_content_layout",
                "com.nhn.android.search:id/clip_form",
                "clip_form"
            )
            for (vidId in naverVideoIds) {
                try {
                    val nodes = root.findAccessibilityNodeInfosByViewId(vidId)
                    if (!nodes.isNullOrEmpty()) {
                        for (n in nodes) {
                            if (n != null) {
                                try {
                                    if (n.isVisibleToUser) return true
                                } catch (ignore: Exception) {
                                    return true
                                }
                            }
                        }
                    }
                } catch (ignore: Exception) {}
            }
            // Check if Clip tab is actively selected on bottom navigation bar
            try {
                val clipTabNodes = root.findAccessibilityNodeInfosByText("클립")
                if (!clipTabNodes.isNullOrEmpty()) {
                    for (n in clipTabNodes) {
                        if (n != null) {
                            val desc = n.contentDescription
                            if (desc != null && desc.toString().contains("선택됨")) {
                                return true
                            }
                            val parent = n.parent
                            if (parent != null) {
                                val pDesc = parent.contentDescription
                                if (pDesc != null && pDesc.toString().contains("선택됨")) {
                                    return true
                                }
                            }
                        }
                    }
                }
            } catch (ignore: Exception) {}
            // Check for in-app clip search results title
            try {
                val clipSearchNodes = root.findAccessibilityNodeInfosByText("클립검색")
                if (!clipSearchNodes.isNullOrEmpty()) {
                    return true
                }
            } catch (ignore: Exception) {}
        } else if ("com.transsion.phoenix" == pkg) {
            val phoenixIds = arrayOf(
                "com.transsion.phoenix:id/video_player",
                "com.transsion.phoenix:id/short_video_flow",
                "com.transsion.phoenix:id/feed_video_player"
            )
            for (vidId in phoenixIds) {
                try {
                    val nodes = root.findAccessibilityNodeInfosByViewId(vidId)
                    if (!nodes.isNullOrEmpty()) return true
                } catch (ignore: Exception) {}
            }
        } else if ("com.ucmobile".equals(pkg, ignoreCase = true) || "com.uc.browser.en".equals(pkg, ignoreCase = true)) {
            val ucIds = arrayOf(
                "$pkg:id/video_player",
                "$pkg:id/video_feed_root",
                "$pkg:id/reels_container"
            )
            for (vidId in ucIds) {
                try {
                    val nodes = root.findAccessibilityNodeInfosByViewId(vidId)
                    if (!nodes.isNullOrEmpty()) return true
                } catch (ignore: Exception) {}
            }
        }
        return false
    }

    private fun isSystemOrLauncher(pkg: String?): Boolean {
        return isSystemOrLauncher(pkg, lastActiveActivityClass)
    }

    private fun isSystemOrLauncher(pkg: String?, activityCls: String?): Boolean {
        if (pkg == null) return false
        if (pkg.contains("permissioncontroller")) {
            return false
        }
        if (pkg == "com.google.android.googlequicksearchbox" && isGeminiActive(pkg, lastBrowserEventClass, null)) {
            if (!isGeminiAllowed()) {
                return false
            }
        }
        // Stage 2: Verified Home Launchers (OEM, third-party, and SIREN proxy)
        if (isLauncherApp(this, pkg)) {
            return true
        }
        // System Picker / Dialog hosted in Settings.apk (e.g. ActivityPicker for Intent.ACTION_PICK_ACTIVITY)
        if (AppClassifier.isSystemSettingsDialog(activityCls)) {
            return true
        }
        // Universal System Partition Gateway for non-browser, non-settings system overlays
        try {
            val pm = packageManager
            if (pm != null) {
                val ai = pm.getApplicationInfo(pkg, 0)
                if ((ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    if (!isBrowserPackage(pkg, null) && !AppClassifier.isSettingsOrDeviceManager(pkg, null, activityCls) && !AppClassifier.isStage1Bloat(pkg, null)) {
                        return true
                    }
                }
            }
        } catch (ignore: Exception) {}
        return false
    }

    private fun isLikelyActivityClass(cls: String?): Boolean {
        if (cls == null || !cls.contains(".")) return false
        val lower = cls.lowercase(Locale.US)
        if (lower.startsWith("android.widget.") ||
            lower.startsWith("android.view.") ||
            lower.startsWith("android.app.dialog") ||
            lower.startsWith("android.app.alertdialog") ||
            lower.startsWith("androidx.") ||
            lower.startsWith("com.android.internal.widget.")) {
            return false
        }
        return true
    }

    private fun resolveCandidateClass(pkg: String?, candidateCls: String?, root: AccessibilityNodeInfo?): String? {
        if (!AppClassifier.isSystemSettingsDialog(candidateCls) && "com.android.settings" == pkg && root != null) {
            try {
                val titles = root.findAccessibilityNodeInfosByViewId("android:id/alertTitle")
                if (!titles.isNullOrEmpty()) {
                    val t = titles[0].text
                    if (t != null && AppClassifier.isSystemSettingsDialog(t.toString())) {
                        return t.toString()
                    }
                }
            } catch (ignore: Exception) {}
        }
        return candidateCls
    }

    private fun resolveActiveWindowTitle(root: AccessibilityNodeInfo?): String? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && root != null) {
                val win = root.window
                if (win != null) {
                    val t = win.title
                    if (!t.isNullOrEmpty()) return t.toString()
                }
            }
            val winList = windows
            if (!winList.isNullOrEmpty()) {
                for (w in winList) {
                    if (w.type == AccessibilityWindowInfo.TYPE_APPLICATION && (w.isActive || w.isFocused)) {
                        val t = w.title
                        if (!t.isNullOrEmpty()) return t.toString()
                    }
                }
                for (w in winList) {
                    if (w.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        val t = w.title
                        if (!t.isNullOrEmpty()) return t.toString()
                    }
                }
            }
        } catch (ignore: Exception) {}
        return null
    }

    private fun isBrowserSearch(url: String?, root: AccessibilityNodeInfo?): Boolean {
        // 0. Flowchart Node B: Internal browser home schemes and New Tab Page / Search Launchpad -> 100% research immune
        if (!url.isNullOrBlank()) {
            val cleanUrl = url.trim().lowercase(Locale.US)
            if (cleanUrl == "chrome://newtab" || cleanUrl.startsWith("chrome://newtab") ||
                cleanUrl == "about:blank" || cleanUrl == "about:home" ||
                cleanUrl.startsWith("chrome-native://") || cleanUrl == "edge://newtab" ||
                cleanUrl == "new tab") {
                return true
            }
        }
        if (isBrowserNewTabPage(root)) {
            return true
        }

        // 1. Flowchart: Active search input box / Query typing in progress -> ALLOW_SEARCH (100% immune)
        if (root != null) {
            try {
                val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focused != null) {
                    try {
                        if (focused.isEditable) {
                            // If the focused input is the native browser address bar / search box, typing is 100% immune
                            if (isAddressBarNode(focused)) {
                                return true // Active Omnibox / address bar typing or autocomplete in progress
                            }
                            // If on a search engine host, an in-page search input (e.g. Google/Bing search box) is also immune
                            if (!url.isNullOrBlank()) {
                                val host = WebBlocklistConstants.extractHost(url.trim().lowercase(Locale.US))
                                if (isSearchEngineHost(host)) {
                                    return true
                                }
                            }
                        }
                    } finally {
                        focused.recycle()
                    }
                }
            } catch (ignore: Exception) {}
        }

        // 2. Search Engine Results Page (SERP) or Search Engine URLs
        if (!url.isNullOrBlank()) {
            val lowerUrl = url.trim().lowercase(Locale.US)

            // Google redirect URLs (google.com/url?q=...) are transitions to destination pages, not search results
            if (lowerUrl.contains("google.") && lowerUrl.contains("/url?")) {
                return false
            }

            // Check if it is a search engine URL with query parameters
            val host = WebBlocklistConstants.extractHost(lowerUrl)
            if (isSearchEngineHost(host) && (lowerUrl.contains("/search") || lowerUrl.contains("?q=") || lowerUrl.contains("&q="))) {
                return true
            }
            if (lowerUrl.contains("v.baidu.com") || lowerUrl.contains("video.baidu.com") ||
                lowerUrl.contains("haokan.baidu.com") || lowerUrl.contains("tieba.baidu.com") ||
                lowerUrl.contains("baidu.com/video") || lowerUrl.contains("m.baidu.com/video")) {
                // Not search results page! Destination media / forum targets
            } else if (lowerUrl.contains("bing.com/search") || lowerUrl.contains("duckduckgo.com") ||
                lowerUrl.contains("search.yahoo.com") || lowerUrl.contains("ecosia.org/search") ||
                lowerUrl.contains("qwant.com") || lowerUrl.contains("baidu.com") || lowerUrl.contains("m.baidu.com") ||
                lowerUrl.contains("naver.com") || lowerUrl.contains("m.naver.com") ||
                lowerUrl.contains("yandex.com/search") || lowerUrl.contains("startpage.com")) {
                return true
            }

            // If it is a destination URL that is NOT a search engine host, it is NEVER a search results page!
            if (WebClassifier.isDestinationUrl(url)) {
                if (!isSearchEngineHost(host)) {
                    return false // Destination page! E.g. gemini.google.com, chatgpt.com, y8.com, etc.
                }
            }
        }

        // 3. Portal Super-App Native Search Home / Results Feed (when url is null)
        if (root != null) {
            try {
                val rootPkg = root.packageName
                if (rootPkg != null) {
                    val pkgStr = rootPkg.toString()
                    if ("com.nhn.android.search" == pkgStr) {
                        // Check if in NAVER Search Home / Results rather than InAppBrowserActivity
                        val inInAppBrowser = (lastActiveActivityClass != null && lastActiveActivityClass!!.contains("InAppBrowserActivity"))
                        if (!inInAppBrowser) {
                            var searchBarNodes = root.findAccessibilityNodeInfosByViewId("com.nhn.android.search:id/searchBarRootView")
                            if (searchBarNodes.isNullOrEmpty()) {
                                searchBarNodes = root.findAccessibilityNodeInfosByViewId("com.nhn.android.search:id/searchBarView")
                            }
                            if (!searchBarNodes.isNullOrEmpty()) {
                                lastActiveActivityClass = "com.nhn.android.search.ui.pages.SearchHomePage"
                                return true // Native NAVER search engine home / search portal
                            }
                        }
                    } else if ("com.baidu.searchbox" == pkgStr) {
                        // Check if in Baidu Search Home / Results with native search bar
                        var searchBoxes = root.findAccessibilityNodeInfosByViewId("com.baidu.searchbox:id/landing_page_box_tv")
                        if (searchBoxes.isNullOrEmpty()) {
                            searchBoxes = root.findAccessibilityNodeInfosByViewId("com.baidu.searchbox:id/search_box_content")
                        }
                        if (!searchBoxes.isNullOrEmpty()) {
                            val webViews = root.findAccessibilityNodeInfosByViewId("com.baidu.searchbox:id/bdframeview_id")
                            if (webViews.isNullOrEmpty()) {
                                return true // Native Baidu search results feed
                            }
                        }
                    }
                }
            } catch (ignore: Exception) {}
        }

        return false
    }

    private fun inspectBrowserWindow(root: AccessibilityNodeInfo?, pkg: String?, fromTicker: Boolean) {
        inspectBrowserWindow(root, pkg, fromTicker, 0)
    }

    private fun inspectBrowserWindow(root: AccessibilityNodeInfo?, pkg: String?, fromTicker: Boolean, pollStep: Int) {
        if (root == null || pkg == null) return

        // Launcher Immunity & Foreign Window Guard:
        // The root window must never be audited as a browser window if it belongs to a launcher or SystemUI!
        val rootPkgChar = root.packageName
        if (rootPkgChar != null) {
            val rootPkg = rootPkgChar.toString()
            if (isLauncherApp(this, rootPkg) || "com.android.systemui" == rootPkg) {
                return // Home launcher & SystemUI are 100% exempt from browser DOM inspection
            }
            if (AppClassifier.isPortalSuperApp(pkg) && rootPkg != pkg) {
                return // Mismatched window hierarchy cannot be a portal super-app web view
            }
        }

        // Link Context Menu Guard:
        // When the user long-presses a hyperlink (on a web page, in search results, or in CustomTabs),
        // the browser or search app displays a Link Context Menu / Bottom Sheet.
        if (isLinkContextMenu(root, pkg)) {
            Log.d(TAG, "inspectBrowserWindow: ignoring link context menu preview overlay for $pkg")
            return
        }

        try {
            // Dedicated Handling: Portal Super-Apps (Baidu, NAVER)
            if (AppClassifier.isPortalSuperApp(pkg)) {
                if (!isVideoPolicyAllowed()) {
                    if (isPortalSuperAppVideoActive(root, pkg)) {
                        Log.w(TAG, "inspectBrowserWindow: detected active video flow in portal superapp $pkg while video policy is disabled -> Auto-Back")
                        remediateDistractingSubActivity(pkg, "portal_video_flow")
                        return
                    }
                }

                val url = extractUrlFromBrowser(root, pkg)
                if (isBrowserSearch(url, root)) {
                    resetBrowserRemediationState()
                    return
                }

                val currentPrefs = prefs
                val allowYoutube = currentPrefs?.getBoolean("allow_youtube", false) ?: false
                val activeServices = currentPrefs?.getStringSet("active_unified_services", HashSet()) ?: HashSet()
                val allowedDomains = HashSet<String>(UnifiedPolicyRegistry.getDomainsForServices(activeServices))
                if (currentPrefs != null) {
                    val customDomains = currentPrefs.getStringSet("allowed_domains", null)
                    if (customDomains != null) allowedDomains.addAll(customDomains)
                }
                if (allowYoutube) {
                    val yt = UnifiedPolicyRegistry.SERVICES["youtube"]
                    if (yt != null) allowedDomains.addAll(yt.domains)
                }

                if (!url.isNullOrBlank()) {
                    if (WebBlocklistConstants.isAcademicExempt(url)) {
                        resetBrowserRemediationState()
                        return
                    }
                    val res = WebClassifier.classify(url, root, allowYoutube, allowedDomains)
                    if (res.isBlocked) {
                        remediateBlockedBrowserTab(pkg, url, res.reason, root)
                    } else {
                        resetBrowserRemediationState()
                    }
                } else {
                    val inAppRes = WebClassifier.classifyInAppWeb(root, allowYoutube, allowedDomains)
                    if (inAppRes.isBlocked) {
                        remediateBlockedBrowserTab(pkg, "in_app_web", inAppRes.reason, root)
                    } else if (inAppRes.isPending) {
                        Log.i(TAG, "inspectBrowserWindow: in-app DOM pending for $pkg, scheduling poll step ${pollStep + 1}")
                        if (pollStep < PROGRESSIVE_DOM_POLL_DELAYS.size) {
                            postBrowserAudit(pkg, pollStep + 1)
                        }
                    } else {
                        resetBrowserRemediationState()
                    }
                }
                return
            }

            // ── GENERAL WEB CONTAINERS: Chrome, Firefox, Custom Tabs, PWA, TWA, WebAPK ──
            var url = extractUrlFromBrowser(root, pkg)
            if (url.isNullOrBlank()) {
                url = resolveHostFromPackageManifest(pkg)
                if (url == null && rootPkgChar != null && rootPkgChar.toString() != pkg) {
                    url = resolveHostFromPackageManifest(rootPkgChar.toString())
                }
                if (url == null && lastForegroundPackage != null && lastForegroundPackage != pkg && !isSystemOrLauncher(lastForegroundPackage)) {
                    url = resolveHostFromPackageManifest(lastForegroundPackage)
                }
                if (url == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try {
                        val win = root.window
                        if (win?.title != null) {
                            val winTitle = win.title.toString().trim()
                            if (winTitle.isNotEmpty() && !winTitle.equals("Chrome", ignoreCase = true) &&
                                !winTitle.equals("Browser", ignoreCase = true) && !winTitle.equals("Navigation bar", ignoreCase = true)) {
                                if (KnownDistractingWeb.isKnownDistractingWeb(winTitle)) {
                                    url = "https://" + winTitle.lowercase(Locale.US)
                                }
                            }
                        }
                    } catch (ignore: Exception) {}
                }
            }

            // Node B: Is it Search Query / SERP?
            if (isBrowserSearch(url, root)) {
                // Node C: Research Immune: ALLOW
                resetBrowserRemediationState()
                return
            }

            val currentPrefs = prefs
            val allowYoutube = currentPrefs?.getBoolean("allow_youtube", false) ?: false

            val activeServices = currentPrefs?.getStringSet("active_unified_services", HashSet()) ?: HashSet()
            val allowedDomains = HashSet<String>(UnifiedPolicyRegistry.getDomainsForServices(activeServices))
            if (currentPrefs != null) {
                val customDomains = currentPrefs.getStringSet("allowed_domains", null)
                if (customDomains != null) allowedDomains.addAll(customDomains)
            }
            if (allowYoutube) {
                val yt = UnifiedPolicyRegistry.SERVICES["youtube"]
                if (yt != null) allowedDomains.addAll(yt.domains)
            }

            // Node D: Is URL viewable?
            val isUrlViewable = !url.isNullOrBlank()

            if (isUrlViewable) {
                val cleanUrl = url!!.trim().lowercase(Locale.US)

                // Window title academic check (Chromium tabs / CustomTabs)
                var isWindowTitleAcademic = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try {
                        val win = root.window
                        if (win?.title != null && KnownSafeWeb.isAcademicExempt(win.title.toString())) {
                            isWindowTitleAcademic = true
                        }
                    } catch (ignore: Exception) {}
                }

                // Node E: Check for KnownDistractingWeb
                val isDistractingWeb = KnownDistractingWeb.isKnownDistractingWeb(cleanUrl)

                // Node N: Check for KnownSafeWeb
                val isSafeWeb = isWindowTitleAcademic ||
                        WebBlocklistConstants.isAcademicExempt(cleanUrl) ||
                        KnownSafeWeb.isAcademicExempt(cleanUrl) ||
                        KnownSafeWeb.isKnownSafeWeb(cleanUrl, allowedDomains)

                // Node O: Check for TruthTable + UnifiedRegistry Policy
                if (isDistractingWeb && !isSafeWeb) {
                    // "Yes Distracting, No Safe" -> Node H: Remediate: Close Tab/Back
                    val reason = KnownDistractingWeb.getDistractionReason(cleanUrl)
                    Log.w(TAG, "inspectBrowserWindow: blocked browser URL: $url ($reason)")
                    remediateBlockedBrowserTab(pkg, url, reason, root)
                    return
                } else if (!isDistractingWeb && isSafeWeb) {
                    // "No Distracting, Yes Safe" -> Node I: Browsing Continues Uninterrupted
                    resetBrowserRemediationState()
                    return
                } else if (isDistractingWeb && isSafeWeb) {
                    // "Yes Distracting, Yes Safe" -> Node I: Browsing Continues Uninterrupted
                    resetBrowserRemediationState()
                    return
                }

                // "No Distracting, No Safe" -> Node F: WebClassifier + DOM
                // Falls through to Node F below
            }

            // Node F: WebClassifier + DOM (evaluated for non-viewable URL OR unclassified viewable URL)
            val domRes = WebClassifier.classifyDom(root, allowYoutube, allowedDomains)
            if (domRes.isBlocked) {
                // F -- Blocked --> Node H: Remediate: Close Tab/Back
                Log.w(TAG, "inspectBrowserWindow: DOM blocked web container: $pkg (${domRes.reason})")
                remediateBlockedBrowserTab(pkg, url ?: "web_container", domRes.reason, root)
            } else if (domRes.isPending) {
                // F -- Pending / Sparse Skeleton --> Schedule next progressive poll step
                Log.i(TAG, "inspectBrowserWindow: DOM pending/loading for $pkg (${domRes.reason}), scheduling poll step ${pollStep + 1}")
                if (pollStep < PROGRESSIVE_DOM_POLL_DELAYS.size) {
                    postBrowserAudit(pkg, pollStep + 1)
                }
                // Invariant: DO NOT call resetBrowserRemediationState() while DOM is still loading/unresolved!
            } else {
                // F -- Allowed --> Node I: Browsing Continues Uninterrupted
                resetBrowserRemediationState()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in inspectBrowserWindow: ${e.message}")
        }
    }

    private fun isSameWebTarget(url1: String?, url2: String?): Boolean {
        if (url1 == null || url2 == null) return false
        if (url1 == url2) return true
        val host1 = extractHost(url1)
        val host2 = extractHost(url2)
        return host1.isNotEmpty() && host1 == host2
    }

    private fun resetBrowserRemediationState() {
        if (consecutiveBlockedUrlHits > 0 || lastRemediatedUrl != null) {
            consecutiveBlockedUrlHits = 0
            lastRemediatedUrl = null
        }
    }

    private fun remediateBlockedBrowserTab(pkg: String, url: String, reason: String?, root: AccessibilityNodeInfo?) {
        val now = System.currentTimeMillis()

        // 1. Debounce: If a remediation action (Auto-Back or Home button) was triggered recently (< 2200ms)
        // for this exact same target origin, allow the browser transition to finish smoothly without interruption.
        if (now - lastBrowserRemediationTime < BROWSER_REMEDIATION_COOLDOWN_MS && isSameWebTarget(url, lastRemediatedUrl)) {
            return
        }

        // 2. Escalation counter: If the browser is STILL on the same blocked origin after the cooldown window
        // (between 2200ms and 6500ms), advance to next hit. Otherwise reset to 1.
        if (isSameWebTarget(url, lastRemediatedUrl) && (now - lastBrowserRemediationTime < BROWSER_REMEDIATION_TIMEOUT_MS)) {
            consecutiveBlockedUrlHits++
        } else {
            consecutiveBlockedUrlHits = 1
        }

        lastBrowserRemediationTime = now
        lastRemediatedUrl = url

        Log.w(TAG, "remediateBlockedBrowserTab on $pkg for $url ($reason) hits=$consecutiveBlockedUrlHits")

        // 1. Display educational Toast notification informing student
        if (now - lastBrowserToastTime > BROWSER_TOAST_COOLDOWN_MS) {
            lastBrowserToastTime = now
            val finalReason = reason ?: "Distracting website"
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, "⚠️ $finalReason. Returning to safe page.", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Custom Tab Remediation: If opened inside a Chrome Custom Tab, close it directly via close_button
        if (closeCustomTab(root, pkg)) {
            Log.i(TAG, "remediateBlockedBrowserTab: closed CustomTab via close_button")
            return
        }

        // 3. Primary Remediation: Auto-Back (Hits 1 and 2)
        if (consecutiveBlockedUrlHits <= 2) {
            runOnMain { performGlobalAction(GLOBAL_ACTION_BACK) }
            Log.i(TAG, "remediateBlockedBrowserTab: executed auto-back (hit $consecutiveBlockedUrlHits)")
            return
        }

        // 4. Secondary Remediation: Repeat hit after multiple backs means no back history in this tab or JS trap
        if (AppClassifier.isPortalSuperApp(pkg)) {
            Log.w(TAG, "remediateBlockedBrowserTab: repeat hit in portal superapp $pkg -> cleanly resetting to home search page")
            try {
                val homeIntent = packageManager?.getLaunchIntentForPackage(pkg)
                if (homeIntent != null) {
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(homeIntent)
                }
            } catch (ignore: Exception) {}
            consecutiveBlockedUrlHits = 0
            return
        }

        Log.i(TAG, "remediateBlockedBrowserTab: repeat hit detected after auto-back, resetting tab in-place via home button")
        val homeClicked = clickBrowserHomeButton(root, pkg)
        if (homeClicked) {
            Log.i(TAG, "remediateBlockedBrowserTab: active tab neutralized in-place via home button (0 new tabs created)")
            return
        }

        // 5. Tertiary Remediation: Reusable safe tab via Intent (Redirect to clean Google Search page)
        Log.w(TAG, "remediateBlockedBrowserTab: home button unavailable, falling back to reused safe tab intent")
        navigateBrowserToSafeBlank(pkg)
        consecutiveBlockedUrlHits = 0
    }

    private fun remediateDistractingSubActivity(pkg: String, activityCls: String?) {
        val now = System.currentTimeMillis()
        if (now - lastSubActivityRemediationTime < 500L) {
            return // Debounce rapid accessibility events while back transition is in flight
        }

        if (activityCls != null && activityCls == lastRemediatedSubActivity && (now - lastSubActivityRemediationTime < 3500L)) {
            consecutiveSubActivityHits++
        } else {
            consecutiveSubActivityHits = 1
        }

        lastSubActivityRemediationTime = now
        lastRemediatedSubActivity = activityCls
        lastActiveActivityClass = null

        Log.w(TAG, "remediateDistractingSubActivity on $pkg ($activityCls) hits=$consecutiveSubActivityHits")

        // Educational Toast notice informing student
        val isGeneralVideo = "portal_video_flow" == activityCls || !isVideoPolicyAllowed()
        Handler(Looper.getMainLooper()).post {
            val msg = if (isGeneralVideo) {
                "⚠️ Videos are restricted during focus lockdown."
            } else {
                "⚠️ Short videos / reels are restricted during focus lockdown."
            }
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }

        // If repeated persistent attempts (> 3 rapid clicks into reels):
        if (consecutiveSubActivityHits > 3) {
            if (AppClassifier.isPortalSuperApp(pkg)) {
                Log.w(TAG, "Persistent sub-activity in portal app $pkg -> cleanly resetting to home search page")
                try {
                    val homeIntent = packageManager?.getLaunchIntentForPackage(pkg)
                    if (homeIntent != null) {
                        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(homeIntent)
                    }
                } catch (ignore: Exception) {}
                consecutiveSubActivityHits = 0
                return
            } else {
                Log.w(TAG, "Persistent reels bypass attempt detected in $pkg -> asserting QIEZKA lock")
                enforceBlock(pkg)
                return
            }
        }

        // Auto-Back: Instantly closes VideoTabActivity / feed flow and returns to search interface
        runOnMain { performGlobalAction(GLOBAL_ACTION_BACK) }
        Log.i(TAG, "remediateDistractingSubActivity: executed Auto-Back")
    }

    private fun closeCustomTab(passedRoot: AccessibilityNodeInfo?, pkg: String?): Boolean {
        var root = passedRoot
        var shouldRecycleRoot = false
        if (root == null) {
            try {
                root = rootInActiveWindow
                shouldRecycleRoot = true
            } catch (ignore: Exception) {}
        }
        if (root == null) return false

        try {
            val closeButtonIds = arrayOf(
                "com.android.chrome:id/close_button",
                "org.chromium.chrome:id/close_button",
                "com.sec.android.app.sbrowser:id/close_button"
            )
            for (closeId in closeButtonIds) {
                val closeNodes = root.findAccessibilityNodeInfosByViewId(closeId)
                if (!closeNodes.isNullOrEmpty()) {
                    for (node in closeNodes) {
                        if (node != null) {
                            try {
                                if (node.isClickable) {
                                    val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    Log.i(TAG, "closeCustomTab: ACTION_CLICK on $closeId -> $clicked")
                                    if (clicked) return true
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed close button click on $closeId: ${e.message}")
                            } finally {
                                node.recycle()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "closeCustomTab error: ${e.message}")
        } finally {
            if (shouldRecycleRoot && root != null) {
                root.recycle()
            }
        }
        return false
    }

    private fun clickBrowserHomeButton(passedRoot: AccessibilityNodeInfo?, pkg: String?): Boolean {
        var root = passedRoot
        var shouldRecycleRoot = false
        if (root == null) {
            try {
                root = rootInActiveWindow
                shouldRecycleRoot = true
            } catch (ignore: Exception) {}
        }
        if (root == null) return false

        try {
            val homeButtonIds = arrayOf(
                "com.android.chrome:id/home_button",
                "com.sec.android.app.sbrowser:id/location_bar_home_button",
                "org.chromium.chrome:id/home_button",
                "com.microsoft.emmx:id/home_button"
            )

            for (homeId in homeButtonIds) {
                val homeNodes = root.findAccessibilityNodeInfosByViewId(homeId)
                if (!homeNodes.isNullOrEmpty()) {
                    for (homeNode in homeNodes) {
                        if (homeNode != null) {
                            try {
                                if (homeNode.isClickable) {
                                    val clicked = homeNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    Log.i(TAG, "clickBrowserHomeButton: ACTION_CLICK on $homeId -> $clicked")
                                    if (clicked) {
                                        return true
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed home button click on $homeId: ${e.message}")
                            } finally {
                                homeNode.recycle()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "clickBrowserHomeButton error: ${e.message}")
        } finally {
            if (shouldRecycleRoot && root != null) {
                root.recycle()
            }
        }
        return false
    }

    private fun navigateBrowserToSafeBlank(pkg: String) {
        try {
            val safeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                setPackage(pkg)
                putExtra(Browser.EXTRA_APPLICATION_ID, packageName)
                putExtra("com.android.browser.application_id", packageName)
                putExtra(Browser.EXTRA_CREATE_NEW_TAB, false)
                putExtra("create_new_tab", false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(safeIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to navigate browser to safe tab: ${e.message}")
        }
    }

    private fun extractUrlFromBrowser(root: AccessibilityNodeInfo?, pkg: String?): String? {
        if (root == null) return null
        if (isLinkContextMenu(root, pkg)) return null

        // Layer 1: Input Focus Guard. If the address bar or search input is actively focused by the user,
        // typing or autocomplete is in progress. Flowchart: ALLOW_SEARCH (100% immune, uninterrupted).
        val focusNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusNode != null) {
            try {
                if (focusNode.isFocused || focusNode.isEditable) {
                    if (isAddressBarNode(focusNode)) {
                        return null // User is actively searching or typing in the browser's address bar
                    }
                }
            } finally {
                focusNode.recycle()
            }
        }

        // 1. Chrome / Chromium family: Query multiple known address bar & Custom Tab view IDs
        val chromeViewIds = arrayOf(
            "com.android.chrome:id/url_bar",
            "com.android.chrome:id/title_url",
            "com.android.chrome:id/search_box_text",
            "com.android.chrome:id/location_bar",
            "com.android.chrome:id/toolbar",
            "com.android.chrome:id/omnibox_text_field",
            "org.chromium.chrome:id/url_bar",
            "org.chromium.chrome:id/title_url"
        )
        for (id in chromeViewIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    if (node != null) {
                        if (node.isFocused) {
                            continue // Active typing or autocomplete in progress
                        }
                        val text = node.text
                        val desc = node.contentDescription
                        val valueStr = if (!text.isNullEmpty()) text.toString().trim() else if (!desc.isNullEmpty()) desc.toString().trim() else null
                        if (isValidUrlCandidate(valueStr)) return valueStr
                    }
                }
            }
        }

        // 2. Samsung Internet
        val sbrowserViewIds = arrayOf(
            "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "com.sec.android.app.sbrowser:id/url_bar",
            "com.sec.android.app.sbrowser:id/location_bar"
        )
        for (id in sbrowserViewIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    if (node != null) {
                        if (node.isFocused) {
                            continue // Active typing or autocomplete in progress
                        }
                        val text = node.text
                        val desc = node.contentDescription
                        val valueStr = if (!text.isNullEmpty()) text.toString().trim() else if (!desc.isNullEmpty()) desc.toString().trim() else null
                        if (isValidUrlCandidate(valueStr)) return valueStr
                    }
                }
            }
        }

        // 3. Firefox
        val firefoxViewIds = arrayOf(
            "org.mozilla.firefox:id/url_bar_title",
            "org.mozilla.firefox:id/toolbar",
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view"
        )
        for (id in firefoxViewIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    if (node != null) {
                        if (node.isFocused) {
                            continue // Active typing or autocomplete in progress
                        }
                        val text = node.text
                        val valueStr = if (!text.isNullEmpty()) text.toString().trim() else null
                        if (isValidUrlCandidate(valueStr)) return valueStr
                    }
                }
            }
        }

        // 4. Edge
        val edgeNodes = root.findAccessibilityNodeInfosByViewId("com.microsoft.emmx:id/url_bar")
        if (!edgeNodes.isNullOrEmpty()) {
            for (node in edgeNodes) {
                if (node != null) {
                    if (node.isFocused) {
                        continue // Active typing or autocomplete in progress
                    }
                    val text = node.text
                    val desc = node.contentDescription
                    val valueStr = if (!text.isNullEmpty()) text.toString().trim() else if (!desc.isNullEmpty()) desc.toString().trim() else null
                    if (isValidUrlCandidate(valueStr)) return valueStr
                }
            }
        }

        // 5. Brave
        val braveNodes = root.findAccessibilityNodeInfosByViewId("com.brave.browser:id/url_bar")
        if (!braveNodes.isNullOrEmpty()) {
            for (node in braveNodes) {
                if (node != null) {
                    if (node.isFocused) {
                        continue // Active typing or autocomplete in progress
                    }
                    val text = node.text
                    val desc = node.contentDescription
                    val valueStr = if (!text.isNullEmpty()) text.toString().trim() else if (!desc.isNullEmpty()) desc.toString().trim() else null
                    if (isValidUrlCandidate(valueStr)) return valueStr
                }
            }
        }

        // 6. 1DM & 1DM+ Browser
        val idmViewIds = arrayOf(
            "$pkg:id/search",
            "$pkg:id/search_container",
            "idm.internet.download.manager:id/search",
            "idm.internet.download.manager.plus:id/search"
        )
        for (id in idmViewIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    if (node != null) {
                        if (node.isFocused) {
                            continue // Active typing or autocomplete in progress
                        }
                        val text = node.text
                        val desc = node.contentDescription
                        val valueStr = if (!text.isNullEmpty()) text.toString().trim() else if (!desc.isNullEmpty()) desc.toString().trim() else null
                        if (isValidUrlCandidate(valueStr)) return valueStr
                    }
                }
            }
        }

        // 6b. Portal Super-Apps (NAVER Search & Browser)
        if ("com.nhn.android.search" == pkg) {
            val naverSearchIds = arrayOf(
                "com.nhn.android.search.InAppBrowser:id/search_window_edit",
                "com.nhn.android.search:id/search_window_edit",
                "com.nhn.android.search:id/nx_query"
            )
            for (id in naverSearchIds) {
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                if (!nodes.isNullOrEmpty()) {
                    for (node in nodes) {
                        if (node != null) {
                            if (node.isFocused) continue
                            val text = node.text
                            val desc = node.contentDescription
                            val valueStr = if (!text.isNullEmpty()) text.toString().trim() else if (!desc.isNullEmpty()) desc.toString().trim() else null
                            if (isValidUrlCandidate(valueStr)) return valueStr
                        }
                    }
                }
            }
        }

        // 7. Generic Heuristic Fallback through hierarchy (Targeting address bar & input nodes only)
        return findUrlInHierarchy(root, 0)
    }

    private fun CharSequence?.isNullEmpty(): Boolean {
        return this == null || this.isEmpty()
    }

    private fun findUrlInHierarchy(node: AccessibilityNodeInfo?, depth: Int): String? {
        if (node == null || depth > 8) return null

        val resId = node.viewIdResourceName
        if (resId != null && resId.lowercase(Locale.US).contains("context_menu")) {
            return null // Skip context menu overlays
        }
        var isAddressOrInput = false
        if (resId != null) {
            val lowerResId = resId.lowercase(Locale.US)
            if (lowerResId.contains("url_bar") || lowerResId.contains("location_bar") ||
                lowerResId.contains("address_bar") || lowerResId.contains("omnibar") ||
                lowerResId.contains("url_field") || lowerResId.contains("title_url") ||
                lowerResId.contains("custom_tab_toolbar") || lowerResId.endsWith(":id/search")) {
                isAddressOrInput = true
            }
        }

        if (isAddressOrInput) {
            val text = node.text
            val desc = node.contentDescription
            val valueStr = if (!text.isNullEmpty()) text.toString().trim() else if (!desc.isNullEmpty()) desc.toString().trim() else null
            if (node.isFocused) {
                return null // Active typing or autocomplete in progress
            }
            if (isValidUrlCandidate(valueStr)) {
                return valueStr
            }
        }

        val className = node.className
        if (className != null) {
            val lowerClass = className.toString().lowercase(Locale.US)
            if (lowerClass.contains("webview") || lowerClass.contains("render") || lowerClass.contains("contentview")) {
                return null
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = findUrlInHierarchy(child, depth + 1)
                if (found != null) return found
            }
        }
        return null
    }

    private fun resolveHostFromPackageManifest(pkg: String?): String? {
        if (pkg == null || pkg.startsWith("com.android.") || pkg.startsWith("com.google.android.") ||
            isLauncherApp(this, pkg) || packageName == pkg) {
            return null
        }
        val cached = packageManifestHostCache[pkg]
        if (cached != null) {
            return if (cached.isEmpty()) null else cached
        }

        try {
            val pm = packageManager
            if (pm != null) {
                val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply {
                    setPackage(pkg)
                }
                val resolved = pm.queryIntentActivities(viewIntent, PackageManager.GET_RESOLVED_FILTER)
                if (resolved != null) {
                    for (ri in resolved) {
                        val filter = ri.filter
                        if (filter != null && filter.countDataAuthorities() > 0) {
                            for (i in 0 until filter.countDataAuthorities()) {
                                val host = filter.getDataAuthority(i).host
                                if (!host.isNullOrBlank() && host.contains(".")) {
                                    val resolvedUrl = "https://${host.trim()}"
                                    packageManifestHostCache[pkg] = resolvedUrl
                                    return resolvedUrl
                                }
                            }
                        }
                    }
                }
            }
        } catch (ignore: Exception) {}

        packageManifestHostCache[pkg] = ""
        return null
    }

    fun isPackageBlocked(pkg: String?): Boolean {
        return isPackageBlocked(pkg, lastActiveActivityClass)
    }

    fun isPackageBlocked(pkg: String?, activityCls: String?): Boolean {
        if (pkg == null) return false
        if (pkg == packageName) return false
        if (isSystemOrLauncher(pkg, activityCls)) return false

        // Query app label for fast-path blacklist matching
        var appLabel: String? = null
        try {
            val pm = packageManager
            if (pm != null) {
                val ai = pm.getApplicationInfo(pkg, 0)
                val lbl = pm.getApplicationLabel(ai)
                if (lbl != null) appLabel = lbl.toString()
            }
        } catch (ignore: Exception) {}

        // STAGE 1 — MASTER VETO GATE: Strict Anti-Tamper, Bloatware & Evasion Shield
        if (AppClassifier.isStage1Vetoed(pkg, appLabel, activityCls)) return true

        // STAGE 2 — Branch 1: Web Browsers (Passed Stage 1, allowed at package level for WebClassifier inspection)
        if (isBrowserPackage(pkg, appLabel)) return false
        if (isKeyboardApp(pkg)) return false
        if (isLauncherApp(this, pkg)) return false
        if (KnownSafe.isCameraApp(this, pkg, appLabel)) return false

        // Gemini / Robin active inside Google App
        if (pkg == "com.google.android.googlequicksearchbox" && isGeminiActive(pkg, lastBrowserEventClass, null)) {
            return !isGeminiAllowed()
        }

        val whitelist = prefs?.getStringSet("whitelist", HashSet()) ?: HashSet()

        // ── STAGE 2 & STAGE 3: On-Device Local App Classifier (Initial Policy, Heuristics & Universal System Gateway) ──
        return AppClassifier.isPackageBlocked(this, pkg, whitelist)
    }

    private fun onTickerTick() {
        if (prefs == null) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        val currentPrefs = prefs ?: return

        val timeOffset = currentPrefs.getLong("time_offset", 0L)
        val effectiveNow = System.currentTimeMillis() + timeOffset
        val inOperatingHours = isInOperatingHours(effectiveNow)

        var isLockdownActive = currentPrefs.getBoolean("lockdown_active", false)
        var isConsequenceActive = currentPrefs.getBoolean("consequence_active", false)
        val lockEndTime = currentPrefs.getLong("lock_end_time", 0L)
        var isHardcore = "hardcore".equals(currentPrefs.getString("operating_mode", "safemode"), ignoreCase = true)

        // 1. Check if lockdown timer has expired
        if (isLockdownActive && !isConsequenceActive && lockEndTime > 0 && effectiveNow >= lockEndTime) {
            Log.i(TAG, "Ticker: lockdown timer expired without passing (effectiveNow=$effectiveNow >= $lockEndTime) -> consequence active")
            currentPrefs.edit()
                .putBoolean("consequence_active", true)
                .remove("lock_end_time")
                .apply()
            AlarmReceiver.cancelLockEndAlarm(this)
            AlarmReceiver.createNotificationChannels(this)
            val notifTitle = if (isHardcore) {
                "⚠️ Homework Expired — Consequence Active (Hardcore)"
            } else {
                "⚠️ Homework Expired — Consequence Active"
            }
            val notifMsg = if (isHardcore) {
                "Study timer expired without passing. Distracting apps remain restricted 24/7 (Hardcore Mode) until rescheduled and passed."
            } else {
                "Study timer expired without passing. Distracting apps remain restricted during operating hours (7 PM – 3 AM) until rescheduled and passed."
            }
            AlarmReceiver.showNotificationStatic(
                this,
                AlarmReceiver.NOTIF_ID_COMPLETED,
                AlarmReceiver.CHANNEL_ID_ALERTS,
                notifTitle,
                notifMsg,
                Notification.PRIORITY_HIGH,
                false
            )
            isConsequenceActive = true
        }

        // 1b. Check active lockdown countdown warnings
        if (isLockdownActive && lockEndTime > 0) {
            val remainingSec = (lockEndTime - effectiveNow) / 1000L
            if (remainingSec > 0) {
                checkAndFireLockdownTimerWarning(remainingSec)
            }
        }

        // 1c. Synchronize LocalDnsVpnService strictly with active session enforcement (Option B: Session-Only Web Guard)
        var isConsequenceEnforcing = isConsequenceActive && (isHardcore || inOperatingHours)
        var isEnforcing = isLockdownActive || isConsequenceEnforcing

        val webMode = currentPrefs.getString("web_protection_mode", "accessibility")
        val isVpnMode = "dual_hybrid".equals(webMode, ignoreCase = true)
        val shouldVpnRun = isVpnMode && isEnforcing

        if (shouldVpnRun) {
            if (!LocalDnsVpnService.isRunning) {
                if (android.net.VpnService.prepare(this) == null) {
                    LocalDnsVpnService.startVpn(this)
                }
            }
        } else {
            if (LocalDnsVpnService.isRunning) {
                LocalDnsVpnService.stopVpn(this)
            }
        }

        // 2. Check schedules from schedules_json
        val schedulesJson = currentPrefs.getString("schedules_json", null)
        if (!schedulesJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(schedulesJson)
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    if (!s.optBoolean("isActive", true)) continue

                    val timeStr = s.optString("activationTime", "")
                    if (!timeStr.contains(":")) continue

                    val parts = timeStr.split(":")
                    val schedHour = parts[0].trim().toInt()
                    val schedMin = parts[1].trim().toInt()
                    val durationMinutes = s.optInt("durationMinutes", 25)
                    val durationMs = durationMinutes * 60L * 1000L

                    val schedCal = Calendar.getInstance().apply {
                        timeInMillis = effectiveNow
                        set(Calendar.HOUR_OF_DAY, schedHour)
                        set(Calendar.MINUTE, schedMin)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val schedStartTime = schedCal.timeInMillis
                    val schedEndTime = schedStartTime + durationMs

                    // A) Check if schedule should start RIGHT NOW (today's window or midnight cross)
                    var inWindow = false
                    var windowEnd = 0L
                    if (effectiveNow >= schedStartTime && effectiveNow < schedEndTime) {
                        inWindow = true
                        windowEnd = schedEndTime
                    } else {
                        val yesterdayStart = schedStartTime - (24L * 3600L * 1000L)
                        val yesterdayEnd = yesterdayStart + durationMs
                        if (effectiveNow >= yesterdayStart && effectiveNow < yesterdayEnd) {
                            inWindow = true
                            windowEnd = yesterdayEnd
                        }
                    }

                    if (inWindow && !isLockdownActive) {
                        val lastCompletedEnd = currentPrefs.getLong("last_completed_window_end_" + s.optString("id", ""), 0L)
                        if (windowEnd <= lastCompletedEnd) {
                            // This schedule session was already completed or ended! Do NOT restart lockdown!
                            continue
                        }

                        Log.i(TAG, "Ticker: schedule ${s.optString("id")} starting NOW! (windowEnd=$windowEnd)")
                        currentPrefs.edit()
                            .putBoolean("lockdown_active", true)
                            .putLong("lock_end_time", windowEnd)
                            .putString("active_schedule_id", s.optString("id", ""))
                            .apply()
                        AlarmReceiver.scheduleLockEndAlarm(this, windowEnd, s.optString("id", ""))
                        AlarmReceiver.showNotificationStatic(
                            this,
                            AlarmReceiver.NOTIF_ID_STATUS,
                            AlarmReceiver.CHANNEL_ID_STATUS,
                            "🔒 QIEZKA Lockdown Engaged",
                            s.optString("title", "Study Session") + " is active (" + durationMinutes + " min). Distracting apps are blocked.",
                            Notification.PRIORITY_HIGH,
                            true
                        )
                        isLockdownActive = true
                        FloatingOverlayService.startService(this, windowEnd, s.optString("title", "Study Session"))

                        // Immediately kick out if user is currently inside a blocked app
                        val activePkg = detectCurrentForegroundPackage()
                        if (activePkg != null && isPackageBlocked(activePkg)) {
                            enforceBlock(activePkg)
                        }
                    }

                    // B) Check advance countdown warnings before schedule start
                    val diffSec = (schedStartTime - effectiveNow) / 1000L
                    if (diffSec in 1..1800) {
                        checkAndFireTickerWarning(s.optString("id", ""), diffSec)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ticker schedule check error: ${e.message}")
            }
        }

        // 3. State Evaluation Gate
        isHardcore = "hardcore".equals(currentPrefs.getString("operating_mode", "safemode"), ignoreCase = true)
        isConsequenceEnforcing = isConsequenceActive && (isHardcore || inOperatingHours)
        isEnforcing = isLockdownActive || isConsequenceEnforcing
        val hasSchedules = hasActiveSchedules()

        if (isConsequenceActive && !isHardcore && !inOperatingHours) {
            return // Safemode Daytime (3am - 7pm): ALLOW_IDLE
        }

        if (!isEnforcing && !hasSchedules) {
            return // Total System Idle: ALLOW_IDLE
        }

        // ── STAGE 1: Anti-Tamper & Task Killer Shield (Active Lockdown, Consequence, or Standby) ──
        if (interceptHomeLauncherChangeAttempt(null, null, null)) {
            return
        }

        val activeRoot = rootInActiveWindow
        if (activeRoot != null) {
            try {
                val p = activeRoot.packageName
                if (p != null) {
                    val rootPkg = p.toString()
                    if (rootPkg != packageName) {
                        if (interceptHomeLauncherChangeAttempt(null, activeRoot, rootPkg)) {
                            return
                        }
                        var appLabel: String? = null
                        try {
                            val pm = packageManager
                            if (pm != null) {
                                val ai = pm.getApplicationInfo(rootPkg, 0)
                                val lbl = pm.getApplicationLabel(ai)
                                if (lbl != null) appLabel = lbl.toString()
                            }
                        } catch (ignore: Exception) {}

                        val activeCls = resolveCandidateClass(rootPkg, lastActiveActivityClass, activeRoot)
                        if (AppClassifier.isSystemSettingsDialog(activeCls)) {
                            lastActiveActivityClass = activeCls
                        }
                        if (AppClassifier.isSettingsOrDeviceManager(rootPkg, appLabel, activeCls) || AppClassifier.isStage1Bloat(rootPkg, appLabel)) {
                            enforceBlock(rootPkg)
                            return
                        }

                        // STAGE 2 & 3: Only when Lockdown or Consequence is Active
                        if (isEnforcing) {
                            auditAndRemediatePictureInPicture()
                            if (isGeminiActive(rootPkg, null, activeRoot) && !isGeminiAllowed()) {
                                Log.w(TAG, "Ticker: detected Gemini active in foreground while AI policy is disabled")
                                enforceBlock("com.google.android.apps.bard")
                                return
                            }
                            if (!isSystemOrLauncher(rootPkg, activeCls)) {
                                if (isBrowserPackage(rootPkg, null)) {
                                    postBrowserAudit(rootPkg)
                                } else if (isPackageBlocked(rootPkg, activeCls)) {
                                    enforceBlock(rootPkg)
                                    return
                                }
                            }
                        }
                    }
                }
            } finally {
                activeRoot.recycle()
            }
        } else {
            // Fallback: When getRootInActiveWindow() is null (e.g. system transitions / secure surfaces)
            if (!isEnforcing) {
                return // Standby: normal apps allowed
            }

            auditAndRemediatePictureInPicture()

            val currentForegroundPkg = detectCurrentForegroundPackage()
            if (currentForegroundPkg != null && !isSystemOrLauncher(currentForegroundPkg) && currentForegroundPkg != packageName) {
                if (isBrowserPackage(currentForegroundPkg, null)) {
                    postBrowserAudit(currentForegroundPkg)
                } else if (isPackageBlocked(currentForegroundPkg)) {
                    enforceBlock(currentForegroundPkg)
                }
            }
        }
    }

    private fun checkAndFireTickerWarning(schedId: String, diffSec: Long) {
        var warnKey: String? = null
        var warnText: String? = null

        if (diffSec in 1795..1800) {
            warnKey = "${schedId}_30m"
            warnText = "⏰ Upcoming study session in 30 minutes! Wrap up your tasks."
        } else if (diffSec in 895..900) {
            warnKey = "${schedId}_15m"
            warnText = "⏰ Study session starts in 15 minutes! Prepare your materials."
        } else if (diffSec in 295..300) {
            warnKey = "${schedId}_5m"
            warnText = "⚠️ Lockdown in 5 minutes! Save your work on other apps."
        } else if (diffSec in 55..60) {
            warnKey = "${schedId}_1m"
            warnText = "⚠️ Lockdown in 1 minute! QIEZKA is preparing to lock."
        } else if (diffSec in 25..30) {
            warnKey = "${schedId}_30s"
            warnText = "🚨 Lockdown starting in 30 seconds! QIEZKA is engaging."
        } else if (diffSec in 5..10) {
            warnKey = "${schedId}_10s"
            warnText = "🚨 Lockdown starting in 10 seconds!"
        }

        if (warnKey != null && warnText != null && !firedWarningKeys.contains(warnKey)) {
            firedWarningKeys.add(warnKey)
            Log.i(TAG, "Firing ticker advance warning: $warnText")
            AlarmReceiver.showNotificationStatic(
                this,
                AlarmReceiver.NOTIF_ID_WARNING,
                AlarmReceiver.CHANNEL_ID_ALERTS,
                "⏰ QIEZKA Upcoming Lockdown",
                warnText,
                Notification.PRIORITY_HIGH,
                false
            )
        }
    }

    private fun checkAndFireLockdownTimerWarning(remSec: Long) {
        var warnKey: String? = null
        var warnText: String? = null

        if (remSec in 1795..1800) {
            warnKey = "lock_timer_30m"
            warnText = "⏳ 30 minutes remaining in active lockdown."
        } else if (remSec in 895..900) {
            warnKey = "lock_timer_15m"
            warnText = "⏳ 15 minutes remaining in active lockdown."
        } else if (remSec in 295..300) {
            warnKey = "lock_timer_5m"
            warnText = "⏳ 5 minutes remaining! Finish your homework submission."
        } else if (remSec in 55..60) {
            warnKey = "lock_timer_1m"
            warnText = "⏳ 1 minute remaining before lockdown auto-unlocks."
        } else if (remSec in 25..30) {
            warnKey = "lock_timer_30s"
            warnText = "⏳ 30 seconds remaining!"
        }

        if (warnKey != null && warnText != null && !firedWarningKeys.contains(warnKey)) {
            firedWarningKeys.add(warnKey)
            Log.i(TAG, "Firing lockdown timer warning: $warnText")
            AlarmReceiver.showNotificationStatic(
                this,
                AlarmReceiver.NOTIF_ID_WARNING,
                AlarmReceiver.CHANNEL_ID_ALERTS,
                "⏳ QIEZKA Lock Timer",
                warnText,
                Notification.PRIORITY_HIGH,
                false
            )
        }
    }

    fun enforceBlock(pkg: String?) {
        if (pkg == null || pkg == packageName || isSystemOrLauncher(pkg)) {
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastBlockTimestamp < 350L && pkg == lastBlockedPackage) {
            return // Debounce rapid accessibility events from the same launch attempt
        }
        lastBlockTimestamp = now
        lastBlockedPackage = pkg

        Log.w(TAG, "enforceBlock on distracting app: $pkg — directly bringing QIEZKA lock to front (no home redirection, no back press)")

        // Directly bring QIEZKA lock screen to front immediately (no synthetic back press)
        launchLockOverlay()

        // Follow-up burst check to re-assert QIEZKA Lock overlay if another activity transitions in
        tickerHandler.postDelayed({
            val fg = detectCurrentForegroundPackage()
            if (fg != null && fg != packageName && !isSystemOrLauncher(fg)) {
                if (isPackageBlocked(fg)) {
                    launchLockOverlay()
                }
            }
        }, 100L)

        tickerHandler.postDelayed({
            val fg = detectCurrentForegroundPackage()
            if (fg != null && fg != packageName && !isSystemOrLauncher(fg)) {
                if (isPackageBlocked(fg)) {
                    launchLockOverlay()
                }
            }
        }, 250L)
    }

    fun auditAndRemediatePictureInPicture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        try {
            val winList = windows
            if (winList.isNullOrEmpty()) return

            for (w in winList) {
                if (w != null && w.isInPictureInPictureMode) {
                    val root = w.root
                    if (root != null) {
                        try {
                            val p = root.packageName
                            if (p != null) {
                                val pipPkg = p.toString()
                                if (pipPkg == packageName || isSystemOrLauncher(pipPkg)) {
                                    continue
                                }

                                val isPortal = AppClassifier.isPortalSuperApp(pipPkg)
                                val isBlocked = isPackageBlocked(pipPkg)
                                val isVideoRestricted = !isVideoPolicyAllowed()

                                if (isBlocked || (isPortal && isVideoRestricted) || (isVideoRestricted && isMediaAppOrPip(pipPkg))) {
                                    val now = System.currentTimeMillis()
                                    if (now - lastPipRemediationTime > 1200L) {
                                        lastPipRemediationTime = now
                                        Log.w(TAG, "🛡️ Unauthorized Picture-in-Picture / miniplayer overlay detected: $pipPkg -> Remediating")

                                        // 1. Standard Accessibility action to dismiss floating window
                                        try {
                                            root.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
                                        } catch (ignore: Exception) {}

                                        // 2. Educational Toast notice (throttled)
                                        if (now - lastBrowserToastTime >= 4000L) {
                                            lastBrowserToastTime = now
                                            Handler(Looper.getMainLooper()).post {
                                                Toast.makeText(applicationContext,
                                                    "⚠️ Video miniplayer / Picture-in-Picture is restricted during focus lockdown.",
                                                    Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        // 3. For portal super-apps (like NAVER): bring search homepage to front, killing PiP task
                                        if (isPortal) {
                                            try {
                                                val homeIntent = packageManager?.getLaunchIntentForPackage(pipPkg)
                                                if (homeIntent != null) {
                                                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                                    startActivity(homeIntent)
                                                }
                                            } catch (ignore: Exception) {}
                                        } else {
                                            // 4. For blocked apps (YouTube, Bilibili, Netflix, etc.): enforce lock overlay
                                            enforceBlock(pipPkg)
                                        }
                                    }
                                }
                            }
                        } finally {
                            root.recycle()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "auditAndRemediatePictureInPicture error: ${e.message}")
        }
    }

    private fun isMediaAppOrPip(pkg: String?): Boolean {
        if (pkg == null) return false
        val lower = pkg.lowercase(Locale.US)
        return lower.contains("video") || lower.contains("media") || lower.contains("player") ||
               lower.contains("stream") || lower.contains("tube") || lower.contains("movie") ||
               lower.contains("bstar") || lower.contains("bilibili") || lower.contains("netflix") ||
               lower.contains("tiktok") || lower.contains("twitch") || lower.contains("nhn.android")
    }

    fun onScheduleStartTriggered() {
        try {
            val activePkg = detectCurrentForegroundPackage()
            if (activePkg != null && activePkg != packageName && isPackageBlocked(activePkg)) {
                enforceBlock(activePkg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onScheduleStartTriggered error: ${e.message}")
        }
    }

    fun detectCurrentForegroundPackage(): String? {
        // 1. Z-Order Window Audit: Check if any active or visible application window belongs to a blocked package.
        try {
            val winList = windows
            if (!winList.isNullOrEmpty()) {
                for (w in winList) {
                    if (w.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        val root = w.root
                        if (root != null) {
                            try {
                                val p = root.packageName
                                if (p != null) {
                                    val pkg = p.toString()
                                    val winTitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && w.title != null) w.title.toString() else null
                                    val candidateCls = resolveCandidateClass(pkg, if (!winTitle.isNullEmpty()) winTitle else lastActiveActivityClass, root)
                                    if (isSystemOrLauncher(pkg, candidateCls) || pkg == packageName || "com.android.systemui" == pkg) {
                                        // continue
                                    } else if ("com.google.android.googlequicksearchbox" == pkg && isGeminiActive(pkg, null, root) && !isGeminiAllowed()) {
                                        lastForegroundPackage = "com.google.android.apps.bard"
                                        return "com.google.android.apps.bard"
                                    } else if (!isSystemOrLauncher(pkg, candidateCls) && isPackageBlocked(pkg, candidateCls)) {
                                        lastForegroundPackage = pkg
                                        return pkg
                                    }
                                }
                            } finally {
                                root.recycle()
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val wt = w.title
                            if (wt != null) {
                                val titleStr = wt.toString()
                                if (isHomeChangeInterceptorActive() && isHomeAppSelectionTitle(titleStr)) {
                                    Log.w(TAG, "🛡️ Sandboxed window matched Home App Selection title: $titleStr")
                                    lastForegroundPackage = "com.android.permissioncontroller"
                                    return "com.android.permissioncontroller"
                                }
                            }
                        }
                    }
                }
            }
        } catch (ignore: Exception) {}

        // 2. Primary: Inspect active root window directly
        try {
            val activeRoot = rootInActiveWindow
            if (activeRoot != null) {
                try {
                    val p = activeRoot.packageName
                    if (p != null) {
                        val rootPkg = p.toString()
                        if ("com.google.android.googlequicksearchbox" == rootPkg && isGeminiActive(rootPkg, null, null) && !isGeminiAllowed()) {
                            lastForegroundPackage = "com.google.android.apps.bard"
                            return "com.google.android.apps.bard"
                        }
                        if ("com.android.systemui" != rootPkg) {
                            lastForegroundPackage = rootPkg
                            return rootPkg
                        }
                    }
                } finally {
                    activeRoot.recycle()
                }
            }
        } catch (ignore: Exception) {}

        // 3. Secondary: Inspect interactive application windows in Z-order
        try {
            val winList = windows
            if (!winList.isNullOrEmpty()) {
                // First pass: Active or Focused application window
                for (w in winList) {
                    if (w.type == AccessibilityWindowInfo.TYPE_APPLICATION && (w.isActive || w.isFocused)) {
                        val root = w.root
                        if (root != null) {
                            try {
                                val p = root.packageName
                                if (p != null) {
                                    val pkg = p.toString()
                                    val winTitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && w.title != null) w.title.toString() else null
                                    val candidateCls = resolveCandidateClass(pkg, if (!winTitle.isNullEmpty()) winTitle else lastActiveActivityClass, root)
                                    if (isSystemOrLauncher(pkg, candidateCls) || pkg == packageName || "com.android.systemui" == pkg) {
                                        // continue
                                    } else if ("com.google.android.googlequicksearchbox" == pkg && isGeminiActive(pkg, null, root) && !isGeminiAllowed()) {
                                        lastForegroundPackage = "com.google.android.apps.bard"
                                        return "com.google.android.apps.bard"
                                    } else if (!isSystemOrLauncher(pkg, candidateCls)) {
                                        lastForegroundPackage = pkg
                                        return pkg
                                    }
                                }
                            } finally {
                                root.recycle()
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val wt = w.title
                            if (wt != null) {
                                val titleStr = wt.toString()
                                if (isHomeChangeInterceptorActive() && isHomeAppSelectionTitle(titleStr)) {
                                    Log.w(TAG, "🛡️ Sandboxed window matched Home App Selection title: $titleStr")
                                    lastForegroundPackage = "com.android.permissioncontroller"
                                    return "com.android.permissioncontroller"
                                }
                            }
                        }
                    }
                }
                // Second pass: Top visible application window
                for (w in winList) {
                    if (w.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        val root = w.root
                        if (root != null) {
                            try {
                                val p = root.packageName
                                if (p != null) {
                                    val pkg = p.toString()
                                    val winTitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && w.title != null) w.title.toString() else null
                                    val candidateCls = resolveCandidateClass(pkg, if (!winTitle.isNullEmpty()) winTitle else lastActiveActivityClass, root)
                                    if (isSystemOrLauncher(pkg, candidateCls) || pkg == packageName || "com.android.systemui" == pkg) {
                                        // continue
                                    } else if ("com.google.android.googlequicksearchbox" == pkg && isGeminiActive(pkg, null, root) && !isGeminiAllowed()) {
                                        lastForegroundPackage = "com.google.android.apps.bard"
                                        return "com.google.android.apps.bard"
                                    } else if (!isSystemOrLauncher(pkg, candidateCls)) {
                                        lastForegroundPackage = pkg
                                        return pkg
                                    }
                                }
                            } finally {
                                root.recycle()
                            }
                        }
                    }
                }
            }
        } catch (ignore: Exception) {}

        // 4. Fallback: last recorded non-system/non-launcher foreground package from events
        if (lastForegroundPackage != null && !isSystemOrLauncher(lastForegroundPackage, lastActiveActivityClass)) {
            return lastForegroundPackage
        }

        return lastForegroundPackage
    }

    private fun launchLockOverlay() {
        try {
            val launch = packageManager?.getLaunchIntentForPackage(packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                val isConsequence = prefs?.getBoolean("consequence_active", false) ?: false
                launch.putExtra("route", if (isConsequence) "logs" else "locked")

                // Android 14/15/16 BAL compliance:
                // Starting an activity directly from background is blocked by OS ActivityTaskManager.
                // Wrap in PendingIntent with MODE_BACKGROUND_ACTIVITY_START_ALLOWED.
                var optionsBundle: android.os.Bundle? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    try {
                        val activityOptions = ActivityOptions.makeBasic()
                        val method = activityOptions.javaClass.getMethod("setPendingIntentBackgroundActivityStartMode", Int::class.javaPrimitiveType)
                        method.invoke(activityOptions, 1) // ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED = 1
                        optionsBundle = activityOptions.toBundle()
                    } catch (ignore: Throwable) {}
                }

                val pi = PendingIntent.getActivity(
                    this,
                    0,
                    launch,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    if (optionsBundle != null) {
                        pi.send(this, 0, null, null, null, null, optionsBundle)
                    } else {
                        pi.send()
                    }
                } catch (e: Exception) {
                    startActivity(launch)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "launchLockOverlay failed: ${e.message}")
        }
    }

    private fun handleSystemUiEvent(event: AccessibilityEvent) {
        val classNameChar = event.className ?: return
        val cls = classNameChar.toString().lowercase(Locale.ROOT)

        // Collapse QS tile panel (but allow the plain notification shade)
        if (cls.contains("qs") ||
            cls.contains("quicksetting") ||
            cls.contains("brightnesscontroller") ||
            cls.contains("tilerecord") ||
            cls.contains("quickstatusbar")) {
            collapseQsPanel()
        }
    }

    private fun collapseQsPanel() {
        try {
            val statusBarService = getSystemService("statusbar")
            if (statusBarService != null) {
                val collapseMethod: Method = statusBarService.javaClass.getMethod("collapsePanels")
                collapseMethod.isAccessible = true
                collapseMethod.invoke(statusBarService)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "StatusBarManager reflection failed: ${e.message}")
        }
        // Fallback
        try {
            performGlobalAction(GLOBAL_ACTION_BACK)
        } catch (e: Exception) {
            Log.e(TAG, "Fallback collapse failed: ${e.message}")
        }
    }

    private fun checkAndAutoStartScheduledLock(prefs: SharedPreferences): Boolean {
        val schedulesJson = prefs.getString("schedules_json", null)
        if (schedulesJson.isNullOrBlank()) return false

        try {
            val arr = JSONArray(schedulesJson)
            val timeOffset = prefs.getLong("time_offset", 0L)
            val effectiveNow = System.currentTimeMillis() + timeOffset

            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                if (!s.optBoolean("isActive", true)) continue

                val timeStr = s.optString("activationTime", "")
                if (!timeStr.contains(":")) continue

                val parts = timeStr.split(":")
                val schedHour = parts[0].trim().toInt()
                val schedMin = parts[1].trim().toInt()
                val durationMinutes = s.optInt("durationMinutes", 25)
                val durationMs = durationMinutes * 60L * 1000L

                val schedCal = Calendar.getInstance().apply {
                    timeInMillis = effectiveNow
                    set(Calendar.HOUR_OF_DAY, schedHour)
                    set(Calendar.MINUTE, schedMin)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val schedStartTime = schedCal.timeInMillis
                val schedEndTime = schedStartTime + durationMs

                // Check today's window
                if (effectiveNow >= schedStartTime && effectiveNow < schedEndTime) {
                    val lastCompletedEnd = prefs.getLong("last_completed_window_end_" + s.optString("id", ""), 0L)
                    if (schedEndTime <= lastCompletedEnd) {
                        continue
                    }
                    prefs.edit()
                        .putBoolean("lockdown_active", true)
                        .putLong("lock_end_time", schedEndTime)
                        .putString("active_schedule_id", s.optString("id", ""))
                        .apply()
                    AlarmReceiver.scheduleLockEndAlarm(this, schedEndTime, s.optString("id", ""))
                    AlarmReceiver.createNotificationChannels(this)
                    AlarmReceiver.showNotificationStatic(
                        this,
                        AlarmReceiver.NOTIF_ID_STATUS,
                        AlarmReceiver.CHANNEL_ID_STATUS,
                        "🔒 QIEZKA Lockdown Engaged",
                        s.optString("title", "Study Session") + " is active (" + durationMinutes + " min). Distracting apps are blocked.",
                        Notification.PRIORITY_HIGH,
                        true
                    )
                    FloatingOverlayService.startService(this, schedEndTime, s.optString("title", "Study Session"))
                    Log.i(TAG, "Native auto-start triggered for schedule: ${s.optString("id", "")}, lockEnd=$schedEndTime")
                    return true
                }

                // Check yesterday's window (crossed midnight)
                val yesterdayStart = schedStartTime - (24L * 3600L * 1000L)
                val yesterdayEnd = yesterdayStart + durationMs
                if (effectiveNow >= yesterdayStart && effectiveNow < yesterdayEnd) {
                    val lastCompletedEnd = prefs.getLong("last_completed_window_end_" + s.optString("id", ""), 0L)
                    if (yesterdayEnd <= lastCompletedEnd) {
                        continue
                    }
                    prefs.edit()
                        .putBoolean("lockdown_active", true)
                        .putLong("lock_end_time", yesterdayEnd)
                        .putString("active_schedule_id", s.optString("id", ""))
                        .apply()
                    AlarmReceiver.scheduleLockEndAlarm(this, yesterdayEnd, s.optString("id", ""))
                    AlarmReceiver.createNotificationChannels(this)
                    AlarmReceiver.showNotificationStatic(
                        this,
                        AlarmReceiver.NOTIF_ID_STATUS,
                        AlarmReceiver.CHANNEL_ID_STATUS,
                        "🔒 QIEZKA Lockdown Engaged",
                        s.optString("title", "Study Session") + " is active (" + durationMinutes + " min). Distracting apps are blocked.",
                        Notification.PRIORITY_HIGH,
                        true
                    )
                    FloatingOverlayService.startService(this, yesterdayEnd, s.optString("title", "Study Session"))
                    Log.i(TAG, "Native auto-start (midnight-cross) triggered for schedule: ${s.optString("id", "")}")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkAndAutoStartScheduledLock error: ${e.message}")
        }
        return false
    }

    private fun countInstalledLauncherCandidates(node: AccessibilityNodeInfo?, maxDepth: Int = 8): Int {
        if (node == null || maxDepth < 0) return 0
        var count = 0
        try {
            val txt = node.text
            if (txt != null && isInstalledLauncherCandidate(txt.toString())) {
                count++
            } else {
                val desc = node.contentDescription
                if (desc != null && isInstalledLauncherCandidate(desc.toString())) {
                    count++
                }
            }
            if (count >= 2) return count

            val childCount = node.childCount
            for (i in 0 until childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    try {
                        count += countInstalledLauncherCandidates(child, maxDepth - 1)
                        if (count >= 2) return count
                    } finally {
                        child.recycle()
                    }
                }
            }
        } catch (ignore: Exception) {}
        return count
    }

    private fun findRootNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        var curr: AccessibilityNodeInfo = node
        while (true) {
            val parent = curr.parent ?: break
            if (curr != node) {
                try { curr.recycle() } catch (ignore: Exception) {}
            }
            curr = parent
        }
        return curr
    }

    private fun isFastHomeRoleEvent(event: AccessibilityEvent?, root: AccessibilityNodeInfo? = null): Boolean {
        if (event == null) return false

        val pkgChar = event.packageName
        val pkgStr = pkgChar?.toString()?.lowercase(Locale.US) ?: ""

        val clsChar = event.className
        val clsStr = clsChar?.toString()?.lowercase(Locale.US) ?: ""

        // 1. Direct Activity class name match (sub-microsecond memory check, 100% language-independent)
        if (clsStr.contains("requestroleactivity") || clsStr.contains("defaultappactivity") ||
            clsStr.contains("homesettingsactivity") || clsStr.contains("defaulthomepicker") ||
            clsStr.contains("rolesearchactivity") || clsStr.contains("specialappaccessactivity") ||
            clsStr.contains("resolveractivity")) {
            return true
        }

        // 2. Structural role / launcher picker interception across ALL packages (language-independent)
        if (root != null) {
            val rootPkg = root.packageName?.toString()?.lowercase(Locale.US) ?: pkgStr
            if (isHomeAppSelectionPackageOrRoot(rootPkg, root)) {
                return true
            }
        } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val src = event.source
            var trueRoot = if (src != null) findRootNode(src) else null
            var needsRecycle = (trueRoot != null && trueRoot != src)
            if (trueRoot == null) {
                trueRoot = rootInActiveWindow
                if (trueRoot != null) needsRecycle = true
            }
            if (trueRoot != null) {
                try {
                    val rootPkg = trueRoot.packageName?.toString()?.lowercase(Locale.US) ?: pkgStr
                    if (isHomeAppSelectionPackageOrRoot(rootPkg, trueRoot)) {
                        return true
                    }
                } finally {
                    if (needsRecycle) {
                        try { trueRoot.recycle() } catch (ignore: Exception) {}
                    }
                    try { src?.recycle() } catch (ignore: Exception) {}
                }
            }
        }

        // 3. Proactive tap interception: User taps "Not set as default" or "Nova is not default launcher" in launcher settings
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val texts = event.text
            if (texts != null) {
                for (t in texts) {
                    if (t != null) {
                        val txt = t.toString().lowercase(Locale.US)
                        if (txt.contains("not set as default") ||
                            txt.contains("default launcher") ||
                            (txt.contains("set as default") && txt.contains("home"))) {
                            return true
                        }
                    }
                }
            }
            val cd = event.contentDescription
            if (cd != null) {
                val cStr = cd.toString().lowercase(Locale.US)
                if (cStr.contains("not set as default") || cStr.contains("default launcher")) {
                    return true
                }
            }
        }

        return false
    }

    private fun interceptHomeLauncherChangeAttempt(event: AccessibilityEvent?, providedRoot: AccessibilityNodeInfo?, pkg: String?): Boolean {
        // Master Gate: ONLY activate interceptor if SIREN is default launcher AND a session/schedule is active
        if (!isHomeChangeInterceptorActive()) {
            return false
        }

        var lowerPkg = pkg?.lowercase(Locale.US) ?: ""
        if (lowerPkg.isEmpty() && event?.packageName != null) {
            lowerPkg = event.packageName.toString().lowercase(Locale.US)
        }

        if (lowerPkg == packageName || lowerPkg == "com.android.systemui" || lowerPkg == "com.siren.homeproxy") {
            return false
        }

        var classStr = ""
        if (event?.className != null) {
            classStr = event.className.toString().lowercase(Locale.US)
        } else if (providedRoot?.className != null) {
            classStr = providedRoot.className.toString().lowercase(Locale.US)
        }

        // 1. Fast Event-level class checks (0ms - instant memory evaluation, 100% language-independent)
        if (classStr.contains("defaultappactivity") || classStr.contains("requestroleactivity") ||
            classStr.contains("homesettingsactivity") || classStr.contains("defaulthomepicker") ||
            classStr.contains("rolesearchactivity") || classStr.contains("specialappaccessactivity") ||
            classStr.contains("resolveractivity")) {
            val rootToDismiss = (providedRoot ?: event?.source)?.let { findRootNode(it) } ?: rootInActiveWindow
            evictHomeLauncherChange(rootToDismiss)
            return true
        }

        // 2. Window Audit via getWindows() (Essential for Android 14/15/16 modal dialogs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val winList = windows
                if (winList != null) {
                    for (w in winList) {
                        if ((w.type == AccessibilityWindowInfo.TYPE_APPLICATION || w.type == AccessibilityWindowInfo.TYPE_SYSTEM) && (w.isActive || w.isFocused)) {
                            val wRoot = w.root
                            if (wRoot != null) {
                                try {
                                    val wp = wRoot.packageName
                                    if (wp != null) {
                                        val wpStr = wp.toString().lowercase(Locale.US)
                                        if (wpStr == packageName || wpStr == "com.android.systemui" || wpStr == "com.siren.homeproxy") {
                                            continue
                                        }
                                        if (isHomeAppSelectionPackageOrRoot(wpStr, wRoot)) {
                                            evictHomeLauncherChange(wRoot)
                                            return true
                                        }
                                    }
                                    val wt = w.title
                                    if (wt != null && isHomeAppSelectionTitle(wt.toString())) {
                                        evictHomeLauncherChange(wRoot)
                                        return true
                                    }
                                } finally {
                                    wRoot.recycle()
                                }
                            }
                        }
                    }
                }
            } catch (ignore: Exception) {}
        }

        var root = if (providedRoot != null) findRootNode(providedRoot) else null
        var needsRecycle = (root != null && root != providedRoot)
        try {
            if (root == null) {
                val src = event?.source
                if (src != null) {
                    root = findRootNode(src)
                    if (root != src) {
                        needsRecycle = true
                    }
                    try { src.recycle() } catch (ignore: Exception) {}
                }
            }
            if (root == null) {
                root = rootInActiveWindow
                if (root != null) needsRecycle = true
            }

            if (root == null) return false

            val rootPkg = root.packageName
            val effectivePkg = rootPkg?.toString()?.lowercase(Locale.US) ?: lowerPkg
            if (effectivePkg == packageName || effectivePkg == "com.android.systemui" || effectivePkg == "com.siren.homeproxy") {
                return false
            }

            if (isHomeAppSelectionPackageOrRoot(effectivePkg, root)) {
                evictHomeLauncherChange(root)
                return true
            }

            // Third party launchers prompting to set default launcher
            if (effectivePkg.contains("launcher") && !isLauncherApp(this, effectivePkg)) {
                val defaultHomeNodes = root.findAccessibilityNodeInfosByText("Default")
                if (!defaultHomeNodes.isNullOrEmpty()) {
                    val launcherNodes = root.findAccessibilityNodeInfosByText("Launcher")
                    if (!launcherNodes.isNullOrEmpty()) {
                        evictHomeLauncherChange(root)
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking home launcher change attempt: ${e.message}")
        } finally {
            if (root != null && needsRecycle) {
                try {
                    root.recycle()
                } catch (ignore: Exception) {}
            }
        }

        return false
    }

    private fun isHomeAppSelectionPackageOrRoot(pkg: String?, root: AccessibilityNodeInfo?): Boolean {
        if (pkg == null || root == null) return false
        val rootPkg = root.packageName?.toString()?.lowercase(Locale.US) ?: ""
        val lower = if (rootPkg.isNotEmpty() && rootPkg != "android") rootPkg else pkg.lowercase(Locale.US)
        if (lower == packageName || lower == "com.android.systemui" || lower == "com.siren.homeproxy") {
            return false
        }

        try {
            // 1. Check exact activity class name on root if available (language-independent)
            val rootClass = root.className
            if (rootClass != null) {
                val rc = rootClass.toString().lowercase(Locale.US)
                if (rc.contains("requestroleactivity") || rc.contains("defaultappactivity") ||
                    rc.contains("homesettingsactivity") || rc.contains("defaulthomepicker") ||
                    rc.contains("rolesearchactivity") || rc.contains("resolveractivity")) {
                    return true
                }
            }

            // 2. Normal launcher desktop / app drawer is 100% immune:
            // An installed launcher running in normal desktop mode must NEVER be blocked!
            val btn1 = root.findAccessibilityNodeInfosByViewId("android:id/button1")
            val btn2 = root.findAccessibilityNodeInfosByViewId("android:id/button2")
            val dontAsk = root.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/dont_ask_again")
            val hasDialogControls = !btn1.isNullOrEmpty() || !btn2.isNullOrEmpty() || !dontAsk.isNullOrEmpty()
            recycleNodes(btn1)
            recycleNodes(btn2)
            recycleNodes(dontAsk)

            if (isLauncherApp(this, lower) && !hasDialogControls) {
                return false
            }

            // 3. PermissionController Role UI: id/list containing candidate launcher items, or dont_ask_again checkbox
            if (lower.contains("permissioncontroller") || rootPkg.contains("permissioncontroller")) {
                val listNodes = root.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/list")
                if (!listNodes.isNullOrEmpty()) {
                    recycleNodes(listNodes)
                    val launcherCount = countInstalledLauncherCandidates(root)
                    if (launcherCount >= 1) {
                        return true
                    }
                    val dontAskNodes = root.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/dont_ask_again")
                    if (!dontAskNodes.isNullOrEmpty()) {
                        recycleNodes(dontAskNodes)
                        return true
                    }
                }
                val dontAskNodes = root.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/dont_ask_again")
                if (!dontAskNodes.isNullOrEmpty()) {
                    recycleNodes(dontAskNodes)
                    return true
                }
            }

            // 4. Settings Default Home Picker UI:
            if (lower.contains("settings") || rootPkg.contains("settings")) {
                val launcherCount = countInstalledLauncherCandidates(root)
                if (launcherCount >= 2) {
                    return true
                }
            }

            // 5. Modal Dialogs with candidate items & dialog confirmation controls
            // (Strictly excludes full-screen launcher desktops and Recents, which lack dialog buttons)
            if (hasDialogControls) {
                val launcherCount = countInstalledLauncherCandidates(root)
                if (launcherCount >= 1) {
                    return true
                }
            }

            // 6. Check title text in permissioncontroller role UI or Settings
            val titleNodes = root.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/title")
            if (!titleNodes.isNullOrEmpty()) {
                for (t in titleNodes) {
                    val txt = t.text
                    if (txt != null && (isHomeAppSelectionTitle(txt.toString()) || isInstalledLauncherCandidate(txt.toString()))) {
                        recycleNodes(titleNodes)
                        return true
                    }
                }
                recycleNodes(titleNodes)
            }
        } catch (ignore: Exception) {}
        return false
    }

    private fun clickCancelOnNode(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        var dismissed = false
        try {
            // 1. Check "Don't ask again" checkbox if present to permanently deny future role requests
            val dontAskList = root.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/dont_ask_again")
            if (!dontAskList.isNullOrEmpty()) {
                for (cb in dontAskList) {
                    if (cb != null && cb.isCheckable && !cb.isChecked) {
                        cb.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.i(TAG, "Checked 'Don't ask again' checkbox on role dialog")
                    }
                }
                recycleNodes(dontAskList)
            }

            // 2. Click standard Cancel button (android:id/button2)
            val cancelButtons = root.findAccessibilityNodeInfosByViewId("android:id/button2")
            if (!cancelButtons.isNullOrEmpty()) {
                for (btn in cancelButtons) {
                    if (btn != null && btn.isClickable && btn.isEnabled) {
                        dismissed = btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.i(TAG, "Dismissed home launcher dialog by clicking Cancel button (android:id/button2): $dismissed")
                        if (dismissed) break
                    }
                }
                recycleNodes(cancelButtons)
            }

            // 3. Fallback: text search for "Cancel"
            if (!dismissed) {
                val textCancel = root.findAccessibilityNodeInfosByText("Cancel")
                if (!textCancel.isNullOrEmpty()) {
                    for (btn in textCancel) {
                        if (btn != null && btn.isClickable && btn.isEnabled) {
                            dismissed = btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            Log.i(TAG, "Dismissed home launcher dialog by clicking text 'Cancel': $dismissed")
                            if (dismissed) break
                        }
                    }
                    recycleNodes(textCancel)
                }
            }
        } catch (ignore: Exception) {}
        return dismissed
    }

    private fun evictHomeLauncherChange() {
        evictHomeLauncherChange(null)
    }

    private fun evictHomeLauncherChange(dialogRoot: AccessibilityNodeInfo?) {
        Log.w(TAG, "🛡️ Intercepted Default Home App / Launcher change attempt! Directly asserting QIEZKA Lock overlay.")

        // 1. Programmatically dismiss dialog via Cancel button click if dialogRoot or activeRoot is available
        var clicked = clickCancelOnNode(dialogRoot)
        if (!clicked && dialogRoot == null) {
            try {
                val activeRoot = rootInActiveWindow
                if (activeRoot != null) {
                    clicked = clickCancelOnNode(activeRoot)
                    activeRoot.recycle()
                }
            } catch (ignore: Exception) {}
        }

        // 2. Fallback: if Cancel button click was not confirmed, perform GLOBAL_ACTION_HOME to return to SIREN
        if (!clicked) {
            Log.w(TAG, "Cancel button not confirmed; performing GLOBAL_ACTION_HOME fallback.")
            performGlobalAction(GLOBAL_ACTION_HOME)
        }

        // 3. Anti-Tamper Toast feedback
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext,
                "🛡️ Changing default home launcher is restricted by QIEZKA",
                Toast.LENGTH_SHORT).show()
        }

        // 4. Re-assert QIEZKA Lock overlay directly (no back press, using BAL-compliant PendingIntent)
        launchLockOverlay()
    }

    override fun onInterrupt() {
        // Required override
    }
}
