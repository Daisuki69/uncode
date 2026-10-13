package com.uncode.app

import android.animation.ValueAnimator
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject

class FloatingOverlayService : Service() {

    companion object {
        private const val TAG = "FloatingOverlayService"
        const val PREFS_NAME = "uncode_lock"

        const val ACTION_START = "com.uncode.app.ACTION_START_OVERLAY"
        const val ACTION_STOP = "com.uncode.app.ACTION_STOP_OVERLAY"
        const val EXTRA_LOCK_END_TIME = "lock_end_time"
        const val EXTRA_TITLE = "title"

        const val NOTIF_ID_OVERLAY = 1004

        @JvmStatic
        fun startService(context: Context, lockEndTime: Long) {
            startService(context, lockEndTime, "Study Session")
        }

        @JvmStatic
        fun startService(context: Context, lockEndTime: Long, title: String?) {
            try {
                if (!Settings.canDrawOverlays(context)) {
                    Log.w(TAG, "Cannot start FloatingOverlayService: SYSTEM_ALERT_WINDOW not granted")
                    return
                }
                val intent = Intent(context, FloatingOverlayService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_LOCK_END_TIME, lockEndTime)
                    putExtra(EXTRA_TITLE, title ?: "Study Session")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start FloatingOverlayService: ${e.message}", e)
            }
        }

        @JvmStatic
        fun stopService(context: Context) {
            try {
                val intent = Intent(context, FloatingOverlayService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop FloatingOverlayService: ${e.message}")
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var tvTimer: TextView? = null

    private var lockEndTime: Long = 0L
    private var sessionTitle: String = "Study Session"
    private val tickerHandler = Handler(Looper.getMainLooper())
    private var isViewAdded = false

    private val tickerRunnable = object : Runnable {
        override fun run() {
            updateTimerDisplay()
            // High-precision 250ms polling against wall-clock eliminates delay-skipping,
            // timer stutter, and live freezes while guaranteeing sub-frame synchronization.
            tickerHandler.postDelayed(this, 250L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && ACTION_STOP == intent.action) {
            removeFloatingView()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent != null) {
            val passedEnd = intent.getLongExtra(EXTRA_LOCK_END_TIME, 0L)
            if (passedEnd > 0) {
                lockEndTime = passedEnd
            }
            val title = intent.getStringExtra(EXTRA_TITLE)
            if (!title.isNullOrBlank()) {
                sessionTitle = title
            }
        }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedEnd = prefs.getLong("lock_end_time", 0L)
        if (storedEnd > 0) {
            lockEndTime = storedEnd
        }

        val timeOffset = prefs.getLong("time_offset", 0L)
        val effectiveNow = System.currentTimeMillis() + timeOffset
        val maxDurationMinutes = getActiveScheduleDurationMinutes(prefs)
        val maxRemainingSec = maxDurationMinutes * 60L
        if (lockEndTime > effectiveNow + (maxRemainingSec * 1000L)) {
            lockEndTime = effectiveNow + (maxRemainingSec * 1000L)
            prefs.edit().putLong("lock_end_time", lockEndTime).apply()
            Log.i(TAG, "onStartCommand: Clamped lockEndTime to duration ${maxDurationMinutes}m")
        }

        // Promote to Foreground Service
        startAsForegroundService()

        // Create and show the floating ball overlay
        createFloatingView()

        return START_STICKY
    }

    private fun startAsForegroundService() {
        try {
            AlarmReceiver.createNotificationChannels(this)

            var launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent == null) {
                launchIntent = Intent().setClassName(packageName, "com.uncode.app.MainActivity")
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

            var pFlags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pFlags = pFlags or PendingIntent.FLAG_IMMUTABLE
            }

            val contentIntent = PendingIntent.getActivity(this, NOTIF_ID_OVERLAY, launchIntent, pFlags)

            val notification = NotificationCompat.Builder(this, AlarmReceiver.CHANNEL_ID_STATUS)
                .setSmallIcon(R.drawable.ic_qiezka_notif)
                .setContentTitle("🔒 QIEZKA Lockdown Active")
                .setContentText("$sessionTitle is in progress. Floating timer enabled.")
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()

            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID_OVERLAY, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIF_ID_OVERLAY, notification)
            }
            Log.i(TAG, "FloatingOverlayService running in foreground")
        } catch (e: Exception) {
            Log.e(TAG, "startAsForegroundService error: ${e.message}", e)
        }
    }

    private fun createFloatingView() {
        if (isViewAdded) {
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "createFloatingView aborted: cannot draw overlays")
            return
        }

        try {
            windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
            val wm = windowManager ?: return

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 40
                y = 260
            }
            layoutParams = params

            val inflater = LayoutInflater.from(this)
            val view = inflater.inflate(R.layout.floating_bubble_layout, null)
            floatingView = view
            tvTimer = view.findViewById(R.id.tv_floating_timer)

            setupTouchListener()

            // Populate initial timer text before adding to screen so it never displays stale or placeholder text
            updateTimerDisplay()

            wm.addView(view, params)
            isViewAdded = true

            tickerHandler.removeCallbacks(tickerRunnable)
            tickerHandler.post(tickerRunnable)

            Log.i(TAG, "Floating timer ball successfully added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating view to WindowManager: ${e.message}", e)
        }
    }

    private fun setupTouchListener() {
        val view = floatingView ?: return

        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private var touchStartTime: Long = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = layoutParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        touchStartTime = System.currentTimeMillis()
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        if (isViewAdded && windowManager != null) {
                            windowManager?.updateViewLayout(floatingView, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val duration = System.currentTimeMillis() - touchStartTime
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)

                        if (duration < 250 && diffX < 15 && diffY < 15) {
                            // Tap event: Bring Qiezka to foreground
                            launchQiezka()
                        } else {
                            // Drag released: snap to nearest edge
                            snapToNearestEdge()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun snapToNearestEdge() {
        val view = floatingView ?: return
        val wm = windowManager ?: return
        val params = layoutParams ?: return
        if (!isViewAdded) return

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(metrics)
        val screenWidth = metrics.widthPixels

        val currentX = params.x
        val targetX = if (currentX + (view.width / 2) < screenWidth / 2) 20 else (screenWidth - view.width - 20)

        val animator = ValueAnimator.ofInt(currentX, targetX)
        animator.duration = 200
        animator.addUpdateListener { animation ->
            if (isViewAdded && windowManager != null && floatingView != null) {
                params.x = animation.animatedValue as Int
                windowManager?.updateViewLayout(floatingView, params)
            }
        }
        animator.start()
    }

    private fun launchQiezka() {
        try {
            val launch = packageManager.getLaunchIntentForPackage(packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val isConsequence = prefs?.getBoolean("consequence_active", false) ?: false
                launch.putExtra("route", if (isConsequence) "logs" else "locked")
                startActivity(launch)
            }
        } catch (e: Exception) {
            Log.e(TAG, "launchQiezka from floating ball failed: ${e.message}")
        }
    }

    private fun updateTimerDisplay() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLockdownActive = prefs.getBoolean("lockdown_active", false)
        val isConsequenceActive = prefs.getBoolean("consequence_active", false)

        val timeOffset = prefs.getLong("time_offset", 0L)
        val effectiveNow = System.currentTimeMillis() + timeOffset
        val inOperatingHours = LockAccessibilityService.isInOperatingHours(this, effectiveNow)

        // If neither lockdown nor consequence is active, cleanly dismiss immediately
        if (!isLockdownActive && !isConsequenceActive) {
            Log.i(TAG, "FloatingOverlayService: neither lockdown nor consequence active, cleanly dismissing")
            removeFloatingView()
            stopForeground(true)
            stopSelf()
            return
        }

        // If in Consequence Mode:
        if (isConsequenceActive) {
            if (!inOperatingHours) {
                // Outside operating hours (3:00 AM to 7:00 PM): QIEZKA does not operate.
                if (floatingView?.visibility == View.VISIBLE) {
                    floatingView?.visibility = View.GONE
                }
                return
            }
            // Inside operating hours (7:00 PM to 3:00 AM):
            if (floatingView?.visibility != View.VISIBLE) {
                floatingView?.visibility = View.VISIBLE
            }
            tvTimer?.text = "⚠️ RETRY"
            return
        }

        val storedEnd = prefs.getLong("lock_end_time", 0L)
        lockEndTime = if (storedEnd > 0) storedEnd else 0L

        // When timer expires without passing, transition to Consequence Mode!
        if (lockEndTime > 0 && effectiveNow >= lockEndTime) {
            prefs.edit()
                .putBoolean("consequence_active", true)
                .remove("lock_end_time")
                .apply()
            tvTimer?.text = "⚠️ RETRY"
            return
        }

        val maxDurationMinutes = getActiveScheduleDurationMinutes(prefs)
        val maxRemainingSec = maxDurationMinutes * 60L

        var remainingSec = if (lockEndTime > effectiveNow) (lockEndTime - effectiveNow) / 1000L else 0L
        if (remainingSec > maxRemainingSec) {
            Log.w(TAG, "Remaining time (${remainingSec}s) exceeds schedule duration (${maxRemainingSec}s). Clamping.")
            remainingSec = maxRemainingSec
            lockEndTime = effectiveNow + (maxRemainingSec * 1000L)
            prefs.edit().putLong("lock_end_time", lockEndTime).apply()
        }

        val hours = remainingSec / 3600
        val minutes = (remainingSec % 3600) / 60
        val seconds = remainingSec % 60

        val formattedTime = if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

        val current = tvTimer?.text
        if (current == null || !formattedTime.contentEquals(current)) {
            tvTimer?.text = formattedTime
        }
    }

    private fun onLockSessionFinished(prefs: SharedPreferences) {
        Log.i(TAG, "FloatingOverlayService: Lockdown completed!")
        prefs.edit()
            .putBoolean("lockdown_active", false)
            .remove("lock_end_time")
            .remove("active_schedule_id")
            .apply()

        AlarmReceiver.cancelLockEndAlarm(this)
        AlarmReceiver.createNotificationChannels(this)
        @Suppress("DEPRECATION")
        AlarmReceiver.showNotificationStatic(
            this,
            AlarmReceiver.NOTIF_ID_COMPLETED,
            AlarmReceiver.CHANNEL_ID_ALERTS,
            "🎉 Study Session Complete!",
            "Great work! Your lock timer expired and all apps are now unlocked.",
            Notification.PRIORITY_HIGH,
            false
        )

        removeFloatingView()
        stopForeground(true)
        stopSelf()
    }

    private fun getActiveScheduleDurationMinutes(prefs: SharedPreferences): Int {
        val activeSchedId = prefs.getString("active_schedule_id", null)
        val schedulesJson = prefs.getString("schedules_json", "[]")
        if (!activeSchedId.isNullOrBlank()) {
            try {
                val arr = JSONArray(schedulesJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null && activeSchedId == obj.optString("id")) {
                        val dur = obj.optInt("durationMinutes", 0)
                        if (dur > 0) {
                            return Math.min(90, dur)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing schedules_json for duration: ${e.message}")
            }
        }
        return 90
    }

    private fun removeFloatingView() {
        tickerHandler.removeCallbacks(tickerRunnable)
        if (isViewAdded && windowManager != null && floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (ignore: Exception) {}
            isViewAdded = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingView()
        Log.i(TAG, "FloatingOverlayService destroyed")
    }
}
