package com.uncode.app

import android.app.Service
import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

/**
 * EnforcementCoreService
 *
 * Headless background keeper for QIEZKA's focus enforcement engine.
 * Ensures native services remain operational in the background without UI.
 *
 * Also maintains a bidirectional kernel [IBinder.DeathRecipient] link to
 * SIREN's ContentProvider (content://com.siren.homeproxy.provider).
 * If SIREN dies or is killed, QIEZKA cold-starts SIREN in < 50ms via IPC.
 */
class EnforcementCoreService : Service() {

    companion object {
        private const val TAG = "EnforcementCore"
        private const val SIREN_PROVIDER_AUTHORITY = "com.siren.homeproxy.provider"
        private val SIREN_PROVIDER_URI = Uri.parse("content://$SIREN_PROVIDER_AUTHORITY")
        private const val RELINK_DELAY_MS = 1000L

        @JvmStatic
        fun ensureRunning(context: Context?) {
            if (context == null) return
            try {
                val intent = Intent(context, EnforcementCoreService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start EnforcementCoreService: ${e.message}")
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var sirenClient: ContentProviderClient? = null
    private var sirenBinder: IBinder? = null
    private var isServiceActive = false

    private val sirenDeathRecipient = object : IBinder.DeathRecipient {
        override fun binderDied() {
            Log.w(TAG, "Kernel death notification received: SIREN Watchdog process died!")
            cleanupSirenLink()
            resurrectSiren()
            scheduleSirenRelink()
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceActive = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "EnforcementCoreService pulse received.")

        // Check active Accessibility Service status
        val las = LockAccessibilityService.getInstance()
        if (las != null) {
            Log.d(TAG, "LockAccessibilityService is active and operational.")
        } else {
            Log.w(TAG, "LockAccessibilityService is not connected. Awaiting user/system bind.")
        }

        // Verify FloatingOverlayService state if lockdown is active
        try {
            val prefs = getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
            val isLockdown = prefs?.getBoolean("lockdown_active", false) ?: false
            val lockEndTime = prefs?.getLong("lock_end_time", 0L) ?: 0L
            val timeOffset = prefs?.getLong("time_offset", 0L) ?: 0L
            val effectiveNow = System.currentTimeMillis() + timeOffset

            if (isLockdown && lockEndTime > effectiveNow) {
                FloatingOverlayService.startService(this, lockEndTime, "Study Session")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking overlay service status: ${e.message}")
        }

        // Maintain reciprocal link to SIREN Watchdog
        tryLinkToSirenDeath()

        return START_STICKY
    }

    override fun onDestroy() {
        isServiceActive = false
        cleanupSirenLink()
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Wakes SIREN immediately via ContentProvider IPC cold-start.
     */
    private fun resurrectSiren() {
        try {
            val bundle = contentResolver.call(
                SIREN_PROVIDER_URI,
                "ping",
                null,
                null
            )
            Log.d(TAG, "Resurrected SIREN via ContentProvider call: $bundle")
        } catch (e: Exception) {
            Log.d(TAG, "SIREN cold-start ping attempt: ${e.message}")
        }
    }

    /**
     * Attempts to acquire SIREN's provider binder and attach the kernel DeathRecipient.
     */
    private fun tryLinkToSirenDeath(): Boolean {
        if (!isServiceActive) return false
        cleanupSirenLink()

        try {
            val client = contentResolver.acquireUnstableContentProviderClient(
                SIREN_PROVIDER_AUTHORITY
            ) ?: run {
                Log.d(TAG, "SIREN provider client currently unavailable.")
                return false
            }

            val binder = getRemoteBinder(client)
            if (binder != null && binder.isBinderAlive) {
                binder.linkToDeath(sirenDeathRecipient, 0)
                sirenClient = client
                sirenBinder = binder
                Log.i(TAG, "Successfully linked kernel DeathRecipient to SIREN provider binder.")
                return true
            } else {
                client.close()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed linking DeathRecipient to SIREN: ${e.message}")
        }
        return false
    }

    private fun getRemoteBinder(client: ContentProviderClient): IBinder? {
        return try {
            val field = client.javaClass.getDeclaredField("mContentProvider")
            field.isAccessible = true
            val provider = field.get(client)
            if (provider is IBinder) {
                provider
            } else if (provider is android.os.IInterface) {
                provider.asBinder()
            } else {
                provider?.javaClass?.getMethod("asBinder")?.invoke(provider) as? IBinder
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun scheduleSirenRelink() {
        if (!isServiceActive) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isServiceActive && (sirenBinder == null || !sirenBinder!!.isBinderAlive)) {
                val success = tryLinkToSirenDeath()
                if (!success) {
                    resurrectSiren()
                    scheduleSirenRelink()
                }
            }
        }, RELINK_DELAY_MS)
    }

    private fun cleanupSirenLink() {
        try {
            sirenBinder?.unlinkToDeath(sirenDeathRecipient, 0)
        } catch (_: Exception) {}
        sirenBinder = null

        try {
            sirenClient?.close()
        } catch (_: Exception) {}
        sirenClient = null
    }
}
