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

class QiezkaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId)
        }
        syncLauncherIconVisibility(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        syncLauncherIconVisibility(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        syncLauncherIconVisibility(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        syncLauncherIconVisibility(context)
    }

    companion object {
        private const val TAG = "QiezkaWidgetProvider"
        const val LAUNCHER_ALIAS_CLASS = "com.uncode.app.MainActivityLauncher"
        const val DECOY_ALIAS_CLASS = "com.uncode.app.DecoyLauncher"

        @JvmStatic
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_qiezka)

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

        @JvmStatic
        fun syncLauncherIconVisibility(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val providerComponent = ComponentName(context, QiezkaWidgetProvider::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(providerComponent)
                val hasActiveWidget = widgetIds != null && widgetIds.isNotEmpty()

                val packageManager = context.packageManager ?: return
                val mainAliasComponent = ComponentName(context.packageName, LAUNCHER_ALIAS_CLASS)
                val decoyAliasComponent = ComponentName(context.packageName, DECOY_ALIAS_CLASS)

                val mainTargetState = if (hasActiveWidget) {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                }

                val decoyTargetState = if (hasActiveWidget) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }

                if (packageManager.getComponentEnabledSetting(mainAliasComponent) != mainTargetState) {
                    packageManager.setComponentEnabledSetting(
                        mainAliasComponent,
                        mainTargetState,
                        PackageManager.DONT_KILL_APP
                    )
                }

                if (packageManager.getComponentEnabledSetting(decoyAliasComponent) != decoyTargetState) {
                    packageManager.setComponentEnabledSetting(
                        decoyAliasComponent,
                        decoyTargetState,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync launcher icon visibility: ${e.message}")
            }
        }

        @JvmStatic
        fun setLauncherIconVisible(context: Context, visible: Boolean) {
            try {
                val packageManager = context.packageManager ?: return
                val mainAliasComponent = ComponentName(context.packageName, LAUNCHER_ALIAS_CLASS)
                val decoyAliasComponent = ComponentName(context.packageName, DECOY_ALIAS_CLASS)

                val mainTargetState = if (visible) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }

                val decoyTargetState = if (visible) {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                }

                if (packageManager.getComponentEnabledSetting(mainAliasComponent) != mainTargetState) {
                    packageManager.setComponentEnabledSetting(
                        mainAliasComponent,
                        mainTargetState,
                        PackageManager.DONT_KILL_APP
                    )
                }

                if (packageManager.getComponentEnabledSetting(decoyAliasComponent) != decoyTargetState) {
                    packageManager.setComponentEnabledSetting(
                        decoyAliasComponent,
                        decoyTargetState,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set launcher icon visibility: ${e.message}")
            }
        }
    }
}
