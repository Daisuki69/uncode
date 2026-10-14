package com.uncode.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput

/**
 * QiezkaNotificationHelper:
 * Centralized, unified notification and toast messaging architecture for QIEZKA.
 *
 * Guarantees zero fragmented strings, eliminates duplicate ongoing notifications,
 * unifies consequence alerts under a single standard, and ensures all on-device toasts
 * adhere to standard remediation and restriction patterns.
 */
object QiezkaNotificationHelper {

    // ── Unified Notification Titles ──
    const val TITLE_UPCOMING_LOCKDOWN = "⏰ QIEZKA Upcoming Lockdown"
    const val TITLE_LOCK_TIMER = "⏳ QIEZKA Lock Timer"
    const val TITLE_CONSEQUENCE_ACTIVE = "⚠️ Homework Expired — Consequence Active"

    // ── Ongoing Active Session Formatting ──
    const val BODY_SESSION_PROGRESS = "Distracting apps are restricted."

    @JvmStatic
    fun formatSessionProgressTitle(title: String?): String {
        val clean = title?.trim()
        val name = if (!clean.isNullOrBlank()) clean else "Study Session"
        return "$name is in progress."
    }

    // ── Standard Remediation Reason Identifiers ──
    const val REASON_VIDEO = "Video"
    const val REASON_MINIPLAYER = "Miniplayer"
    const val REASON_SHOPPING = "Shopping section"

    // ── Toast Formatting Helpers ──

    /**
     * Pattern 1: Inline Remediation (Redirecting back to safe content)
     * Output: "⚠️ [Reason]. Returning to safe page."
     */
    @JvmStatic
    fun formatSafePageToast(reason: String): String {
        val clean = reason.trim().removePrefix("⚠️").trim().removeSuffix(".")
        return "⚠️ $clean. Returning to safe page."
    }

    /**
     * Pattern 2a: Settings Restriction
     * Output: "🛡️ Settings is restricted by QIEZKA"
     */
    @JvmStatic
    fun formatSettingsRestrictedToast(): String {
        return "🛡️ Settings is restricted by QIEZKA"
    }

    /**
     * Pattern 2b: Distracting App Restriction
     * Output: "🛡️ [AppName] is restricted by QIEZKA"
     */
    @JvmStatic
    fun formatAppRestrictedToast(appName: String): String {
        val clean = appName.trim().removePrefix("🛡️").trim()
        return "🛡️ $clean is restricted by QIEZKA"
    }

    /**
     * Resolves human-readable application label from package name.
     */
    @JvmStatic
    fun resolveAppLabel(context: Context, pkg: String): String {
        try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(pkg, 0)
            val lbl = pm.getApplicationLabel(ai)
            if (!lbl.isNullOrBlank()) {
                return lbl.toString()
            }
        } catch (ignore: Exception) {}
        return pkg
    }

    // ── Throttled Toast Dispatcher ──
    @Volatile
    private var lastToastText: String? = null
    @Volatile
    private var lastToastTime: Long = 0L
    private const val TOAST_COOLDOWN_MS = 2500L

    /**
     * Dispatches Toast on the main UI thread with debounce protection
     * to prevent spamming from rapid accessibility events or taps.
     */
    @JvmStatic
    fun showToastThrottled(context: Context, message: String) {
        val now = System.currentTimeMillis()
        if (message == lastToastText && now - lastToastTime < TOAST_COOLDOWN_MS) {
            return
        }
        lastToastText = message
        lastToastTime = now
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
            } catch (ignore: Exception) {}
        }
    }

    // ── Interactive Resource Query Notification Architecture ──
    const val CHANNEL_ID_RESOURCE_QUERY = "qiezka_resource_query"
    const val CHANNEL_NAME_RESOURCE_QUERY = "Resource Query"
    const val NOTIF_ID_RESOURCE_QUERY = 1006
    const val BASE_NOTIF_ID_AUX = 1100
    const val KEY_TEXT_REPLY = "key_resource_query_reply"
    const val ACTION_RESOURCE_QUERY = "com.uncode.app.ACTION_RESOURCE_QUERY"
    const val ACTION_REVIVE_RESOURCE_QUERY = "com.uncode.app.ACTION_REVIVE_RESOURCE_QUERY"

    @JvmStatic
    fun formatNotFoundText(query: String): String {
        return "$query does not exist"
    }

    @JvmStatic
    fun createResourceQueryChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_RESOURCE_QUERY,
                CHANNEL_NAME_RESOURCE_QUERY,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Interactive inline-reply study resource queries"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    @JvmStatic
    fun buildResourceQueryNotification(
        context: Context,
        title: String?,
        text: String
    ): Notification {
        createResourceQueryChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        var pFlags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pFlags = pFlags or PendingIntent.FLAG_IMMUTABLE
        }
        val contentIntent = PendingIntent.getActivity(context, NOTIF_ID_RESOURCE_QUERY, launchIntent, pFlags)

        // RemoteInput action for direct inline reply without placeholder
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .build()

        val replyIntent = Intent(context, ResourceQueryReceiver::class.java).apply {
            action = ACTION_RESOURCE_QUERY
        }
        var replyPFlags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            replyPFlags = replyPFlags or PendingIntent.FLAG_MUTABLE
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIF_ID_RESOURCE_QUERY,
            replyIntent,
            replyPFlags
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_qiezka_notif,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        // DeleteIntent for automatic revival when swiped away or dismissed by user
        val deleteIntent = Intent(context, ResourceQueryReceiver::class.java).apply {
            action = ACTION_REVIVE_RESOURCE_QUERY
        }
        var deletePFlags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            deletePFlags = deletePFlags or PendingIntent.FLAG_IMMUTABLE
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIF_ID_RESOURCE_QUERY,
            deleteIntent,
            deletePFlags
        )

        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(text)
        if (!title.isNullOrBlank()) {
            bigTextStyle.setBigContentTitle(title)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_RESOURCE_QUERY)
            .setSmallIcon(R.drawable.ic_qiezka_notif)
            .setContentText(text)
            .setStyle(bigTextStyle)
            .setOngoing(false) // Normal swipeable notification like other notifications
            .setContentIntent(contentIntent)
            .setDeleteIntent(deletePendingIntent)
            .addAction(replyAction)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (!title.isNullOrBlank()) {
            builder.setContentTitle(title)
        }

        return builder.build()
    }

    @JvmStatic
    fun buildAuxiliaryNotification(
        context: Context,
        title: String?,
        text: String,
        notifId: Int
    ): Notification {
        createResourceQueryChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        var pFlags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pFlags = pFlags or PendingIntent.FLAG_IMMUTABLE
        }
        val contentIntent = PendingIntent.getActivity(context, notifId, launchIntent, pFlags)

        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(text)
        if (!title.isNullOrBlank()) {
            bigTextStyle.setBigContentTitle(title)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_RESOURCE_QUERY)
            .setSmallIcon(R.drawable.ic_qiezka_notif)
            .setContentText(text)
            .setStyle(bigTextStyle)
            .setOngoing(false) // Swipeable auxiliary card
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (!title.isNullOrBlank()) {
            builder.setContentTitle(title)
        }

        return builder.build()
    }

    @JvmStatic
    fun postAlwaysActiveResourceQueryNotification(context: Context) {
        try {
            val notif = buildResourceQueryNotification(
                context,
                null,
                ""
            )
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(NOTIF_ID_RESOURCE_QUERY, notif)
        } catch (ignore: Exception) {}
    }
}
