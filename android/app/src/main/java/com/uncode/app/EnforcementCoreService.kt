package com.uncode.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * EnforcementCoreService
 *
 * Headless background keeper for QIEZKA's focus enforcement engine.
 * Ensures the native services (Accessibility ticker, DNS VPN sinkhole, and timers)
 * remain operational in the background without launching any Activity or touching the UI.
 */
class EnforcementCoreService : Service() {

    companion object {
        private const val TAG = "EnforcementCore"

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

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
