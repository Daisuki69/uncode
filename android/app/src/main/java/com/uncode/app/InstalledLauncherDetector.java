package com.uncode.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * InstalledLauncherDetector
 *
 * Dedicated resolver that queries, analyzes, and returns all installed Home launchers
 * (system pre-installed and third-party) on the device, along with their icons and metadata.
 * Excludes QIEZKA itself to present candidates for the Home Shell proxy architecture.
 */
public class InstalledLauncherDetector {
    private static final String TAG = "LauncherDetector";

    public static class LauncherInfo {
        public final String packageName;
        public final String activityName;
        public final String name;
        public final String icon; // Base64 PNG data URL
        public final boolean isSystem;
        public final boolean isCurrentDefault;

        public LauncherInfo(String packageName, String activityName, String name, String icon, boolean isSystem, boolean isCurrentDefault) {
            this.packageName = packageName;
            this.activityName = activityName;
            this.name = name;
            this.icon = icon;
            this.isSystem = isSystem;
            this.isCurrentDefault = isCurrentDefault;
        }

        public JSObject toJsObject() {
            JSObject obj = new JSObject();
            obj.put("packageName", packageName);
            obj.put("activityName", activityName);
            obj.put("name", name);
            if (icon != null) {
                obj.put("icon", icon);
            }
            obj.put("isSystem", isSystem);
            obj.put("isCurrentDefault", isCurrentDefault);
            return obj;
        }
    }

    /**
     * Determines whether a ResolveInfo responds as a legitimate user-facing Home Launcher,
     * filtering out internal fallback activities (such as FallbackHome), direct boot placeholders,
     * and apps vetoed by QIEZKA's Stage 1 Master Veto Gate (Settings, Device Managers, Bloatware, Distractions).
     */
    public static boolean isRealLauncher(ResolveInfo info, String myPkg) {
        if (info == null || info.activityInfo == null || info.activityInfo.packageName == null) {
            return false;
        }

        String pkg = info.activityInfo.packageName.trim();

        // 1. Exclude self
        if (myPkg != null && pkg.equalsIgnoreCase(myPkg.trim())) {
            return false;
        }

        // 2. Android Manifest priority invariant: Negative priority indicates fallback / emergency placeholder
        // e.g., com.android.settings.FallbackHome has android:priority="-1000"
        if (info.priority < 0) {
            return false;
        }

        // 3. Activity name checks: FallbackHome placeholder
        String activityName = info.activityInfo.name != null ? info.activityInfo.name : "";
        if (activityName.toLowerCase(java.util.Locale.ROOT).contains("fallbackhome")) {
            return false;
        }

        // 4. QIEZKA 3-Stage Model: Stage 1 Master Veto Gate
        // Strictly rejects Android Settings, Device Care, Phone Managers, Setup Wizards, Bloatware, and Distractions
        if (AppClassifier.isStage1Vetoed(pkg, null)) {
            return false;
        }

        return true;
    }

    public static List<LauncherInfo> getInstalledLaunchers(Context context) {
        if (context == null) return Collections.emptyList();
        List<LauncherInfo> result = new ArrayList<>();

        try {
            PackageManager pm = context.getPackageManager();
            if (pm == null) return Collections.emptyList();

            String myPkg = context.getPackageName();

            // 1. Resolve current default Home handler in the OS
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo defaultResolve = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
            String defaultPkg = null;
            if (defaultResolve != null && isRealLauncher(defaultResolve, myPkg)) {
                defaultPkg = defaultResolve.activityInfo.packageName;
            }

            // 2. Query all installed activities responding to CATEGORY_HOME
            List<ResolveInfo> candidates = pm.queryIntentActivities(homeIntent, 0);
            if (candidates == null) return Collections.emptyList();

            Set<String> seenPackages = new HashSet<>();

            for (ResolveInfo info : candidates) {
                if (!isRealLauncher(info, myPkg)) {
                    continue;
                }
                String pkg = info.activityInfo.packageName;
                if (seenPackages.contains(pkg)) {
                    continue;
                }
                seenPackages.add(pkg);

                String activityName = info.activityInfo.name;
                String label;
                boolean isSystem = false;
                Drawable iconDrawable = null;

                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                    label = pm.getApplicationLabel(appInfo).toString();
                    isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    iconDrawable = pm.getApplicationIcon(appInfo);
                } catch (Exception e) {
                    label = info.loadLabel(pm).toString();
                    try {
                        iconDrawable = info.loadIcon(pm);
                    } catch (Exception ignore) {}
                }

                String iconBase64 = drawableToBase64(iconDrawable);
                boolean isCurrentDefault = defaultPkg != null && defaultPkg.equals(pkg);

                result.add(new LauncherInfo(pkg, activityName, label, iconBase64, isSystem, isCurrentDefault));
            }

            // Sort: Current default first, then system launchers, then third-party
            Collections.sort(result, new Comparator<LauncherInfo>() {
                @Override
                public int compare(LauncherInfo a, LauncherInfo b) {
                    if (a.isCurrentDefault != b.isCurrentDefault) {
                        return a.isCurrentDefault ? -1 : 1;
                    }
                    if (a.isSystem != b.isSystem) {
                        return a.isSystem ? -1 : 1;
                    }
                    return a.name.compareToIgnoreCase(b.name);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error discovering installed launchers", e);
        }

        return result;
    }

    public static JSArray getInstalledLaunchersJson(Context context) {
        List<LauncherInfo> list = getInstalledLaunchers(context);
        JSArray array = new JSArray();
        for (LauncherInfo info : list) {
            array.put(info.toJsObject());
        }
        return array;
    }

    public static String drawableToBase64(Drawable drawable) {
        if (drawable == null) return null;
        try {
            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            } else {
                int width = Math.max(1, drawable.getIntrinsicWidth());
                int height = Math.max(1, drawable.getIntrinsicHeight());
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }

            if (bitmap == null) return null;

            // Scale down thumbnail to 96x96 px to ensure ultra-fast IPC transfer across Capacitor bridge
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 96, 96, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to encode launcher icon to base64: " + t.getMessage());
            return null;
        }
    }
}
