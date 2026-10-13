package com.uncode.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * EnforcementCoreService
 *
 * Headless background keeper for QIEZKA's focus enforcement engine.
 * Ensures the native services (Accessibility ticker, DNS VPN sinkhole, and timers)
 * remain operational in the background without launching any Activity or touching the UI.
 */
public class EnforcementCoreService extends Service {
    private static final String TAG = "EnforcementCore";

    public static void ensureRunning(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent(context, EnforcementCoreService.class);
            context.startService(intent);
        } catch (Exception e) {
            Log.w(TAG, "Failed to start EnforcementCoreService: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "EnforcementCoreService pulse received.");

        // Check active Accessibility Service status
        LockAccessibilityService las = LockAccessibilityService.getInstance();
        if (las != null) {
            Log.d(TAG, "LockAccessibilityService is active and operational.");
        } else {
            Log.w(TAG, "LockAccessibilityService is not connected. Awaiting user/system bind.");
        }

        // Verify FloatingOverlayService state if lockdown is active
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("uncode_lock", Context.MODE_PRIVATE);
            boolean isLockdown = prefs != null && prefs.getBoolean("lockdown_active", false);
            long lockEndTime = prefs != null ? prefs.getLong("lock_end_time", 0L) : 0L;
            long timeOffset = prefs != null ? prefs.getLong("time_offset", 0L) : 0L;
            long effectiveNow = System.currentTimeMillis() + timeOffset;

            if (isLockdown && lockEndTime > effectiveNow) {
                FloatingOverlayService.startService(this, lockEndTime, "Study Session");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking overlay service status: " + e.getMessage());
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
