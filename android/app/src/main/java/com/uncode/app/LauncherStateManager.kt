package com.uncode.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log

/**
 * LauncherStateManager
 *
 * Dedicated persistent state manager for QIEZKA's Home proxy target.
 * Stores and retrieves the user's selected real launcher (One UI, Nova, Lawnchair, etc.)
 * backed by SharedPreferences ("qiezka_home_proxy").
 *
 * Enforces Stage 1 Master Veto to guarantee that system settings, setup wizards,
 * or fallback activities can NEVER be persisted or selected as the target launcher.
 */
object LauncherStateManager {
    private const val TAG = "LauncherStateManager"
    const val PREFS_NAME = "qiezka_home_proxy"
    const val KEY_SELECTED_LAUNCHER_PKG = "selected_launcher_pkg"
    const val KEY_SELECTED_LAUNCHER_CLS = "selected_launcher_cls"
    const val ACTION_UPDATE_LAUNCHER = "com.siren.homeproxy.ACTION_UPDATE_LAUNCHER"
    const val SIREN_PACKAGE = "com.siren.homeproxy"

    /**
     * Actively pushes the launcher configuration to SIREN HomeProxy via explicit broadcast.
     * This avoids any 1-press delay or continuous polling on Home key clicks.
     */
    @JvmStatic
    fun notifySirenLauncherChanged(context: Context?, pkg: String?, cls: String?) {
        if (context == null) return
        try {
            val updateIntent = Intent(ACTION_UPDATE_LAUNCHER).apply {
                setPackage(SIREN_PACKAGE)
                if (!pkg.isNullOrBlank()) {
                    putExtra("package", pkg.trim())
                    putExtra("class", cls?.trim() ?: "")
                }
            }
            context.sendBroadcast(updateIntent)
            Log.i(TAG, "Dispatched ACTION_UPDATE_LAUNCHER to SIREN: pkg=$pkg, cls=$cls")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to broadcast launcher update to SIREN: ${t.message}")
        }
    }

    /**
     * Saves the chosen target launcher to persistent disk storage and actively pushes to SIREN.
     * Vetoes any package that fails Stage 1 Master Veto or attempts to set SIREN as its own target.
     */
    @JvmStatic
    fun saveSelectedLauncher(context: Context?, pkg: String?, cls: String?): Boolean {
        if (context == null || pkg.isNullOrBlank()) {
            return false
        }

        val trimmedPkg = pkg.trim()
        val trimmedCls = cls?.trim() ?: ""

        // Reject SIREN itself to prevent loopback
        if (SIREN_PACKAGE == trimmedPkg) {
            Log.w(TAG, "Cannot set SIREN itself as target launcher")
            return false
        }

        // Stage 1 Master Veto Protection: Reject Settings, Device Care, Setup Wizards, Bloatware, Distractions
        if (AppClassifier.isStage1Vetoed(trimmedPkg, null)) {
            Log.w(TAG, "Attempted to save vetoed package as launcher: $trimmedPkg")
            return false
        }

        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_SELECTED_LAUNCHER_PKG, trimmedPkg)
                .putString(KEY_SELECTED_LAUNCHER_CLS, trimmedCls)
                .apply()
            Log.i(TAG, "Saved selected launcher: $trimmedPkg / $trimmedCls")
            notifySirenLauncherChanged(context, trimmedPkg, trimmedCls)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save selected launcher: ${e.message}")
            false
        }
    }

    /**
     * Clears the explicitly saved launcher from persistent disk storage
     * and signals SIREN HomeProxy to fall back to auto-discovery.
     */
    @JvmStatic
    fun clearSelectedLauncher(context: Context?) {
        if (context == null) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove(KEY_SELECTED_LAUNCHER_PKG)
                .remove(KEY_SELECTED_LAUNCHER_CLS)
                .apply()
            Log.i(TAG, "Cleared selected launcher")
            notifySirenLauncherChanged(context, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear selected launcher: ${e.message}")
        }
    }

    /**
     * Retrieves the current selected launcher component.
     * If no launcher is explicitly saved, or if the saved launcher is invalid/corrupted,
     * it auto-discovers and persists the best installed real launcher (excluding QIEZKA and fallbacks).
     */
    @JvmStatic
    fun getSelectedLauncher(context: Context?): ComponentName? {
        if (context == null) return null

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var savedPkg = prefs.getString(KEY_SELECTED_LAUNCHER_PKG, null)
        var savedCls = prefs.getString(KEY_SELECTED_LAUNCHER_CLS, null)

        // Sanitize: Purge if vetoed by Stage 1 Master Veto (e.g. legacy com.android.settings)
        if (savedPkg != null && AppClassifier.isStage1Vetoed(savedPkg, null)) {
            Log.w(TAG, "Purging vetoed launcher from prefs: $savedPkg")
            prefs.edit()
                .remove(KEY_SELECTED_LAUNCHER_PKG)
                .remove(KEY_SELECTED_LAUNCHER_CLS)
                .apply()
            savedPkg = null
            savedCls = null
        }

        val pm = context.packageManager ?: return null

        // If saved launcher exists and is valid on device, return it
        if (!savedPkg.isNullOrEmpty()) {
            if (!savedCls.isNullOrEmpty()) {
                try {
                    pm.getActivityInfo(ComponentName(savedPkg, savedCls), 0)
                    return ComponentName(savedPkg, savedCls)
                } catch (ignore: Exception) {}
            }
            // Resolve default activity if class name wasn't specified
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(savedPkg)
                val matches = pm.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                if (!matches.isNullOrEmpty() && matches[0].activityInfo != null) {
                    val actName = matches[0].activityInfo.name
                    return ComponentName(savedPkg, actName)
                }
            } catch (ignore: Exception) {}
        }

        // Auto-discover genuine installed launchers (excluding self, FallbackHome, SIREN, and settings)
        try {
            val queryIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val candidates = pm.queryIntentActivities(queryIntent, 0)

            for (info in candidates) {
                if (InstalledLauncherDetector.isRealLauncher(info, context.packageName)) {
                    val pkg = info.activityInfo.packageName
                    if (SIREN_PACKAGE.equals(pkg, ignoreCase = true)) continue
                    val cls = info.activityInfo.name
                    if (saveSelectedLauncher(context, pkg, cls)) {
                        Log.i(TAG, "Auto-discovered and set target launcher: $pkg / $cls")
                        return ComponentName(pkg, cls)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering launcher: ${e.message}")
        }

        return null
    }
}
