package com.uncode.app;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONObject;

public class FloatingOverlayService extends Service {

    private static final String TAG = "FloatingOverlayService";
    public static final String PREFS_NAME = "uncode_lock";

    public static final String ACTION_START = "com.uncode.app.ACTION_START_OVERLAY";
    public static final String ACTION_STOP = "com.uncode.app.ACTION_STOP_OVERLAY";
    public static final String EXTRA_LOCK_END_TIME = "lock_end_time";
    public static final String EXTRA_TITLE = "title";

    public static final int NOTIF_ID_OVERLAY = 1004;

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams layoutParams;
    private TextView tvTimer;

    private long lockEndTime = 0L;
    private String sessionTitle = "Study Session";
    private final Handler tickerHandler = new Handler(Looper.getMainLooper());
    private boolean isViewAdded = false;

    private final Runnable tickerRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimerDisplay();
            tickerHandler.postDelayed(this, 1000L);
        }
    };

    public static void startService(Context context, long lockEndTime, String title) {
        try {
            if (!Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Cannot start FloatingOverlayService: SYSTEM_ALERT_WINDOW not granted");
                return;
            }
            Intent intent = new Intent(context, FloatingOverlayService.class);
            intent.setAction(ACTION_START);
            intent.putExtra(EXTRA_LOCK_END_TIME, lockEndTime);
            intent.putExtra(EXTRA_TITLE, title != null ? title : "Study Session");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start FloatingOverlayService: " + e.getMessage(), e);
        }
    }

    public static void stopService(Context context) {
        try {
            Intent intent = new Intent(context, FloatingOverlayService.class);
            intent.setAction(ACTION_STOP);
            context.startService(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop FloatingOverlayService: " + e.getMessage());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            removeFloatingView();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            long passedEnd = intent.getLongExtra(EXTRA_LOCK_END_TIME, 0L);
            if (passedEnd > 0) {
                lockEndTime = passedEnd;
            }
            String title = intent.getStringExtra(EXTRA_TITLE);
            if (title != null && !title.trim().isEmpty()) {
                sessionTitle = title;
            }
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long storedEnd = prefs.getLong("lock_end_time", 0L);
        if (storedEnd > 0) {
            lockEndTime = storedEnd;
        }

        long timeOffset = prefs.getLong("time_offset", 0L);
        long effectiveNow = System.currentTimeMillis() + timeOffset;
        int maxDurationMinutes = getActiveScheduleDurationMinutes(prefs);
        long maxRemainingSec = maxDurationMinutes * 60L;
        if (lockEndTime > effectiveNow + (maxRemainingSec * 1000L)) {
            lockEndTime = effectiveNow + (maxRemainingSec * 1000L);
            prefs.edit().putLong("lock_end_time", lockEndTime).apply();
            Log.i(TAG, "onStartCommand: Clamped lockEndTime to duration " + maxDurationMinutes + "m");
        }

        // Promote to Foreground Service
        startAsForegroundService();

        // Create and show the floating ball overlay
        createFloatingView();

        return START_STICKY;
    }

    private void startAsForegroundService() {
        try {
            AlarmReceiver.createNotificationChannels(this);

            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launchIntent == null) {
                launchIntent = new Intent().setClassName(getPackageName(), "com.uncode.app.MainActivity");
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            int pFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pFlags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent contentIntent = PendingIntent.getActivity(this, NOTIF_ID_OVERLAY, launchIntent, pFlags);

            Notification notification = new NotificationCompat.Builder(this, AlarmReceiver.CHANNEL_ID_STATUS)
                    .setSmallIcon(R.drawable.ic_qiezka_notif)
                    .setContentTitle("🔒 QIEZKA Lockdown Active")
                    .setContentText(sessionTitle + " is in progress. Floating timer enabled.")
                    .setOngoing(true)
                    .setContentIntent(contentIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .build();

            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID_OVERLAY, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIF_ID_OVERLAY, notification);
            }
            Log.i(TAG, "FloatingOverlayService running in foreground");
        } catch (Exception e) {
            Log.e(TAG, "startAsForegroundService error: " + e.getMessage(), e);
        }
    }

    private void createFloatingView() {
        if (isViewAdded) {
            return;
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "createFloatingView aborted: cannot draw overlays");
            return;
        }

        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager == null) return;

            int layoutType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutType = WindowManager.LayoutParams.TYPE_PHONE;
            }

            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );

            layoutParams.gravity = Gravity.TOP | Gravity.START;
            layoutParams.x = 40;
            layoutParams.y = 260;

            LayoutInflater inflater = LayoutInflater.from(this);
            floatingView = inflater.inflate(R.layout.floating_bubble_layout, null);
            tvTimer = floatingView.findViewById(R.id.tv_floating_timer);

            setupTouchListener();

            // Populate initial timer text before adding to screen so it never displays stale or placeholder text
            updateTimerDisplay();

            windowManager.addView(floatingView, layoutParams);
            isViewAdded = true;

            tickerHandler.removeCallbacks(tickerRunnable);
            tickerHandler.post(tickerRunnable);

            Log.i(TAG, "Floating timer ball successfully added to WindowManager");
        } catch (Exception e) {
            Log.e(TAG, "Failed to add floating view to WindowManager: " + e.getMessage(), e);
        }
    }

    private void setupTouchListener() {
        if (floatingView == null) return;

        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private long touchStartTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchStartTime = System.currentTimeMillis();
                        initialX = layoutParams.x;
                        initialY = layoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        layoutParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        layoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        if (isViewAdded && windowManager != null) {
                            windowManager.updateViewLayout(floatingView, layoutParams);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        long duration = System.currentTimeMillis() - touchStartTime;
                        float diffX = Math.abs(event.getRawX() - initialTouchX);
                        float diffY = Math.abs(event.getRawY() - initialTouchY);

                        if (duration < 250 && diffX < 15 && diffY < 15) {
                            // Tap event: Bring Qiezka to foreground
                            launchQiezka();
                        } else {
                            // Drag released: snap to nearest edge
                            snapToNearestEdge();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void snapToNearestEdge() {
        if (!isViewAdded || windowManager == null || floatingView == null) return;

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;

        int currentX = layoutParams.x;
        int targetX = (currentX + (floatingView.getWidth() / 2) < screenWidth / 2) ? 20 : (screenWidth - floatingView.getWidth() - 20);

        ValueAnimator animator = ValueAnimator.ofInt(currentX, targetX);
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            if (isViewAdded && windowManager != null && floatingView != null) {
                layoutParams.x = (int) animation.getAnimatedValue();
                windowManager.updateViewLayout(floatingView, layoutParams);
            }
        });
        animator.start();
    }

    private void launchQiezka() {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                launch.putExtra("route", "locked");
                startActivity(launch);
            }
        } catch (Exception e) {
            Log.e(TAG, "launchQiezka from floating ball failed: " + e.getMessage());
        }
    }

    private void updateTimerDisplay() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isLockdownActive = prefs.getBoolean("lockdown_active", false);

        long storedEnd = prefs.getLong("lock_end_time", 0L);
        if (storedEnd > 0) {
            lockEndTime = storedEnd;
        } else {
            lockEndTime = 0L;
        }

        long timeOffset = prefs.getLong("time_offset", 0L);
        long effectiveNow = System.currentTimeMillis() + timeOffset;

        // If lockdown was cancelled or released externally, cleanly dismiss immediately
        if (!isLockdownActive) {
            Log.i(TAG, "FloatingOverlayService: lockdown_active is false, cleanly dismissing floating view and stopping service");
            removeFloatingView();
            stopForeground(true);
            stopSelf();
            return;
        }

        // Only trigger session completion if lockdown was legitimately active and the clock has passed the end
        if (lockEndTime > 0 && effectiveNow >= lockEndTime) {
            onLockSessionFinished(prefs);
            return;
        }

        int maxDurationMinutes = getActiveScheduleDurationMinutes(prefs);
        long maxRemainingSec = maxDurationMinutes * 60L;

        long remainingSec = lockEndTime > effectiveNow ? (lockEndTime - effectiveNow) / 1000L : 0L;
        if (remainingSec > maxRemainingSec) {
            Log.w(TAG, "Remaining time (" + remainingSec + "s) exceeds schedule duration (" + maxRemainingSec + "s). Clamping.");
            remainingSec = maxRemainingSec;
            lockEndTime = effectiveNow + (maxRemainingSec * 1000L);
            prefs.edit().putLong("lock_end_time", lockEndTime).apply();
        }

        long hours = remainingSec / 3600;
        long minutes = (remainingSec % 3600) / 60;
        long seconds = remainingSec % 60;

        String formattedTime;
        if (hours > 0) {
            formattedTime = String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            formattedTime = String.format("%02d:%02d", minutes, seconds);
        }

        if (tvTimer != null) {
            CharSequence current = tvTimer.getText();
            if (current == null || !formattedTime.contentEquals(current)) {
                tvTimer.setText(formattedTime);
            }
        }
    }

    private void onLockSessionFinished(SharedPreferences prefs) {
        Log.i(TAG, "FloatingOverlayService: Lockdown completed!");
        prefs.edit()
                .putBoolean("lockdown_active", false)
                .remove("lock_end_time")
                .remove("active_schedule_id")
                .apply();

        AlarmReceiver.cancelLockEndAlarm(this);
        AlarmReceiver.createNotificationChannels(this);
        AlarmReceiver.showNotificationStatic(
                this,
                AlarmReceiver.NOTIF_ID_COMPLETED,
                AlarmReceiver.CHANNEL_ID_ALERTS,
                "🎉 Study Session Complete!",
                "Great work! Your lock timer expired and all apps are now unlocked.",
                Notification.PRIORITY_HIGH,
                false
        );

        removeFloatingView();
        stopForeground(true);
        stopSelf();
    }

    private int getActiveScheduleDurationMinutes(SharedPreferences prefs) {
        String activeSchedId = prefs.getString("active_schedule_id", null);
        String schedulesJson = prefs.getString("schedules_json", "[]");
        if (activeSchedId != null && !activeSchedId.trim().isEmpty()) {
            try {
                JSONArray arr = new JSONArray(schedulesJson);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj != null && activeSchedId.equals(obj.optString("id"))) {
                        int dur = obj.optInt("durationMinutes", 0);
                        if (dur > 0) {
                            return Math.min(90, dur);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parsing schedules_json for duration: " + e.getMessage());
            }
        }
        return 90;
    }

    private void removeFloatingView() {
        tickerHandler.removeCallbacks(tickerRunnable);
        if (isViewAdded && windowManager != null && floatingView != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception ignore) {}
            isViewAdded = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeFloatingView();
        Log.i(TAG, "FloatingOverlayService destroyed");
    }
}
