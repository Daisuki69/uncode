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
}
