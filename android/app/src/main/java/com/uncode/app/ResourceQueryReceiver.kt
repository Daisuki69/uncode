package com.uncode.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import org.json.JSONArray
import java.io.File

/**
 * ResourceQueryReceiver:
 * Broadcast receiver processing inline direct-reply (RemoteInput) resource searches
 * from the always-active persistent notification shade card.
 *
 * Scans saved resources in studom_resources line-by-line using QIEZKA's Master Syntax:
 * [Term] | [Definition]
 *
 * Behavior:
 * 1. Instantly collapses ONLY the Main Notification (ID = 1006) upon receiving a query.
 *    Any previously dispatched auxiliary notifications RETAIN in the shade.
 * 2. Scans across all saved resources for any line containing the query (case-insensitive).
 * 3. Notification dispatch:
 *    - Match 0: Dispatched to Main Notification (ID = 1006) with RemoteInput "Reply" action attached.
 *    - Matches 1..N: Dispatched as Auxiliary Notifications (IDs = 1100, 1101, ...) without Reply actions.
 * 4. If no match found: Main Notification (ID = 1006) displays "<query> does not exist" without title.
 */
class ResourceQueryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        // 1. Automatic Revival on Swipe / Dismiss
        if (intent.action == QiezkaNotificationHelper.ACTION_REVIVE_RESOURCE_QUERY) {
            QiezkaNotificationHelper.postAlwaysActiveResourceQueryNotification(context)
            return
        }

        // 2. Navigation Actions: NEXT and PREV (Covert sequence detector when locked, in-place paging when unlocked)
        if (intent.action == QiezkaNotificationHelper.ACTION_CARD_NEXT || intent.action == QiezkaNotificationHelper.ACTION_CARD_PREV) {
            val isNext = (intent.action == QiezkaNotificationHelper.ACTION_CARD_NEXT)
            val actionTag = if (isNext) "NEXT" else "PREV"

            val prefs = context.getSharedPreferences("uncode_study_cards", Context.MODE_PRIVATE)
            val isUnlocked = prefs.getBoolean("is_unlocked", false)

            if (!isUnlocked) {
                // Card is locked under disguise: feed sequence detector with strict 2.0s overall timer from 1st tap
                val now = System.currentTimeMillis()
                synchronized(sequenceBuffer) {
                    if (sequenceBuffer.isEmpty() || (now - firstClickTimestamp > SEQUENCE_TOTAL_TIMEOUT_MS)) {
                        sequenceBuffer.clear()
                        firstClickTimestamp = now
                    }

                    sequenceBuffer.add(actionTag)

                    if (sequenceBuffer.size == TARGET_SEQUENCE.size) {
                        val elapsed = now - firstClickTimestamp
                        if (elapsed <= SEQUENCE_TOTAL_TIMEOUT_MS && sequenceBuffer == TARGET_SEQUENCE) {
                            // Secret sequence Prev -> Next -> Prev -> Prev -> Next completed within 2.0s! Unlock!
                            sequenceBuffer.clear()
                            firstClickTimestamp = 0L
                            prefs.edit().putBoolean("is_unlocked", true).apply()
                            QiezkaNotificationHelper.postAlwaysActiveResourceQueryNotification(context)
                            restorePastQueryIfExists(context)
                        } else {
                            // Invalid sequence or exceeded 2.0s: clear
                            sequenceBuffer.clear()
                            firstClickTimestamp = 0L
                        }
                    } else if (sequenceBuffer.size > TARGET_SEQUENCE.size) {
                        sequenceBuffer.clear()
                        firstClickTimestamp = 0L
                    }
                }
                return
            }

            // Already unlocked: page through study cards in-place
            handleCardPaging(context, delta = if (isNext) 1 else -1)
            return
        }

        if (intent.action != QiezkaNotificationHelper.ACTION_RESOURCE_QUERY) return

        val bundle = RemoteInput.getResultsFromIntent(intent) ?: return
        val rawQuery = bundle.getCharSequence(QiezkaNotificationHelper.KEY_TEXT_REPLY)?.toString() ?: ""
        val query = rawQuery.trim()

        val prefs = context.getSharedPreferences("uncode_study_cards", Context.MODE_PRIVATE)

        if (query.isEmpty()) {
            prefs.edit().putString("last_query", "").apply()
            QiezkaNotificationHelper.postAlwaysActiveResourceQueryNotification(context)
            return
        }

        // Save last_query so it can be restored on future unlocks
        prefs.edit().putString("last_query", query).apply()

        // Non-destructive search: Main card (ID 1006) remains untouched in its flashcard deck!
        dispatchSearchResults(context, query)

        // Clear RemoteInput inline reply spinner on main card (1006) while preserving
        // current deck and card index so user can immediately continue paging Prev/Next.
        QiezkaNotificationHelper.postAlwaysActiveResourceQueryNotification(context)
    }

    private fun handleCardPaging(context: Context, delta: Int) {
        val decks = getAllStudyDecks(context)
        val validDecks = decks.filter { it.cards.isNotEmpty() }
        if (validDecks.isEmpty()) return
        val totalCards = validDecks.sumOf { it.cards.size }
        if (totalCards == 0) return

        val prefs = context.getSharedPreferences("uncode_study_cards", Context.MODE_PRIVATE)
        val currentFlatIndex = prefs.getInt("card_index", 0)
        var newFlatIndex = (currentFlatIndex + delta) % totalCards
        if (newFlatIndex < 0) newFlatIndex += totalCards
        prefs.edit().putInt("card_index", newFlatIndex).apply()

        val resolved = resolveDeckAndCard(validDecks, newFlatIndex) ?: return
        val notif = QiezkaNotificationHelper.buildStudyCardNotification(
            context,
            resolved.deck.title,
            resolved.card,
            resolved.cardIndexInDeck,
            resolved.deck.cards.size,
            isUnlocked = true
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(QiezkaNotificationHelper.NOTIF_ID_RESOURCE_QUERY, notif)
    }

    companion object {
        val TARGET_SEQUENCE = listOf("PREV", "NEXT", "PREV", "PREV", "NEXT")
        private val sequenceBuffer = mutableListOf<String>()
        private var firstClickTimestamp = 0L
        const val SEQUENCE_TOTAL_TIMEOUT_MS = 2000L

        @JvmStatic
        fun dispatchSearchResults(context: Context, query: String) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val matches = searchAllResourceMatches(context, query)
            if (matches.isNotEmpty()) {
                val maxAux = minOf(matches.size, 5)
                for (i in 0 until maxAux) {
                    val match = matches[i]
                    val auxNotifId = QiezkaNotificationHelper.BASE_NOTIF_ID_AUX + i
                    val resultTitle = if (match.first.isNotBlank()) {
                        "Your schedule with Qiezka says • ${match.first}"
                    } else {
                        "Your schedule with Qiezka says • Search: $query"
                    }
                    val auxNotif = QiezkaNotificationHelper.buildAuxiliaryNotification(
                        context,
                        resultTitle,
                        match.second
                    )
                    nm?.notify(auxNotifId, auxNotif)
                }
            } else {
                val notFoundTitle = "Your schedule with Qiezka says • Search: $query"
                val notFoundNotif = QiezkaNotificationHelper.buildAuxiliaryNotification(
                    context,
                    notFoundTitle,
                    QiezkaNotificationHelper.formatNotFoundText(query)
                )
                nm?.notify(QiezkaNotificationHelper.BASE_NOTIF_ID_AUX, notFoundNotif)
            }
        }

        @JvmStatic
        fun restorePastQueryIfExists(context: Context) {
            val prefs = context.getSharedPreferences("uncode_study_cards", Context.MODE_PRIVATE)
            val lastQuery = prefs.getString("last_query", "")?.trim() ?: ""
            if (lastQuery.isNotEmpty()) {
                dispatchSearchResults(context, lastQuery)
            }
        }

        @JvmStatic
        fun onShadeCollapsed(context: Context) {
            val prefs = context.getSharedPreferences("uncode_study_cards", Context.MODE_PRIVATE)
            val wasUnlocked = prefs.getBoolean("is_unlocked", false)
            if (wasUnlocked) {
                prefs.edit().putBoolean("is_unlocked", false).apply()
                synchronized(sequenceBuffer) {
                    sequenceBuffer.clear()
                    firstClickTimestamp = 0L
                }

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                for (i in 0 until 10) {
                    nm?.cancel(QiezkaNotificationHelper.BASE_NOTIF_ID_AUX + i)
                }
                QiezkaNotificationHelper.postAlwaysActiveResourceQueryNotification(context)
            }
        }

        @JvmStatic
        fun getSequenceBufferForTesting(): List<String> {
            return synchronized(sequenceBuffer) { sequenceBuffer.toList() }
        }

        @JvmStatic
        fun feedSequenceForTesting(action: String, now: Long = System.currentTimeMillis()): Boolean {
            return synchronized(sequenceBuffer) {
                if (sequenceBuffer.isEmpty() || (now - firstClickTimestamp > SEQUENCE_TOTAL_TIMEOUT_MS)) {
                    sequenceBuffer.clear()
                    firstClickTimestamp = now
                }
                sequenceBuffer.add(action)
                if (sequenceBuffer.size == TARGET_SEQUENCE.size) {
                    val elapsed = now - firstClickTimestamp
                    if (elapsed <= SEQUENCE_TOTAL_TIMEOUT_MS && sequenceBuffer == TARGET_SEQUENCE) {
                        sequenceBuffer.clear()
                        firstClickTimestamp = 0L
                        true
                    } else {
                        sequenceBuffer.clear()
                        firstClickTimestamp = 0L
                        false
                    }
                } else if (sequenceBuffer.size > TARGET_SEQUENCE.size) {
                    sequenceBuffer.clear()
                    firstClickTimestamp = 0L
                    false
                } else {
                    false
                }
            }
        }

        @JvmStatic
        fun resetSequenceBufferForTesting() {
            synchronized(sequenceBuffer) {
                sequenceBuffer.clear()
                firstClickTimestamp = 0L
            }
        }

        data class ResolvedCardPosition(
            val deck: QiezkaNotificationHelper.StudyDeck,
            val card: QiezkaNotificationHelper.StudyCard,
            val cardIndexInDeck: Int,
            val flatIndex: Int
        )

        @JvmStatic
        fun resolveDeckAndCard(decks: List<QiezkaNotificationHelper.StudyDeck>, rawFlatIndex: Int): ResolvedCardPosition? {
            val validDecks = decks.filter { it.cards.isNotEmpty() }
            if (validDecks.isEmpty()) return null
            val totalCards = validDecks.sumOf { it.cards.size }
            if (totalCards == 0) return null

            val flatIndex = ((rawFlatIndex % totalCards) + totalCards) % totalCards

            var accumulated = 0
            for (deck in validDecks) {
                if (flatIndex < accumulated + deck.cards.size) {
                    val indexInDeck = flatIndex - accumulated
                    return ResolvedCardPosition(
                        deck = deck,
                        card = deck.cards[indexInDeck],
                        cardIndexInDeck = indexInDeck,
                        flatIndex = flatIndex
                    )
                }
                accumulated += deck.cards.size
            }
            val first = validDecks.first()
            return ResolvedCardPosition(first, first.cards.first(), 0, 0)
        }

        @JvmStatic
        fun getAllStudyDecks(context: Context): List<QiezkaNotificationHelper.StudyDeck> {
            val candidateFiles = listOf(
                File(context.filesDir, "studom_resources"),
                File(context.filesDir, "Documents/studom_resources"),
                File(context.getExternalFilesDir(null), "studom_resources")
            )
            val file = candidateFiles.firstOrNull { it.exists() } ?: return emptyList()
            return try {
                val json = file.readText(Charsets.UTF_8)
                parseStudyDecksFromJson(json)
            } catch (ignore: Exception) {
                emptyList()
            }
        }

        @JvmStatic
        fun parseStudyDecksFromJson(json: String): List<QiezkaNotificationHelper.StudyDeck> {
            val resources = parseResources(json)
            val decks = mutableListOf<QiezkaNotificationHelper.StudyDeck>()
            for (res in resources) {
                val title = res.first
                val lines = res.second.lines()
                val cards = mutableListOf<QiezkaNotificationHelper.StudyCard>()
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    if (trimmed.contains("|")) {
                        val parts = trimmed.split("|", limit = 2)
                        val term = parts[0].trim()
                        val def = if (parts.size > 1) parts[1].trim() else ""
                        if (term.isNotEmpty()) {
                            cards.add(QiezkaNotificationHelper.StudyCard(title, term, def))
                        }
                    } else {
                        cards.add(QiezkaNotificationHelper.StudyCard(title, trimmed, ""))
                    }
                }
                if (cards.isNotEmpty()) {
                    decks.add(QiezkaNotificationHelper.StudyDeck(title, cards))
                }
            }
            return decks
        }

        @JvmStatic
        fun getAllStudyCards(context: Context): List<QiezkaNotificationHelper.StudyCard> {
            return getAllStudyDecks(context).flatMap { it.cards }
        }

        @JvmStatic
        fun parseStudyCardsFromJson(json: String): List<QiezkaNotificationHelper.StudyCard> {
            return parseStudyDecksFromJson(json).flatMap { it.cards }
        }
        /**
         * Scans saved resources for all lines matching [query] (case-insensitive contains).
         * Returns List of Pair(resourceSetTitle, matchedLine).
         */
        @JvmStatic
        fun searchAllResourceMatches(context: Context, query: String): List<Pair<String, String>> {
            val candidateFiles = listOf(
                File(context.filesDir, "studom_resources"),
                File(context.filesDir, "Documents/studom_resources"),
                File(context.getExternalFilesDir(null), "studom_resources")
            )
            val file = candidateFiles.firstOrNull { it.exists() } ?: return emptyList()
            return try {
                val json = file.readText(Charsets.UTF_8)
                searchAllInJsonString(json, query)
            } catch (ignore: Exception) {
                emptyList()
            }
        }

        @JvmStatic
        fun searchResourceMatch(context: Context, query: String): Pair<String, String>? {
            return searchAllResourceMatches(context, query).firstOrNull()
        }

        @JvmStatic
        fun parseResources(json: String): List<Pair<String, String>> {
            val list = mutableListOf<Pair<String, String>>()
            try {
                val arr = JSONArray(json)
                if (arr.length() > 0) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val title = obj.optString("title", "").trim()
                        val content = obj.optString("content", "")
                        list.add(Pair(title, content))
                    }
                    return list
                }
            } catch (ignore: Throwable) {}

            // Fallback regex search for JVM unit tests where returnDefaultValues produces empty JSONArray
            val contentPattern = Regex("\"content\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"", RegexOption.DOT_MATCHES_ALL)
            val titlePattern = Regex("\"title\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"", RegexOption.DOT_MATCHES_ALL)

            val contentMatches = contentPattern.findAll(json).toList()
            val titleMatches = titlePattern.findAll(json).toList()

            for (i in contentMatches.indices) {
                val rawContent = contentMatches[i].groupValues[1]
                val content = rawContent.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
                val title = if (i < titleMatches.size) {
                    titleMatches[i].groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\").trim()
                } else ""
                list.add(Pair(title, content))
            }
            return list
        }

        @JvmStatic
        fun searchAllInResources(resources: List<Pair<String, String>>, cleanQuery: String): List<Pair<String, String>> {
            if (cleanQuery.isEmpty()) return emptyList()

            val matches = mutableListOf<Pair<String, String>>()
            val seen = mutableSetOf<String>()

            // Scan all lines across all resources for substring contains match
            for (resource in resources) {
                val title = resource.first
                val content = resource.second
                val lines = content.lines()
                for (line in lines) {
                    val trimmedLine = line.trim()
                    if (trimmedLine.isEmpty()) continue
                    val displayLine = if (trimmedLine.contains("|")) {
                        val parts = trimmedLine.split("|", limit = 2)
                        val term = parts[0].trim()
                        val def = if (parts.size > 1) parts[1].trim() else ""
                        if (def.isNotEmpty()) "$term | $def" else term
                    } else {
                        trimmedLine
                    }

                    if (displayLine.contains(cleanQuery, ignoreCase = true)) {
                        val key = "$title::$displayLine"
                        if (seen.add(key)) {
                            matches.add(Pair(title, displayLine))
                        }
                    }
                }
            }

            // Fallback: If no lines matched, check if resource set title contains query
            if (matches.isEmpty()) {
                for (resource in resources) {
                    val title = resource.first
                    val content = resource.second
                    if (title.isNotEmpty() && title.contains(cleanQuery, ignoreCase = true)) {
                        val firstLine = content.lines().firstOrNull { it.isNotBlank() }?.trim() ?: title
                        val key = "$title::$firstLine"
                        if (seen.add(key)) {
                            matches.add(Pair(title, firstLine))
                        }
                    }
                }
            }

            return matches
        }

        @JvmStatic
        fun searchAllInJsonString(json: String, query: String): List<Pair<String, String>> {
            val cleanQuery = query.trim()
            if (cleanQuery.isEmpty()) return emptyList()
            val resources = parseResources(json)
            return searchAllInResources(resources, cleanQuery)
        }

        @JvmStatic
        fun searchInJsonString(json: String, query: String): Pair<String, String>? {
            return searchAllInJsonString(json, query).firstOrNull()
        }
    }
}
