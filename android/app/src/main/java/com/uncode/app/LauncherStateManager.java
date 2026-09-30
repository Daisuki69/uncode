package com.uncode.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import java.util.List;

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
public class LauncherStateManager {
    private static final String TAG = "LauncherStateManager";
    public static final String PREFS_NAME = "qiezka_home_proxy";
    public static final String KEY_SELECTED_LAUNCHER_PKG = "selected_launcher_pkg";
    public static final String KEY_SELECTED_LAUNCHER_CLS = "selected_launcher_cls";
    public static final String ACTION_UPDATE_LAUNCHER = "com.siren.homeproxy.ACTION_UPDATE_LAUNCHER";
    public static final String SIREN_PACKAGE = "com.siren.homeproxy";

    /**
     * Actively pushes the launcher configuration to SIREN HomeProxy via explicit broadcast.
     * This avoids any 1-press delay or continuous polling on Home key clicks.
     */
    public static void notifySirenLauncherChanged(Context context, String pkg, String cls) {
        if (context == null) return;
        try {
            Intent updateIntent = new Intent(ACTION_UPDATE_LAUNCHER);
            updateIntent.setPackage(SIREN_PACKAGE);
            if (pkg != null && !pkg.trim().isEmpty()) {
                updateIntent.putExtra("package", pkg.trim());
                updateIntent.putExtra("class", cls != null ? cls.trim() : "");
            }
            context.sendBroadcast(updateIntent);
            Log.i(TAG, "Dispatched ACTION_UPDATE_LAUNCHER to SIREN: pkg=" + pkg + ", cls=" + cls);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to broadcast launcher update to SIREN: " + t.getMessage());
        }
    }

    /**
     * Saves the chosen target launcher to persistent disk storage and actively pushes to SIREN.
     * Vetoes any package that fails Stage 1 Master Veto or attempts to set SIREN as its own target.
     */
    public static boolean saveSelectedLauncher(Context context, String pkg, String cls) {
        if (context == null || pkg == null || pkg.trim().isEmpty()) {
            return false;
        }

        String trimmedPkg = pkg.trim();
        String trimmedCls = cls != null ? cls.trim() : "";

        // Reject SIREN itself to prevent loopback
        if (SIREN_PACKAGE.equals(trimmedPkg)) {
            Log.w(TAG, "Cannot set SIREN itself as target launcher");
            return false;
        }

        // Stage 1 Master Veto Protection: Reject Settings, Device Care, Setup Wizards, Bloatware, Distractions
        if (AppClassifier.isStage1Vetoed(trimmedPkg, null)) {
            Log.w(TAG, "Attempted to save vetoed package as launcher: " + trimmedPkg);
            return false;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                .putString(KEY_SELECTED_LAUNCHER_PKG, trimmedPkg)
                .putString(KEY_SELECTED_LAUNCHER_CLS, trimmedCls)
                .apply();
            Log.i(TAG, "Saved selected launcher: " + trimmedPkg + " / " + trimmedCls);
            notifySirenLauncherChanged(context, trimmedPkg, trimmedCls);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save selected launcher: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clears the explicitly saved launcher from persistent disk storage
     * and signals SIREN HomeProxy to fall back to auto-discovery.
     */
    public static void clearSelectedLauncher(Context context) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                .remove(KEY_SELECTED_LAUNCHER_PKG)
                .remove(KEY_SELECTED_LAUNCHER_CLS)
                .apply();
            Log.i(TAG, "Cleared selected launcher");
            notifySirenLauncherChanged(context, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear selected launcher: " + e.getMessage());
        }
    }

    /**
     * Retrieves the current selected launcher component.
     * If no launcher is explicitly saved, or if the saved launcher is invalid/corrupted,
     * it auto-discovers and persists the best installed real launcher (excluding QIEZKA and fallbacks).
     */
    public static ComponentName getSelectedLauncher(Context context) {
        if (context == null) return null;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedPkg = prefs.getString(KEY_SELECTED_LAUNCHER_PKG, null);
        String savedCls = prefs.getString(KEY_SELECTED_LAUNCHER_CLS, null);

        // Sanitize: Purge if vetoed by Stage 1 Master Veto (e.g. legacy com.android.settings)
        if (savedPkg != null && AppClassifier.isStage1Vetoed(savedPkg, null)) {
            Log.w(TAG, "Purging vetoed launcher from prefs: " + savedPkg);
            prefs.edit()
                .remove(KEY_SELECTED_LAUNCHER_PKG)
                .remove(KEY_SELECTED_LAUNCHER_CLS)
                .apply();
            savedPkg = null;
            savedCls = null;
        }

        PackageManager pm = context.getPackageManager();

        // If saved launcher exists and is valid on device, return it
        if (savedPkg != null && !savedPkg.isEmpty()) {
            if (savedCls != null && !savedCls.isEmpty()) {
                try {
                    pm.getActivityInfo(new ComponentName(savedPkg, savedCls), 0);
                    return new ComponentName(savedPkg, savedCls);
                } catch (Exception ignore) {}
            }
            // Resolve default activity if class name wasn't specified
            try {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(savedPkg);
                List<ResolveInfo> matches = pm.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
                if (matches != null && !matches.isEmpty() && matches.get(0).activityInfo != null) {
                    String actName = matches.get(0).activityInfo.name;
                    return new ComponentName(savedPkg, actName);
                }
            } catch (Exception ignore) {}
        }

        // Auto-discover genuine installed launchers (excluding self, FallbackHome, SIREN, and settings)
        try {
            Intent queryIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> candidates = pm.queryIntentActivities(queryIntent, 0);

            if (candidates != null) {
                for (ResolveInfo info : candidates) {
                    if (InstalledLauncherDetector.isRealLauncher(info, context.getPackageName())) {
                        String pkg = info.activityInfo.packageName;
                        if (SIREN_PACKAGE.equalsIgnoreCase(pkg)) continue;
                        String cls = info.activityInfo.name;
                        if (saveSelectedLauncher(context, pkg, cls)) {
                            Log.i(TAG, "Auto-discovered and set target launcher: " + pkg + " / " + cls);
                            return new ComponentName(pkg, cls);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error discovering launcher: " + e.getMessage());
        }

        return null;
    }
}
