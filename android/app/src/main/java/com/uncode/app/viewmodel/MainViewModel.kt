package com.uncode.app.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.provider.Settings
import com.uncode.app.AppClassifier
import com.uncode.app.KnownDistracting
import com.uncode.app.KnownSafe
import com.uncode.app.LocalDnsVpnService
import com.uncode.app.LockPlugin
import com.uncode.app.ScheduleManager
import com.uncode.app.UnifiedPolicyRegistry
import com.uncode.app.WebClassifier
import java.util.Calendar
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uncode.app.data.AnnotationItem
import com.uncode.app.data.AppDatabase
import com.uncode.app.data.Deck
import com.uncode.app.data.Flashcard
import com.uncode.app.data.QuestionReviewLog
import com.uncode.app.data.QuizSession
import com.uncode.app.data.StoredKnowledgeEngine
import com.uncode.app.repository.DeckRepository
import androidx.compose.runtime.mutableStateListOf
import com.uncode.app.ui.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class QuizQuestionItem(
    val cardId: Long,
    val deckId: Long,
    val deckName: String,
    val prompt: String,
    val answer: String,
    val photoUri: String?,
    val isAnnotated: Boolean,
    val annotations: List<AnnotationItem>,
    val options: List<String> = emptyList(),
    val targetAnnotationId: String? = null
)

data class DecayRiskDeck(
    val deckId: Long,
    val title: String,
    val daysSinceLastStudied: Int,
    val riskLevel: String
)

data class LeechCardSummary(
    val cardId: Long,
    val term: String,
    val failureCount: Int,
    val timesReviewed: Int,
    val deckName: String
)

data class ForecastItem(
    val dayLabel: String,
    val projectedDueCards: Int,
    val isToday: Boolean
)

data class EditableCardDraft(
    val tempKey: String = java.util.UUID.randomUUID().toString(),
    val id: Long = 0L,
    var term: String = "",
    var definition: String = "",
    var photoUri: String? = null,
    var isAnnotated: Boolean = false,
    var annotations: List<AnnotationItem> = emptyList(),
    var masteryLevel: Int = 0,
    var timesReviewed: Int = 0,
    var timesCorrect: Int = 0
)

data class DeckPerformanceSummary(
    val deckId: Long,
    val title: String,
    val category: String,
    val colorHex: String,
    val cardCount: Int,
    val masteredCount: Int,
    val accuracy: Int,
    val lastStudiedAt: Long?,
    val timesReviewed: Int
)

data class ScoreTrendPoint(
    val sessionIndex: Int,
    val scorePercent: Int,
    val timestamp: Long,
    val deckNames: String
)

data class DailyActivity(
    val dayLabel: String,
    val dateEpoch: Long,
    val questionCount: Int,
    val isToday: Boolean
)

data class ModeBreakdown(
    val singleCount: Int = 0,
    val combinedPoolCount: Int = 0,
    val stitchedCount: Int = 0
)

data class OverallDeckStatistics(
    val totalDecks: Int = 0,
    val totalCards: Int = 0,
    val totalQuizzes: Int = 0,
    val overallAccuracy: Int = 0,
    val masteredCards: Int = 0,
    val learningCards: Int = 0,
    val newCards: Int = 0,
    val leechCardsCount: Int = 0,
    val avgScore: Int = 0,
    val totalStudyTimeMinutes: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val averageSecondsPerCard: Float = 0f,
    val retentionZoneLabel: String = "Optimal (85-90%)",
    val retentionZoneColorHex: String = "#2E7D32",
    val visualCardsCount: Int = 0,
    val textCardsCount: Int = 0,
    val scoreTrend: List<ScoreTrendPoint> = emptyList(),
    val deckPerformances: List<DeckPerformanceSummary> = emptyList(),
    val weeklyActivity: List<DailyActivity> = emptyList(),
    val atRiskDecks: List<DecayRiskDeck> = emptyList(),
    val topLeechCards: List<LeechCardSummary> = emptyList(),
    val workloadForecast: List<ForecastItem> = emptyList(),
    val modeBreakdown: ModeBreakdown = ModeBreakdown(),
    val readinessScore: Int = 0,
    val strongestDeck: String? = null,
    val weakestDeck: String? = null
)

data class SirenSessionRecommendation(
    val questionCount: Int,
    val reason: String
)

data class AllowedAppItem(
    val packageName: String,
    val name: String,
    val isWhitelisted: Boolean = true
)

data class FocusScheduleItem(
    val id: String,
    val title: String,
    val activationTime: String,
    val durationMinutes: Int,
    val selectedDeckIds: List<Long> = emptyList(),
    val deckNames: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val isRescheduled: Boolean = false,
    val isAutoScheduled: Boolean = false
)

data class FailedFocusSessionItem(
    val id: String,
    val title: String,
    val selectedDeckIds: List<Long> = emptyList(),
    val deckNames: String = "",
    val durationMinutes: Int = 25,
    val failedTimestamp: Long = System.currentTimeMillis(),
    val earnedPoints: Float = 0f,
    val maxPoints: Float = 0f
)

data class ScoreThresholds(
    val unreal: Float = 1.5f,
    val instant: Float = 2.5f,
    val sharp: Float = 3.5f,
    val deliberate: Float = 4.5f,
    val hesitant: Float = 5.5f
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeckRepository(AppDatabase.getDatabase(application))

    val navigationBackStack = mutableStateListOf<Screen>(Screen.Dashboard)

    private val _focusSchedules = MutableStateFlow<List<FocusScheduleItem>>(emptyList())
    val focusSchedules: StateFlow<List<FocusScheduleItem>> = _focusSchedules.asStateFlow()

    private val _failedFocusSessions = MutableStateFlow<List<FailedFocusSessionItem>>(emptyList())
    val failedFocusSessions: StateFlow<List<FailedFocusSessionItem>> = _failedFocusSessions.asStateFlow()

    private val _isLockdownActive = MutableStateFlow(false)
    val isLockdownActive: StateFlow<Boolean> = _isLockdownActive.asStateFlow()

    private val _isConsequenceActive = MutableStateFlow(false)
    val isConsequenceActive: StateFlow<Boolean> = _isConsequenceActive.asStateFlow()

    private val _operatingMode = MutableStateFlow("safemode")
    val operatingMode: StateFlow<String> = _operatingMode.asStateFlow()

    private val _webProtectionMode = MutableStateFlow("accessibility")
    val webProtectionMode: StateFlow<String> = _webProtectionMode.asStateFlow()

    private val _activeUnifiedServices = MutableStateFlow<Set<String>>(emptySet())
    val activeUnifiedServices: StateFlow<Set<String>> = _activeUnifiedServices.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _isVpnRunning = MutableStateFlow(false)
    val isVpnRunning: StateFlow<Boolean> = _isVpnRunning.asStateFlow()

    private val _allowedApps = MutableStateFlow<List<AllowedAppItem>>(emptyList())
    val allowedApps: StateFlow<List<AllowedAppItem>> = _allowedApps.asStateFlow()

    private val _eligibleApps = MutableStateFlow<List<AllowedAppItem>>(emptyList())
    val eligibleApps: StateFlow<List<AllowedAppItem>> = _eligibleApps.asStateFlow()

    private val _activeQuizQuestions = MutableStateFlow<List<QuizQuestionItem>>(emptyList())
    val activeQuizQuestions: StateFlow<List<QuizQuestionItem>> = _activeQuizQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

    private val _quizStreak = MutableStateFlow(0)
    val quizStreak: StateFlow<Int> = _quizStreak.asStateFlow()

    private val _quizEarnedPoints = MutableStateFlow(0f)
    val quizEarnedPoints: StateFlow<Float> = _quizEarnedPoints.asStateFlow()

    private val _quizLogs = MutableStateFlow<List<QuestionReviewLog>>(emptyList())
    val quizLogs: StateFlow<List<QuestionReviewLog>> = _quizLogs.asStateFlow()

    private val _quizFinishedSessionId = MutableStateFlow<Long?>(null)
    val quizFinishedSessionId: StateFlow<Long?> = _quizFinishedSessionId.asStateFlow()

    private val _lastFinishedSession = MutableStateFlow<QuizSession?>(null)
    val lastFinishedSession: StateFlow<QuizSession?> = _lastFinishedSession.asStateFlow()

    private val _autoScheduleEnabled = MutableStateFlow(false)
    val autoScheduleEnabled: StateFlow<Boolean> = _autoScheduleEnabled.asStateFlow()

    private val _autoScheduleSelectedDeckId = MutableStateFlow<Long?>(null)
    val autoScheduleSelectedDeckId: StateFlow<Long?> = _autoScheduleSelectedDeckId.asStateFlow()

    private val _autoScheduleSelectedDuration = MutableStateFlow<Int?>(null)
    val autoScheduleSelectedDuration: StateFlow<Int?> = _autoScheduleSelectedDuration.asStateFlow()

    private val _autoScheduleDeletedCount = MutableStateFlow(0)
    val autoScheduleDeletedCount: StateFlow<Int> = _autoScheduleDeletedCount.asStateFlow()

    private val _autoQuizEnabled = MutableStateFlow(false)
    val autoQuizEnabled: StateFlow<Boolean> = _autoQuizEnabled.asStateFlow()

    private val _autoQuizMode = MutableStateFlow("usage_30")
    val autoQuizMode: StateFlow<String> = _autoQuizMode.asStateFlow()

    private val _autoQuizSelectedDeckId = MutableStateFlow<Long?>(null)
    val autoQuizSelectedDeckId: StateFlow<Long?> = _autoQuizSelectedDeckId.asStateFlow()

    init {
        StoredKnowledgeEngine.init(application.applicationContext)
        loadAllowedApps()
        loadEligibleApps()
        loadFocusSchedules()
        loadFailedFocusSessions()
        loadAutoScheduleConfig()
        loadAutoQuizConfig()
        refreshLockdownAndServiceState()
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                syncPastDueSchedules()
                checkAutoScheduleDailyStatus()
            }
        }
    }

    val allDecks: StateFlow<List<Deck>> = repository.allDecks
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allFlashcards: StateFlow<List<Flashcard>> = repository.allFlashcards
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentSessions: StateFlow<List<QuizSession>> = repository.recentSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<QuizSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overallStats: StateFlow<OverallDeckStatistics> = combine(
        allDecks, allFlashcards, allSessions
    ) { decks, cards, sessions ->
        val totalCards = cards.size
        val totalDecks = decks.size
        val totalQuizzes = sessions.size
        val mastered = cards.count { it.masteryLevel == 2 }
        val learning = cards.count { it.masteryLevel == 1 }
        val newCount = cards.count { it.masteryLevel == 0 }

        val totalReviewed = cards.sumOf { it.timesReviewed }
        val totalCorrect = cards.sumOf { it.timesCorrect }
        val overallAcc = if (totalReviewed > 0) ((totalCorrect.toFloat() / totalReviewed) * 100).toInt() else 0
        val avgScore = if (sessions.isNotEmpty()) (sessions.map { it.scorePercent }.average()).toInt() else 0

        val totalStudyTimeMinutes = sessions.sumOf { it.durationSeconds } / 60
        val totalQuestionsAnswered = sessions.sumOf { it.totalQuestions }
        val totalDurationSec = sessions.sumOf { it.durationSeconds }
        val avgSpeedSeconds = if (totalQuestionsAnswered > 0 && totalDurationSec > 0) {
            (totalDurationSec.toFloat() / totalQuestionsAnswered.toFloat())
        } else {
            8.5f
        }

        val leechCards = cards.filter { card ->
            val failures = card.timesReviewed - card.timesCorrect
            card.timesReviewed >= 3 && failures >= 2
        }
        val topLeechList = leechCards.take(5).map { card ->
            val deckName = decks.find { it.id == card.deckId }?.title ?: "Deck"
            LeechCardSummary(
                cardId = card.id,
                term = card.termOrQuestion,
                failureCount = card.timesReviewed - card.timesCorrect,
                timesReviewed = card.timesReviewed,
                deckName = deckName
            )
        }

        val (retentionZone, retentionZoneColor) = when {
            overallAcc in 85..90 -> "Optimal (85-90%)" to "#2E7D32"
            overallAcc > 90 -> "High (>90%)" to "#1976D2"
            overallAcc in 70..84 -> "Active (70-84%)" to "#FB8C00"
            else -> "Needs Focus (<70%)" to "#E53935"
        }

        val visualCards = cards.count { it.isAnnotated || !it.photoUri.isNullOrBlank() }
        val textCards = (totalCards - visualCards).coerceAtLeast(0)

        val cardsByDeck = cards.groupBy { it.deckId }
        val deckPerfs = decks.map { deck ->
            val deckCards = cardsByDeck[deck.id].orEmpty()
            val reviewed = deckCards.sumOf { it.timesReviewed }
            val correct = deckCards.sumOf { it.timesCorrect }
            val acc = if (reviewed > 0) ((correct.toFloat() / reviewed) * 100).toInt() else 0
            val mast = deckCards.count { it.masteryLevel == 2 }
            DeckPerformanceSummary(
                deckId = deck.id,
                title = deck.title,
                category = deck.category,
                colorHex = deck.colorHex,
                cardCount = deckCards.size,
                masteredCount = mast,
                accuracy = acc,
                lastStudiedAt = deck.lastStudiedAt,
                timesReviewed = reviewed
            )
        }.sortedByDescending { it.accuracy }

        val decksWithReviews = deckPerfs.filter { it.timesReviewed > 0 }
        val strongest = decksWithReviews.maxByOrNull { it.accuracy }?.title
        val weakest = if (decksWithReviews.size > 1) decksWithReviews.minByOrNull { it.accuracy }?.title else null

        val nowEpoch = System.currentTimeMillis()
        val decayRiskDecks = decks.map { deck ->
            val daysSince = if (deck.lastStudiedAt != null) {
                ((nowEpoch - deck.lastStudiedAt) / (1000L * 60 * 60 * 24)).toInt()
            } else {
                -1
            }
            val risk = when {
                daysSince >= 4 || daysSince == -1 -> "HIGH"
                daysSince >= 2 -> "MEDIUM"
                else -> "FRESH"
            }
            DecayRiskDeck(
                deckId = deck.id,
                title = deck.title,
                daysSinceLastStudied = daysSince,
                riskLevel = risk
            )
        }.sortedByDescending { if (it.daysSinceLastStudied == -1) 999 else it.daysSinceLastStudied }

        val sortedSessions = sessions.sortedBy { it.timestamp }
        val trend = sortedSessions.takeLast(10).mapIndexed { index, session ->
            ScoreTrendPoint(
                sessionIndex = index + 1,
                scorePercent = session.scorePercent,
                timestamp = session.timestamp,
                deckNames = session.deckNames
            )
        }

        val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        val weekly = (6 downTo 0).map { daysAgo ->
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = nowEpoch
                add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000L - 1
            val dayLabel = dayFormat.format(java.util.Date(startOfDay))
            val dayQuestions = sessions.filter { it.timestamp in startOfDay..endOfDay }
                .sumOf { it.totalQuestions }
            DailyActivity(
                dayLabel = dayLabel,
                dateEpoch = startOfDay,
                questionCount = dayQuestions,
                isToday = daysAgo == 0
            )
        }

        val forecast = (0..4).map { dayOffset ->
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = nowEpoch
                add(java.util.Calendar.DAY_OF_YEAR, dayOffset)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            val end = start + 24 * 60 * 60 * 1000L - 1
            val dayLabel = dayFormat.format(java.util.Date(start))

            val estimatedSpacedDue = when (dayOffset) {
                0 -> (learning * 0.4f).toInt()
                1 -> (learning * 0.3f).toInt()
                2 -> (learning * 0.2f).toInt()
                else -> (learning * 0.1f).toInt()
            }
            ForecastItem(
                dayLabel = if (dayOffset == 0) "Today" else dayLabel,
                projectedDueCards = estimatedSpacedDue,
                isToday = dayOffset == 0
            )
        }

        val singleCount = sessions.count { it.combinationMode == "SINGLE" }
        val combinedCount = sessions.count { it.combinationMode == "COMBINED_POOL" }
        val stitchedCount = sessions.count { it.combinationMode == "STITCHED" }

        val coverage = if (totalCards > 0) ((mastered + learning).toFloat() / totalCards) * 100 else 0f
        val masteryRatio = if (totalCards > 0) (mastered.toFloat() / totalCards) * 100 else 0f
        val readiness = (overallAcc * 0.35f + masteryRatio * 0.30f + avgScore * 0.25f + coverage * 0.10f).toInt().coerceIn(0, 100)

        OverallDeckStatistics(
            totalDecks = totalDecks,
            totalCards = totalCards,
            totalQuizzes = totalQuizzes,
            overallAccuracy = overallAcc,
            masteredCards = mastered,
            learningCards = learning,
            newCards = newCount,
            leechCardsCount = leechCards.size,
            avgScore = avgScore,
            totalStudyTimeMinutes = totalStudyTimeMinutes,
            totalQuestionsAnswered = totalQuestionsAnswered,
            averageSecondsPerCard = avgSpeedSeconds,
            retentionZoneLabel = retentionZone,
            retentionZoneColorHex = retentionZoneColor,
            visualCardsCount = visualCards,
            textCardsCount = textCards,
            scoreTrend = trend,
            deckPerformances = deckPerfs,
            weeklyActivity = weekly,
            atRiskDecks = decayRiskDecks,
            topLeechCards = topLeechList,
            workloadForecast = forecast,
            modeBreakdown = ModeBreakdown(singleCount, combinedCount, stitchedCount),
            readinessScore = readiness,
            strongestDeck = strongest,
            weakestDeck = weakest
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallDeckStatistics())

    private val appPrefs = application.getSharedPreferences("siren_app_prefs", android.content.Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        try {
            val saved = appPrefs.getString("theme_mode", AppThemeMode.SYSTEM.name)
            AppThemeMode.valueOf(saved ?: AppThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        appPrefs.edit().putString("theme_mode", mode.name).apply()
    }

    private val _scoreThresholds = MutableStateFlow(
        ScoreThresholds(
            unreal = appPrefs.getFloat("score_tier_unreal", 1.5f),
            instant = appPrefs.getFloat("score_tier_instant", 2.5f),
            sharp = appPrefs.getFloat("score_tier_sharp", 3.5f),
            deliberate = appPrefs.getFloat("score_tier_deliberate", 4.5f),
            hesitant = appPrefs.getFloat("score_tier_hesitant", 5.5f)
        )
    )
    val scoreThresholds: StateFlow<ScoreThresholds> = _scoreThresholds.asStateFlow()

    fun updateScoreThresholds(unreal: Float, instant: Float, sharp: Float, deliberate: Float, hesitant: Float): Boolean {
        if (unreal <= 0f || unreal >= instant || instant >= sharp || sharp >= deliberate || deliberate >= hesitant) {
            return false
        }
        appPrefs.edit()
            .putFloat("score_tier_unreal", unreal)
            .putFloat("score_tier_instant", instant)
            .putFloat("score_tier_sharp", sharp)
            .putFloat("score_tier_deliberate", deliberate)
            .putFloat("score_tier_hesitant", hesitant)
            .apply()
        _scoreThresholds.value = ScoreThresholds(unreal, instant, sharp, deliberate, hesitant)
        return true
    }

    fun resetScoreThresholds() {
        appPrefs.edit()
            .remove("score_tier_unreal")
            .remove("score_tier_instant")
            .remove("score_tier_sharp")
            .remove("score_tier_deliberate")
            .remove("score_tier_hesitant")
            .apply()
        _scoreThresholds.value = ScoreThresholds()
    }

    private val _selectedDeckIdsForQuiz = MutableStateFlow<Set<Long>>(emptySet())
    val selectedDeckIdsForQuiz: StateFlow<Set<Long>> = _selectedDeckIdsForQuiz.asStateFlow()

    private val _deckDistribution = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val deckDistribution: StateFlow<Map<Long, Int>> = _deckDistribution.asStateFlow()

    private val _qiezkaDeterminedOrder = MutableStateFlow<List<Deck>>(emptyList())
    val qiezkaDeterminedOrder: StateFlow<List<Deck>> = _qiezkaDeterminedOrder.asStateFlow()

    private val _qiezkaOrderExplanation = MutableStateFlow("")
    val qiezkaOrderExplanation: StateFlow<String> = _qiezkaOrderExplanation.asStateFlow()

    fun createDeck(
        title: String,
        description: String,
        category: String,
        colorHex: String,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val newDeck = Deck(
                title = title.ifBlank { "Untitled Deck" },
                description = description,
                category = category.trim(),
                colorHex = colorHex
            )
            val id = repository.insertDeck(newDeck)
            onCreated(id)
        }
    }

    fun importCustomDeck(
        title: String,
        cards: List<Pair<String, String>>,
        onImported: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val newDeck = Deck(
                title = title.ifBlank { "Imported Deck" },
                description = "Custom imported with ${cards.size} flashcards",
                category = "",
                colorHex = "#FF8533"
            )
            val deckId = repository.insertDeck(newDeck)
            val flashcards = cards.map { (term, def) ->
                Flashcard(
                    deckId = deckId,
                    termOrQuestion = term.trim(),
                    definitionOrAnswer = def.trim()
                )
            }
            if (flashcards.isNotEmpty()) {
                repository.insertFlashcards(flashcards)
            }
            onImported(deckId)
        }
    }

    fun deleteDeck(deckId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val isLocked = _focusSchedules.value.any { deckId in it.selectedDeckIds } ||
                _failedFocusSessions.value.any { deckId in it.selectedDeckIds }
            if (isLocked) {
                onDeleted()
                return@launch
            }
            repository.deleteDeck(deckId)
            onDeleted()
        }
    }

    fun updateDeck(deck: Deck, onUpdated: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateDeck(deck)
            onUpdated()
        }
    }

    fun addFlashcard(
        deckId: Long,
        term: String,
        definition: String,
        photoUri: String?,
        isAnnotated: Boolean,
        annotations: List<AnnotationItem>,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val card = Flashcard(
                deckId = deckId,
                termOrQuestion = term.ifBlank { "Question" },
                definitionOrAnswer = definition.ifBlank { "Answer" },
                photoUri = photoUri,
                isAnnotated = isAnnotated,
                annotationsJson = AnnotationItem.toJsonString(annotations)
            )
            repository.insertFlashcard(card)
            onSaved()
        }
    }

    fun updateFlashcard(
        card: Flashcard,
        onUpdated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateFlashcard(card)
            onUpdated()
        }
    }

    fun deleteFlashcard(cardId: Long) {
        viewModelScope.launch {
            repository.deleteFlashcard(cardId)
        }
    }

    fun saveAllCardsForDeck(
        deckId: Long,
        cardsToSave: List<EditableCardDraft>,
        deletedCardIds: List<Long>,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val toInsert = cardsToSave.filter { it.id == 0L && (it.term.isNotBlank() || it.definition.isNotBlank() || !it.photoUri.isNullOrBlank()) }.map { draft ->
                Flashcard(
                    deckId = deckId,
                    termOrQuestion = draft.term.trim(),
                    definitionOrAnswer = if (draft.definition.isNotBlank()) draft.definition.trim() else draft.term.trim(),
                    photoUri = draft.photoUri,
                    isAnnotated = draft.isAnnotated,
                    annotationsJson = AnnotationItem.toJsonString(draft.annotations)
                )
            }
            val toUpdate = cardsToSave.filter { it.id > 0L }.map { draft ->
                Flashcard(
                    id = draft.id,
                    deckId = deckId,
                    termOrQuestion = draft.term.trim(),
                    definitionOrAnswer = if (draft.definition.isNotBlank()) draft.definition.trim() else draft.term.trim(),
                    photoUri = draft.photoUri,
                    isAnnotated = draft.isAnnotated,
                    annotationsJson = AnnotationItem.toJsonString(draft.annotations),
                    masteryLevel = draft.masteryLevel,
                    timesReviewed = draft.timesReviewed,
                    timesCorrect = draft.timesCorrect
                )
            }
            repository.saveDeckFlashcards(deckId, toInsert, toUpdate, deletedCardIds)
            onComplete()
        }
    }

    fun toggleDeckSelectionForQuiz(deckId: Long) {
        val current = _selectedDeckIdsForQuiz.value.toMutableSet()
        if (current.contains(deckId)) {
            current.remove(deckId)
        } else {
            current.add(deckId)
        }
        _selectedDeckIdsForQuiz.value = current
        recalculateDefaultDistribution(current)
    }

    fun selectSingleDeckForQuiz(deckId: Long) {
        _selectedDeckIdsForQuiz.value = setOf(deckId)
    }

    fun setDeckPercentage(deckId: Long, percentage: Int) {
        val dist = _deckDistribution.value.toMutableMap()
        dist[deckId] = percentage
        _deckDistribution.value = dist
    }

    private fun recalculateDefaultDistribution(deckIds: Set<Long>) {
        if (deckIds.isEmpty()) {
            _deckDistribution.value = emptyMap()
            return
        }
        val equalShare = 100 / deckIds.size
        val remainder = 100 % deckIds.size
        val map = mutableMapOf<Long, Int>()
        deckIds.toList().forEachIndexed { index, id ->
            map[id] = equalShare + (if (index == 0) remainder else 0)
        }
        _deckDistribution.value = map
    }

    fun prepareQiezkaStitchedOrder(onReady: () -> Unit = {}) {
        viewModelScope.launch {
            val selectedIds = _selectedDeckIdsForQuiz.value
            val decks = allDecks.value.filter { it.id in selectedIds }
            val allCards = repository.allFlashcardsSync()
            val cardsByDeck = allCards.groupBy { it.deckId }

            val sortedDecks = decks.sortedWith(
                compareBy<Deck> { deck ->
                    val deckCards = cardsByDeck[deck.id].orEmpty()
                    val reviewed = deckCards.sumOf { c -> c.timesReviewed }
                    val correct = deckCards.sumOf { c -> c.timesCorrect }
                    if (reviewed > 0) (correct.toFloat() / reviewed) else 0f
                }.thenBy { deck -> deck.lastStudiedAt ?: 0L }
            )

            _qiezkaDeterminedOrder.value = sortedDecks
            _qiezkaOrderExplanation.value = buildString {
                append("SIREN Quiz App analyzed your mastery levels across selected decks:\n")
                sortedDecks.forEachIndexed { idx, d ->
                    val deckCards = cardsByDeck[d.id].orEmpty()
                    val totalRev = deckCards.sumOf { c -> c.timesReviewed }
                    val totalCorr = deckCards.sumOf { c -> c.timesCorrect }
                    val acc = if (totalRev > 0) ((totalCorr.toFloat() / totalRev) * 100).toInt() else 0
                    append("${idx + 1}. ${d.title} (Accuracy: $acc% \u2022 Priority Sequence)\n")
                }
            }
            onReady()
        }
    }

    fun computeSirenSessionRecommendation(cards: List<Flashcard>): SirenSessionRecommendation {
        val total = cards.size
        if (total <= 8) {
            return SirenSessionRecommendation(total, "Complete deck review ($total concepts)")
        }
        val leechCount = cards.count { (it.timesReviewed - it.timesCorrect) > 0 }
        val learningCount = cards.count { it.masteryLevel == 1 }
        val newCount = cards.count { it.masteryLevel == 0 }

        return when {
            leechCount >= 3 -> {
                val count = minOf(leechCount + 3, 10).coerceAtMost(total)
                SirenSessionRecommendation(count, "Targeting $leechCount high-friction mistake concepts")
            }
            learningCount + leechCount >= 8 -> {
                val count = minOf(learningCount + leechCount, 15).coerceAtMost(total)
                SirenSessionRecommendation(count, "Active retention consolidation ($count due cards)")
            }
            newCount > total * 0.5 -> {
                val count = minOf(10, total)
                SirenSessionRecommendation(count, "Optimal 10-concept microlearning batch")
            }
            else -> {
                val count = minOf(10, total)
                SirenSessionRecommendation(count, "Targeted 10-question spaced practice")
            }
        }
    }

    fun startSingleDeckQuiz(deckId: Long, onStarted: () -> Unit) {
        startSingleDeckQuiz(deckId, null, onStarted)
    }

    fun startSingleDeckQuiz(deckId: Long, explicitLimit: Int? = null, onStarted: () -> Unit = {}) {
        viewModelScope.launch {
            val deck = repository.getDeck(deckId) ?: return@launch
            val allCards = repository.getFlashcardsSync(deckId)
            if (allCards.isEmpty()) return@launch

            val targetCount = explicitLimit ?: computeSirenSessionRecommendation(allCards).questionCount
            val targetCards = if (targetCount in 1 until allCards.size) {
                val prioritized = allCards.sortedWith(
                    compareByDescending<Flashcard> { (it.timesReviewed - it.timesCorrect).coerceAtLeast(0) }
                        .thenBy { it.masteryLevel }
                        .thenBy { it.timesReviewed }
                )
                prioritized.take(targetCount).shuffled()
            } else {
                allCards.shuffled()
            }

            val quizItems = withContext(Dispatchers.Default) {
                buildQuizQuestionItems(targetCards, mapOf(deckId to deck.title))
            }
            initializeQuizSession(quizItems, "SINGLE", listOf(deck.id), deck.title)
            onStarted()
        }
    }

    private var focusSessionQuizJob: Job? = null

    fun startFocusSessionQuiz(deckIds: List<Long>, onStarted: () -> Unit = {}) {
        if (focusSessionQuizJob?.isActive == true) return
        focusSessionQuizJob = viewModelScope.launch {
            _quizFinishedSessionId.value = null
            _quizEarnedPoints.value = 0f
            _activeQuizQuestions.value = emptyList()
            val validDeckIds = if (deckIds.isNotEmpty()) {
                deckIds
            } else {
                allDecks.value.map { it.id }
            }

            val decks = allDecks.value.filter { it.id in validDeckIds }
            val deckNameMap = decks.associate { it.id to it.title }
            var cards = if (validDeckIds.isNotEmpty()) {
                repository.getFlashcardsForDecksSync(validDeckIds)
            } else {
                emptyList()
            }
            if (cards.isEmpty()) {
                cards = repository.allFlashcardsSync()
            }
            if (cards.isEmpty()) return@launch

            val targetCount = minOf(10, cards.size)
            val selectedCards = cards.shuffled().take(targetCount)

            val quizItems = withContext(Dispatchers.Default) {
                buildQuizQuestionItems(selectedCards, deckNameMap)
            }
            val title = decks.joinToString(", ") { it.title }.ifBlank { "Focus Quiz" }
            initializeQuizSession(quizItems, "FOCUS_LOCKED", validDeckIds, title)
            onStarted()
        }
    }

    fun startCombinedPoolQuiz(targetTotalQuestions: Int = 10, onStarted: () -> Unit) {
        viewModelScope.launch {
            val selectedIds = _selectedDeckIdsForQuiz.value.toList()
            val decks = allDecks.value.filter { it.id in selectedIds }
            val deckNameMap = decks.associate { it.id to it.title }
            val allSelectedCards = repository.getFlashcardsForDecksSync(selectedIds)

            if (allSelectedCards.isEmpty()) return@launch

            val dist = _deckDistribution.value
            val pooledCards = mutableListOf<Flashcard>()

            selectedIds.forEach { dId ->
                val deckCards = allSelectedCards.filter { it.deckId == dId }.shuffled()
                val pct = (dist[dId] ?: (100 / selectedIds.size)).coerceIn(0, 100)
                val targetCount = ((pct / 100f) * targetTotalQuestions).toInt().coerceAtLeast(1)
                pooledCards.addAll(deckCards.take(targetCount))
            }

            val shuffledPool = pooledCards.shuffled()
            val quizItems = withContext(Dispatchers.Default) {
                buildQuizQuestionItems(shuffledPool, deckNameMap)
            }
            val deckTitles = decks.joinToString(", ") { it.title }
            initializeQuizSession(quizItems, "COMBINED_POOL", selectedIds, deckTitles)
            onStarted()
        }
    }

    fun startStitchedQuiz(onStarted: () -> Unit) {
        viewModelScope.launch {
            val orderedDecks = _qiezkaDeterminedOrder.value
            if (orderedDecks.isEmpty()) return@launch

            val orderedIds = orderedDecks.map { it.id }
            val deckNameMap = orderedDecks.associate { it.id to it.title }
            val allCards = repository.getFlashcardsForDecksSync(orderedIds)

            val stitchedCards = mutableListOf<Flashcard>()
            orderedDecks.forEach { deck ->
                val cardsForThisDeck = allCards.filter { it.deckId == deck.id }.shuffled()
                stitchedCards.addAll(cardsForThisDeck)
            }

            if (stitchedCards.isEmpty()) return@launch

            val quizItems = withContext(Dispatchers.Default) {
                buildQuizQuestionItems(stitchedCards, deckNameMap)
            }
            val deckTitles = orderedDecks.joinToString(" \u2794 ") { it.title }
            initializeQuizSession(quizItems, "STITCHED", orderedIds, deckTitles)
            onStarted()
        }
    }

    private fun buildQuizQuestionItems(cards: List<Flashcard>, deckNames: Map<Long, String>): List<QuizQuestionItem> {
        val allAnswers = cards.map { it.definitionOrAnswer }.distinct()
        val cardPairs = cards.map { it.termOrQuestion to it.definitionOrAnswer }

        return cards.flatMap { card ->
            val annotations = AnnotationItem.parseList(card.annotationsJson)
            val validAnnotations = annotations.filter { it.labelAnswer.isNotBlank() }
            val isAnnotated = card.isAnnotated && validAnnotations.isNotEmpty()

            if (isAnnotated) {
                validAnnotations.shuffled().map { targetAnnotation ->
                    val correctAnswer = targetAnnotation.labelAnswer
                    val prompt = if (card.termOrQuestion.isNotBlank()) {
                        card.termOrQuestion
                    } else {
                        "Identify highlighted target"
                    }

                    val siblingLabels = validAnnotations
                        .filter { it.id != targetAnnotation.id && !it.labelAnswer.equals(correctAnswer, ignoreCase = true) }
                        .map { it.labelAnswer }
                        .distinct()

                    val sameDiagramDistractors = siblingLabels.shuffled().take(3)
                    val neededExtra = 3 - sameDiagramDistractors.size

                    val extraDistractors = if (neededExtra > 0) {
                        com.uncode.app.data.StoredKnowledgeEngine.generateDistractors(
                            targetTerm = prompt,
                            targetAnswer = correctAnswer,
                            deckCategory = deckNames[card.deckId] ?: "",
                            existingCards = cardPairs,
                            existingCardAnswers = allAnswers,
                            count = neededExtra
                        )
                    } else {
                        emptyList()
                    }

                    val combinedOptions = (sameDiagramDistractors + extraDistractors + correctAnswer).distinct().toMutableList()
                    if (combinedOptions.size < 4) {
                        val fallback = com.uncode.app.data.StoredKnowledgeEngine.generateDistractors(
                            targetTerm = prompt,
                            targetAnswer = correctAnswer,
                            deckCategory = deckNames[card.deckId] ?: "",
                            existingCards = cardPairs,
                            existingCardAnswers = allAnswers,
                            count = 4
                        )
                        for (item in fallback) {
                            if (!combinedOptions.contains(item)) {
                                combinedOptions.add(item)
                                if (combinedOptions.size == 4) break
                            }
                        }
                    }

                    val options = combinedOptions.shuffled()

                    QuizQuestionItem(
                        cardId = card.id,
                        deckId = card.deckId,
                        deckName = deckNames[card.deckId] ?: "Deck",
                        prompt = prompt,
                        answer = correctAnswer,
                        photoUri = card.photoUri,
                        isAnnotated = true,
                        annotations = annotations,
                        options = options,
                        targetAnnotationId = targetAnnotation.id
                    )
                }
            } else {
                val correctAnswer = card.definitionOrAnswer.ifBlank { card.termOrQuestion }
                val prompt = if (!card.photoUri.isNullOrBlank() && card.termOrQuestion.isNotBlank()) {
                    card.termOrQuestion
                } else if (!card.photoUri.isNullOrBlank()) {
                    "Identify this image"
                } else {
                    card.termOrQuestion
                }

                val distractors = com.uncode.app.data.StoredKnowledgeEngine.generateDistractors(
                    targetTerm = prompt,
                    targetAnswer = correctAnswer,
                    deckCategory = deckNames[card.deckId] ?: "",
                    existingCards = cardPairs,
                    existingCardAnswers = allAnswers,
                    count = 3
                )
                val options = (distractors + correctAnswer).distinct().shuffled()

                listOf(
                    QuizQuestionItem(
                        cardId = card.id,
                        deckId = card.deckId,
                        deckName = deckNames[card.deckId] ?: "Deck",
                        prompt = prompt,
                        answer = correctAnswer,
                        photoUri = card.photoUri,
                        isAnnotated = false,
                        annotations = emptyList(),
                        options = options,
                        targetAnnotationId = null
                    )
                )
            }
        }
    }

    private fun initializeQuizSession(
        items: List<QuizQuestionItem>,
        mode: String,
        deckIds: List<Long>,
        deckNames: String
    ) {
        _activeQuizQuestions.value = items
        _currentQuestionIndex.value = 0
        _quizScore.value = 0
        _quizStreak.value = 0
        _quizEarnedPoints.value = 0f
        _quizLogs.value = emptyList()
        _quizFinishedSessionId.value = null
        _lastFinishedSession.value = null
    }

    fun submitAnswer(userAnswer: String, isSelfRatedCorrect: Boolean? = null, earnedPoints: Float = 0f) {
        val questions = _activeQuizQuestions.value
        val index = _currentQuestionIndex.value
        if (index !in questions.indices) return

        val q = questions[index]
        val isCorrect = isSelfRatedCorrect ?: userAnswer.trim().equals(q.answer.trim(), ignoreCase = true)

        if (isCorrect) {
            _quizScore.value += 1
            _quizStreak.value += 1
            _quizEarnedPoints.value += earnedPoints
        } else {
            _quizStreak.value = 0
        }

        val log = QuestionReviewLog(
            cardId = q.cardId,
            question = q.prompt,
            correctAnswer = q.answer,
            userAnswer = userAnswer,
            isCorrect = isCorrect,
            deckName = q.deckName,
            photoUri = q.photoUri,
            isAnnotated = q.isAnnotated,
            annotationsJson = AnnotationItem.toJsonString(q.annotations)
        )
        _quizLogs.value = _quizLogs.value + log

        viewModelScope.launch {
            repository.recordCardReview(q.cardId, isCorrect)
        }

        if (index + 1 < questions.size) {
            _currentQuestionIndex.value = index + 1
        } else {
            finishQuizSession()
        }
    }

    private fun finishQuizSession() {
        val questions = _activeQuizQuestions.value
        val total = questions.size
        val correct = _quizScore.value
        val percent = if (total > 0) ((correct.toFloat() / total) * 100).toInt() else 0
        val logs = _quizLogs.value
        val deckIds = questions.map { it.deckId }.distinct()
        val deckNames = questions.map { it.deckName }.distinct().joinToString(", ")

        viewModelScope.launch {
            val session = QuizSession(
                deckIdsJson = deckIds.toString(),
                deckNames = deckNames,
                combinationMode = if (deckIds.size > 1) "MULTI" else "SINGLE",
                totalQuestions = total,
                correctCount = correct,
                scorePercent = percent,
                durationSeconds = total * 8,
                answerLogsJson = QuestionReviewLog.toJsonString(logs)
            )
            val sessionId = repository.saveQuizSession(session)
            _quizFinishedSessionId.value = sessionId
            _lastFinishedSession.value = session.copy(id = sessionId)
        }
    }

    fun loadSessionForReview(sessionId: Long, onLoaded: (QuizSession) -> Unit) {
        viewModelScope.launch {
            val session = repository.getSession(sessionId)
            if (session != null) {
                _lastFinishedSession.value = session
                onLoaded(session)
            }
        }
    }

    fun retakeQuizFromSession(session: QuizSession, onRetakeReady: () -> Unit) {
        viewModelScope.launch {
            val raw = session.deckIdsJson.removePrefix("[").removeSuffix("]")
            val ids = raw.split(",").mapNotNull { it.trim().toLongOrNull() }
            if (ids.size == 1) {
                startSingleDeckQuiz(ids.first(), onRetakeReady)
            } else if (ids.isNotEmpty()) {
                _selectedDeckIdsForQuiz.value = ids.toSet()
                if (session.combinationMode == "STITCHED") {
                    prepareQiezkaStitchedOrder {
                        startStitchedQuiz(onRetakeReady)
                    }
                } else {
                    recalculateDefaultDistribution(ids.toSet())
                    startCombinedPoolQuiz(session.totalQuestions, onRetakeReady)
                }
            }
        }
    }

    private fun isAiAppKeywords(packageName: String?, label: String?): Boolean {
        if (packageName != null) {
            val lower = packageName.lowercase(Locale.US)
            if (lower.contains("chatgpt") || lower.contains("bard") || lower.contains("gemini") ||
                lower.contains("claude") || lower.contains("copilot") || lower.contains("perplexity") ||
                lower.contains("deepseek") || lower.contains(".poe") || lower.contains("grok")) {
                return true
            }
        }
        if (label != null) {
            val lowerLabel = label.lowercase(Locale.US)
            if (lowerLabel.contains("chatgpt") || lowerLabel.contains("gemini") || lowerLabel.contains("claude") ||
                lowerLabel.contains("copilot") || lowerLabel.contains("perplexity") || lowerLabel.contains("deepseek") ||
                lowerLabel.contains("ai assistant") || lowerLabel.contains("poe") || lowerLabel.contains("grok")) {
                return true
            }
        }
        return false
    }

    private fun isKeyboardAppKeywords(packageName: String?): Boolean {
        if (packageName == null) return false
        val lower = packageName.lowercase(Locale.US)
        return lower.contains("inputmethod") ||
            lower.contains("honeyboard") ||
            lower.contains("keyboard") ||
            lower.contains("gboard") ||
            lower.contains("swiftkey") ||
            lower.contains(".ime")
    }

    private fun isHiddenInfrastructureApp(packageName: String?, label: String?): Boolean {
        if (packageName != null) {
            val lower = packageName.lowercase(Locale.US)
            if (lower.contains("cameraextension") ||
                lower.contains("extensionproxy") ||
                lower.contains("lenslauncher") ||
                lower.contains("aperturelenslauncher") ||
                lower.contains("opensourcemusicplayer") ||
                lower.contains("androidopensourcemusicplayer") ||
                lower.contains("packageinstaller") ||
                lower.contains(".installer") ||
                lower == "com.android.vending" ||
                lower == "com.google.android.feedback" ||
                lower == "com.google.android.gms") {
                return true
            }
        }
        if (label != null) {
            val lowerLabel = label.lowercase(Locale.US).replace(" ", "")
            if (lowerLabel.contains("cameraextensionproxy") ||
                lowerLabel.contains("cameraextension") ||
                lowerLabel.contains("lenslauncher") ||
                lowerLabel.contains("aperturelenslauncher") ||
                lowerLabel.contains("aperaturelenslauncher") ||
                lowerLabel.contains("androidopensourcemusicplayer") ||
                lowerLabel.contains("opensourcemusicplayer") ||
                lowerLabel.contains("packageinstaller") ||
                lowerLabel.contains("googleplaystore") ||
                lowerLabel.contains("playstore")) {
                return true
            }
        }
        return false
    }

    private fun isEligibleApp(context: Context, pm: PackageManager, pkg: String): Boolean {
        if (pkg == context.packageName) return false
        if (AppClassifier.isSettingsOrDeviceManager(pkg, null) ||
            AppClassifier.isStage1Bloat(pkg, null) ||
            AppClassifier.isForbiddenDistraction(context, pkg) ||
            AppClassifier.isPwaOrWebApk(pkg) ||
            KnownDistracting.isKnownDistracting(pkg)) {
            return false
        }
        val isBaselineSafe = KnownSafe.BASELINE_SAFE_PACKAGES.contains(pkg)
        if (!isBaselineSafe && (UnifiedPolicyRegistry.isPackageRegisteredInAnyService(pkg) || isAiAppKeywords(pkg, null))) {
            return false
        }
        if (isKeyboardAppKeywords(pkg)) {
            return false
        }

        val appInfo = try {
            pm.getApplicationInfo(pkg, 0)
        } catch (_: Exception) {
            return false
        }
        val appLabel = pm.getApplicationLabel(appInfo).toString()

        if (isHiddenInfrastructureApp(pkg, appLabel)) {
            return false
        }
        if (AppClassifier.isSettingsOrDeviceManager(pkg, appLabel) ||
            AppClassifier.isStage1Bloat(pkg, appLabel) ||
            AppClassifier.isPwaOrWebApk(pkg) ||
            KnownDistracting.isKnownDistracting(pkg, appLabel) ||
            (!isBaselineSafe && isAiAppKeywords(pkg, appLabel))) {
            return false
        }

        if (KnownSafe.isLauncherApp(context, pkg)) {
            return false
        }

        if (AppClassifier.isPassedStage3SystemAllow(appInfo, pkg, appLabel)) {
            return false
        }

        val isHardcoded = KnownSafe.isHardcodedApp(context, appInfo, pkg, appLabel)
        if (!isHardcoded && !isBaselineSafe && AppClassifier.isPackageBlocked(context, pkg, null)) {
            return false
        }

        return true
    }

    fun loadAllowedApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pm = context.packageManager
            val appPrefs = context.getSharedPreferences("siren_app_prefs", Context.MODE_PRIVATE)
            val isInitialized = appPrefs.getBoolean("allowed_apps_initialized", false)

            val activePackages: Set<String> = if (!isInitialized) {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val launchable = pm.queryIntentActivities(intent, 0)
                val initialEligible = mutableSetOf<String>()
                for (r in launchable) {
                    val pkg = r.activityInfo?.packageName ?: continue
                    if (isEligibleApp(context, pm, pkg)) {
                        initialEligible.add(pkg)
                    }
                }
                appPrefs.edit()
                    .putStringSet("user_allowed_apps", initialEligible)
                    .putBoolean("allowed_apps_initialized", true)
                    .apply()
                val lockPrefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
                lockPrefs.edit().putStringSet("whitelist", initialEligible).apply()
                AppClassifier.clearCache()
                initialEligible
            } else {
                appPrefs.getStringSet("user_allowed_apps", emptySet()) ?: emptySet()
            }

            val items = activePackages.mapNotNull { pkg ->
                try {
                    val ai = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(ai).toString()
                    AllowedAppItem(packageName = pkg, name = label, isWhitelisted = true)
                } catch (_: Exception) {
                    null
                }
            }.sortedBy { it.name.lowercase() }

            _allowedApps.value = items
        }
    }

    fun loadEligibleApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pm = context.packageManager
            val appPrefs = context.getSharedPreferences("siren_app_prefs", Context.MODE_PRIVATE)
            val currentAllowed = appPrefs.getStringSet("user_allowed_apps", emptySet()) ?: emptySet()
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val launchable = pm.queryIntentActivities(intent, 0)
            val seen = mutableSetOf<String>()
            val list = mutableListOf<AllowedAppItem>()

            for (r in launchable) {
                val pkg = r.activityInfo?.packageName ?: continue
                if (seen.contains(pkg)) continue
                if (!isEligibleApp(context, pm, pkg)) continue

                try {
                    val ai = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(ai).toString()
                    seen.add(pkg)
                    list.add(
                        AllowedAppItem(
                            packageName = pkg,
                            name = label,
                            isWhitelisted = currentAllowed.contains(pkg)
                        )
                    )
                } catch (_: Exception) {}
            }
            _eligibleApps.value = list.sortedBy { it.name.lowercase() }
        }
    }

    fun updateAllowedApps(newAllowed: Set<String>) {
        val context = getApplication<Application>()
        val appPrefs = context.getSharedPreferences("siren_app_prefs", Context.MODE_PRIVATE)
        appPrefs.edit()
            .putStringSet("user_allowed_apps", newAllowed)
            .putBoolean("allowed_apps_initialized", true)
            .apply()
        val lockPrefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        lockPrefs.edit().putStringSet("whitelist", newAllowed).apply()
        AppClassifier.clearCache()
        loadAllowedApps()
        loadEligibleApps()
    }

    fun toggleAllowedApp(packageName: String) {
        val context = getApplication<Application>()
        val appPrefs = context.getSharedPreferences("siren_app_prefs", Context.MODE_PRIVATE)
        val current = appPrefs.getStringSet("user_allowed_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        updateAllowedApps(current)
    }

    fun clearLockdownSession() {
        try {
            val context = getApplication<Application>()
            LockPlugin.clearLockdownState(context)
            refreshLockdownAndServiceState()
        } catch (_: Exception) {}
    }

    fun startConsequenceSession(scheduleId: String?) {
        try {
            val context = getApplication<Application>()
            val lockPrefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
            val whitelist = lockPrefs.getStringSet("whitelist", emptySet()) ?: emptySet()
            LockPlugin.startConsequenceState(context, scheduleId, whitelist)
            refreshLockdownAndServiceState()
        } catch (_: Exception) {}
    }

    fun refreshLockdownAndServiceState() {
        val context = getApplication<Application>()
        val lockPrefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        _isLockdownActive.value = lockPrefs.getBoolean("lockdown_active", false)
        _isConsequenceActive.value = lockPrefs.getBoolean("consequence_active", false)
        _operatingMode.value = lockPrefs.getString("operating_mode", "safemode") ?: "safemode"
        _webProtectionMode.value = lockPrefs.getString("web_protection_mode", "accessibility") ?: "accessibility"
        _isVpnRunning.value = LocalDnsVpnService.isRunning

        val activeServices = lockPrefs.getStringSet("active_unified_services", emptySet()) ?: emptySet()
        val set = HashSet(activeServices)
        if (lockPrefs.getBoolean("allow_youtube", false)) {
            set.add("youtube")
        }
        _activeUnifiedServices.value = set

        var a11y = false
        try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (enabledServices != null && enabledServices.contains("LockAccessibilityService")) {
                a11y = true
            }
        } catch (_: Exception) {}
        _isAccessibilityEnabled.value = a11y
        syncPastDueSchedules()
    }

    fun setOperatingMode(mode: String): Boolean {
        val normalized = mode.lowercase(Locale.US).trim()
        val context = getApplication<Application>()
        val lockPrefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        val isLockActive = lockPrefs.getBoolean("lock_active", false)
        val isConsequence = lockPrefs.getBoolean("consequence_active", false)
        val isPolicyLocked = isLockActive || isConsequence || _isLockdownActive.value || _isConsequenceActive.value ||
            _failedFocusSessions.value.isNotEmpty() || _focusSchedules.value.any { it.isRescheduled }
        if (isPolicyLocked) {
            return false
        }
        if (normalized == "safemode") {
            val allSchedulesToCheck = mutableListOf<FocusScheduleItem>()
            allSchedulesToCheck.addAll(_focusSchedules.value)
            val prefs = context.getSharedPreferences(ScheduleManager.PREFS_NAME, Context.MODE_PRIVATE)
            val diskJson = prefs.getString("schedules_json", null)
            if (!diskJson.isNullOrBlank()) {
                try {
                    val arr = JSONArray(diskJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id", "")
                        if (allSchedulesToCheck.none { it.id == id }) {
                            allSchedulesToCheck.add(
                                FocusScheduleItem(
                                    id = id,
                                    title = obj.optString("title", "Focus Session"),
                                    activationTime = obj.optString("activationTime", "09:00"),
                                    durationMinutes = obj.optInt("durationMinutes", 25),
                                    isActive = obj.optBoolean("isActive", true),
                                    isRescheduled = obj.optBoolean("isRescheduled", false)
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }

            for (schedule in allSchedulesToCheck) {
                val parts = schedule.activationTime.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: continue
                val m = parts.getOrNull(1)?.toIntOrNull() ?: continue
                val isOutsideOperatingHours = h in 3..18
                val start = if (h >= 19) h * 60 + m else 1440 + h * 60 + m
                val isExceedingCurfew = start + schedule.durationMinutes > 1620
                if (isOutsideOperatingHours || isExceedingCurfew) {
                    return false
                }
            }

        }
        lockPrefs.edit().putString("operating_mode", normalized).apply()
        _operatingMode.value = normalized
        return true
    }

    fun toggleUnifiedService(serviceId: String, allowed: Boolean) {
        val context = getApplication<Application>()
        val lockPrefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        val isLockActive = lockPrefs.getBoolean("lock_active", false)
        val isConsequence = lockPrefs.getBoolean("consequence_active", false)
        val isPolicyLocked = isLockActive || isConsequence || _isLockdownActive.value || _isConsequenceActive.value ||
            _failedFocusSessions.value.isNotEmpty() || _focusSchedules.value.any { it.isRescheduled }
        if (isPolicyLocked) {
            return
        }
        val currentServices = lockPrefs.getStringSet("active_unified_services", emptySet()) ?: emptySet()
        val activeServices = HashSet(currentServices)
        val key = serviceId.lowercase(Locale.US).trim()
        if (allowed) {
            activeServices.add(key)
        } else {
            activeServices.remove(key)
        }
        val editor = lockPrefs.edit()
        editor.putStringSet("active_unified_services", activeServices)
        if (key == "youtube") {
            editor.putBoolean("allow_youtube", allowed)
        }
        val domains = UnifiedPolicyRegistry.getDomainsForServices(activeServices)
        editor.putStringSet("allowed_domains", domains)
        editor.apply()
        AppClassifier.clearCache()
        WebClassifier.clearCache()
        _activeUnifiedServices.value = activeServices
        loadAllowedApps()
        loadEligibleApps()
    }

    fun setWebProtectionMode(mode: String, onVpnPermissionNeeded: ((Intent) -> Unit)? = null) {
        val context = getApplication<Application>()
        val lockPrefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        val isLockActive = lockPrefs.getBoolean("lock_active", false)
        val isConsequence = lockPrefs.getBoolean("consequence_active", false)
        val isPolicyLocked = isLockActive || isConsequence || _isLockdownActive.value || _isConsequenceActive.value ||
            _failedFocusSessions.value.isNotEmpty() || _focusSchedules.value.any { it.isRescheduled }
        if (isPolicyLocked) {
            return
        }
        if (mode == "dual_hybrid") {
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent != null && onVpnPermissionNeeded != null) {
                onVpnPermissionNeeded(vpnIntent)
                return
            }
        }
        lockPrefs.edit().putString("web_protection_mode", mode).apply()
        _webProtectionMode.value = mode
        if (_isLockdownActive.value) {
            if (mode == "dual_hybrid") {
                LocalDnsVpnService.startVpn(context)
            } else {
                LocalDnsVpnService.stopVpn(context)
            }
        }
        _isVpnRunning.value = LocalDnsVpnService.isRunning
    }

    fun toggleSimulatedLockdown(onVpnPermissionNeeded: ((Intent) -> Unit)? = null) {
        val context = getApplication<Application>()
        val lockPrefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        if (_isLockdownActive.value) {
            LockPlugin.clearLockdownState(context)
            _isLockdownActive.value = false
            _isVpnRunning.value = false
        } else {
            val webMode = lockPrefs.getString("web_protection_mode", "accessibility") ?: "accessibility"
            if (webMode == "dual_hybrid") {
                val vpnIntent = VpnService.prepare(context)
                if (vpnIntent != null && onVpnPermissionNeeded != null) {
                    onVpnPermissionNeeded(vpnIntent)
                    return
                }
            }
            val appPrefs = context.getSharedPreferences("siren_app_prefs", Context.MODE_PRIVATE)
            val currentAllowed = appPrefs.getStringSet("user_allowed_apps", emptySet()) ?: emptySet()
            lockPrefs.edit()
                .putBoolean("lockdown_active", true)
                .remove("lock_end_time")
                .putStringSet("whitelist", currentAllowed)
                .apply()
            AppClassifier.clearCache()
            WebClassifier.clearCache()
            if (webMode == "dual_hybrid") {
                LocalDnsVpnService.startVpn(context)
            }
            _isLockdownActive.value = true
            _isVpnRunning.value = LocalDnsVpnService.isRunning
        }
    }

    fun loadFocusSchedules() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences(ScheduleManager.PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString("schedules_json", null)
        if (json.isNullOrBlank()) {
            _focusSchedules.value = emptyList()
            return
        }
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<FocusScheduleItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val deckIds = mutableListOf<Long>()
                val deckIdsArr = obj.optJSONArray("selectedDeckIds")
                if (deckIdsArr != null) {
                    for (d in 0 until deckIdsArr.length()) {
                        deckIds.add(deckIdsArr.getLong(d))
                    }
                }
                list.add(
                    FocusScheduleItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = obj.optString("title", "Focus Session"),
                        activationTime = obj.optString("activationTime", "09:00"),
                        durationMinutes = obj.optInt("durationMinutes", 25),
                        selectedDeckIds = deckIds,
                        deckNames = obj.optString("deckNames", ""),
                        isActive = obj.optBoolean("isActive", true),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        isRescheduled = obj.optBoolean("isRescheduled", false),
                        isAutoScheduled = obj.optBoolean("isAutoScheduled", false)
                    )
                )
            }
            _focusSchedules.value = list
            syncPastDueSchedules()
        } catch (_: Exception) {
            _focusSchedules.value = emptyList()
        }
    }

    fun createFocusSchedule(
        title: String,
        selectedDeckIds: List<Long>,
        deckNames: String,
        activationTime: String,
        durationMinutes: Int,
        isRescheduled: Boolean = false,
        scheduleId: String? = null,
        isAutoScheduled: Boolean = false
    ): Boolean {
        val parts = activationTime.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return false
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return false
        val isHardcore = _operatingMode.value.equals("hardcore", ignoreCase = true)
        if (!isHardcore && (h in 3..18)) {
            return false
        }
        if (!isHardcore) {
            val normalizedStart = if (h >= 19) h * 60 + m else 1440 + h * 60 + m
            if (normalizedStart + durationMinutes > 1620) {
                return false
            }
        }
        val proposedStart = h * 60 + m
        val proposedEnd = proposedStart + durationMinutes

        val current = _focusSchedules.value.toMutableList()
        val hasOverlap = current.any { existing ->
            if (!existing.isActive) return@any false
            val exParts = existing.activationTime.split(":")
            val exH = exParts.getOrNull(0)?.toIntOrNull() ?: return@any false
            val exM = exParts.getOrNull(1)?.toIntOrNull() ?: return@any false
            val exStart = exH * 60 + exM
            val exEnd = exStart + existing.durationMinutes

            listOf(-1440, 0, 1440).any { offset ->
                val exStartOff = exStart + offset
                val exEndOff = exEnd + offset
                proposedStart < exEndOff + 30 && proposedEnd + 30 > exStartOff
            }
        }
        if (hasOverlap) {
            return false
        }

        val newItem = FocusScheduleItem(
            id = scheduleId ?: java.util.UUID.randomUUID().toString(),
            title = title,
            activationTime = activationTime,
            durationMinutes = durationMinutes,
            selectedDeckIds = selectedDeckIds,
            deckNames = deckNames,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            isRescheduled = isRescheduled,
            isAutoScheduled = isAutoScheduled
        )
        current.add(newItem)
        saveFocusSchedules(current)
        return true
    }

    fun loadAutoQuizConfig() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        _autoQuizEnabled.value = prefs.getBoolean("auto_quiz_enabled", false)
        _autoQuizMode.value = prefs.getString("auto_quiz_mode", "usage_30") ?: "usage_30"
        val dId = prefs.getLong("auto_quiz_deck_id", -1L)
        _autoQuizSelectedDeckId.value = if (dId == -1L) null else dId
    }

    private fun saveAutoQuizConfig() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("auto_quiz_enabled", _autoQuizEnabled.value)
            .putString("auto_quiz_mode", _autoQuizMode.value)
            .putLong("auto_quiz_deck_id", _autoQuizSelectedDeckId.value ?: -1L)
            .apply()
    }

    fun setAutoQuizEnabled(enabled: Boolean) {
        _autoQuizEnabled.value = enabled
        saveAutoQuizConfig()
    }

    fun setAutoQuizMode(mode: String) {
        _autoQuizMode.value = mode
        saveAutoQuizConfig()
    }

    fun setAutoQuizDeckId(deckId: Long?) {
        _autoQuizSelectedDeckId.value = deckId
        saveAutoQuizConfig()
    }

    fun startAutoQuiz(onStarted: (String) -> Unit = {}) {
        viewModelScope.launch {
            clearActiveQuiz()
            var availableDecks = allDecks.value
            if (availableDecks.isEmpty()) {
                availableDecks = repository.allDecks.first()
            }
            val chosenDeckId = _autoQuizSelectedDeckId.value
            val targetDeckId = if (chosenDeckId != null) {
                chosenDeckId
            } else {
                if (availableDecks.isEmpty()) -1L
                else {
                    val stats = overallStats.value
                    val lowestDeck = availableDecks.minByOrNull { deck ->
                        val perf = stats.deckPerformances.find { it.deckId == deck.id }
                        perf?.accuracy ?: 0
                    } ?: availableDecks.first()
                    lowestDeck.id
                }
            }
            val autoQuizId = "auto_quiz_${System.currentTimeMillis()}"
            val context = getApplication<Application>()
            val prefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("auto_quiz_in_progress", true)
                .putString("active_auto_quiz_id", autoQuizId)
                .apply()
            startFocusSessionQuiz(if (targetDeckId != -1L) listOf(targetDeckId) else emptyList()) {
                onStarted(autoQuizId)
            }
        }
    }

    fun loadAutoScheduleConfig() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        _autoScheduleEnabled.value = prefs.getBoolean("auto_schedule_enabled", false)
        val dId = prefs.getLong("auto_schedule_deck_id", -1L)
        _autoScheduleSelectedDeckId.value = if (dId == -1L) null else dId
        val dur = prefs.getInt("auto_schedule_duration", 0)
        _autoScheduleSelectedDuration.value = if (dur == 0) null else dur
        _autoScheduleDeletedCount.value = getAutoScheduleDeletedCountForToday()
    }

    private fun saveAutoScheduleConfig() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("auto_schedule_enabled", _autoScheduleEnabled.value)
            .putLong("auto_schedule_deck_id", _autoScheduleSelectedDeckId.value ?: -1L)
            .putInt("auto_schedule_duration", _autoScheduleSelectedDuration.value ?: 0)
            .apply()
    }

    fun setAutoScheduleEnabled(enabled: Boolean) {
        _autoScheduleEnabled.value = enabled
        saveAutoScheduleConfig()
        if (enabled) {
            armAutoSchedule()
        }
    }

    fun setAutoScheduleDeckId(deckId: Long?) {
        _autoScheduleSelectedDeckId.value = deckId
        saveAutoScheduleConfig()
    }

    fun setAutoScheduleDuration(duration: Int?) {
        _autoScheduleSelectedDuration.value = duration
        saveAutoScheduleConfig()
    }

    fun getTodayDateString(): String {
        val todayCal = Calendar.getInstance()
        return String.format(Locale.US, "%04d-%02d-%02d",
            todayCal.get(Calendar.YEAR),
            todayCal.get(Calendar.MONTH) + 1,
            todayCal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun isAutoScheduleMetForToday(): Boolean {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        val todayStr = getTodayDateString()
        return prefs.getBoolean("auto_schedule_activated_date_$todayStr", false)
    }

    fun markAutoScheduleActivatedForToday() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        val todayStr = getTodayDateString()
        prefs.edit().putBoolean("auto_schedule_activated_date_$todayStr", true).apply()
    }

    fun getAutoScheduleDeletedCountForToday(): Int {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        val todayStr = getTodayDateString()
        return prefs.getInt("auto_schedule_deleted_count_$todayStr", 0)
    }

    fun incrementAutoScheduleDeletedCountForToday() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        val todayStr = getTodayDateString()
        val next = prefs.getInt("auto_schedule_deleted_count_$todayStr", 0) + 1
        prefs.edit().putInt("auto_schedule_deleted_count_$todayStr", next).apply()
        _autoScheduleDeletedCount.value = next
    }

    private fun checkAutoScheduleDailyStatus() {
        if (!_autoScheduleEnabled.value) return
        val now = System.currentTimeMillis()
        val currentSchedules = _focusSchedules.value

        val activeAuto = currentSchedules.find { sched ->
            if (!sched.isActive) return@find false
            if (!sched.isAutoScheduled && !sched.title.startsWith("Auto:")) return@find false
            val parts = sched.activationTime.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: return@find false
            val m = parts.getOrNull(1)?.toIntOrNull() ?: return@find false
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val durationMs = sched.durationMinutes * 60_000L
            if (cal.timeInMillis + durationMs <= sched.createdAt) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            val startMs = cal.timeInMillis
            val endMs = startMs + durationMs
            now in startMs until endMs
        }
        if (activeAuto != null) {
            markAutoScheduleActivatedForToday()
        }

        if (!isAutoScheduleMetForToday()) {
            val hasPending = currentSchedules.any {
                it.isActive && (it.isAutoScheduled || it.title.startsWith("Auto:"))
            }
            if (!hasPending) {
                armAutoSchedule()
            }
        }
    }

    fun armAutoSchedule(): Boolean {
        if (!_autoScheduleEnabled.value) {
            return false
        }
        if (isAutoScheduleMetForToday()) {
            return false
        }
        val hasPendingAuto = _focusSchedules.value.any {
            it.isActive && (it.isAutoScheduled || it.title.startsWith("Auto:"))
        }
        if (hasPendingAuto) {
            return false
        }

        val availableDecks = allDecks.value
        if (availableDecks.isEmpty()) {
            return false
        }

        val chosenDeckId = _autoScheduleSelectedDeckId.value
        val targetDeck = if (chosenDeckId != null) {
            availableDecks.find { it.id == chosenDeckId } ?: availableDecks.first()
        } else {
            val stats = overallStats.value
            availableDecks.minByOrNull { deck ->
                val perf = stats.deckPerformances.find { it.deckId == deck.id }
                perf?.accuracy ?: 0
            } ?: availableDecks.shuffled().first()
        }

        val chosenDuration = _autoScheduleSelectedDuration.value
        val finalDuration = if (chosenDuration != null) {
            chosenDuration.coerceIn(10, 90)
        } else {
            25
        }

        val now = System.currentTimeMillis()
        val isHardcore = _operatingMode.value.equals("hardcore", ignoreCase = true)

        val candidateTimes = mutableListOf<String>()
        if (isHardcore) {
            val offsets = (30..1440 step 5).toList().shuffled()
            for (offset in offsets) {
                val targetMs = now + offset * 60_000L
                val cal = Calendar.getInstance().apply { timeInMillis = targetMs }
                val h = cal.get(Calendar.HOUR_OF_DAY)
                val m = cal.get(Calendar.MINUTE)
                candidateTimes.add(String.format(Locale.US, "%02d:%02d", h, m))
            }
        } else {
            val nowCal = Calendar.getInstance().apply { timeInMillis = now }
            val curH = nowCal.get(Calendar.HOUR_OF_DAY)
            val curM = nowCal.get(Calendar.MINUTE)
            val normalizedCurMins = if (curH in 0..2) (curH + 24) * 60 + curM else curH * 60 + curM
            val isCurrentlyInSafemodeWindow = (curH >= 19 || curH < 3)

            val candidateSlots = mutableListOf<String>()
            if (isCurrentlyInSafemodeWindow) {
                val minLeadSlot = normalizedCurMins + 30
                for (slot in 1140..1620 step 5) {
                    if (slot >= minLeadSlot && slot + finalDuration <= 1620) {
                        val normM = slot % 1440
                        candidateSlots.add(String.format(Locale.US, "%02d:%02d", normM / 60, normM % 60))
                    }
                }
            }
            for (slot in 1140..1620 step 5) {
                if (slot + finalDuration <= 1620) {
                    val normM = slot % 1440
                    val timeStr = String.format(Locale.US, "%02d:%02d", normM / 60, normM % 60)
                    if (!candidateSlots.contains(timeStr)) {
                        candidateSlots.add(timeStr)
                    }
                }
            }
            candidateTimes.addAll(candidateSlots.shuffled())
        }

        for (candidateTime in candidateTimes) {
            val created = createFocusSchedule(
                title = "Auto: ${targetDeck.title}",
                selectedDeckIds = listOf(targetDeck.id),
                deckNames = targetDeck.title,
                activationTime = candidateTime,
                durationMinutes = finalDuration,
                isAutoScheduled = true
            )
            if (created) {
                return true
            }
        }

        return false
    }

    fun deleteFocusSchedule(id: String) {
        val target = _focusSchedules.value.find { it.id == id } ?: return
        if (target.isRescheduled) {
            val currentFailed = _failedFocusSessions.value.toMutableList()
            currentFailed.removeAll { it.id == target.id }
            currentFailed.add(
                0,
                FailedFocusSessionItem(
                    id = target.id,
                    title = target.title,
                    selectedDeckIds = target.selectedDeckIds,
                    deckNames = target.deckNames,
                    durationMinutes = target.durationMinutes,
                    failedTimestamp = System.currentTimeMillis(),
                    earnedPoints = 0f,
                    maxPoints = 0f
                )
            )
            saveFailedFocusSessions(currentFailed)
            val updated = _focusSchedules.value.filter { it.id != id }
            saveFocusSchedules(updated)
            startConsequenceSession(target.id)
            return
        }

        if (target.isAutoScheduled || target.title.startsWith("Auto:")) {
            if (getAutoScheduleDeletedCountForToday() >= 3) {
                return
            }
            incrementAutoScheduleDeletedCountForToday()
            val updated = _focusSchedules.value.filter { it.id != id }
            saveFocusSchedules(updated)
            armAutoSchedule()
            return
        }

        val updated = _focusSchedules.value.filter { it.id != id }
        saveFocusSchedules(updated)
    }

    private fun saveFocusSchedules(schedules: List<FocusScheduleItem>) {
        val context = getApplication<Application>()
        val arr = JSONArray()
        for (item in schedules) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("activationTime", item.activationTime)
            obj.put("durationMinutes", item.durationMinutes)
            val idsArr = JSONArray()
            for (id in item.selectedDeckIds) {
                idsArr.put(id)
            }
            obj.put("selectedDeckIds", idsArr)
            obj.put("deckNames", item.deckNames)
            obj.put("isActive", item.isActive)
            obj.put("createdAt", item.createdAt)
            obj.put("isRescheduled", item.isRescheduled)
            obj.put("isAutoScheduled", item.isAutoScheduled)
            arr.put(obj)
        }
        val appPrefs = context.getSharedPreferences("siren_app_prefs", Context.MODE_PRIVATE)
        val whitelist = appPrefs.getStringSet("user_allowed_apps", emptySet()) ?: emptySet()
        ScheduleManager.syncSchedules(context, arr.toString(), whitelist)
        _focusSchedules.value = schedules
    }

    fun loadFailedFocusSessions() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences(ScheduleManager.PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString("failed_sessions_json", null)
        if (json.isNullOrBlank()) {
            _failedFocusSessions.value = emptyList()
            return
        }
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<FailedFocusSessionItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val deckIds = mutableListOf<Long>()
                val deckIdsArr = obj.optJSONArray("selectedDeckIds")
                if (deckIdsArr != null) {
                    for (d in 0 until deckIdsArr.length()) {
                        deckIds.add(deckIdsArr.getLong(d))
                    }
                }
                list.add(
                    FailedFocusSessionItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = obj.optString("title", "Failed Session"),
                        selectedDeckIds = deckIds,
                        deckNames = obj.optString("deckNames", ""),
                        durationMinutes = obj.optInt("durationMinutes", 25),
                        failedTimestamp = obj.optLong("failedTimestamp", System.currentTimeMillis()),
                        earnedPoints = obj.optDouble("earnedPoints", 0.0).toFloat(),
                        maxPoints = obj.optDouble("maxPoints", 0.0).toFloat()
                    )
                )
            }
            _failedFocusSessions.value = list
        } catch (_: Exception) {
            _failedFocusSessions.value = emptyList()
        }
    }

    private fun saveFailedFocusSessions(list: List<FailedFocusSessionItem>) {
        val context = getApplication<Application>()
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            val idsArr = JSONArray()
            for (id in item.selectedDeckIds) {
                idsArr.put(id)
            }
            obj.put("selectedDeckIds", idsArr)
            obj.put("deckNames", item.deckNames)
            obj.put("durationMinutes", item.durationMinutes)
            obj.put("failedTimestamp", item.failedTimestamp)
            obj.put("earnedPoints", item.earnedPoints.toDouble())
            obj.put("maxPoints", item.maxPoints.toDouble())
            arr.put(obj)
        }
        val prefs = context.getSharedPreferences(ScheduleManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("failed_sessions_json", arr.toString()).apply()
        _failedFocusSessions.value = list
    }

    fun completeFocusSchedule(scheduleId: String?, passed: Boolean, maxPoints: Float = 0f, earnedPoints: Float = 0f) {
        if (scheduleId != null && scheduleId.startsWith("auto_quiz")) {
            val context = getApplication<Application>()
            val prefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
            prefs.edit()
                .remove("auto_quiz_in_progress")
                .remove("active_auto_quiz_id")
                .putLong("auto_quiz_accumulated_seconds", 0L)
                .apply()
            if (!passed) {
                val currentFailed = _failedFocusSessions.value.toMutableList()
                currentFailed.removeAll { it.id == scheduleId }
                val targetDeck = _autoQuizSelectedDeckId.value?.let { dId -> allDecks.value.find { it.id == dId } }
                val quizTitle = if (targetDeck != null) "AutoQuiz: ${targetDeck.title}" else "AutoQuiz: Pop Quiz"
                currentFailed.add(
                    0,
                    FailedFocusSessionItem(
                        id = scheduleId,
                        title = quizTitle,
                        selectedDeckIds = if (targetDeck != null) listOf(targetDeck.id) else emptyList(),
                        deckNames = targetDeck?.title ?: "AutoQuiz",
                        durationMinutes = 5,
                        failedTimestamp = System.currentTimeMillis(),
                        earnedPoints = earnedPoints,
                        maxPoints = maxPoints
                    )
                )
                saveFailedFocusSessions(currentFailed)
                startConsequenceSession(scheduleId)
            } else {
                val currentFailed = _failedFocusSessions.value.toMutableList()
                currentFailed.removeAll { it.id == scheduleId }
                saveFailedFocusSessions(currentFailed)
                val hasOtherRescheduled = _focusSchedules.value.any { it.isRescheduled }
                if (currentFailed.isEmpty() && !hasOtherRescheduled) {
                    clearConsequenceSession()
                } else if (currentFailed.isNotEmpty()) {
                    startConsequenceSession(currentFailed.first().id)
                }
                clearLockdownSession()
            }
            return
        }
        val target = _focusSchedules.value.find { it.id == scheduleId }
            ?: _focusSchedules.value.firstOrNull { it.isActive }
            ?: _focusSchedules.value.firstOrNull()

        if (target != null) {
            val updated = _focusSchedules.value.filter { it.id != target.id }
            saveFocusSchedules(updated)
            if (!passed) {
                val currentFailed = _failedFocusSessions.value.toMutableList()
                currentFailed.removeAll { it.id == target.id }
                currentFailed.add(
                    0,
                    FailedFocusSessionItem(
                        id = target.id,
                        title = target.title,
                        selectedDeckIds = target.selectedDeckIds,
                        deckNames = target.deckNames,
                        durationMinutes = target.durationMinutes,
                        failedTimestamp = System.currentTimeMillis(),
                        earnedPoints = earnedPoints,
                        maxPoints = maxPoints
                    )
                )
                saveFailedFocusSessions(currentFailed)
                startConsequenceSession(target.id)
            } else {
                val currentFailed = _failedFocusSessions.value.toMutableList()
                currentFailed.removeAll { it.id == target.id }
                saveFailedFocusSessions(currentFailed)
                val hasOtherRescheduled = updated.any { it.isRescheduled }
                if (currentFailed.isEmpty() && !hasOtherRescheduled) {
                    clearConsequenceSession()
                } else if (currentFailed.isNotEmpty()) {
                    startConsequenceSession(currentFailed.first().id)
                } else {
                    val nextRescheduled = updated.firstOrNull { it.isRescheduled }
                    startConsequenceSession(nextRescheduled?.id)
                }
            }
        } else if (scheduleId != null) {
            val updated = _focusSchedules.value.filter { it.id != scheduleId }
            saveFocusSchedules(updated)
        }
    }

    fun isFocusSessionTimerActive(scheduleId: String?): Boolean {
        val sched = _focusSchedules.value.find { it.id == scheduleId }
            ?: _focusSchedules.value.firstOrNull { it.isActive }
            ?: return false
        val context = getApplication<Application>()
        val lockPrefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        val storedLockEnd = lockPrefs.getLong("lock_end_time", 0L)
        val now = System.currentTimeMillis()
        if (storedLockEnd > now) {
            return true
        }
        val parts = sched.activationTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val totalDurationMs = sched.durationMinutes * 60L * 1000L
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis + totalDurationMs < now - 60_000L) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        val targetEndMs = cal.timeInMillis + totalDurationMs
        return now < targetEndMs
    }

    fun clearActiveQuiz() {
        _activeQuizQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        _quizScore.value = 0
        _quizStreak.value = 0
        _quizEarnedPoints.value = 0f
        _quizFinishedSessionId.value = null
    }

    fun handleSessionTimeout(scheduleId: String?) {
        val target = _focusSchedules.value.find { it.id == scheduleId }
            ?: _focusSchedules.value.firstOrNull { it.isActive }
            ?: _focusSchedules.value.firstOrNull()

        if (target != null) {
            val currentFailed = _failedFocusSessions.value.toMutableList()
            currentFailed.removeAll { it.id == target.id }
            currentFailed.add(
                0,
                FailedFocusSessionItem(
                    id = target.id,
                    title = target.title,
                    selectedDeckIds = target.selectedDeckIds,
                    deckNames = target.deckNames,
                    durationMinutes = target.durationMinutes,
                    failedTimestamp = System.currentTimeMillis(),
                    earnedPoints = 0f,
                    maxPoints = 0f
                )
            )
            saveFailedFocusSessions(currentFailed)
            val updated = _focusSchedules.value.filter { it.id != target.id }
            saveFocusSchedules(updated)
            startConsequenceSession(target.id)
        } else if (scheduleId != null) {
            val updated = _focusSchedules.value.filter { it.id != scheduleId }
            saveFocusSchedules(updated)
            startConsequenceSession(scheduleId)
        }
    }

    fun syncPastDueSchedules() {
        val now = System.currentTimeMillis()
        val currentSchedules = _focusSchedules.value.toMutableList()
        val expiredToFail = mutableListOf<FocusScheduleItem>()
        val isCurrentlyInQuiz = _activeQuizQuestions.value.isNotEmpty()

        if (isCurrentlyInQuiz) return

        for (schedule in currentSchedules) {
            val parts = schedule.activationTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: continue
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: continue
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val durationMs = schedule.durationMinutes * 60L * 1000L
            if (cal.timeInMillis + durationMs <= schedule.createdAt) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            val startMs = cal.timeInMillis
            val endMs = startMs + durationMs

            if (now >= endMs) {
                expiredToFail.add(schedule)
            }
        }

        if (expiredToFail.isNotEmpty()) {
            val currentFailed = _failedFocusSessions.value.toMutableList()
            for (item in expiredToFail) {
                currentSchedules.removeAll { it.id == item.id }
                if (currentFailed.none { it.id == item.id }) {
                    currentFailed.add(
                        0,
                        FailedFocusSessionItem(
                            id = item.id,
                            title = item.title,
                            selectedDeckIds = item.selectedDeckIds,
                            deckNames = item.deckNames,
                            durationMinutes = item.durationMinutes,
                            failedTimestamp = now,
                            earnedPoints = 0f,
                            maxPoints = 0f
                        )
                    )
                }
            }
            saveFocusSchedules(currentSchedules)
            saveFailedFocusSessions(currentFailed)
            startConsequenceSession(expiredToFail.first().id)
        }
    }

    fun rescheduleFailedSession(failedId: String, activationTime: String, durationMinutes: Int): Boolean {
        val failed = _failedFocusSessions.value.find { it.id == failedId } ?: return false
        val created = createFocusSchedule(
            title = failed.title,
            selectedDeckIds = failed.selectedDeckIds,
            deckNames = failed.deckNames,
            activationTime = activationTime,
            durationMinutes = durationMinutes,
            isRescheduled = true,
            scheduleId = failedId
        )
        if (created) {
            val updated = _failedFocusSessions.value.filter { it.id != failedId }
            saveFailedFocusSessions(updated)
            startConsequenceSession(failedId)
            return true
        }
        return false
    }

    fun clearConsequenceSession() {
        try {
            val context = getApplication<Application>()
            LockPlugin.clearConsequenceState(context)
            refreshLockdownAndServiceState()
        } catch (_: Exception) {}
    }

    fun clearOrphanedConsequence() {
        clearConsequenceSession()
    }
}
