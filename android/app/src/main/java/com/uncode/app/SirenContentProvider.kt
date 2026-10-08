package com.uncode.app

import android.content.ComponentName
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log

/**
 * SirenContentProvider
 *
 * Ultra-lightweight ContentProvider that responds to IPC inquiries from SIRENHomeProxy
 * (com.siren.homeproxy) via authority "com.uncode.app.provider".
 *
 * Serves the target Home launcher configuration to SIREN in < 1ms without launching
 * any Activity, window, or Chromium WebView runtime.
 */
class SirenContentProvider : ContentProvider() {

    companion object {
        private const val TAG = "SirenContentProvider"
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if ("getLauncher" == method) {
            try {
                val launcher = LauncherStateManager.getSelectedLauncher(context)
                val bundle = Bundle()
                if (launcher != null) {
                    bundle.putString("package", launcher.packageName)
                    bundle.putString("class", launcher.className)
                    bundle.putString("launcher_package", launcher.packageName)
                    bundle.putString("launcher_class", launcher.className)
                    Log.d(TAG, "Supplying target launcher to SIREN: ${launcher.packageName}")
                }
                return bundle
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving launcher for SIREN: ${e.message}")
            }
        }
        return super.call(method, arg, extras)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
