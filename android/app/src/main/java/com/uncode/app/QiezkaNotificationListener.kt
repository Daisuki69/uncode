package com.uncode.app

import android.content.ComponentName
import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.IInterface
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * QiezkaNotificationListener
 *
 * Secondary 24/7 OS-bound daemon (BIND_NOTIFICATION_LISTENER_SERVICE).
 * Provides critical self-healing capabilities via NotificationListenerService.requestRebind(ComponentName),
 * operates with zero Foreground Service notifications (completely immune to Android 14–16's "Active apps" task killer),
 * provides a natural zero-battery OS event heartbeat, and holds a reciprocal kernel linkToDeath on SIREN.
 */
class QiezkaNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "QiezkaNotifListener"
        private const val SIREN_PROVIDER_AUTHORITY = "com.siren.homeproxy.provider"

        @Volatile
        private var sIsConnected: Boolean = false

        @JvmStatic
        fun isConnected(): Boolean = sIsConnected

        @JvmStatic
        fun requestRebind(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val comp = ComponentName(context, QiezkaNotificationListener::class.java)
                    requestRebind(comp)
                    Log.i(TAG, "Requested OS rebind for QiezkaNotificationListener")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to requestRebind: ${e.message}")
            }
        }
    }

    private var sirenDeathRecipient: IBinder.DeathRecipient? = null
    private var linkedSirenBinder: IBinder? = null
    private var sirenClient: ContentProviderClient? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onListenerConnected() {
        super.onListenerConnected()
        sIsConnected = true
        Log.i(TAG, "QiezkaNotificationListener successfully connected to NotificationManagerService")
        linkSirenWatchdog()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        sIsConnected = false
        Log.w(TAG, "QiezkaNotificationListener disconnected! Requesting immediate self-healing rebind...")
        requestRebind(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Natural OS heartbeat: any notification posted across the system verifies SIREN connection
        ensureSirenWatchdogAlive()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Natural OS heartbeat on notification dismissal
        ensureSirenWatchdogAlive()
    }

    private fun linkSirenWatchdog() {
        cleanupSirenLink()
        try {
            val client = contentResolver.acquireUnstableContentProviderClient(SIREN_PROVIDER_AUTHORITY) ?: run {
                Log.d(TAG, "linkSirenWatchdog: SIREN provider client not currently available")
                return
            }
            val binder = getRemoteBinder(client)
            if (binder != null && binder.isBinderAlive) {
                val recipient = IBinder.DeathRecipient {
                    Log.w(TAG, "SIREN kernel death detected in QiezkaNotificationListener! Reviving SIREN...")
                    onSirenDied()
                }
                binder.linkToDeath(recipient, 0)
                sirenDeathRecipient = recipient
                linkedSirenBinder = binder
                sirenClient = client
                Log.i(TAG, "Successfully linked kernel DeathRecipient on SIREN in QiezkaNotificationListener")
            } else {
                client.close()
            }
        } catch (e: Exception) {
            Log.d(TAG, "linkSirenWatchdog error: ${e.message}")
        }
    }

    private fun getRemoteBinder(client: ContentProviderClient): IBinder? {
        return try {
            val field = client.javaClass.getDeclaredField("mContentProvider")
            field.isAccessible = true
            val provider = field.get(client)
            if (provider is IBinder) {
                provider
            } else if (provider is IInterface) {
                provider.asBinder()
            } else {
                provider?.javaClass?.getMethod("asBinder")?.invoke(provider) as? IBinder
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun onSirenDied() {
        try {
            // Revive SIREN via explicit broadcast with FLAG_INCLUDE_STOPPED_PACKAGES
            val revive = Intent("com.siren.homeproxy.ACTION_REVIVE").apply {
                setPackage("com.siren.homeproxy")
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            sendBroadcast(revive)

            // Direct content resolver ping to trigger cold-start
            val uri = Uri.parse("content://com.siren.homeproxy.provider")
            contentResolver.call(uri, "ping", null, null)
        } catch (_: Exception) {}

        // Schedule re-link attempt after SIREN finishes reviving
        mainHandler.postDelayed({
            linkSirenWatchdog()
        }, 1500L)
    }

    private fun ensureSirenWatchdogAlive() {
        try {
            val binder = linkedSirenBinder
            if (binder == null || !binder.isBinderAlive) {
                linkSirenWatchdog()
            }
        } catch (_: Exception) {}
    }

    private fun cleanupSirenLink() {
        try {
            val binder = linkedSirenBinder
            val recipient = sirenDeathRecipient
            if (binder != null && recipient != null) {
                binder.unlinkToDeath(recipient, 0)
            }
        } catch (_: Exception) {}
        sirenDeathRecipient = null
        linkedSirenBinder = null

        try {
            sirenClient?.close()
        } catch (_: Exception) {}
        sirenClient = null
    }

    override fun onDestroy() {
        super.onDestroy()
        sIsConnected = false
        cleanupSirenLink()
        mainHandler.removeCallbacksAndMessages(null)
        Log.i(TAG, "QiezkaNotificationListener destroyed")
    }
}
