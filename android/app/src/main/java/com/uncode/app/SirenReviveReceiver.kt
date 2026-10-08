package com.uncode.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * SirenReviveReceiver
 *
 * Catches the "com.uncode.app.ACTION_REVIVE" broadcast dispatched by SIRENHomeProxy
 * (com.siren.homeproxy) on device boot or watchdog liveness pings.
 *
 * Strictly revives the Headless Enforcement Core.
 * INVARIANT: NEVER calls startActivity(MainActivity) — zero UI intrusion or screen popping.
 */
class SirenReviveReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SirenReviveReceiver"
        const val ACTION_REVIVE = "com.uncode.app.ACTION_REVIVE"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action
        if (ACTION_REVIVE != action) return

        Log.i(TAG, "Received ACTION_REVIVE from SIREN watchdog. Refreshing Headless Core.")

        try {
            EnforcementCoreService.ensureRunning(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring EnforcementCoreService running: ${e.message}")
        }
    }
}
