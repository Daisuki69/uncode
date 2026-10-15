package com.uncode.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * On-device local app classification engine.
 *
 * Implements a 5-layer offline rule hierarchy using Android ApplicationInfo metadata
 * (declared categories, application labels, and semantic keywords) to dynamically
 * distinguish legitimate study/productivity tools from hostile or distracting apps.
 *
 * 100% offline — preserves the client-side BYOK architecture with zero server dependencies
 * and zero Gemini API token overhead.
 */
object AppClassifier {

    private const val TAG = "AppClassifier"

    // Standard Android ApplicationInfo category constants (API 26+)
    const val CATEGORY_UNDEFINED: Int = -1
    const val CATEGORY_GAME: Int = 0
    const val CATEGORY_AUDIO: Int = 1
    const val CATEGORY_VIDEO: Int = 2
    const val CATEGORY_IMAGE: Int = 3
    const val CATEGORY_SOCIAL: Int = 4
    const val CATEGORY_NEWS: Int = 5
    const val CATEGORY_MAPS: Int = 6
    const val CATEGORY_PRODUCTIVITY: Int = 7
    const val CATEGORY_ACCESSIBILITY: Int = 8 // API 31+

    /**
     * Thread-safe cache mapping packageName -> isBlocked (true = BLOCK, false = ALLOW).
     * Avoids querying PackageManager repeatedly during frequent accessibility events.
     */
    private val decisionCache: MutableMap<String, Boolean> = ConcurrentHashMap()

    /**
     * Backward-compatible helper for PackageManager.getApplicationInfo.
     * Uses ApplicationInfoFlags on Android 13+ (API 33+) and deprecated overload on older releases.
     */
    @Suppress("DEPRECATION")
    private fun getApplicationInfoCompat(pm: PackageManager, packageName: String, flags: Long = 0L): ApplicationInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags))
        } else {
            pm.getApplicationInfo(packageName, flags.toInt())
        }
    }

    /**
     * Backward-compatible helper for PackageManager.getPackageInfo.
     * Uses PackageInfoFlags on Android 13+ (API 33+) and deprecated overload on older releases.
     */
    @Suppress("DEPRECATION")
    private fun getPackageInfoCompat(pm: PackageManager, packageName: String, flags: Long = 0L): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags))
        } else {
            pm.getPackageInfo(packageName, flags.toInt())
        }
    }

    /**
     * Backward-compatible helper for PackageManager.queryIntentActivities.
     */
    @Suppress("DEPRECATION")
    private fun queryIntentActivitiesCompat(pm: PackageManager, intent: Intent, flags: Long = 0L): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags))
        } else {
            pm.queryIntentActivities(intent, flags.toInt())
        }
    }

    /**
     * Backward-compatible helper for PackageManager.queryIntentServices.
     */
    @Suppress("DEPRECATION")
    private fun queryIntentServicesCompat(pm: PackageManager, intent: Intent, flags: Long = 0L): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(flags))
        } else {
            pm.queryIntentServices(intent, flags.toInt())
        }
    }

    /**
     * Dynamically identifies SIM Card Toolkit (STK), MMS, Carrier Notifications (e.g. Globe Telecom, Smart, DITO),
     * USSD, Flash SMS (Class 0), and Cell Broadcast alerts across various Android OEMs and carriers.
     */
    @JvmStatic
    fun isSimOrCarrierService(pkg: String?, appLabel: String?): Boolean {
        if (pkg == null) return false

        val lowerPkg = pkg.lowercase(Locale.US)

        // SIM Toolkit (STK) package patterns
        if (lowerPkg == "com.android.stk" || lowerPkg == "com.android.stk2" ||
            lowerPkg.contains(".stk") || lowerPkg.endsWith(".stk") ||
            lowerPkg.contains("simtoolkit") || lowerPkg.contains("simapp") ||
            lowerPkg.contains("simsetting") || lowerPkg.contains("simprocessor") ||
            lowerPkg.contains("simcard") || lowerPkg.contains("simcontacts") ||
            lowerPkg.contains(".uim")
        ) {
            return true
        }

        // MMS & native messaging services
        if (lowerPkg == "com.android.mms" || lowerPkg.contains(".mms") || lowerPkg.endsWith(".mms") ||
            lowerPkg.contains("mms.service") || lowerPkg == "com.google.android.apps.messaging" ||
            lowerPkg == "com.samsung.android.messaging" || lowerPkg.contains("messaging")
        ) {
            // Guard: ensure it is not a third-party social messenger (e.g. Facebook Messenger)
            if (!lowerPkg.contains("facebook") && !lowerPkg.contains("orca") && !lowerPkg.contains("telegram") && !lowerPkg.contains("whatsapp")) {
                return true
            }
        }

        // Cell Broadcast & Emergency alerts
        if (lowerPkg.contains("cellbroadcast") || lowerPkg.contains("emergencyalert") || lowerPkg.contains(".cbr")) {
            return true
        }

        // Carrier default apps and configurations (including Globe, Smart, DITO)
        if (lowerPkg.contains("carrierdefaultapp") || lowerPkg.contains("carrierconfig") ||
            lowerPkg.contains("telephonyui") || lowerPkg.contains(".ims") || lowerPkg.contains("imsservice")
        ) {
            return true
        }

        // Philippine carriers (Globe, Smart, DITO)
        if (lowerPkg.startsWith("ph.com.globe") || lowerPkg.startsWith("com.globe") ||
            lowerPkg.startsWith("ph.com.smart") || lowerPkg.startsWith("com.smart") ||
            lowerPkg.startsWith("ph.dito") || lowerPkg.startsWith("com.dito") ||
            (lowerPkg.contains("globe") && (lowerPkg.contains("sim") || lowerPkg.contains("service") || lowerPkg.contains("carrier")))
        ) {
            return true
        }

        // Application Label inspection (catches OEM-customized STK and carrier dialogs)
        if (!appLabel.isNullOrBlank()) {
            val lowerLabel = appLabel.lowercase(Locale.US)
            if (lowerLabel == "sim toolkit" || lowerLabel == "sim card toolkit" ||
                lowerLabel == "sim menu" || lowerLabel == "menu ng sim" ||
                lowerLabel == "stk" || lowerLabel.startsWith("stk ") ||
                lowerLabel.contains("globe services") || lowerLabel.contains("smart menu") ||
                lowerLabel.contains("dito menu") || lowerLabel.contains("cell broadcast") ||
                lowerLabel.contains("emergency alert") || lowerLabel.contains("wireless alerts") ||
                lowerLabel.contains("wireless emergency alerts") || lowerLabel == "mms service" ||
                lowerLabel == "carrier default app" || lowerLabel.contains("carrier services")
            ) {
                return true
            }
        }

        return false
    }

    @JvmStatic
    fun isSimOrCarrierService(pkg: String?): Boolean = isSimOrCarrierService(pkg, null)

    @JvmField
    val KNOWN_BROWSER_PACKAGES: Set<String> = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.opera.gx",
        "com.brave.browser",
        "com.duckduckgo.mobile.android",
        "com.sec.android.app.sbrowser",
        "com.sec.android.app.sbrowser.beta",
        "com.mi.globalbrowser",
        "com.vivo.browser",
        "com.heytap.browser",
        "com.coloros.browser",
        "com.huawei.browser",
        "com.android.browser",
        "idm.internet.download.manager",
        "idm.internet.download.manager.plus",
        "idm.internet.download.manager.lite"
    )

    /**
     * Dynamically resolved web browser packages discovered via Android OS native Intent resolution.
     * Pre-warmed at service startup and cached in-memory for instant O(1) performance.
     */
    private val dynamicBrowserPackages: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Pre-warms the dynamic browser cache by querying Android OS native intent resolution
     * for all applications that register Intent.ACTION_VIEW + Intent.CATEGORY_BROWSABLE with https scheme.
     * Pure native OS classification — completely language-independent.
     */
    @JvmStatic
    fun refreshBrowserCache(context: Context?) {
        if (context == null) return
        try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            val activities = queryIntentActivitiesCompat(pm, intent, 0L)
            for (info in activities) {
                val candidatePkg = info.activityInfo?.packageName
                if (candidatePkg != null && !isDisallowedPortalSuperApp(candidatePkg)) {
                    dynamicBrowserPackages.add(candidatePkg)
                }
            }
            Log.d(TAG, "Dynamic browser cache refreshed, found ${dynamicBrowserPackages.size} browsers")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh dynamic browser cache", e)
        }
    }

    @JvmStatic
    fun addDynamicBrowserPackageForTesting(pkg: String?) {
        if (pkg != null) dynamicBrowserPackages.add(pkg)
    }

    @JvmStatic
    fun clearDynamicBrowserPackagesForTesting() {
        dynamicBrowserPackages.clear()
    }

    /**
     * Universal Web Browser Resolution Gate (Stage 2).
     *
     * Evaluates whether an application is a legitimate general-purpose web browser.
     * Web browsers are permitted at the package level during active lockdown because
     * WebClassifier dynamically inspects visited URLs, search queries, and DOM nodes in real-time.
     */
    @JvmStatic
    fun isBrowserPackage(context: Context?, pkg: String?, appLabel: String?): Boolean {
        if (pkg.isNullOrBlank()) return false

        // 1. STAGE 1 — MASTER VETO GATE: Strict Anti-Tamper, Bloatware & Evasion Shield
        // Proxy browsers, cloud renderers, and onion bypasses are strictly vetoed here
        if (isStage1Vetoed(pkg, appLabel)) return false

        // 1b. Disallowed / Unmanaged Super-Apps & Portal Browsers are strictly blocked regardless
        if (isDisallowedPortalSuperApp(pkg)) {
            return false
        }

        // 2. Explicit Proxy & VPN Keyword Exclusion
        val lower = pkg.lowercase(Locale.ROOT).trim()
        if (lower.contains("proxy") || lower.contains("vpn") || lower.contains("unblock")) {
            return false
        }

        // 3. Fast-Path O(1) Lookup: Standard Known Set, Pre-warmed Dynamic Cache, or Allowed Portal Super-App
        if (KNOWN_BROWSER_PACKAGES.contains(pkg) || dynamicBrowserPackages.contains(pkg) || isPortalSuperApp(pkg)) {
            return true
        }

        // 4. 1DM / IDM Downloaders
        if (pkg.startsWith("idm.internet.download.manager")) return true

        // 5. Standard Latin Substring Fallback for Browsers, WebAPKs, PWAs & TWAs
        if (lower.contains("browser") || lower.contains("chrome") || lower.contains("chromium") ||
            lower.contains("webapk") || lower.contains(".twa") || lower.endsWith(".twa") ||
            lower.contains(".pwa") || lower.endsWith(".pwa") || lower.contains("firefox") || lower.contains("explorer")
        ) {
            return true
        }

        // 6. Universal Native Android OS Component & Intent Resolution for Web Containers (TWAs, PWAs, CustomTabs)
        if (context != null) {
            try {
                val pm = context.packageManager
                // Check if package declares AndroidX Trusted Web Activity service
                val twaIntent = Intent("android.support.customtabs.trusted.TRUSTED_WEB_ACTIVITY_SERVICE").apply {
                    setPackage(pkg)
                }
                val twaServices = queryIntentServicesCompat(pm, twaIntent, 0L)
                if (twaServices.isNotEmpty()) {
                    dynamicBrowserPackages.add(pkg)
                    return true
                }

                // Check general browsable intent activities
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
                val resolved = queryIntentActivitiesCompat(pm, intent, 0L)
                for (info in resolved) {
                    if (info.activityInfo != null && pkg == info.activityInfo.packageName) {
                        if (!isDisallowedPortalSuperApp(pkg)) {
                            dynamicBrowserPackages.add(pkg)
                            return true
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return false
    }

    @JvmStatic
    fun isBrowserPackage(context: Context?, pkg: String?): Boolean = isBrowserPackage(context, pkg, null)

    @JvmStatic
    fun isBrowserPackage(pkg: String?): Boolean = isBrowserPackage(null, pkg, null)

    /**
     * Identifies in-app browser and custom tab activities inside non-browser host apps
     * (e.g. Facebook Messenger, Instagram, TikTok, Telegram, Twitter/X, Discord, Reddit, Slack, LINE, etc.)
     */
    @JvmStatic
    fun isInAppBrowserActivity(activityCls: String?): Boolean {
        if (activityCls.isNullOrBlank()) return false
        val lower = activityCls.lowercase(Locale.ROOT)
        return lower.contains("browserlite") ||
               lower.contains(".browser.lite.") ||
               lower.contains("customtabactivity") ||
               lower.contains("customtabsactivity") ||
               lower.contains("chromecustomtab") ||
               lower.contains("inappbrowser") ||
               lower.contains("inappweb") ||
               lower.contains("webviewactivity") ||
               lower.contains("webbrowseractivity") ||
               lower.contains("internalbrowser") ||
               lower.contains("webcontaineractivity") ||
               lower.contains("urlhandleractivity") ||
               lower.endsWith(".browseractivity")
    }

    /**
     * Checks if a package is a Progressive Web App (PWA), Trusted Web Activity (TWA),
     * or Chromium WebAPK container shortcut.
     */
    @JvmStatic
    fun isPwaOrWebApk(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        val lower = pkg.trim().lowercase(Locale.ROOT)
        return lower.startsWith("org.chromium.webapk.") ||
               lower.contains(".webapk") ||
               lower.endsWith(".webapk") ||
               lower.contains(".twa") ||
               lower.endsWith(".twa") ||
               lower.contains(".pwa") ||
               lower.endsWith(".pwa")
    }

    /**
     * Curated Allowed Portal Super-Apps. Exactly two official super-apps are permitted during lockdown:
     * 1. Baidu Search & Portal App (China Gaokao/Kaoyan search & study)
     * 2. NAVER Search & Portal App (South Korea Suneung/CSAT search & study)
     */
    @JvmField
    val ALLOWED_PORTAL_SUPERAPPS: Set<String> = setOf(
        "com.baidu.searchbox",          // Baidu Search & Portal App (China)
        "com.nhn.android.search"        // NAVER Search & Portal App (South Korea)
    )

    // Backward-compatibility alias
    @JvmField
    val KNOWN_PORTAL_SUPERAPPS: Set<String> = ALLOWED_PORTAL_SUPERAPPS

    /**
     * Recognized unmanaged or disguised super-apps / portal browsers that aggressively inject
     * viral reels, news feeds, or entertainment hubs. Strictly blocked during active lockdown regardless.
     */
    @JvmField
    val DISALLOWED_PORTAL_SUPERAPPS: Set<String> = setOf(
        "com.transsion.phoenix",        // Phoenix Browser (viral video & news portal)
        "com.UCMobile",                 // UC Browser
        "com.uc.browser.en",            // UC Browser Global
        "com.opera.mini.native"         // Opera Mini with feed/notification injection
    )

    @JvmStatic
    fun isPortalSuperApp(pkg: String?): Boolean {
        if (pkg == null) return false
        return ALLOWED_PORTAL_SUPERAPPS.contains(pkg.trim())
    }

    @JvmStatic
    fun isDisallowedPortalSuperApp(pkg: String?): Boolean {
        if (pkg == null) return false
        return DISALLOWED_PORTAL_SUPERAPPS.contains(pkg.trim())
    }

    /**
     * Identifies distracting sub-activities inside portal super-apps (such as short-video reels,
     * video feed flows, or novel readers) while allowing primary search and educational browsing.
     */
    @JvmStatic
    fun isDistractingSubActivity(@Suppress("UNUSED_PARAMETER") pkg: String?, activityCls: String?): Boolean {
        if (activityCls == null) return false
        val lowerCls = activityCls.lowercase(Locale.ROOT)

        // 1. Short Video Feeds & Reels Player Activities (Baidu VideoTabActivity, Naver ClipActivity)
        if (lowerCls.contains(".video.feedflow.") ||
            lowerCls.contains("videotabactivity") ||
            lowerCls.contains("shortvideoactivity") ||
            lowerCls.contains("feedflowactivity") ||
            lowerCls.contains("reelsactivity") ||
            lowerCls.contains("videoflowactivity") ||
            lowerCls.contains(".clip.ui.") ||
            lowerCls.contains("clipactivity") ||
            lowerCls.contains("clipvieweractivity")
        ) {
            return true
        }

        // 2. Web Novel / Serialized Fiction Hubs
        if (lowerCls.contains("novelhomeactivity") ||
            lowerCls.contains("novelreaderactivity")
        ) {
            return true
        }

        return false
    }

    /**
     * Identifies in-page video player, feed flow, or reels container view IDs inside
     * portal super-apps (such as Baidu LightSearchActivity, Naver UniverseActivity) where video playback
     * is mounted natively into the view hierarchy without launching a distinct activity.
     */
    @JvmStatic
    fun isPortalSuperAppVideoViewId(pkg: String?, viewId: String?): Boolean {
        if (pkg == null || viewId == null) return false
        val lowerId = viewId.lowercase(Locale.ROOT)

        // 1. Baidu Search & Portal App (com.baidu.searchbox)
        if ("com.baidu.searchbox" == pkg) {
            return lowerId.contains("video_flow_cmp_player") ||
                   lowerId.contains("video_flow_tab_component") ||
                   lowerId.contains("video_item_portrait_root") ||
                   lowerId.contains("first_init_video_item_container") ||
                   lowerId.contains("video_flow_cmp_list") ||
                   lowerId.contains("video_flow_next_big_card") ||
                   lowerId.contains("video_flow_cmp_seek_bar")
        }

        // 2. NAVER Search & Portal App (com.nhn.android.search)
        if ("com.nhn.android.search" == pkg) {
            return lowerId.contains("container_clip_viewpager") ||
                   lowerId.contains("clip_follow_view_pager") ||
                   lowerId.contains("videoview") ||
                   lowerId.contains("videogroup") ||
                   lowerId.contains("container_clip_nested_scrollable_host") ||
                   lowerId.contains("shortentsnowviewpager") ||
                   lowerId.contains("clipcontentsoundtoggle") ||
                   lowerId.contains("clip_root") ||
                   lowerId.contains("clip_player") ||
                   lowerId.contains("clip_player_view") ||
                   lowerId.contains("clip_view_pager") ||
                   lowerId.contains("clip_content_layout") ||
                   lowerId.contains("clip_form")
        }

        // 3. Unmanaged / Disallowed Super-Apps (defensive detection)
        if ("com.transsion.phoenix" == pkg) {
            return lowerId.contains("video_player") ||
                   lowerId.contains("short_video_flow") ||
                   lowerId.contains("feed_video_player")
        }

        if ("com.ucmobile".equals(pkg, ignoreCase = true) || "com.uc.browser.en".equals(pkg, ignoreCase = true)) {
            return lowerId.contains("video_player") ||
                   lowerId.contains("video_feed_root") ||
                   lowerId.contains("reels_container")
        }

        return false
    }

    /**
     * Recognized Direct Messaging & Communication applications.
     * Unlike social media feeds, messaging apps can be whitelisted by users in Allowed Apps.
     */
    @JvmField
    val KNOWN_MESSAGING_PACKAGES: Set<String> = setOf(
        "org.telegram.messenger",
        "org.telegram.plus",
        "org.thunderdog.challegram",
        "nekox.messenger",
        "org.forkgram.messenger",
        "org.telegram.BApplication",
        "com.facebook.orca",
        "com.facebook.mlite",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.thoughtcrime.securesms",
        "com.viber.voip",
        "jp.naver.line.android",
        "com.tencent.mm",
        "com.discord",
        "com.Slack",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms"
    )

    @JvmStatic
    fun isMessagingApp(pkg: String?, appLabel: String?): Boolean {
        if (pkg == null) return false
        val lowerPkg = pkg.lowercase(Locale.ROOT).trim()
        if (KNOWN_MESSAGING_PACKAGES.contains(lowerPkg)) {
            return true
        }
        // Exclude social network feeds explicitly
        if (lowerPkg.contains("facebook.katana") || lowerPkg.contains("facebook.lite") ||
            lowerPkg.contains("instagram") || lowerPkg.contains("twitter") ||
            lowerPkg.contains("reddit") || lowerPkg.contains("tiktok") ||
            lowerPkg.contains("musically")
        ) {
            return false
        }
        if (lowerPkg.contains("telegram") || lowerPkg.contains("challegram") ||
            lowerPkg.contains("nekogram") || lowerPkg.contains("forkgram") ||
            lowerPkg.contains("bgram") || lowerPkg.contains("messenger") ||
            lowerPkg.contains("whatsapp") || lowerPkg.contains(".signal") ||
            lowerPkg.contains("viber") || lowerPkg.contains("wechat") ||
            lowerPkg.contains(".line") || lowerPkg.contains("orca")
        ) {
            return true
        }
        if (!appLabel.isNullOrBlank()) {
            val lowerLabel = appLabel.lowercase(Locale.ROOT).trim()
            if ((lowerLabel.contains("facebook") && !lowerLabel.contains("messenger")) || lowerLabel.contains("instagram") ||
                lowerLabel.contains("twitter") || lowerLabel.contains("reddit") ||
                lowerLabel.contains("tiktok")
            ) {
                return false
            }
            if (lowerLabel.contains("telegram") || lowerLabel.contains("messenger") ||
                lowerLabel.contains("whatsapp") || lowerLabel.contains("signal") ||
                lowerLabel.contains("viber") || lowerLabel.contains("wechat") ||
                lowerLabel.contains("challegram") || lowerLabel.contains("nekogram")
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Recognized Online Class, Lecture & Video Conferencing applications.
     * Essential communication and class infrastructure (Google Meet, Zoom, Teams, Webex).
     */
    @JvmField
    val KNOWN_CONFERENCE_PACKAGES: Set<String> = setOf(
        "com.google.android.apps.tachyon",
        "com.google.android.apps.meetings",
        "us.zoom.videomeetings",
        "com.microsoft.teams",
        "com.cisco.webex.meetings"
    )

    @JvmStatic
    fun isMeetingOrVideoConferenceApp(pkg: String?, appLabel: String?): Boolean {
        if (pkg == null) return false
        val lowerPkg = pkg.lowercase(Locale.ROOT).trim()
        if (KNOWN_CONFERENCE_PACKAGES.contains(lowerPkg)) {
            return true
        }
        if (lowerPkg.contains(".tachyon") || lowerPkg.contains(".zoom.") ||
            lowerPkg.contains("teams.quickshare") || lowerPkg.contains("webex")
        ) {
            return true
        }
        if (!appLabel.isNullOrBlank()) {
            val lowerLabel = appLabel.lowercase(Locale.ROOT).trim()
            if (lowerLabel == "meet" || lowerLabel == "google meet" ||
                lowerLabel == "zoom" || lowerLabel == "zoom workplace" ||
                lowerLabel == "teams" || lowerLabel == "microsoft teams" ||
                lowerLabel == "webex" || lowerLabel == "webex meetings"
            ) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun isCameraApp(context: Context?, pkg: String?, appLabel: String?): Boolean {
        return KnownSafe.isCameraApp(context, pkg, appLabel)
    }

    @JvmStatic
    fun isCameraIntentHandler(pm: PackageManager?, pkg: String?): Boolean {
        if (pm == null || pkg.isNullOrBlank()) return false
        try {
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { setPackage(pkg) }
            val captureList = queryIntentActivitiesCompat(pm, captureIntent, 0L)
            if (captureList.isNotEmpty()) return true

            val stillIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply { setPackage(pkg) }
            val stillList = queryIntentActivitiesCompat(pm, stillIntent, 0L)
            if (stillList.isNotEmpty()) return true

            val secureIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE).apply { setPackage(pkg) }
            val secureList = queryIntentActivitiesCompat(pm, secureIntent, 0L)
            if (secureList.isNotEmpty()) return true
        } catch (_: Exception) {}
        return false
    }

    /**
     * Remote Desktop, Screen Sharing, and Screen Mirroring packages.
     * Intercepted to prevent using desktop streaming or Kali NetHunter VNC/KeX as a bypass workaround.
     */
    @JvmField
    val REMOTE_DESKTOP_PACKAGES: Set<String> = setOf(
        "com.teamviewer.teamviewer.market.mobile",
        "com.teamviewer.quicksupport.market",
        "com.teamviewer.host.market",
        "com.anydesk.anydeskandroid",
        "com.anydesk.adcontrol.ad1",
        "com.carriez.rustdesk",
        "com.google.chromeremotedesktop",
        "com.microsoft.rdc.android",
        "com.splashtop.remote.pad.v2",
        "com.splashtop.remote",
        "com.realvnc.viewer.android",
        "tv.parsec.client",
        "com.limelight",
        "com.valvesoftware.steamlink",
        "com.sand.airdroid",
        "com.sand.airmirror",
        "com.apowersoft.mirror",
        "com.koushikdutta.vysor",
        "info.dvkr.screenstream",
        // Kali NetHunter & Magisk Screen Share / VNC
        "com.offsec.nethunter",
        "com.offsec.nethunter.kex",
        "com.offsec.nhkex",
        "net.christianbeier.droidvnc_ng",
        "com.termux.x11"
    )

    @JvmStatic
    fun isRemoteDesktopOrScreenShare(pkg: String?, appLabel: String?): Boolean {
        if (pkg == null) return false
        val lowerPkg = pkg.lowercase(Locale.ROOT).trim()
        if (REMOTE_DESKTOP_PACKAGES.contains(lowerPkg)) {
            return true
        }
        if (lowerPkg.contains("teamviewer") || lowerPkg.contains("anydesk") ||
            lowerPkg.contains("rustdesk") || lowerPkg.contains("remotedesktop") ||
            lowerPkg.contains("screenmirror") || lowerPkg.contains("screenshare") ||
            lowerPkg.contains("screenstream") || lowerPkg.contains("splashtop") ||
            lowerPkg.contains(".parsec.") || lowerPkg.contains("airdroid") ||
            lowerPkg.contains("airmirror") || lowerPkg.contains("apowermirror") ||
            lowerPkg.contains("nethunter") || lowerPkg.contains("nhkex") ||
            lowerPkg.contains("droidvnc") || lowerPkg.contains("vncviewer") ||
            lowerPkg.contains(".vnc.")
        ) {
            return true
        }
        if (!appLabel.isNullOrBlank()) {
            val lowerLabel = appLabel.lowercase(Locale.ROOT).trim()
            if (lowerLabel.contains("teamviewer") || lowerLabel.contains("anydesk") ||
                lowerLabel.contains("rustdesk") || lowerLabel.contains("remote desktop") ||
                lowerLabel.contains("screen share") || lowerLabel.contains("screen mirror") ||
                lowerLabel.contains("screen stream") || lowerLabel.contains("splashtop") ||
                lowerLabel.contains("parsec") || lowerLabel.contains("moonlight") ||
                lowerLabel.contains("steam link") || lowerLabel.contains("vysor") ||
                lowerLabel.contains("airdroid") || lowerLabel.contains("nethunter") ||
                lowerLabel.contains("kex") || lowerLabel.contains("droidvnc") ||
                lowerLabel.contains("vnc viewer")
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Negative Distraction Keywords (Milestone 17: Widened Recognition Engine).
     * If an app's label contains any of these terms, it is strictly BLOCKED,
     * even if the APK falsely declares itself as CATEGORY_PRODUCTIVITY.
     */
    private val NEGATIVE_LABEL_KEYWORDS = arrayOf(
        // Gaming & Gambling (Widened Recognition)
        "game", "games", "gaming", "arcade", "puzzle", "simulator", "tycoon", "rpg", "mmorpg", "mmo",
        "fps", "tps", "battle royale", "racing", "dungeon", "quest", "brawl", "clash", "casino", "poker",
        "slots", "jackpot", "betting", "lottery", "roulette", "blackjack", "solitaire", "idle", "clicker",
        "hypercasual", "survivor", "arena", "runner", "crafting", "tower defense", "mahjong", "bingo",
        "gacha", "otome", "visual novel", "tamagotchi", "virtual pet", "block craft", "zombie", "shooter",
        "sniper", "defense", "multiplayer", "board game", "card game", "minigame",
        // Rhythm Games & Tap Games
        "rhythm game", "coxeta", "costheta", "phigros", "arcaea", "cytus", "deemo", "dynamix",
        "kalpa", "lanota", "rotaeno", "musedash", "muse dash", "project sekai",

        // Remote Desktop, Screen Share & VNC Workarounds
        "teamviewer", "anydesk", "rustdesk", "remote desktop", "screen share", "screen mirror",
        "screen stream", "splashtop", "parsec", "moonlight", "steam link", "vysor", "airdroid",
        "airmirror", "apowermirror", "nethunter", "kex", "droidvnc", "vnc viewer",

        // Parallel Space, Virtual Containers & App Cloners (Milestone 17)
        "parallel space", "dual space", "dual app", "multi space", "multiple accounts", "2accounts",
        "clone app", "app cloner", "virtual android", "virtual space", "second space", "dual clone",
        "super clone", "do multiple", "water clone", "island", "shelter", "vmos", "vphonegaga", "f1 vm",
        "f1vm", "x8 sandbox", "two accounts", "space lite", "parallel lite", "multi parallel",

        // Cheating, Modding & App Hiders
        "lucky patcher", "gameguardian", "cheat", "hack", "patcher", "app hider", "hide apps",
        "calculator vault", "photo vault", "gallery lock", "secret vault",

        // Casual Dating & Hookups
        "dating", "tinder", "bumble", "flirt", "randomchat", "hookup", "omegle", "ometv", "chat room",

        // Short-Dramas, Web Novels, Comics & Anime Streamers
        "short drama", "shortmax", "reelshort", "dramabox", "goodshort", "snackshort", "moboreels",
        "netshort", "webtoon", "manga", "manhwa", "manhua", "comic", "comics", "anime", "webnovel",
        "light novel", "fanfiction", "wattpad", "wuxia", "livestream", "live stream", "broadcast",
        "kissasian", "bilibili", "loklok", "cloudstream", "stremio", "onstream",

        // Social Feeds, Video & Forum Distractions
        "tiktok", "tik tok", "douyin", "reddit",

        // Online Shopping & E-Commerce Trap Applications
        "shopping", "online shopping", "ecommerce", "e-commerce", "shopee", "lazada", "shein", "temu",
        "aliexpress", "taobao", "tmall", "amazon shopping", "ebay", "tokopedia", "bukalapak", "coupang",
        "11st", "gmarket", "carousell", "pinduoduo", "mercari", "rakuten shopping", "flipkart"
    )

    private val NEGATIVE_PKG_SUBSTRINGS = arrayOf(
        ".game.", ".games.", ".gaming.", ".casino.", ".poker.", ".slots.", ".bet.",
        ".arcade.", ".simulator.", ".tycoon.", ".dating.", ".hookup.", ".cheat.",
        ".cloner.", ".dualspace.", ".parallel.", ".vmos.", ".virtual.",
        // Rhythm Game Signatures
        ".costheta.", ".coxeta.", ".phigros.", ".arcaea.", ".cytus.", ".dynamix.",
        ".musedash.", ".teamrhythmicals.", ".rhythm.",
        // Remote Desktop, Screen Share & VNC Workarounds
        ".teamviewer.", ".anydesk.", ".rustdesk.", ".remotedesktop.", ".screenshare.",
        ".screenmirror.", ".screenstream.", ".splashtop.", ".parsec.", ".airdroid.",
        ".nethunter.", ".kex.", ".droidvnc.",
        // Milestone 17: Container & Sandbox Signatures
        ".clone.", ".dual.", ".double.", ".secondspace.", ".appcloner.", ".island.",
        ".shelter.", ".vphonegaga.", ".f1player.", ".gspace.", ".vault.", ".gallerylock.",
        ".multispace.", ".superclone.", ".2accounts.", ".x8zs.", ".shortdrama.", ".dramabox.",
        ".reelshort.", ".shortmax.", ".goodshort.", ".webtoon.", ".manga.", ".manhwa.", ".comic.",
        ".webnovel.", ".wattpad.", ".gacha.", ".rpg.", ".brawl.",
        // Short video & social signatures
        ".musically.", ".trill.", ".aweme.",
        // Stage 1 Bloatware & Game Booster Signatures (UAD-NG Ground Truth)
        "joyose", "gamecenter", "gamebooster", "gamemode", "gamehome", "gamespace",
        "shortvideo", "mipicks", "palmstore", "glance",
        // Online Shopping & E-Commerce Package Signatures
        ".shopping.", ".shopee.", ".lazada.", ".temu.", ".shein.", ".aliexpress.", ".taobao.",
        ".tmall.", ".coupang.", ".tokopedia.", ".bukalapak.", ".carousell.", ".flipkart.", ".pinduoduo."
    )

    /**
     * Positive Academic & Educational Keywords.
     * Used to promote unknown apps (such as unlisted university portals, local study hubs,
     * or specialized textbook readers) with CATEGORY_UNDEFINED to ALLOWED.
     */
    private val POSITIVE_ACADEMIC_KEYWORDS = arrayOf(
        // Academic & School Institutions
        "study", "school", "university", "college", "campus", "academy", "course",
        "coursework", "class", "classroom", "lecture", "curriculum", "syllabus",
        "student", "faculty", "edu",
        // Learning Tools, Science, Math & Languages
        "reader", "pdf", "epub", "ebook", "dictionary", "thesaurus", "vocab",
        "vocabulary", "grammar", "translate", "translator", "formula", "calculator",
        "desmos", "geometry", "algebra", "physics", "chemistry", "biology", "science",
        "history", "anatomy", "flashcard", "flashcards", "quizlet", "quiz", "quizzes",
        "gizmo", "saveall", "tutor", "tutoring", "exam", "exams", "testprep", "revision",
        "spaced repetition", "studysmarter", "kahoot", "quizizz", "brainscape",
        "homework", "learning", "coding", "compiler", "terminal", "pydroid", "ide",
        "meeting", "conference", "webinar", "teams", "zoom",
        "camera", "kamera", "gcam", "lmc", "sgcam", "agc", "snapcam", "photograph", "lens"
    )

    /**
     * Clears the classification cache. Called when whitelist changes in AppSettings.
     */
    @JvmStatic
    fun clearCache() {
        decisionCache.clear()
        Log.d(TAG, "Classification decision cache cleared")
    }

    /**
     * Invalidates a specific package from cache (e.g. upon package update/install).
     */
    @JvmStatic
    fun invalidatePackage(pkg: String?) {
        if (pkg != null) {
            decisionCache.remove(pkg)
        }
    }

    /**
     * Evaluates active unified policy services configured in SharedPreferences.
     */
    @JvmStatic
    fun getActiveServices(context: Context?): Set<String> {
        if (context == null) return emptySet()
        try {
            val prefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
            if (prefs != null) {
                val activeServices = prefs.getStringSet("active_unified_services", null)
                val effectiveServices = if (activeServices != null) HashSet(activeServices) else HashSet<String>()
                if (prefs.getBoolean("allow_youtube", false)) {
                    effectiveServices.add("youtube")
                }
                return effectiveServices
            }
        } catch (_: Exception) {}
        return emptySet()
    }

    /**
     * Evaluates whether a package is recognized in KnownSafe (MSG_GATE2).
     * Delegates directly to authoritative KnownSafe.kt.
     */
    @JvmStatic
    fun isKnownSafe(context: Context?, pkg: String?, userWhitelist: Set<String>?): Boolean {
        return KnownSafe.isKnownSafe(context, pkg, userWhitelist, getActiveServices(context))
    }

    /**
     * Evaluates whether a package is allowed by the user via the Unified Policy Registry.
     */
    @JvmStatic
    fun isPackageAllowedByUnifiedPolicy(context: Context?, pkg: String?): Boolean {
        if (context == null || pkg.isNullOrBlank()) {
            return false
        }
        return UnifiedPolicyRegistry.isPackageAllowedByService(pkg, getActiveServices(context))
    }

    /**
     * Classifies whether a package should be blocked during lockdown.
     * Follows the Stage 1 -> Stage 2 (MSG_GATE -> MSG_GATE2 -> MSG_GATE3) -> Secondary Classifier -> Stage 3 Gateway architecture.
     *
     * @param context       Android Context for PackageManager access.
     * @param pkg           The target package name.
     * @param userWhitelist The user-configured whitelist from AppSettings.
     * @return true if BLOCKED, false if ALLOWED.
     */
    @JvmStatic
    fun isPackageBlocked(context: Context?, pkg: String?, userWhitelist: Set<String>?): Boolean {
        if (pkg.isNullOrBlank()) {
            return false
        }

        // Self-immunity: QIEZKA itself is never blocked
        if (context != null && pkg == context.packageName) {
            return false
        }

        var appLabel: String? = null
        if (context != null) {
            try {
                val pm = context.packageManager
                if (pm != null) {
                    val ai = getApplicationInfoCompat(pm, pkg, 0L)
                    val lbl = pm.getApplicationLabel(ai)
                    appLabel = lbl.toString()
                }
            } catch (_: Exception) {}
        }

        // STAGE 1 — MASTER VETO GATE: Strict Anti-Tamper, Bloatware & Evasion Shield
        if (isStage1Vetoed(pkg, appLabel)) {
            return true
        }

        // STAGE 2 — Branch 1: Web Browsers are inspected by WebClassifier at URL/DOM level and NEVER blocked at package level
        if (isBrowserPackage(context, pkg, appLabel)) {
            return false
        }

        // STAGE 2 — Branch 2: Home Launchers (including system and third-party launchers) are never blocked
        if (KnownSafe.isLauncherApp(context, pkg)) {
            return false
        }

        // Check in-memory decision cache
        val cached = decisionCache[pkg]
        if (cached != null) {
            return cached
        }

        // ── STAGE 2 — INITIAL APP POLICY EVALUATION (Truth Table) ──
        val isDistracting = KnownDistracting.isKnownDistracting(pkg, appLabel) // MSG_GATE
        val isSafe = isKnownSafe(context, pkg, userWhitelist)                  // MSG_GATE2

        // MSG_GATE3 — Native Truth Table (Flowchart Alignment)
        // 1. YES KnownDistracting, YES KnownSafe (e.g. YouTube / AI enabled by user in Unified Policy) -> ALLOW
        if (isDistracting && isSafe) {
            decisionCache[pkg] = false
            return false
        }

        // 2. NO KnownDistracting, YES KnownSafe (e.g. Google Classroom / Anki / Gboard / User-Allowed App) -> ALLOW
        if (!isDistracting && isSafe) {
            decisionCache[pkg] = false
            return false
        }

        // 3. YES KnownDistracting, NO KnownSafe (e.g. TikTok, Steam, Coxeta, or disabled Unified Policy service) -> BLOCK
        if (isDistracting && !isSafe) {
            decisionCache[pkg] = true
            return true
        }

        // 4. NO KnownDistracting, NO KnownSafe -> Proceed to Secondary App Classifier heuristics
        val blocked = evaluatePackage(context, pkg)
        decisionCache[pkg] = blocked
        return blocked
    }

    /**
     * Internal multi-layer heuristic evaluation.
     */
    private fun evaluatePackage(context: Context?, pkg: String): Boolean {
        // STAGE 1 — MASTER VETO GATE: Anti-Tamper Shield & Bloatware
        if (isSettingsOrDeviceManager(pkg, null) || isStage1Bloat(pkg, null)) {
            return true
        }

        if (context == null) {
            return true // Conservative fallback
        }

        val pm = context.packageManager ?: return true

        val appInfo: ApplicationInfo
        var appLabel = ""
        try {
            appInfo = getApplicationInfoCompat(pm, pkg, 0L)
            val labelChar = pm.getApplicationLabel(appInfo)
            if (labelChar != null) {
                appLabel = labelChar.toString()
            }
        } catch (_: PackageManager.NameNotFoundException) {
            // App not found or uninstalled
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Error querying ApplicationInfo for $pkg: ${e.message}")
            return true
        }

        val lowerLabel = appLabel.lowercase(Locale.ROOT).trim()
        val lowerPkg = pkg.lowercase(Locale.ROOT).trim()

        // STAGE 1 — MASTER VETO GATE: Anti-Tamper Shield (Checks both package name and app label)
        if (isSettingsOrDeviceManager(pkg, appLabel) || isStage1Bloat(pkg, appLabel)) {
            Log.i(TAG, "Blocked by Stage 1 Master Veto: $pkg ($appLabel)")
            return true
        }

        // Milestone 17: Fake Calculator & Secret Vault Inspection
        if (isFakeCalculatorVault(pm, pkg, lowerLabel, lowerPkg)) {
            Log.i(TAG, "Blocked as fake calculator vault: $pkg ($appLabel)")
            return true
        }

        // ── Layer 4a: Negative Distraction Signals ──
        // Overrides any claimed category: if it looks like a game/gamble/mod, block immediately!
        if (hasNegativeDistractionSignals(lowerLabel, lowerPkg)) {
            Log.i(TAG, "Blocked by negative distraction heuristics: $pkg ($appLabel)")
            return true
        }

        // ── Layer 3: Android OS Application Category ──
        var category = CATEGORY_UNDEFINED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            category = appInfo.category
        }

        when (category) {
            CATEGORY_GAME -> {
                Log.i(TAG, "Blocked by CATEGORY_GAME: $pkg ($appLabel)")
                return true
            }

            CATEGORY_SOCIAL -> {
                // Direct messaging applications (WhatsApp, Telegram, Signal, Messenger)
                // and online lecture conferencing tools (Meet, Zoom, Teams)
                if (isMessagingApp(pkg, appLabel) || isMeetingOrVideoConferenceApp(pkg, appLabel)) {
                    Log.d(TAG, "Allowed messaging/meeting app claiming CATEGORY_SOCIAL: $pkg")
                    return false
                }
                Log.i(TAG, "Blocked by CATEGORY_SOCIAL: $pkg ($appLabel)")
                return true
            }

            CATEGORY_VIDEO -> {
                // Video conferencing and online lecture tools are legitimate communication tools,
                // distinct from entertainment video streaming services (YouTube, Netflix, TikTok).
                if (isMeetingOrVideoConferenceApp(pkg, appLabel)) {
                    Log.d(TAG, "Allowed meeting / video conference app claiming CATEGORY_VIDEO: $pkg")
                    return false
                }
                Log.i(TAG, "Blocked by CATEGORY_VIDEO: $pkg ($appLabel)")
                return true
            }

            CATEGORY_NEWS -> {
                Log.i(TAG, "Blocked by CATEGORY_NEWS: $pkg ($appLabel)")
                return true
            }

            CATEGORY_ACCESSIBILITY -> {
                Log.d(TAG, "Allowed by CATEGORY_ACCESSIBILITY: $pkg")
                return false
            }

            CATEGORY_MAPS -> {
                Log.d(TAG, "Allowed by CATEGORY_MAPS: $pkg")
                return false
            }

            CATEGORY_AUDIO -> {
                // Safe study audio / podcasts (since negative signals were already ruled out)
                Log.d(TAG, "Allowed by CATEGORY_AUDIO: $pkg")
                return false
            }

            CATEGORY_IMAGE -> {
                // Safe photo / scanning / art tools
                Log.d(TAG, "Allowed by CATEGORY_IMAGE: $pkg")
                return false
            }

            CATEGORY_PRODUCTIVITY -> {
                // Safe productivity / cloud / study app (unless it is a remote desktop or screen share bypass)
                if (isRemoteDesktopOrScreenShare(lowerPkg, lowerLabel)) {
                    Log.i(TAG, "Blocked remote desktop / screen share claiming CATEGORY_PRODUCTIVITY: $pkg ($appLabel)")
                    return true
                }
                Log.d(TAG, "Allowed by CATEGORY_PRODUCTIVITY: $pkg ($appLabel)")
                return false
            }

            CATEGORY_UNDEFINED -> {}
            else -> {}
        }

        // ── Camera App Gate (Celso Azevedo GCam Ports, Open Camera & OEM Cameras) ──
        if (KnownSafe.isCameraApp(context, pkg, appLabel) || isCameraIntentHandler(pm, pkg)) {
            Log.d(TAG, "Allowed as verified camera application: $pkg ($appLabel)")
            return false
        }

        // ── Layer 4b: Positive Academic & Educational Promotion ──
        // Promotes unknown or un-categorized apps if their label or package contains academic indicators
        if (hasPositiveAcademicSignals(lowerLabel, lowerPkg)) {
            Log.i(TAG, "Promoted & Allowed by academic keyword heuristics: $pkg ($appLabel)")
            return false
        }

        // ── Stage 2: SIM or Carrier Service Exemption ──
        if (isSimOrCarrierService(pkg, appLabel)) {
            Log.d(TAG, "Allowed as SIM or Carrier Service: $pkg ($appLabel)")
            return false
        }

        // ── Stage 2: Home Launcher Exemption ──
        // Home launchers (user's home screen) are always allowed to execute and must never fall through to Layer 5
        if (KnownSafe.isLauncherApp(context, pkg)) {
            Log.d(TAG, "Allowed as Home Launcher: $pkg ($appLabel)")
            return false
        }

        // ── STAGE 3 — UNIVERSAL SYSTEM GATEWAY (SYSALLOW) FOR UNDEFINED APPS ──
        // Flowchart Alignment (Option B):
        // In the Secondary App Classifier, an application reaches the Undefined gate only after
        // surviving all Stage 1 Master Vetoes, with NO detected distracting signals, fake vaults,
        // or distracting categories, and having no positive academic/camera/carrier signals.
        // If the undefined app resides on the system partition (FLAG_SYSTEM or FLAG_UPDATED_SYSTEM_APP),
        // it IS ALWAYS ALLOWED!
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                          (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        if (isSystemApp) {
            // Guard: Ensure general web browsers still route through WebClassifier URL inspection
            if (!isBrowserPackage(context, pkg, appLabel)) {
                Log.d(TAG, "Allowed by Stage 3 Universal System Gateway (SYSALLOW): $pkg ($appLabel)")
                return false
            }
        }

        // ── Layer 5: Conservative Fallback ──
        // Unknown user-installed third-party app with undefined category and no educational signals is blocked
        Log.i(TAG, "Blocked by conservative fallback (unknown user app): $pkg ($appLabel)")
        return true
    }

    /**
     * Identifies legitimate system picker and utility dialog activities hosted inside Settings.apk
     * (e.g. ActivityPicker for "Choose activity" / Intent.ACTION_PICK_ACTIVITY, BluetoothPairingDialog, WifiDialogActivity).
     * These activities are purely OS selection dialogs with NO access to settings, app management,
     * permissions, or data clearing.
     */
    @JvmStatic
    fun isSystemSettingsDialog(activityOrTitle: String?): Boolean {
        if (activityOrTitle.isNullOrBlank()) return false
        val lower = activityOrTitle.lowercase(Locale.ROOT).trim()
        return lower.endsWith(".activitypicker") ||
               lower == "activitypicker" ||
               lower.contains(".activitypicker") ||
               lower.contains("activitypicker") ||
               lower.contains("choose activity") ||
               lower.contains("activity picker") ||
               lower.endsWith(".bluetoothpairingdialog") ||
               lower == "bluetoothpairingdialog" ||
               lower.endsWith(".wifidialogactivity") ||
               lower == "wifidialogactivity"
    }

    /**
     * Identifies legitimate system role assignment activities for default home launcher selection
     * (e.g. RequestRoleActivity in PermissionController, DefaultHomePicker in Settings).
     * These activities are dedicated to assigning or reassigning the default home app.
     */
    @JvmStatic
    fun isHomeRoleActivity(activityOrTitle: String?): Boolean {
        if (activityOrTitle.isNullOrBlank()) return false
        val lower = activityOrTitle.lowercase(Locale.ROOT).trim()
        return lower.endsWith(".requestroleactivity") ||
               lower == "requestroleactivity" ||
               lower.contains(".requestroleactivity") ||
               lower.contains("requestroleactivity") ||
               lower.endsWith(".defaulthomepicker") ||
               lower == "defaulthomepicker" ||
               lower.contains(".defaulthomepicker") ||
               lower.contains("defaulthomepicker") ||
               lower.endsWith(".defaulthomesettingsactivity") ||
               lower == "defaulthomesettingsactivity" ||
               lower.contains(".defaulthomesettingsactivity") ||
               lower.contains("defaulthomesettingsactivity") ||
               lower.endsWith(".defaultappactivity") ||
               lower == "defaultappactivity" ||
               lower.contains(".defaultappactivity") ||
               lower.contains("defaultappactivity") ||
               lower.endsWith(".homesettingsactivity") ||
               lower == "homesettingsactivity" ||
               lower.contains(".homesettingsactivity") ||
               lower.contains("homesettingsactivity") ||
               lower.contains("defaultassistpicker") ||
               lower.contains("defaultassistsettingsactivity") ||
               lower.contains("manageassistactivity") ||
               lower.contains("voiceinputsettingsactivity") ||
               lower.contains("voiceinteractionservicesettings") ||
               lower.contains("rolesearchactivity") ||
               lower.contains("specialappaccessactivity")
    }

    /**
     * Identifies Android Settings, MIUI Security Center, and OEM Device Care/Phone Managers
     * that provide UI to force-stop QIEZKA, clear app data, or revoke permissions.
     */
    @JvmStatic
    fun isSettingsOrDeviceManager(pkg: String?, appLabel: String?, activityCls: String?): Boolean {
        if (pkg == null) return false

        // System picker and pairing dialogs hosted inside Settings.apk are NOT settings or device managers
        if (isSystemSettingsDialog(activityCls)) {
            return false
        }

        // Default Home Launcher selection dialogs and role activities are handled exclusively by HomeRoleTruthTable
        if (isHomeRoleActivity(activityCls)) {
            return false
        }

        val lowerPkg = pkg.lowercase(Locale.ROOT)
        if (lowerPkg == "com.android.settings" ||
            lowerPkg == "com.miui.securitycenter" ||
            lowerPkg == "com.coloros.safecenter" ||
            lowerPkg == "com.samsung.android.lool" ||
            lowerPkg == "com.transsion.phonemaster" ||
            lowerPkg == "com.vivo.safecenter" ||
            lowerPkg == "com.iqoo.secure" ||
            lowerPkg == "com.huawei.systemmanager" ||
            lowerPkg == "com.google.android.apps.wellbeing" ||
            lowerPkg.contains(".settings") || lowerPkg.contains("securitycore") ||
            lowerPkg.contains("permissioncontroller") ||
            lowerPkg.contains("setupwizard") ||
            lowerPkg == "com.android.provision" ||
            lowerPkg.contains("smartswitch") ||
            lowerPkg.contains("easymover") ||
            lowerPkg.contains("switchphone")
        ) {
            return true
        }
        if (!appLabel.isNullOrBlank()) {
            val lowerLabel = appLabel.lowercase(Locale.ROOT)
            if (lowerLabel == "settings" || lowerLabel.contains("phone manager") ||
                lowerLabel.contains("device care") || lowerLabel.contains("security center") ||
                lowerLabel.contains("app manager") || lowerLabel.contains("cleaner") ||
                lowerLabel.contains("battery saver") || lowerLabel.contains("system manager") ||
                lowerLabel.contains("setup wizard")
            ) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun isSettingsOrDeviceManager(pkg: String?, appLabel: String?): Boolean =
        isSettingsOrDeviceManager(pkg, appLabel, null)

    /**
     * Identifies UAD-NG-derived hardware-level bloatware, Game Turbo daemons,
     * and instant game portal stores.
     */
    @JvmStatic
    fun isStage1Bloat(pkg: String?, appLabel: String?): Boolean {
        if (pkg == null) return false
        val lowerPkg = pkg.lowercase(Locale.ROOT)
        if (lowerPkg.contains("joyose") || lowerPkg.contains("gamecenter") ||
            lowerPkg.contains("gamebooster") || lowerPkg.contains("gamemode") ||
            lowerPkg.contains("gamehome") || lowerPkg.contains("gamespace") ||
            lowerPkg.contains("mipicks") || lowerPkg.contains("palmstore") ||
            lowerPkg.contains("glance")
        ) {
            return true
        }
        if (!appLabel.isNullOrBlank()) {
            val lowerLabel = appLabel.lowercase(Locale.ROOT)
            if (lowerLabel.contains("game center") || lowerLabel.contains("game booster") ||
                lowerLabel.contains("palm store") || lowerLabel.contains("getapps")
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Unified Stage 1 Master Veto Gate.
     * Evaluates anti-tamper protections (Settings, Device Managers, Setup Wizards),
     * OEM bloatware / game boosters, and hostile circumvention proxies.
     * Any package returning true here is strictly vetoed across the entire OS (cannot run,
     * cannot be exempted, and cannot be a Home launcher).
     */
    @JvmStatic
    fun isStage1Vetoed(pkg: String?, appLabel: String?, activityCls: String?): Boolean {
        if (pkg.isNullOrBlank()) return true
        return isSettingsOrDeviceManager(pkg, appLabel, activityCls) ||
               isStage1Bloat(pkg, appLabel) ||
               isHostileEvasionProxy(pkg, appLabel)
    }

    @JvmStatic
    fun isStage1Vetoed(pkg: String?, appLabel: String?): Boolean = isStage1Vetoed(pkg, appLabel, null)

    @JvmStatic
    fun isHostileEvasionProxy(pkg: String?, appLabel: String?): Boolean =
        KnownDistracting.isHostileEvasionProxy(pkg, appLabel)

    /**
     * Evaluates whether an application qualifies as an allowed Stage 3 Universal System Gateway (SYSALLOW) app.
     * If an app is on the system partition (pre-installed by OEM in /system, /vendor, /product) and passed all
     * Stage 1 Master Veto checks (not settings/device manager, not bloatware, not games, not distracting video/social,
     * not a general web browser), it is already permitted dynamically by the operating system during lockdown.
     *
     * In accordance with UI declutter architecture, apps that pass Stage 3 SYSALLOW must NOT be rendered
     * at the UI (they are hidden from candidate selection and user whitelist).
     */
    @JvmStatic
    fun isPassedStage3SystemAllow(appInfo: ApplicationInfo?, pkg: String?, appLabel: String?, activityCls: String?): Boolean {
        if (appInfo == null || pkg == null) return false
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                          (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        if (!isSystemApp) return false

        // System picker and pairing dialogs hosted inside Settings.apk naturally pass Stage 3 SYSALLOW
        if (isSystemSettingsDialog(activityCls)) {
            return true
        }

        val lowerPkg = pkg.lowercase(Locale.ROOT).trim()
        val lowerLabel = appLabel?.lowercase(Locale.ROOT)?.trim() ?: ""

        // Web browsers must NOT be treated as invisible system utilities; they require WebClassifier URL inspection
        if (lowerPkg.contains("browser") || lowerPkg.contains("chrome") || lowerPkg.contains("firefox") || lowerPkg.contains("opera")) {
            return false
        }

        // Must not be anti-tamper settings or device managers (Stage 1)
        if (isSettingsOrDeviceManager(pkg, appLabel, activityCls)) return false

        // Must not be hardware bloatware or game boosters (Stage 1)
        if (isStage1Bloat(pkg, appLabel)) return false

        // Must not be a known distraction or game
        if (KnownDistracting.isKnownDistracting(pkg, appLabel)) return false

        // Negative distraction heuristics (games, gambling, social feeds)
        if (hasNegativeDistractionSignals(lowerLabel, lowerPkg)) return false

        // Verified legitimate OEM system partition tool!
        return true
    }

    @JvmStatic
    fun isPassedStage3SystemAllow(appInfo: ApplicationInfo?, pkg: String?, appLabel: String?): Boolean =
        isPassedStage3SystemAllow(appInfo, pkg, appLabel, null)

    @JvmStatic
    fun isForbiddenDistraction(context: Context?, pkg: String?): Boolean {
        if (pkg == null || context == null) return false
        if (pkg == "com.android.settings" || isSettingsOrDeviceManager(pkg, null)) return true
        if (isStage1Bloat(pkg, null)) return true
        if (KnownDistracting.isKnownDistracting(pkg)) return true

        try {
            val pm = context.packageManager ?: return false
            val appInfo = getApplicationInfoCompat(pm, pkg, 0L)
            val labelChar = pm.getApplicationLabel(appInfo)
            val appLabel = labelChar.toString()
            if (KnownDistracting.isKnownDistracting(pkg, appLabel)) return true
            val lowerPkg = pkg.lowercase(Locale.ROOT).trim()
            val lowerLabel = appLabel.lowercase(Locale.ROOT).trim()

            // 1. Remote Desktop, Screen Sharing & Kali NetHunter KeX/VNC Workarounds
            if (isRemoteDesktopOrScreenShare(lowerPkg, lowerLabel)) {
                return true
            }

            // 2. BlacklistConstants check
            if (BlacklistConstants.isBlacklisted(pkg, appLabel)) return true

            // 3. Short video & TikTok variants (both global musically, trill, and aweme)
            if (lowerPkg.contains("musically") || lowerPkg.contains(".trill") || lowerPkg.contains(".aweme") ||
                lowerLabel.contains("tiktok") || lowerLabel.contains("tik tok") || lowerLabel.contains("douyin")
            ) {
                return true
            }

            // 4. Video Streaming & Entertainment (YouTube, Netflix, Twitch, Bilibili, Stremio, ReVanced)
            if (lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("twitch") ||
                lowerPkg.contains("bilibili") || lowerPkg.contains("danmaku.bili") || lowerPkg.contains("stremio") ||
                lowerPkg.contains("revanced") || lowerPkg.contains("newpipe") ||
                lowerLabel.contains("youtube") || lowerLabel.contains("netflix") || lowerLabel.contains("twitch") ||
                lowerLabel.contains("bilibili") || lowerLabel.contains("stremio")
            ) {
                // Ensure safe audio (like YouTube Music) is not blocked as a video distraction
                if (!lowerPkg.contains("music") && !lowerLabel.contains("music")) {
                    return true
                }
            }

            // 5. Social Media Feeds & Forums (Instagram, Twitter/X, Facebook Feed, Reddit, Threads, Pinterest)
            if (lowerPkg.contains("instagram") || lowerPkg.contains("twitter") ||
                lowerPkg == "com.facebook.katana" || lowerPkg == "com.facebook.lite" ||
                lowerPkg.contains("reddit") || lowerPkg.contains("pinterest") || lowerPkg.contains("threads") ||
                lowerLabel.contains("instagram") || lowerLabel.contains("twitter") ||
                lowerLabel == "facebook" || lowerLabel.contains("reddit") || lowerLabel.contains("pinterest")
            ) {
                return true
            }

            // 6. Category evaluation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (appInfo.category == ApplicationInfo.CATEGORY_GAME) {
                    return true
                }
                if (appInfo.category == ApplicationInfo.CATEGORY_VIDEO) {
                    if (!lowerPkg.contains("music") && !lowerLabel.contains("music")) {
                        return true
                    }
                }
                if (appInfo.category == ApplicationInfo.CATEGORY_NEWS) {
                    if (lowerPkg.contains("reddit") || lowerLabel.contains("reddit")) {
                        return true
                    }
                }
                if (appInfo.category == ApplicationInfo.CATEGORY_SOCIAL) {
                    // DIRECT MESSAGING APPS (Telegram, Messenger, WhatsApp, Signal) ARE NOT FORBIDDEN DISTRACTIONS!
                    // They can be whitelisted by the user in Allowed Apps.
                    if (!isMessagingApp(lowerPkg, lowerLabel)) {
                        return true
                    }
                }
            }
            @Suppress("DEPRECATION")
            if ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
                return true
            }

            // 7. Fake Calculator Vaults
            if (isFakeCalculatorVault(pm, pkg, lowerLabel, lowerPkg)) {
                return true
            }

            // 8. Negative Distraction Heuristics (Games, Gambling, Modding, Adult, Comics)
            if (hasNegativeDistractionSignals(lowerLabel, lowerPkg)) {
                return true
            }
        } catch (_: Exception) {}
        return false
    }

    /**
     * Inspects whether an application declared as Calculator/Calc is actually a disguised vault.
     * Genuine offline calculators never request Camera, Storage/Media, or Photo access.
     */
    @JvmStatic
    fun isFakeCalculatorVault(pm: PackageManager?, pkg: String?, lowerLabel: String?, lowerPkg: String?): Boolean {
        if (lowerLabel != null && lowerPkg != null &&
            (lowerLabel.contains("calc") || lowerLabel.contains("calculator") ||
             lowerPkg.contains("calc") || lowerPkg.contains("calculator"))
        ) {
            // Substring vault indicators
            if (lowerPkg.contains("vault") || lowerPkg.contains("hide") || lowerPkg.contains("secret") ||
                lowerPkg.contains("lock") || lowerPkg.contains("protect") || lowerPkg.contains("private") ||
                lowerPkg.contains("privacy") || lowerLabel.contains("vault") || lowerLabel.contains("hide") ||
                lowerLabel.contains("lock") || lowerLabel.contains("secret")
            ) {
                return true
            }

            // Real Android permission inspection (Milestone 17):
            // Genuine calculators (Google, Samsung, Mi, Desmos) never need camera or media storage.
            if (pm != null && pkg != null) {
                try {
                    val pi = getPackageInfoCompat(pm, pkg, PackageManager.GET_PERMISSIONS.toLong())
                    val requestedPermissions = pi.requestedPermissions
                    if (requestedPermissions != null) {
                        for (perm in requestedPermissions) {
                            if (perm == null) continue
                            if (perm == "android.permission.CAMERA" ||
                                perm == "android.permission.READ_EXTERNAL_STORAGE" ||
                                perm == "android.permission.MANAGE_EXTERNAL_STORAGE" ||
                                perm == "android.permission.READ_MEDIA_IMAGES" ||
                                perm == "android.permission.READ_MEDIA_VIDEO" ||
                                perm == "android.permission.SYSTEM_ALERT_WINDOW"
                            ) {
                                Log.w(TAG, "Fake calculator vault detected (requests $perm): $pkg")
                                return true
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        return false
    }

    @JvmStatic
    fun hasNegativeDistractionSignals(lowerLabel: String?, lowerPkg: String?): Boolean {
        if (lowerLabel == null || lowerPkg == null) return false

        // Milestone 17: Fake Calculator Vault Substring Detection Fallback
        if (lowerLabel.contains("calc") || lowerLabel.contains("calculator")) {
            if (lowerPkg.contains("vault") || lowerPkg.contains("hide") || lowerPkg.contains("secret") ||
                lowerPkg.contains("lock") || lowerPkg.contains("protect") || lowerPkg.contains("private") ||
                lowerPkg.contains("privacy") || lowerLabel.contains("vault") || lowerLabel.contains("hide") ||
                lowerLabel.contains("lock") || lowerLabel.contains("secret")
            ) {
                return true
            }
        }

        for (kw in NEGATIVE_LABEL_KEYWORDS) {
            if (lowerLabel.contains(kw)) {
                return true
            }
        }
        for (sub in NEGATIVE_PKG_SUBSTRINGS) {
            if (lowerPkg.contains(sub)) {
                return true
            }
        }
        return false
    }

    private fun hasPositiveAcademicSignals(lowerLabel: String, lowerPkg: String): Boolean {
        for (kw in POSITIVE_ACADEMIC_KEYWORDS) {
            if (lowerLabel.contains(kw) || lowerPkg.contains(".$kw") || lowerPkg.contains("$kw.")) {
                return true
            }
        }
        return false
    }
}
