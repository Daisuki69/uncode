package com.uncode.app

import org.junit.Assert.*
import org.junit.Test

class HomeHandlerTest {

    @Test
    fun testMainActivityDefaultLifecycleState() {
        // By default in unit test environment without an active activity instance,
        // MainActivity.isRunning() must return false
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
    fun testHomeHandlerConstants() {
        assertEquals("qiezka_home_proxy", QiezkaHomeHandlerActivity.PREFS_NAME)
        assertEquals("selected_launcher_pkg", QiezkaHomeHandlerActivity.KEY_SELECTED_LAUNCHER_PKG)
        assertEquals("selected_launcher_cls", QiezkaHomeHandlerActivity.KEY_SELECTED_LAUNCHER_CLS)
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
        // Core OS settings and device managers must be vetoed by Stage 1
        assertTrue(AppClassifier.isStage1Vetoed("com.android.settings", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.google.android.settings", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.samsung.android.settings", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.miui.securitycenter", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.coloros.safecenter", null))
        assertTrue(AppClassifier.isStage1Vetoed("android", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.android.systemui", null))

        // Setup wizards and device provisioning must be vetoed by Stage 1
        assertTrue(AppClassifier.isStage1Vetoed("com.google.android.setupwizard", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.sec.android.app.SecSetupWizard", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.android.setupwizard", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.android.provision", null))

        // Hardware bloatware / game boosters must be vetoed by Stage 1
        assertTrue(AppClassifier.isStage1Vetoed("com.xiaomi.joyose", null))

        // Known distracting apps must be vetoed by Stage 1
        assertTrue(AppClassifier.isStage1Vetoed("com.zhiliaoapp.musically", null)) // TikTok

        // Genuine OEM and 3rd-party launchers must PASS Stage 1
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
    fun testIsRealLauncherFiltersSettingsAndFallbacks() {
        val myPkg = "com.uncode.app"

        // 1. Null ResolveInfo
        assertFalse(InstalledLauncherDetector.isRealLauncher(null, myPkg))

        // 2. QIEZKA self package
        val selfInfo = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "com.uncode.app"
                name = "com.uncode.app.QiezkaHomeHandlerActivity"
            }
            priority = 1
        }
        assertFalse(InstalledLauncherDetector.isRealLauncher(selfInfo, myPkg))

        // 3. com.android.settings / FallbackHome
        val fallbackHome = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "com.android.settings"
                name = "com.android.settings.FallbackHome"
            }
            priority = -1000
        }
        assertFalse(InstalledLauncherDetector.isRealLauncher(fallbackHome, myPkg))

        // 4. Setup Wizard with CATEGORY_HOME
        val setupWizard = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "com.google.android.setupwizard"
                name = "com.google.android.setupwizard.SetupWizardActivity"
            }
            priority = 10
        }
        assertFalse(InstalledLauncherDetector.isRealLauncher(setupWizard, myPkg))

        // 5. Negative priority fallback
        val negativePriority = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "com.example.fallback"
                name = "com.example.fallback.EmergencyHome"
            }
            priority = -1
        }
        assertFalse(InstalledLauncherDetector.isRealLauncher(negativePriority, myPkg))

        // 6. Legitimate OEM Launcher (Samsung One UI Home)
        val oneUi = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "com.sec.android.app.launcher"
                name = "com.sec.android.app.launcher.activities.LauncherActivity"
            }
            priority = 0
        }
        assertTrue(InstalledLauncherDetector.isRealLauncher(oneUi, myPkg))

        // 7. Legitimate 3rd-party Launcher (Lawnchair)
        val lawnchair = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "ch.deletescape.lawnchair"
                name = "ch.deletescape.lawnchair.LawnchairLauncher"
            }
            priority = 0
        }
        assertTrue(InstalledLauncherDetector.isRealLauncher(lawnchair, myPkg))
    }
}
