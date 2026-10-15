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
    const val NOTIF_TITLE_DEFAULT = "Your schedule with Qiezka says"
    const val TITLE_UPCOMING_LOCKDOWN = "Your schedule with Qiezka says: Upcoming Lockdown"
    const val TITLE_LOCK_TIMER = "Your schedule with Qiezka says: Lock Timer"
    const val TITLE_CONSEQUENCE_ACTIVE = "Your schedule with Qiezka says: Homework Expired — Consequence Active"

    // ── Ongoing Active Session Formatting ──
    const val BODY_SESSION_PROGRESS = "Distracting apps are restricted."

    @JvmStatic
    fun formatSessionProgressTitle(title: String?): String {
        val clean = title?.trim()
        val name = if (!clean.isNullOrBlank()) clean else "Study Session"
        return "Your schedule with Qiezka says: $name is in progress."
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
    const val ACTION_CARD_NEXT = "com.uncode.app.ACTION_CARD_NEXT"
    const val ACTION_CARD_PREV = "com.uncode.app.ACTION_CARD_PREV"

    data class StudyCard(
        val resourceTitle: String,
        val term: String,
        val definition: String
    )

    data class StudyDeck(
        val title: String,
        val cards: List<StudyCard>
    )

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

        // Note: contentIntent is deliberately omitted so tapping the notification body
        // is completely inert and cannot open QIEZKA.

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

        val displayTitle = if (title.isNullOrBlank()) NOTIF_TITLE_DEFAULT else title
        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(text)
        if (!displayTitle.isNullOrBlank()) {
            bigTextStyle.setBigContentTitle(displayTitle)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_RESOURCE_QUERY)
            .setSmallIcon(R.drawable.ic_qiezka_notif)
            .setContentTitle(displayTitle)
            .setContentText(text)
            .setStyle(bigTextStyle)
            .setOngoing(false) // Normal swipeable notification like other notifications
            .setDeleteIntent(deletePendingIntent)
            .addAction(replyAction)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        return builder.build()
    }

    @JvmStatic
    fun buildAuxiliaryNotification(
        context: Context,
        title: String?,
        text: String
    ): Notification {
        createResourceQueryChannel(context)

        // Note: contentIntent is deliberately omitted so tapping the auxiliary card
        // is completely inert and cannot open QIEZKA.

        val displayTitle = if (title.isNullOrBlank()) NOTIF_TITLE_DEFAULT else title
        val (collapsedText, expandedText) = if (text.contains("|")) {
            val parts = text.split("|", limit = 2)
            val term = parts[0].trim()
            val def = if (parts.size > 1) parts[1].trim() else ""
            Pair("📌 $term", if (def.isNotBlank()) "📌 $term\n\n$def" else "📌 $term")
        } else {
            Pair(text.lines().firstOrNull()?.trim() ?: text, text)
        }

        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(expandedText)
        if (!displayTitle.isNullOrBlank()) {
            bigTextStyle.setBigContentTitle(displayTitle)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_RESOURCE_QUERY)
            .setSmallIcon(R.drawable.ic_qiezka_notif)
            .setContentTitle(displayTitle)
            .setContentText(collapsedText)
            .setStyle(bigTextStyle)
            .setOngoing(false) // Swipeable auxiliary card
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        return builder.build()
    }

    @JvmStatic
    fun buildStudyCardNotification(
        context: Context,
        deckTitle: String?,
        card: StudyCard?,
        cardIndexInDeck: Int,
        totalCardsInDeck: Int,
        isUnlocked: Boolean = true
    ): Notification {
        createResourceQueryChannel(context)

        val prevIntent = Intent(context, ResourceQueryReceiver::class.java).apply {
            action = ACTION_CARD_PREV
        }
        var prevPFlags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            prevPFlags = prevPFlags or PendingIntent.FLAG_IMMUTABLE
        }
        val prevPendingIntent = PendingIntent.getBroadcast(
            context,
            1007,
            prevIntent,
            prevPFlags
        )
        val prevAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_previous,
            "◀ Prev",
            prevPendingIntent
        ).build()

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).build()
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

        val nextIntent = Intent(context, ResourceQueryReceiver::class.java).apply {
            action = ACTION_CARD_NEXT
        }
        var nextPFlags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            nextPFlags = nextPFlags or PendingIntent.FLAG_IMMUTABLE
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            1008,
            nextIntent,
            nextPFlags
        )
        val nextAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_next,
            "Next ▶",
            nextPendingIntent
        ).build()

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

        val title = if (isUnlocked && card != null && totalCardsInDeck > 0) {
            val dTitle = if (!deckTitle.isNullOrBlank()) deckTitle else card.resourceTitle
            if (dTitle.isNotBlank()) {
                "Your schedule with Qiezka says • $dTitle (${cardIndexInDeck + 1}/$totalCardsInDeck)"
            } else {
                "Your schedule with Qiezka says (${cardIndexInDeck + 1}/$totalCardsInDeck)"
            }
        } else {
            NOTIF_TITLE_DEFAULT
        }

        // Single-line compact summary for collapsed view
        val collapsedText = if (isUnlocked && card != null && totalCardsInDeck > 0) {
            "📌 ${card.term}"
        } else {
            ""
        }

        // Full multi-line details displayed when user expands the card
        val expandedText = if (isUnlocked && card != null && totalCardsInDeck > 0) {
            if (card.definition.isNotBlank()) {
                "📌 ${card.term}\n\n${card.definition}"
            } else {
                "📌 ${card.term}"
            }
        } else {
            ""
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(expandedText)
            .setBigContentTitle(title)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_RESOURCE_QUERY)
            .setSmallIcon(R.drawable.ic_qiezka_notif)
            .setContentTitle(title)
            .setContentText(collapsedText)
            .setStyle(bigTextStyle)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setDeleteIntent(deletePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(prevAction)
            .addAction(replyAction)
            .addAction(nextAction)

        return builder.build()
    }

    @JvmStatic
    fun postAlwaysActiveResourceQueryNotification(context: Context) {
        try {
            val prefs = context.getSharedPreferences("uncode_study_cards", Context.MODE_PRIVATE)
            val isUnlocked = prefs.getBoolean("is_unlocked", false)
            val savedFlatIndex = prefs.getInt("card_index", 0)

            val notif = if (isUnlocked) {
                val decks = ResourceQueryReceiver.getAllStudyDecks(context)
                val resolved = ResourceQueryReceiver.resolveDeckAndCard(decks, savedFlatIndex)
                if (resolved != null) {
                    buildStudyCardNotification(
                        context,
                        resolved.deck.title,
                        resolved.card,
                        resolved.cardIndexInDeck,
                        resolved.deck.cards.size,
                        isUnlocked = true
                    )
                } else {
                    buildStudyCardNotification(
                        context,
                        null,
                        null,
                        0,
                        0,
                        isUnlocked = false
                    )
                }
            } else {
                buildStudyCardNotification(
                    context,
                    null,
                    null,
                    0,
                    0,
                    isUnlocked = false
                )
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(NOTIF_ID_RESOURCE_QUERY, notif)
        } catch (ignore: Exception) {}
    }
}
