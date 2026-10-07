package com.uncode.app

import org.junit.Assert.*
import org.junit.Test

class HomeHandlerTest {

    @Test
    fun testMainActivityDefaultLifecycleState() {
        assertFalse(MainActivity.isRunning())
        assertNull(MainActivity.getInstance())
    }

    @Test
    fun testQiezkaPackageExcludedFromLauncherList() {
        val qiezkaPackage = "com.uncode.app"
        val candidatePackages = listOf(
            "com.sec.android.app.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.uncode.app",
            "ch.deletescape.lawnchair"
        )

        val filtered = candidatePackages.filter { it != qiezkaPackage }

        assertEquals(3, filtered.size)
        assertFalse(filtered.contains("com.uncode.app"))
        assertTrue(filtered.contains("com.sec.android.app.launcher"))
        assertTrue(filtered.contains("com.google.android.apps.nexuslauncher"))
        assertTrue(filtered.contains("ch.deletescape.lawnchair"))
    }

    @Test
    fun testLauncherStateManagerConstants() {
        assertEquals("qiezka_home_proxy", LauncherStateManager.PREFS_NAME)
        assertEquals("selected_launcher_pkg", LauncherStateManager.KEY_SELECTED_LAUNCHER_PKG)
        assertEquals("selected_launcher_cls", LauncherStateManager.KEY_SELECTED_LAUNCHER_CLS)
        assertEquals("com.siren.homeproxy.ACTION_UPDATE_LAUNCHER", LauncherStateManager.ACTION_UPDATE_LAUNCHER)
        assertEquals("com.siren.homeproxy", LauncherStateManager.SIREN_PACKAGE)
    }

    @Test
    fun testSirenRevivalConstants() {
        assertEquals("com.uncode.app.ACTION_REVIVE", SirenReviveReceiver.ACTION_REVIVE)
    }

    @Test
    fun testLauncherInfoDataModel() {
        val launcher = InstalledLauncherDetector.LauncherInfo(
            "com.sec.android.app.launcher",
            "com.sec.android.app.launcher.activities.LauncherActivity",
            "One UI Home",
            "data:image/png;base64,fakeIconData",
            true,
            true
        )

        assertEquals("com.sec.android.app.launcher", launcher.packageName)
        assertEquals("com.sec.android.app.launcher.activities.LauncherActivity", launcher.activityName)
        assertEquals("One UI Home", launcher.name)
        assertEquals("data:image/png;base64,fakeIconData", launcher.icon)
        assertTrue(launcher.isSystem)
        assertTrue(launcher.isCurrentDefault)
    }

    @Test
    fun testStage1MasterVetoForLaunchers() {
        assertTrue(AppClassifier.isStage1Vetoed("com.android.settings", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.google.android.settings", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.samsung.android.settings", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.miui.securitycenter", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.coloros.safecenter", null))
        assertFalse(AppClassifier.isStage1Vetoed("android", null))
        assertFalse(AppClassifier.isStage1Vetoed("com.android.systemui", null))

        assertTrue(AppClassifier.isStage1Vetoed("com.google.android.setupwizard", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.sec.android.app.SecSetupWizard", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.android.setupwizard", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.android.provision", null))

        assertTrue(AppClassifier.isStage1Vetoed("com.xiaomi.joyose", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.zhiliaoapp.musically", null))

        assertFalse(AppClassifier.isStage1Vetoed("com.sec.android.app.launcher", null))
        assertFalse(AppClassifier.isStage1Vetoed("com.google.android.apps.nexuslauncher", null))
        assertFalse(AppClassifier.isStage1Vetoed("com.miui.home", null))
        assertFalse(AppClassifier.isStage1Vetoed("com.huawei.android.launcher", null))
        assertFalse(AppClassifier.isStage1Vetoed("com.oneplus.launcher", null))
        assertFalse(AppClassifier.isStage1Vetoed("com.oppo.launcher", null))
        assertFalse(AppClassifier.isStage1Vetoed("ch.deletescape.lawnchair", null))
        assertFalse(AppClassifier.isStage1Vetoed("com.teslacoilsw.launcher", null))
    }

    @Test
    fun testCommercialVpnDistractionClassification() {
        // Commercial & bypass VPNs must be classified as KnownDistracting (Stage 2)
        assertTrue(KnownDistracting.isKnownDistracting("free.vpn.unblock.proxy.turbovpn"))
        assertTrue(KnownDistracting.isKnownDistracting("com.psiphon3"))
        assertTrue(KnownDistracting.isKnownDistracting("com.jrzheng.supervpnfree"))
        assertTrue(KnownDistracting.isKnownDistracting("com.nordvpn.android"))
        assertTrue(KnownDistracting.isKnownDistracting("com.expressvpn.vpn"))
        assertTrue(KnownDistracting.isKnownDistracting("com.v2ray.ang"))
        assertTrue(KnownDistracting.isKnownDistracting("com.github.kr328.clash"))

        // Legitimate academic & enterprise VPNs must NOT be classified as distracting
        assertFalse(KnownDistracting.isKnownDistracting("com.wireguard.android"))
        assertFalse(KnownDistracting.isKnownDistracting("com.tailscale.ipn"))
        assertFalse(KnownDistracting.isKnownDistracting("com.cisco.anyconnect.vpn.android.avf"))
        assertFalse(KnownDistracting.isKnownDistracting("com.paloaltonetworks.globalprotect"))
        assertFalse(KnownDistracting.isKnownDistracting("de.blinkt.openvpn"))
    }

    @Test
    fun testGoogleMeetAndLectureConferencingAllowed() {
        // Google Meet, Zoom, Teams, Webex are in BASELINE_SAFE_PACKAGES
        assertTrue(KnownSafe.BASELINE_SAFE_PACKAGES.contains("com.google.android.apps.tachyon"))
        assertTrue(KnownSafe.BASELINE_SAFE_PACKAGES.contains("com.google.android.apps.meetings"))
        assertTrue(KnownSafe.BASELINE_SAFE_PACKAGES.contains("us.zoom.videomeetings"))
        assertTrue(KnownSafe.BASELINE_SAFE_PACKAGES.contains("com.microsoft.teams"))
        assertTrue(KnownSafe.BASELINE_SAFE_PACKAGES.contains("com.cisco.webex.meetings"))

        // KnownSafe.isKnownSafe returns true
        assertTrue(KnownSafe.isKnownSafe(null, "com.google.android.apps.tachyon", null, null))
        assertTrue(KnownSafe.isKnownSafe(null, "us.zoom.videomeetings", null, null))
        assertTrue(KnownSafe.isKnownSafe(null, "com.microsoft.teams", null, null))

        // AppClassifier.isMeetingOrVideoConferenceApp helper returns true
        assertTrue(AppClassifier.isMeetingOrVideoConferenceApp("com.google.android.apps.tachyon", "Google Meet"))
        assertTrue(AppClassifier.isMeetingOrVideoConferenceApp("us.zoom.videomeetings", "Zoom"))
        assertTrue(AppClassifier.isMeetingOrVideoConferenceApp("com.microsoft.teams", "Teams"))

        // Entertainment video apps are NOT conference apps
        assertFalse(AppClassifier.isMeetingOrVideoConferenceApp("com.google.android.youtube", "YouTube"))
        assertFalse(AppClassifier.isMeetingOrVideoConferenceApp("com.netflix.mediaclient", "Netflix"))
    }

    @Test
    fun testLauncherExcludedFromHardcodedAppUI() {
        // Home launchers must NOT be identified as hardcoded display apps for the Allowed Apps drawer
        assertFalse(KnownSafe.isHardcodedApp(null, null, "com.sec.android.app.launcher", "One UI Home"))
        assertFalse(KnownSafe.isHardcodedApp(null, null, "com.google.android.apps.nexuslauncher", "Pixel Launcher"))
        assertFalse(KnownSafe.isHardcodedApp(null, null, "ch.deletescape.lawnchair", "Lawnchair"))
    }

    @Test
    fun testLauncherStateManagerRejectsVetoedPackages() {
        assertFalse(LauncherStateManager.saveSelectedLauncher(null, "com.sec.android.app.launcher", ""))
        assertFalse(LauncherStateManager.saveSelectedLauncher(null, "", ""))
        assertFalse(LauncherStateManager.saveSelectedLauncher(null, LauncherStateManager.SIREN_PACKAGE, ""))
    }

    @Test
    fun testIsRealLauncherFiltersSettingsAndFallbacks() {
        val myPkg = "com.uncode.app"

        assertFalse(InstalledLauncherDetector.isRealLauncher(null, myPkg))

        val selfInfo = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "com.uncode.app"
                name = "com.uncode.app.MainActivity"
            }
            priority = 1
        }
        assertFalse(InstalledLauncherDetector.isRealLauncher(selfInfo, myPkg))

        val fallbackHome = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "com.android.settings"
                name = "com.android.settings.FallbackHome"
            }
            priority = -1000
        }
        assertFalse(InstalledLauncherDetector.isRealLauncher(fallbackHome, myPkg))

        val setupWizard = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "com.google.android.setupwizard"
                name = "com.google.android.setupwizard.SetupWizardActivity"
            }
            priority = 10
        }
        assertFalse(InstalledLauncherDetector.isRealLauncher(setupWizard, myPkg))

        val negativePriority = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "com.example.fallback"
                name = "com.example.fallback.EmergencyHome"
            }
            priority = -1
        }
        assertFalse(InstalledLauncherDetector.isRealLauncher(negativePriority, myPkg))

        val oneUi = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "com.sec.android.app.launcher"
                name = "com.sec.android.app.launcher.activities.LauncherActivity"
            }
            priority = 0
        }
        assertTrue(InstalledLauncherDetector.isRealLauncher(oneUi, myPkg))

        val lawnchair = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "ch.deletescape.lawnchair"
                name = "ch.deletescape.lawnchair.LawnchairLauncher"
            }
            priority = 0
        }
        assertTrue(InstalledLauncherDetector.isRealLauncher(lawnchair, myPkg))

        val sirenInfo = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "com.siren.homeproxy"
                name = "com.siren.homeproxy.SirenHomeActivity"
            }
            priority = 1
        }
        assertFalse(InstalledLauncherDetector.isRealLauncher(sirenInfo, myPkg))
    }

    @Test
    fun testHomeAppSelectionTitleClassification() {
        // 1. Language-independent exact Activity class names
        assertTrue(LockAccessibilityService.isHomeAppSelectionTitle("com.android.permissioncontroller.role.ui.RequestRoleActivity"))
        assertTrue(LockAccessibilityService.isHomeAppSelectionTitle("com.android.permissioncontroller.role.ui.DefaultAppActivity"))
        assertTrue(LockAccessibilityService.isHomeAppSelectionTitle("com.android.settings.Settings\$HomeSettingsActivity"))
        assertTrue(LockAccessibilityService.isHomeAppSelectionTitle("com.android.settings.applications.defaultapps.DefaultHomePicker"))
        assertTrue(LockAccessibilityService.isHomeAppSelectionTitle("RequestRoleActivity"))
        assertTrue(LockAccessibilityService.isHomeAppSelectionTitle("DefaultAppActivity"))
        assertTrue(LockAccessibilityService.isHomeAppSelectionTitle("HomeSettingsActivity"))
        assertTrue(LockAccessibilityService.isHomeAppSelectionTitle("DefaultHomePicker"))

        // 2. Installed launcher brand names are NOT home app selection titles (they are window titles of launchers)
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Lawnchair"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Microsoft Launcher"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Niagara Launcher"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Nova Launcher"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Nexus Launcher"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Pixel Launcher"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("One UI Home"))

        // 3. Fallback keywords
        assertTrue(LockAccessibilityService.isHomeAppSelectionTitle("Default home app"))
        assertTrue(LockAccessibilityService.isHomeAppSelectionTitle("default home"))
        assertTrue(LockAccessibilityService.isHomeAppSelectionTitle("Home app"))
        assertTrue(LockAccessibilityService.isHomeAppSelectionTitle("Home launcher"))

        // 4. False: Generic "Open with..." and MIME type / intent resolver dialogs
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Open with"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Open with..."))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Complete action using"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Just once"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Always"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Use a different app"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Drive PDF Viewer"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Google Chrome"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("WPS Office"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle("Select an app"))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle(null))
        assertFalse(LockAccessibilityService.isHomeAppSelectionTitle(""))
    }

    @Test
    fun testIsInstalledLauncherCandidate() {
        // True for launcher brand signatures
        assertTrue(LockAccessibilityService.isInstalledLauncherCandidate("Lawnchair"))
        assertTrue(LockAccessibilityService.isInstalledLauncherCandidate("Microsoft Launcher"))
        assertTrue(LockAccessibilityService.isInstalledLauncherCandidate("Niagara Launcher"))
        assertTrue(LockAccessibilityService.isInstalledLauncherCandidate("Nova Launcher"))
        assertTrue(LockAccessibilityService.isInstalledLauncherCandidate("Nexus Launcher"))
        assertTrue(LockAccessibilityService.isInstalledLauncherCandidate("Trebuchet"))
        assertTrue(LockAccessibilityService.isInstalledLauncherCandidate("Pixel Launcher"))
        assertTrue(LockAccessibilityService.isInstalledLauncherCandidate("One UI Home"))

        // False for non-launcher apps
        assertFalse(LockAccessibilityService.isInstalledLauncherCandidate("Drive PDF Viewer"))
        assertFalse(LockAccessibilityService.isInstalledLauncherCandidate("WPS Office"))
        assertFalse(LockAccessibilityService.isInstalledLauncherCandidate("Google Chrome"))
        assertFalse(LockAccessibilityService.isInstalledLauncherCandidate("Adobe Acrobat"))
        assertFalse(LockAccessibilityService.isInstalledLauncherCandidate("VLC"))
        assertFalse(LockAccessibilityService.isInstalledLauncherCandidate("Open with"))
        assertFalse(LockAccessibilityService.isInstalledLauncherCandidate(null))
        assertFalse(LockAccessibilityService.isInstalledLauncherCandidate(""))
    }

    @Test
    fun testCentralizedSecurityClassification() {
        // 1. KnownSafe.isLauncherApp
        assertTrue(KnownSafe.isLauncherApp(null, "com.siren.homeproxy"))
        assertFalse(KnownSafe.isLauncherApp(null, "com.android.settings"))
        assertFalse(KnownSafe.isLauncherApp(null, null))

        // Dynamic registration check
        KnownSafe.dynamicLauncherPackages.add("ch.deletescape.lawnchair.ci")
        assertTrue(KnownSafe.isLauncherApp(null, "ch.deletescape.lawnchair.ci"))
        assertTrue(LockAccessibilityService.isLauncherApp(null, "ch.deletescape.lawnchair.ci"))

        // 2. AppClassifier.isBrowserPackage
        assertTrue(AppClassifier.isBrowserPackage("com.android.chrome"))
        assertTrue(AppClassifier.isBrowserPackage("org.mozilla.firefox"))
        assertTrue(AppClassifier.isBrowserPackage("idm.internet.download.manager"))
        assertTrue(AppClassifier.isBrowserPackage("id.kisskh.twa"))
        assertTrue(AppClassifier.isBrowserPackage("org.chromium.webapk.a803cdaf2d8785085_v2"))
        assertTrue(AppClassifier.isBrowserPackage("com.example.app.pwa"))
        assertTrue(LockAccessibilityService.isBrowserPackage("com.android.chrome"))
        assertTrue(LockAccessibilityService.isBrowserPackage("id.kisskh.twa"))
        assertFalse(AppClassifier.isBrowserPackage("com.facebook.katana"))
        assertFalse(AppClassifier.isBrowserPackage(null))

        // 2b. AppClassifier.isPwaOrWebApk
        assertTrue(AppClassifier.isPwaOrWebApk("org.chromium.webapk.a803cdaf2d8785085_v2"))
        assertTrue(AppClassifier.isPwaOrWebApk("com.sec.android.app.sbrowser.webapk.wb123"))
        assertTrue(AppClassifier.isPwaOrWebApk("id.kisskh.twa"))
        assertTrue(AppClassifier.isPwaOrWebApk("com.example.app.pwa"))
        assertFalse(AppClassifier.isPwaOrWebApk("com.android.chrome"))
        assertFalse(AppClassifier.isPwaOrWebApk("com.google.android.calculator"))
        assertFalse(AppClassifier.isPwaOrWebApk("com.whatsapp"))
        assertFalse(AppClassifier.isPwaOrWebApk(null))

        // Shopping domain checks
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("https://shopee.ph"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("https://shopee.com/cart"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("https://lazada.com.ph"))
        assertTrue(KnownDistractingWeb.isKnownDistractingWeb("https://kisskh.id"))

        // WebClassifier ClassificationResult states
        val pendingRes = WebClassifier.ClassificationResult.pendingLoad("skeleton")
        assertTrue(pendingRes.isPending)
        assertFalse(pendingRes.isBlocked)
        assertEquals("skeleton", pendingRes.reason)

        val allowedRes = WebClassifier.ClassificationResult.allowed()
        assertFalse(allowedRes.isPending)
        assertFalse(allowedRes.isBlocked)

        val blockedRes = WebClassifier.ClassificationResult.blocked("games")
        assertFalse(blockedRes.isPending)
        assertTrue(blockedRes.isBlocked)
        assertEquals("games", blockedRes.reason)

        // 3. AppClassifier.isSimOrCarrierService
        assertTrue(AppClassifier.isSimOrCarrierService("com.android.stk", null))
        assertTrue(AppClassifier.isSimOrCarrierService("ph.com.globe", "Globe Services"))
        assertTrue(AppClassifier.isSimOrCarrierService("ph.com.smart", "Smart Menu"))
        assertTrue(AppClassifier.isSimOrCarrierService("com.android.mms", null))
        assertTrue(LockAccessibilityService.isSimOrCarrierService("com.android.stk"))
        assertFalse(AppClassifier.isSimOrCarrierService("com.facebook.orca", "Messenger"))
        assertFalse(AppClassifier.isSimOrCarrierService(null))
    }

    @Test
    fun testStage1MasterVetoBeforeStage2BrowserGate() {
        // Hostile proxy browsers must be strictly VETOED in Stage 1 and BLOCKED by isPackageBlocked
        val hostileBrowsers = listOf(
            "org.torproject.torbrowser",
            "org.torproject.torbrowser_alpha",
            "com.cloudmosa.puffin",
            "com.cloudmosa.puffinfree",
            "com.ucmobile.intl",
            "com.uc.browser.en"
        )
        for (pkg in hostileBrowsers) {
            assertTrue("Expected $pkg to be vetoed by Stage 1 Master Veto", AppClassifier.isStage1Vetoed(pkg, null))
            assertFalse("Expected $pkg NOT to qualify as a standard browser", AppClassifier.isBrowserPackage(pkg))
            assertTrue("Expected $pkg to be blocked by isPackageBlocked", AppClassifier.isPackageBlocked(null, pkg, null))
        }

        // Standard browsers must pass Stage 1 Master Veto and be ALLOWED at package level for WebClassifier inspection
        val standardBrowsers = listOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.sec.android.app.sbrowser",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.opera.browser"
        )
        for (pkg in standardBrowsers) {
            assertFalse("Expected $pkg NOT to be vetoed by Stage 1", AppClassifier.isStage1Vetoed(pkg, null))
            assertTrue("Expected $pkg to qualify as isBrowserPackage", AppClassifier.isBrowserPackage(pkg))
            assertFalse("Expected $pkg to be allowed at package level for WebClassifier", AppClassifier.isPackageBlocked(null, pkg, null))
        }

        // Generic non-vetoed 3rd-party browser from Play Store
        val genericBrowser = "com.sample.custom.browser"
        assertFalse(AppClassifier.isStage1Vetoed(genericBrowser, null))
        assertTrue(AppClassifier.isBrowserPackage(genericBrowser))
        assertFalse(AppClassifier.isPackageBlocked(null, genericBrowser, null))
    }

    @Test
    fun testStage1MultilingualProxyEvasionVeto() {
        // 1. Evasion package signatures
        val evasionPackages = listOf(
            "free.vpn.unblock.proxy.turbovpn",
            "org.torproject.torbrowser",
            "com.cloudmosa.puffin",
            "com.psiphon3",
            "com.fake.vpn.client",
            "com.network.bypass.tool",
            "com.shadowsocks.speed",
            "com.v2ray.client",
            "com.aloha.browser"
        )
        for (pkg in evasionPackages) {
            assertTrue("Expected $pkg to be vetoed by Stage 1 Master Veto", AppClassifier.isStage1Vetoed(pkg, null))
            assertFalse("Expected $pkg NOT to qualify as browser", AppClassifier.isBrowserPackage(null, pkg, null))
            assertTrue("Expected $pkg to be blocked by isPackageBlocked", AppClassifier.isPackageBlocked(null, pkg, null))
        }

        // 2. Multilingual evasion labels on otherwise generic/unclassified package names
        val foreignEvasionApps = listOf(
            "com.network.tool.ru" to "Прокси Браузер",
            "com.network.tool.ru2" to "Супер ВПН Бесплатно",
            "com.anonymizer.tool" to "Анонимайзер Веб",
            "com.tool.zh" to "翻墙极速版",
            "com.tool.zh2" to "科学上网神器",
            "com.tool.zh3" to "免费代理浏览器",
            "com.tool.zh4" to "网络加速器",
            "com.tool.ja" to "プロキシ ブラウザ",
            "com.tool.ar" to "متصفح بروكسي",
            "com.tool.ar2" to "تطبيق في بي ان",
            "com.tool.es" to "Navegador Proxy Rápido",
            "com.tool.es2" to "Desbloquear Sitios Web"
        )
        for ((pkg, label) in foreignEvasionApps) {
            assertTrue("Expected $pkg ($label) to be vetoed by Stage 1 Master Veto", AppClassifier.isStage1Vetoed(pkg, label))
            assertFalse("Expected $pkg ($label) NOT to qualify as browser", AppClassifier.isBrowserPackage(null, pkg, label))
        }

        // 3. Audio / music exemptions: Ensure legitimate audio player labels are NOT falsely vetoed
        val audioApps = listOf(
            "com.music.player" to "Music Player & Proxy",
            "com.podcast.app" to "Audio Podcast VPN"
        )
        for ((pkg, label) in audioApps) {
            assertFalse("Audio app $pkg ($label) should NOT be vetoed by evasion heuristic", AppClassifier.isStage1Vetoed(pkg, label))
        }
    }

    @Test
    fun testStage2UniversalBrowserResolutionAndDynamicCache() {
        try {
            AppClassifier.clearDynamicBrowserPackagesForTesting()

            // 1. Static known set
            assertTrue(AppClassifier.isBrowserPackage("com.android.chrome"))
            assertTrue(AppClassifier.isBrowserPackage("org.mozilla.firefox"))
            assertTrue(AppClassifier.isBrowserPackage("com.microsoft.emmx"))
            assertTrue(AppClassifier.isBrowserPackage("idm.internet.download.manager.plus"))

            // 2. Foreign browsers lacking Latin "browser" in package ID (e.g. QQ Browser, Naver Whale, Via)
            val qqBrowser = "com.tencent.mtt"
            val naverWhale = "com.nhn.android.whale"
            val viaBrowser = "mark.via.gp"

            // Before dynamic resolution/caching:
            // Since context is null, they aren't in static list or dynamic cache yet
            assertFalse(AppClassifier.isStage1Vetoed(qqBrowser, "QQ浏览器"))
            assertFalse(AppClassifier.isStage1Vetoed(naverWhale, "네이버 웨일"))
            assertFalse(AppClassifier.isStage1Vetoed(viaBrowser, "Via"))

            // Simulate dynamic Android OS Intent resolution populating dynamicBrowserPackages
            AppClassifier.addDynamicBrowserPackageForTesting(qqBrowser)
            AppClassifier.addDynamicBrowserPackageForTesting(naverWhale)
            AppClassifier.addDynamicBrowserPackageForTesting(viaBrowser)

            // Now, they resolve in O(1) time without needing app label inspection!
            assertTrue("QQ Browser should be recognized as browser via dynamic cache", AppClassifier.isBrowserPackage(null, qqBrowser, "QQ浏览器"))
            assertTrue("Naver Whale should be recognized as browser via dynamic cache", AppClassifier.isBrowserPackage(null, naverWhale, "네이버 웨일"))
            assertTrue("Via Browser should be recognized as browser via dynamic cache", AppClassifier.isBrowserPackage(null, viaBrowser, "Via"))

            // And isPackageBlocked returns false (allowed at package level for WebClassifier inspection)
            assertFalse(AppClassifier.isPackageBlocked(null, qqBrowser, null))
            assertFalse(AppClassifier.isPackageBlocked(null, naverWhale, null))
            assertFalse(AppClassifier.isPackageBlocked(null, viaBrowser, null))
        } finally {
            AppClassifier.clearDynamicBrowserPackagesForTesting()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class FakeTestPreferences(private val data: Map<String, Any> = emptyMap()) : android.content.SharedPreferences {
        override fun getAll(): Map<String, *> = data
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? = data[key] as? Set<String> ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = (data[key] as? Number)?.toInt() ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (data[key] as? Number)?.toLong() ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (data[key] as? Number)?.toFloat() ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun edit(): android.content.SharedPreferences.Editor = throw UnsupportedOperationException()
        override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    @Test
    fun testHomeChangeInterceptorDualConditionMatrix() {
        try {
            // Case 1: SIREN is NOT default -> MUST NOT activate even if lockdown is active!
            LockAccessibilityService.setCachedIsSirenDefaultForTesting(false)
            val lockdownPrefs = FakeTestPreferences(mapOf("lockdown_active" to true))
            assertFalse(LockAccessibilityService.isHomeChangeInterceptorActive(null, lockdownPrefs))

            val consequencePrefs = FakeTestPreferences(mapOf("consequence_active" to true, "operating_mode" to "hardcore"))
            assertFalse(LockAccessibilityService.isHomeChangeInterceptorActive(null, consequencePrefs))

            val schedulePrefs = FakeTestPreferences(mapOf("schedules_json" to "[{\"id\":\"s1\",\"isActive\":true}]"))
            assertFalse(LockAccessibilityService.isHomeChangeInterceptorActive(null, schedulePrefs))

            // Case 2: SIREN IS default, but NO session or schedule -> MUST NOT activate!
            LockAccessibilityService.setCachedIsSirenDefaultForTesting(true)
            val emptyPrefs = FakeTestPreferences(emptyMap())
            assertFalse(LockAccessibilityService.isHomeChangeInterceptorActive(null, emptyPrefs))

            val inactiveSchedulePrefs = FakeTestPreferences(mapOf("schedules_json" to "[{\"id\":\"s1\",\"isActive\":false}]"))
            assertFalse(LockAccessibilityService.isHomeChangeInterceptorActive(null, inactiveSchedulePrefs))

            // Case 3: BOTH conditions met (SIREN is default AND active session/schedule) -> MUST activate!
            assertTrue(LockAccessibilityService.isHomeChangeInterceptorActive(null, lockdownPrefs))
            assertTrue(LockAccessibilityService.isHomeChangeInterceptorActive(null, consequencePrefs))
            assertTrue(LockAccessibilityService.isHomeChangeInterceptorActive(null, schedulePrefs))
        } finally {
            LockAccessibilityService.setCachedIsSirenDefaultForTesting(null)
        }
    }

    @Test
    fun testStage3SystemAllowSecondaryAppClassifierOptionB() {
        val sysApp = android.content.pm.ApplicationInfo().apply {
            flags = android.content.pm.ApplicationInfo.FLAG_SYSTEM
        }
        val userApp = android.content.pm.ApplicationInfo().apply {
            flags = 0
        }

        // 1. Clean system utility / OS dialog passes Stage 3 SYSALLOW
        assertTrue(AppClassifier.isPassedStage3SystemAllow(sysApp, "android", "Android System"))
        assertTrue(AppClassifier.isPassedStage3SystemAllow(sysApp, "com.android.internal.app.ResolverActivity", "ResolverActivity"))
        assertTrue(AppClassifier.isPassedStage3SystemAllow(sysApp, "com.android.documentsui", "Files"))

        // 2. Pre-installed system game is REJECTED by Stage 3 (Detected Distracting comes first in Option B)
        assertFalse(AppClassifier.isPassedStage3SystemAllow(sysApp, "com.oem.systemgame", "Space Shooter 3D Game"))

        // 3. System web browser is NOT marked as invisible SYSALLOW utility (routed to WebClassifier)
        assertFalse(AppClassifier.isPassedStage3SystemAllow(sysApp, "com.android.chrome", "Chrome"))

        // 4. Anti-tamper settings and OEM bloatware on system partition are strictly vetoed
        assertFalse(AppClassifier.isPassedStage3SystemAllow(sysApp, "com.android.settings", "Settings"))
        assertFalse(AppClassifier.isPassedStage3SystemAllow(sysApp, "com.xiaomi.joyose", "Joyose"))

        // 5. Third-party downloaded apps (non-system) never pass Stage 3 SYSALLOW
        assertFalse(AppClassifier.isPassedStage3SystemAllow(userApp, "com.example.unknownapp", "Generic App"))
    }

    @Test
    fun testSystemSettingsDialogActivityPickerUnblocked() {
        val sysApp = android.content.pm.ApplicationInfo().apply {
            flags = android.content.pm.ApplicationInfo.FLAG_SYSTEM
        }

        // 1. AppClassifier.isSystemSettingsDialog matches ActivityPicker and standard system dialogs
        assertTrue(AppClassifier.isSystemSettingsDialog("com.android.settings.ActivityPicker"))
        assertTrue(AppClassifier.isSystemSettingsDialog(".ActivityPicker"))
        assertTrue(AppClassifier.isSystemSettingsDialog("ActivityPicker"))
        assertTrue(AppClassifier.isSystemSettingsDialog("Choose activity"))
        assertTrue(AppClassifier.isSystemSettingsDialog("choose activity"))
        assertTrue(AppClassifier.isSystemSettingsDialog("Activity Picker"))
        assertTrue(AppClassifier.isSystemSettingsDialog("com.android.settings.bluetooth.BluetoothPairingDialog"))
        assertTrue(AppClassifier.isSystemSettingsDialog("com.android.settings.wifi.WifiDialogActivity"))

        // 2. Dangerous anti-tamper Settings activities are NOT system settings dialogs
        assertFalse(AppClassifier.isSystemSettingsDialog("com.android.settings.Settings"))
        assertFalse(AppClassifier.isSystemSettingsDialog("com.android.settings.SubSettings"))
        assertFalse(AppClassifier.isSystemSettingsDialog("com.android.settings.applications.InstalledAppDetails"))
        assertFalse(AppClassifier.isSystemSettingsDialog("com.android.settings.applications.ManageApplications"))
        assertFalse(AppClassifier.isSystemSettingsDialog("com.android.settings.DevelopmentSettings"))
        assertFalse(AppClassifier.isSystemSettingsDialog(null))
        assertFalse(AppClassifier.isSystemSettingsDialog(""))

        // 3. isSettingsOrDeviceManager exempts ActivityPicker / Choose activity but strictly blocks actual Settings
        assertFalse(AppClassifier.isSettingsOrDeviceManager("com.android.settings", "Settings", "com.android.settings.ActivityPicker"))
        assertFalse(AppClassifier.isSettingsOrDeviceManager("com.android.settings", "Settings", "Choose activity"))
        assertFalse(AppClassifier.isSettingsOrDeviceManager("com.android.settings", "Settings", ".ActivityPicker"))
        assertTrue(AppClassifier.isSettingsOrDeviceManager("com.android.settings", "Settings", "com.android.settings.Settings"))
        assertTrue(AppClassifier.isSettingsOrDeviceManager("com.android.settings", "Settings", "com.android.settings.SubSettings"))
        assertTrue(AppClassifier.isSettingsOrDeviceManager("com.android.settings", "Settings", null))

        // 4. isStage1Vetoed respects the activity-level exemption
        assertFalse(AppClassifier.isStage1Vetoed("com.android.settings", "Settings", "com.android.settings.ActivityPicker"))
        assertFalse(AppClassifier.isStage1Vetoed("com.android.settings", "Settings", "Choose activity"))
        assertTrue(AppClassifier.isStage1Vetoed("com.android.settings", "Settings", "com.android.settings.Settings"))
        assertTrue(AppClassifier.isStage1Vetoed("com.android.settings", "Settings", null))

        // 5. Stage 3 SYSALLOW allows ActivityPicker dialog hosted inside Settings.apk
        assertTrue(AppClassifier.isPassedStage3SystemAllow(sysApp, "com.android.settings", "Settings", "com.android.settings.ActivityPicker"))
        assertTrue(AppClassifier.isPassedStage3SystemAllow(sysApp, "com.android.settings", "Settings", "Choose activity"))
        assertFalse(AppClassifier.isPassedStage3SystemAllow(sysApp, "com.android.settings", "Settings", "com.android.settings.Settings"))
        assertFalse(AppClassifier.isPassedStage3SystemAllow(sysApp, "com.android.settings", "Settings", null))
    }

    @Test
    fun testPortalSuperAppVideoViewIdClassification() {
        val baiduPkg = "com.baidu.searchbox"
        val naverPkg = "com.nhn.android.search"

        // 1. Curated allowed portal super-app recognition (Exactly 2 allowed)
        assertTrue(AppClassifier.isPortalSuperApp(baiduPkg))
        assertTrue(AppClassifier.isPortalSuperApp(naverPkg))
        assertFalse(AppClassifier.isPortalSuperApp("com.transsion.phoenix"))
        assertFalse(AppClassifier.isPortalSuperApp("com.UCMobile"))
        assertFalse(AppClassifier.isPortalSuperApp("com.uc.browser.en"))
        assertFalse(AppClassifier.isPortalSuperApp("com.android.chrome"))
        assertFalse(AppClassifier.isPortalSuperApp("com.sec.android.app.sbrowser"))

        // 2. Disallowed unmanaged super-apps & portal browsers
        assertTrue(AppClassifier.isDisallowedPortalSuperApp("com.transsion.phoenix"))
        assertTrue(AppClassifier.isDisallowedPortalSuperApp("com.UCMobile"))
        assertTrue(AppClassifier.isDisallowedPortalSuperApp("com.uc.browser.en"))
        assertTrue(AppClassifier.isDisallowedPortalSuperApp("com.opera.mini.native"))
        assertFalse(AppClassifier.isDisallowedPortalSuperApp(baiduPkg))
        assertFalse(AppClassifier.isDisallowedPortalSuperApp(naverPkg))
        assertFalse(AppClassifier.isDisallowedPortalSuperApp("com.android.chrome"))

        // 3. Browser package gate: Standard browsers allowed, curated superapps allowed, unmanaged portals strictly blocked
        assertTrue(AppClassifier.isBrowserPackage("com.android.chrome"))
        assertTrue(AppClassifier.isBrowserPackage("com.sec.android.app.sbrowser"))
        assertTrue(AppClassifier.isBrowserPackage("org.mozilla.firefox"))
        assertTrue(AppClassifier.isBrowserPackage("com.microsoft.emmx"))
        assertTrue(AppClassifier.isBrowserPackage("com.brave.browser"))
        assertTrue(AppClassifier.isBrowserPackage(baiduPkg))
        assertTrue(AppClassifier.isBrowserPackage(naverPkg))
        assertFalse(AppClassifier.isBrowserPackage("com.transsion.phoenix"))
        assertFalse(AppClassifier.isBrowserPackage("com.UCMobile"))
        assertFalse(AppClassifier.isBrowserPackage("com.uc.browser.en"))

        // 4. Distracting sub-activity detection for Baidu and Naver
        assertTrue(AppClassifier.isDistractingSubActivity(baiduPkg, "com.baidu.searchbox.video.feedflow.tab.VideoTabActivity"))
        assertTrue(AppClassifier.isDistractingSubActivity(naverPkg, "com.nhn.android.clip.ui.ClipActivity"))
        assertTrue(AppClassifier.isDistractingSubActivity(naverPkg, "com.nhn.android.search.clip.ClipViewerActivity"))
        assertFalse(AppClassifier.isDistractingSubActivity(baiduPkg, "com.baidu.browser.search.LightSearchActivity"))
        assertFalse(AppClassifier.isDistractingSubActivity(naverPkg, "com.nhn.android.search.universe.UniverseActivity"))
        assertFalse(AppClassifier.isDistractingSubActivity(naverPkg, "com.nhn.android.search.browser.InAppBrowserActivity"))

        // 5. Baidu native video flow and reels component view IDs
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(baiduPkg, "com.baidu.searchbox:id/video_flow_cmp_player"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(baiduPkg, "com.baidu.searchbox:id/video_flow_tab_component"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(baiduPkg, "com.baidu.searchbox:id/video_item_portrait_root"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(baiduPkg, "com.baidu.searchbox:id/first_init_video_item_container"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(baiduPkg, "com.baidu.searchbox:id/video_flow_cmp_list"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(baiduPkg, "com.baidu.searchbox:id/video_flow_next_big_card"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(baiduPkg, "com.baidu.searchbox:id/video_flow_cmp_seek_bar"))

        // 6. Baidu legitimate search UI view IDs must NOT be classified as video view IDs
        assertFalse(AppClassifier.isPortalSuperAppVideoViewId(baiduPkg, "com.baidu.searchbox:id/search_box_content"))
        assertFalse(AppClassifier.isPortalSuperAppVideoViewId(baiduPkg, "com.baidu.searchbox:id/landing_page_box_tv"))
        assertFalse(AppClassifier.isPortalSuperAppVideoViewId(baiduPkg, "com.baidu.searchbox:id/bdframeview_id"))
        assertFalse(AppClassifier.isPortalSuperAppVideoViewId(baiduPkg, "com.baidu.searchbox:id/main_fragment_content"))
        assertFalse(AppClassifier.isPortalSuperAppVideoViewId(baiduPkg, null))
        assertFalse(AppClassifier.isPortalSuperAppVideoViewId(baiduPkg, ""))

        // 7. NAVER native Clip and video component view IDs
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, "com.nhn.android.search:id/container_clip_viewpager"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, "com.nhn.android.search:id/clip_follow_view_pager"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, "com.nhn.android.search:id/videoView"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, "com.nhn.android.search:id/videoGroup"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, "com.nhn.android.search:id/container_clip_nested_scrollable_host"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, "com.nhn.android.search:id/shortentsNowViewPager"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, "com.nhn.android.search:id/clipContentSoundToggle"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, "com.nhn.android.search:id/clip_root"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, "com.nhn.android.search:id/clip_player"))
        assertTrue(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, "com.nhn.android.search:id/clip_form"))

        // 8. NAVER legitimate search UI view IDs must NOT be classified as video view IDs
        assertFalse(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, "com.nhn.android.search.InAppBrowser:id/search_window_edit"))
        assertFalse(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, "com.nhn.android.search:id/inappWebView"))
        assertFalse(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, "com.nhn.android.search:id/nx_query"))
        assertFalse(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, null))
        assertFalse(AppClassifier.isPortalSuperAppVideoViewId(naverPkg, ""))

        // 9. Standard general-purpose browsers never trigger portal video view IDs
        assertFalse(AppClassifier.isPortalSuperAppVideoViewId("com.android.chrome", "com.android.chrome:id/video_player"))
        assertFalse(AppClassifier.isPortalSuperAppVideoViewId("com.sec.android.app.sbrowser", "com.sec.android.app.sbrowser:id/video_flow_cmp_player"))
    }
}



