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
            // Main card was dismissed or swiped away by user!
            // Permanently clear last_query, cancel all auxiliary search notifications,
            // reset sequence and is_unlocked = false, and revive card in disguised blank camouflage!
            val prefs = context.getSharedPreferences("uncode_study_cards", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("is_unlocked", false)
                .putString("last_query", "")
                .apply()
            synchronized(sequenceBuffer) {
                sequenceBuffer.clear()
                firstClickTimestamp = 0L
            }
            lastTapTime = 0L
            lastTapAction = null
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            for (i in 0 until 10) {
                nm?.cancel(QiezkaNotificationHelper.BASE_NOTIF_ID_AUX + i)
            }
            QiezkaNotificationHelper.postAlwaysActiveResourceQueryNotification(context)
            return
        }

        // 1b. User swiped away an auxiliary search notification!
        if (intent.action == QiezkaNotificationHelper.ACTION_DISMISS_SEARCH_QUERY) {
            val prefs = context.getSharedPreferences("uncode_study_cards", Context.MODE_PRIVATE)
            prefs.edit().putString("last_query", "").apply()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            for (i in 0 until 10) {
                nm?.cancel(QiezkaNotificationHelper.BASE_NOTIF_ID_AUX + i)
            }
            return
        }

        // 2a. Dedicated Deck Skipping Actions: ACTION_DECK_NEXT (>>), ACTION_DECK_PREV (<<)
        if (intent.action == QiezkaNotificationHelper.ACTION_DECK_NEXT ||
            intent.action == QiezkaNotificationHelper.ACTION_DECK_PREV) {
            val prefs = context.getSharedPreferences("uncode_study_cards", Context.MODE_PRIVATE)
            if (prefs.getBoolean("is_unlocked", false)) {
                handleDeckSkipping(context, next = (intent.action == QiezkaNotificationHelper.ACTION_DECK_NEXT))
            }
            return
        }

        // 2b. Card Paging Actions: ACTION_CARD_NEXT (>), ACTION_CARD_PREV (<)
        if (intent.action == QiezkaNotificationHelper.ACTION_CARD_NEXT ||
            intent.action == QiezkaNotificationHelper.ACTION_CARD_PREV) {

            val isNext = (intent.action == QiezkaNotificationHelper.ACTION_CARD_NEXT)
            val actionTag = if (isNext) "NEXT" else "PREV"

            val prefs = context.getSharedPreferences("uncode_study_cards", Context.MODE_PRIVATE)
            val isUnlocked = prefs.getBoolean("is_unlocked", false)

            if (!isUnlocked) {
                // Card is locked under disguise: native buttons strictly feed sequence detector with 2.0s timer
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
                            lastTapTime = 0L
                            lastTapAction = null
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

            // Unlocked State: Pure 1-card stepping with zero double-tap interference!
            // Rapid clicking advances single cards immediately without delay.
            handleCardPaging(context, delta = if (isNext) 1 else -1)
            return
        }

        if (intent.action != QiezkaNotificationHelper.ACTION_RESOURCE_QUERY) return

        val prefs = context.getSharedPreferences("uncode_study_cards", Context.MODE_PRIVATE)
        val isUnlocked = prefs.getBoolean("is_unlocked", false)

        if (!isUnlocked) {
            // In locked state: Reply strictly rejects queries, resets sequence buffer, and NEVER unlocks!
            synchronized(sequenceBuffer) {
                sequenceBuffer.clear()
                firstClickTimestamp = 0L
            }
            QiezkaNotificationHelper.postAlwaysActiveResourceQueryNotification(context)
            return
        }

        val bundle = RemoteInput.getResultsFromIntent(intent)
        if (bundle == null) {
            QiezkaNotificationHelper.postAlwaysActiveResourceQueryNotification(context)
            return
        }

        val rawQuery = bundle.getCharSequence(QiezkaNotificationHelper.KEY_TEXT_REPLY)?.toString() ?: ""
        val query = rawQuery.trim()

        if (query.isEmpty()) {
            prefs.edit().putString("last_query", "").apply()
            QiezkaNotificationHelper.postAlwaysActiveResourceQueryNotification(context)
            return
        }

        // Valid query submitted via RemoteInput in unlocked state!
        prefs.edit().putString("last_query", query).apply()

        // Non-destructive search: Main card (ID 1006) remains untouched in its flashcard deck!
        dispatchSearchResults(context, query)

        // Clear RemoteInput inline reply spinner on main card (1006) immediately!
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

    private fun handleDeckSkipping(context: Context, next: Boolean) {
        val decks = getAllStudyDecks(context)
        val validDecks = decks.filter { it.cards.isNotEmpty() }
        if (validDecks.isEmpty()) return

        val prefs = context.getSharedPreferences("uncode_study_cards", Context.MODE_PRIVATE)
        val currentFlatIndex = prefs.getInt("card_index", 0)
        val newFlatIndex = if (next) {
            calculateNextDeckIndex(validDecks, currentFlatIndex)
        } else {
            calculatePrevDeckIndex(validDecks, currentFlatIndex)
        }
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
        const val DOUBLE_TAP_WINDOW_MS = 450L
        @Volatile
        var lastTapTime = 0L
        @Volatile
        var lastTapAction: String? = null

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
                lastTapTime = 0L
                lastTapAction = null
                QiezkaNotificationHelper.postAlwaysActiveResourceQueryNotification(context)
            }
            // UNCONDITIONALLY cancel all auxiliary search notifications on shade collapse
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            for (i in 0 until 10) {
                nm?.cancel(QiezkaNotificationHelper.BASE_NOTIF_ID_AUX + i)
            }
        }

        @JvmStatic
        fun simulateNavigationTapForTesting(isNext: Boolean, now: Long): Boolean {
            val actionTag = if (isNext) "NEXT" else "PREV"
            val isDoubleTap = (lastTapAction == actionTag && (now - lastTapTime) < DOUBLE_TAP_WINDOW_MS)
            if (isDoubleTap) {
                lastTapTime = 0L
                lastTapAction = null
            } else {
                lastTapTime = now
                lastTapAction = actionTag
            }
            return isDoubleTap
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
        fun calculateNextDeckIndex(validDecks: List<QiezkaNotificationHelper.StudyDeck>, currentFlatIndex: Int): Int {
            if (validDecks.isEmpty()) return 0
            val currentPos = resolveDeckAndCard(validDecks, currentFlatIndex) ?: return 0
            val currentDeckIndex = validDecks.indexOfFirst { it.title == currentPos.deck.title }
            val nextDeckIndex = if (currentDeckIndex >= 0) (currentDeckIndex + 1) % validDecks.size else 0
            var startFlatIndex = 0
            for (i in 0 until nextDeckIndex) {
                startFlatIndex += validDecks[i].cards.size
            }
            return startFlatIndex
        }

        @JvmStatic
        fun calculatePrevDeckIndex(validDecks: List<QiezkaNotificationHelper.StudyDeck>, currentFlatIndex: Int): Int {
            if (validDecks.isEmpty()) return 0
            val currentPos = resolveDeckAndCard(validDecks, currentFlatIndex) ?: return 0
            val currentDeckIndex = validDecks.indexOfFirst { it.title == currentPos.deck.title }
            val prevDeckIndex = if (currentDeckIndex > 0) currentDeckIndex - 1 else validDecks.size - 1
            var startFlatIndex = 0
            for (i in 0 until prevDeckIndex) {
                startFlatIndex += validDecks[i].cards.size
            }
            return startFlatIndex
        }

        @JvmStatic
        fun getAllStudyDecks(context: Context): List<QiezkaNotificationHelper.StudyDeck> {
            try {
                val db = com.uncode.app.data.AppDatabase.getDatabase(context)
                val roomDecks = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                    val allCards = db.flashcardDao().getAllFlashcardsSync()
                    if (allCards.isEmpty()) return@runBlocking emptyList<QiezkaNotificationHelper.StudyDeck>()
                    val deckList = mutableListOf<QiezkaNotificationHelper.StudyDeck>()
                    val grouped = allCards.groupBy { it.deckId }
                    for ((deckId, cards) in grouped) {
                        val d = db.deckDao().getDeckById(deckId)
                        val title = d?.title ?: "Study Deck"
                        val studyCards = cards.map {
                            QiezkaNotificationHelper.StudyCard(title, it.termOrQuestion, it.definitionOrAnswer)
                        }
                        if (studyCards.isNotEmpty()) {
                            deckList.add(QiezkaNotificationHelper.StudyDeck(title, studyCards))
                        }
                    }
                    deckList
                }
                if (roomDecks.isNotEmpty()) return roomDecks
            } catch (_: Exception) {}

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
