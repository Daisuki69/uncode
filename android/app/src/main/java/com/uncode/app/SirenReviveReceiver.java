package com.uncode.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * SirenReviveReceiver
 *
 * Catches the "com.uncode.app.ACTION_REVIVE" broadcast dispatched by SIRENHomeProxy
 * (com.siren.homeproxy) on device boot or watchdog liveness pings.
 *
 * Strictly revives the Headless Enforcement Core.
 * INVARIANT: NEVER calls startActivity(MainActivity) — zero UI intrusion or screen popping.
 */
public class SirenReviveReceiver extends BroadcastReceiver {
    private static final String TAG = "SirenReviveReceiver";
    public static final String ACTION_REVIVE = "com.uncode.app.ACTION_REVIVE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();
        if (!ACTION_REVIVE.equals(action)) return;

        Log.i(TAG, "Received ACTION_REVIVE from SIREN watchdog. Refreshing Headless Core.");

        try {
            EnforcementCoreService.ensureRunning(context);
        } catch (Exception e) {
            Log.e(TAG, "Error ensuring EnforcementCoreService running: " + e.getMessage());
        }
    }
}
