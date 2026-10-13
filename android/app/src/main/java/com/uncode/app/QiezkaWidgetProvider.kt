package com.uncode.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.RemoteViews

/**
 * Native Android Home Screen App Widget for QIEZKA.
 *
 * Displays the high-resolution QIEZKA app icon and launches [MainActivity]
 * with FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP when tapped.
 *
 * Dynamically toggles the app drawer launcher alias (.MainActivityLauncher):
 * - Active widget present on home screen -> hides launcher icon from app drawer.
 * - No active widget on home screen -> restores launcher icon to app drawer.
 */
class QiezkaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId)
        }
        // Verify alignment of launcher icon visibility
        syncLauncherIconVisibility(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.i(TAG, "First Qiezka widget added -> hiding app drawer launcher icon")
        setLauncherIconVisible(context, false)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.i(TAG, "Last Qiezka widget removed -> restoring app drawer launcher icon")
        setLauncherIconVisible(context, true)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Check if any widgets still remain after this deletion
        syncLauncherIconVisibility(context)
    }

    companion object {
        private const val TAG = "QiezkaWidgetProvider"
        const val LAUNCHER_ALIAS_CLASS = "com.uncode.app.MainActivityLauncher"

        @JvmStatic
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_qiezka)

            // Intent to launch QIEZKA MainActivity directly (never points to alias)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                flags
            )

            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * Checks whether any QIEZKA widgets are currently active on the home screen.
         * If at least one exists, the app drawer launcher icon is hidden.
         * If zero exist, the app drawer launcher icon is restored.
         */
        @JvmStatic
        fun syncLauncherIconVisibility(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val providerComponent = ComponentName(context, QiezkaWidgetProvider::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(providerComponent)
                val hasActiveWidget = widgetIds != null && widgetIds.isNotEmpty()

                // Visible in drawer ONLY if there is NO active widget
                setLauncherIconVisible(context, !hasActiveWidget)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync launcher icon visibility: ${e.message}")
            }
        }

        /**
         * Sets the enabled state of the activity alias (.MainActivityLauncher).
         * [visible] = true -> COMPONENT_ENABLED_STATE_ENABLED (visible in drawer)
         * [visible] = false -> COMPONENT_ENABLED_STATE_DISABLED (hidden from drawer)
         *
         * Uses [PackageManager.DONT_KILL_APP] to prevent killing the process during state change.
         */
        @JvmStatic
        fun setLauncherIconVisible(context: Context, visible: Boolean) {
            try {
                val packageManager = context.packageManager ?: return
                val aliasComponent = ComponentName(context.packageName, LAUNCHER_ALIAS_CLASS)
                val targetState = if (visible) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }

                val currentState = packageManager.getComponentEnabledSetting(aliasComponent)
                if (currentState != targetState) {
                    packageManager.setComponentEnabledSetting(
                        aliasComponent,
                        targetState,
                        PackageManager.DONT_KILL_APP
                    )
                    Log.i(TAG, "Launcher alias visibility updated: visible=$visible (state=$targetState)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set launcher icon visibility: ${e.message}")
            }
        }
    }
}
