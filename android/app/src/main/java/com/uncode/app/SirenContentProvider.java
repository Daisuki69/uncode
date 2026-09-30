package com.uncode.app;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * SirenContentProvider
 *
 * Ultra-lightweight ContentProvider that responds to IPC inquiries from SIRENHomeProxy
 * (com.siren.homeproxy) via authority "com.uncode.app.provider".
 *
 * Serves the target Home launcher configuration to SIREN in < 1ms without launching
 * any Activity, window, or Chromium WebView runtime.
 */
public class SirenContentProvider extends ContentProvider {
    private static final String TAG = "SirenContentProvider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if ("getLauncher".equals(method)) {
            try {
                ComponentName launcher = LauncherStateManager.getSelectedLauncher(getContext());
                Bundle bundle = new Bundle();
                if (launcher != null) {
                    bundle.putString("package", launcher.getPackageName());
                    bundle.putString("class", launcher.getClassName());
                    bundle.putString("launcher_package", launcher.getPackageName());
                    bundle.putString("launcher_class", launcher.getClassName());
                    Log.d(TAG, "Supplying target launcher to SIREN: " + launcher.getPackageName());
                }
                return bundle;
            } catch (Exception e) {
                Log.e(TAG, "Error resolving launcher for SIREN: " + e.getMessage());
            }
        }
        return super.call(method, arg, extras);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
