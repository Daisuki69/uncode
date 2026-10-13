package com.uncode.app

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Log
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import java.io.ByteArrayOutputStream
import java.util.ArrayList
import java.util.Collections
import java.util.HashSet
import java.util.Locale

/**
 * InstalledLauncherDetector
 *
 * Dedicated resolver that queries, analyzes, and returns all installed Home launchers
 * (system pre-installed and third-party) on the device, along with their icons and metadata.
 * Excludes QIEZKA itself to present candidates for the Home Shell proxy architecture.
 */
object InstalledLauncherDetector {
    private const val TAG = "LauncherDetector"

    data class LauncherInfo(
        @JvmField val packageName: String,
        @JvmField val activityName: String,
        @JvmField val name: String,
        @JvmField val icon: String?,
        @JvmField val isSystem: Boolean,
        @JvmField val isCurrentDefault: Boolean
    ) {
        fun toJsObject(): JSObject {
            val obj = JSObject()
            obj.put("packageName", packageName)
            obj.put("activityName", activityName)
            obj.put("name", name)
            if (icon != null) {
                obj.put("icon", icon)
            }
            obj.put("isSystem", isSystem)
            obj.put("isCurrentDefault", isCurrentDefault)
            return obj
        }
    }

    /**
     * Determines whether a ResolveInfo responds as a legitimate user-facing Home Launcher,
     * filtering out internal fallback activities (such as FallbackHome), direct boot placeholders,
     * and apps vetoed by QIEZKA's Stage 1 Master Veto Gate (Settings, Device Managers, Bloatware, Distractions).
     */
    @JvmStatic
    fun isRealLauncher(info: ResolveInfo?, myPkg: String?): Boolean {
        if (info?.activityInfo?.packageName == null) {
            return false
        }

        val pkg = info.activityInfo.packageName.trim()

        // 1. Exclude self
        if (myPkg != null && pkg.equals(myPkg.trim(), ignoreCase = true)) {
            return false
        }

        // 1b. Exclude SIREN HomeProxy (companion proxy shell, not a selectable target launcher)
        if (LauncherStateManager.SIREN_PACKAGE.equals(pkg, ignoreCase = true)) {
            return false
        }

        // 2. Android Manifest priority invariant: Negative priority indicates fallback / emergency placeholder
        // e.g., com.android.settings.FallbackHome has android:priority="-1000"
        if (info.priority < 0) {
            return false
        }

        // 3. Activity name checks: FallbackHome placeholder
        val activityName = info.activityInfo.name ?: ""
        if (activityName.lowercase(Locale.ROOT).contains("fallbackhome")) {
            return false
        }

        // 4. QIEZKA 3-Stage Model: Stage 1 Master Veto Gate
        // Strictly rejects Android Settings, Device Care, Phone Managers, Setup Wizards, Bloatware, and Distractions
        if (AppClassifier.isStage1Vetoed(pkg, null)) {
            return false
        }

        return true
    }

    @JvmStatic
    fun getInstalledLaunchers(context: Context?): List<LauncherInfo> {
        if (context == null) return emptyList()
        val result = ArrayList<LauncherInfo>()

        try {
            val pm = context.packageManager ?: return emptyList()
            val myPkg = context.packageName

            // 1. Resolve current default Home handler in the OS
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val defaultResolve = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            var defaultPkg: String? = null
            if (defaultResolve != null && isRealLauncher(defaultResolve, myPkg)) {
                defaultPkg = defaultResolve.activityInfo.packageName
            }

            // 2. Query all installed activities responding to CATEGORY_HOME
            val candidates = pm.queryIntentActivities(homeIntent, 0)
            if (candidates.isNullOrEmpty()) return emptyList()

            val seenPackages = HashSet<String>()

            for (info in candidates) {
                if (!isRealLauncher(info, myPkg)) {
                    continue
                }
                val pkg = info.activityInfo.packageName
                if (seenPackages.contains(pkg)) {
                    continue
                }
                seenPackages.add(pkg)

                val activityName = info.activityInfo.name
                var label: String
                var isSystem = false
                var iconDrawable: Drawable? = null

                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    label = pm.getApplicationLabel(appInfo).toString()
                    isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    iconDrawable = pm.getApplicationIcon(appInfo)
                } catch (e: Exception) {
                    label = info.loadLabel(pm).toString()
                    try {
                        iconDrawable = info.loadIcon(pm)
                    } catch (ignore: Exception) {}
                }

                val iconBase64 = drawableToBase64(iconDrawable)
                val isCurrentDefault = defaultPkg != null && defaultPkg == pkg

                result.add(LauncherInfo(pkg, activityName, label, iconBase64, isSystem, isCurrentDefault))
            }

            // Sort: Current default first, then system launchers, then third-party
            Collections.sort(result) { a, b ->
                if (a.isCurrentDefault != b.isCurrentDefault) {
                    if (a.isCurrentDefault) -1 else 1
                } else if (a.isSystem != b.isSystem) {
                    if (a.isSystem) -1 else 1
                } else {
                    a.name.compareTo(b.name, ignoreCase = true)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error discovering installed launchers", e)
        }

        return result
    }

    @JvmStatic
    fun getInstalledLaunchersJson(context: Context?): JSArray {
        val list = getInstalledLaunchers(context)
        val array = JSArray()
        for (info in list) {
            array.put(info.toJsObject())
        }
        return array
    }

    @JvmStatic
    fun drawableToBase64(drawable: Drawable?): String? {
        if (drawable == null) return null
        return try {
            val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val width = Math.max(1, drawable.intrinsicWidth)
                val height = Math.max(1, drawable.intrinsicHeight)
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            } ?: return null

            // Scale down thumbnail to 96x96 px to ensure ultra-fast IPC transfer across Capacitor bridge
            val scaled = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val bytes = baos.toByteArray()
            "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to encode launcher icon to base64: ${t.message}")
            null
        }
    }
}
