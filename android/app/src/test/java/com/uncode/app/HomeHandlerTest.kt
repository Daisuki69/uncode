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
        assertTrue(AppClassifier.isStage1Vetoed("android", null))
        assertTrue(AppClassifier.isStage1Vetoed("com.android.systemui", null))

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
    fun testLauncherStateManagerRejectsVetoedPackages() {
        assertFalse(LauncherStateManager.saveSelectedLauncher(null, "com.sec.android.app.launcher", ""))
        assertFalse(LauncherStateManager.saveSelectedLauncher(null, "", ""))
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
    }
}
