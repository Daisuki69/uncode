package com.uncode.app

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uncode.app.ui.Screen
import com.uncode.app.ui.screens.CreateFocusSessionScreen
import com.uncode.app.ui.screens.FocusLockScreen
import com.uncode.app.ui.screens.CustomImportScreen
import com.uncode.app.ui.screens.DashboardScreen
import com.uncode.app.ui.screens.FailedHomeworksScreen
import com.uncode.app.ui.screens.DeckDetailScreen
import com.uncode.app.ui.screens.DeckListScreen
import com.uncode.app.ui.screens.DeckStatsScreen
import com.uncode.app.ui.screens.FlashcardEditorScreen
import com.uncode.app.ui.screens.LockedQuizScreen
import com.uncode.app.ui.screens.EvaluationScreen
import com.uncode.app.ui.screens.OverallStatsScreen
import com.uncode.app.ui.screens.QuizHistoryScreen
import com.uncode.app.ui.screens.QuizResultScreen
import com.uncode.app.ui.screens.QuizScreen
import com.uncode.app.ui.screens.SettingsScreen
import com.uncode.app.ui.screens.StartQuizScreen
import com.uncode.app.ui.screens.TemplateLibraryScreen
import com.uncode.app.ui.theme.MyApplicationTheme
import com.uncode.app.viewmodel.AppThemeMode
import com.uncode.app.viewmodel.MainViewModel
import java.util.Calendar
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile
        private var sIsRunning: Boolean = false

        @Volatile
        private var sInstance: MainActivity? = null

        @JvmStatic
        fun isRunning(): Boolean {
            val inst = sInstance
            return sIsRunning && inst != null && !inst.isFinishing && !inst.isDestroyed
        }

        @JvmStatic
        fun getInstance(): MainActivity? {
            return sInstance
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        sIsRunning = true
        sInstance = this
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0xFF0A0E17.toInt()))
        super.onCreate(savedInstanceState)

        val lightNavScrim = android.graphics.Color.argb(0x80, 0xF8, 0xFA, 0xFC)
        val darkNavScrim = android.graphics.Color.argb(0x80, 0x0F, 0x17, 0x2A)
        enableEdgeToEdge(
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                lightNavScrim,
                darkNavScrim
            )
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        QiezkaNotificationHelper.postAlwaysActiveResourceQueryNotification(this)

        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = isDark) {
                val view = androidx.compose.ui.platform.LocalView.current
                if (!view.isInEditMode) {
                    androidx.compose.runtime.SideEffect {
                        val navColor = if (isDark) darkNavScrim else lightNavScrim
                        window.setBackgroundDrawable(
                            android.graphics.drawable.ColorDrawable(
                                if (isDark) 0xFF0B0F17.toInt() else 0xFFF8FAFC.toInt()
                            )
                        )
                        window.navigationBarColor = navColor
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            window.isNavigationBarContrastEnforced = false
                        }
                    }
                }
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    SirenQuizApp(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sIsRunning = true
        sInstance = this
        QiezkaWidgetProvider.syncLauncherIconVisibility(this)
        QiezkaNotificationListener.requestRebind(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        sIsRunning = false
        if (sInstance == this) {
            sInstance = null
        }
        super.onDestroy()
    }
}

@Composable
fun SirenQuizApp(
    viewModel: MainViewModel = viewModel()
) {
    val backStack = viewModel.navigationBackStack
    val currentScreen = backStack.lastOrNull() ?: Screen.Dashboard

    fun navigateTo(screen: Screen) {
        backStack.add(screen)
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun popToDashboard() {
        backStack.clear()
        backStack.add(Screen.Dashboard)
    }

    BackHandler(enabled = backStack.size > 1) {
        navigateBack()
    }

    val focusSchedules by viewModel.focusSchedules.collectAsStateWithLifecycle()
    val isLockdownActive by viewModel.isLockdownActive.collectAsStateWithLifecycle()
    val operatingMode by viewModel.operatingMode.collectAsStateWithLifecycle()
    val isSafemode = operatingMode.equals("safemode", ignoreCase = true)
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    var pendingQuizSchedule by remember { mutableStateOf<com.uncode.app.viewmodel.FocusScheduleItem?>(null) }

    val activeSchedule = remember(focusSchedules, isLockdownActive, currentTimeMillis, pendingQuizSchedule, isSafemode) {
        if (pendingQuizSchedule != null) {
            return@remember pendingQuizSchedule
        }
        val inWindowSchedule = focusSchedules.firstOrNull { schedule ->
            if (!schedule.isActive) return@firstOrNull false
            val parts = schedule.activationTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            if (isSafemode) {
                val isOutsideOperatingHours = hour in 3..18
                val start = if (hour >= 19) hour * 60 + minute else 1440 + hour * 60 + minute
                val isExceedingCurfew = start + schedule.durationMinutes > 1620
                if (isOutsideOperatingHours || isExceedingCurfew) return@firstOrNull false
            }
            val cal = Calendar.getInstance().apply {
                timeInMillis = currentTimeMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val durationMs = schedule.durationMinutes * 60L * 1000L
            val startMs = cal.timeInMillis
            val endMs = startMs + durationMs
            listOf(-86_400_000L, 0L, 86_400_000L).any { offset ->
                currentTimeMillis in (startMs + offset)..(endMs + offset + 10_000L)
            }
        }
        if (inWindowSchedule != null) {
            inWindowSchedule
        } else if (isLockdownActive) {
            val currentHour = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }.get(Calendar.HOUR_OF_DAY)
            if (isSafemode && currentHour in 3..18) {
                null
            } else {
                val context = viewModel.getApplication<android.app.Application>()
                val lockPrefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
                val lockEnd = lockPrefs.getLong("lock_end_time", 0L)
                if (lockEnd <= 0L || currentTimeMillis >= lockEnd) {
                    null
                } else {
                    val activeId = lockPrefs.getString("active_schedule_id", null)
                    focusSchedules.find { it.id == activeId }
                }
            }
        } else {
            null
        }
    }

    if (activeSchedule != null && currentScreen !is Screen.LockedQuiz && currentScreen !is Screen.QuizResult && currentScreen !is Screen.Evaluation && currentScreen !is Screen.CreateFocusSession) {
        FocusLockScreen(
            viewModel = viewModel,
            scheduleId = activeSchedule.id,
            onTakeQuizNow = { sched ->
                pendingQuizSchedule = sched
                viewModel.startFocusSessionQuiz(sched.selectedDeckIds) {
                    navigateTo(Screen.LockedQuiz(sched.id))
                    pendingQuizSchedule = null
                }
            },
            onTimeout = { sched ->
                pendingQuizSchedule = sched
                viewModel.startFocusSessionQuiz(sched.selectedDeckIds) {
                    navigateTo(Screen.LockedQuiz(sched.id))
                    pendingQuizSchedule = null
                }
            }
        )
    } else {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                (fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(90))).using(null)
            },
            label = "ScreenTransition",
            modifier = Modifier.fillMaxSize()
        ) { screen ->
        when (screen) {
            is Screen.Dashboard -> {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToMyDecks = { navigateTo(Screen.DeckList) },
                    onNavigateToDeckDetail = { deckId -> navigateTo(Screen.DeckDetail(deckId)) },
                    onNavigateToCreateDeck = { navigateTo(Screen.DeckList) },
                    onNavigateToStartQuiz = { navigateTo(Screen.StartQuiz()) },
                    onNavigateToOverallStats = { navigateTo(Screen.OverallStats) },
                    onNavigateToRecentQuizzes = { navigateTo(Screen.QuizHistory) },
                    onNavigateToQuizResult = { sessionId -> navigateTo(Screen.QuizResult(sessionId)) },
                    onNavigateToSettings = { navigateTo(Screen.Settings) },
                    onNavigateToCreateFocusSession = { navigateTo(Screen.CreateFocusSession) },
                    onNavigateToFailedHomeworks = { navigateTo(Screen.FailedHomeworks) }
                )
            }

            is Screen.Settings -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() }
                )
            }

            is Screen.DeckList -> {
                DeckListScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onSelectDeck = { deckId -> navigateTo(Screen.DeckDetail(deckId)) },
                    onNavigateToOverallStats = { navigateTo(Screen.OverallStats) },
                    onNavigateToTemplateLibrary = { navigateTo(Screen.TemplateLibrary) },
                    onNavigateToManualEditor = { deckId -> navigateTo(Screen.FlashcardEditor(deckId)) },
                    onNavigateToCustomImport = { navigateTo(Screen.CustomImport) }
                )
            }

            is Screen.DeckDetail -> {
                DeckDetailScreen(
                    deckId = screen.deckId,
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onNavigateToEditor = { dId -> navigateTo(Screen.FlashcardEditor(dId)) },
                    onNavigateToDeckStats = { dId -> navigateTo(Screen.DeckStats(dId)) },
                    onStartQuizThisDeck = { dId ->
                        viewModel.selectSingleDeckForQuiz(dId)
                        viewModel.startSingleDeckQuiz(dId) {
                            navigateTo(Screen.ActiveQuiz)
                        }
                    }
                )
            }

            is Screen.FlashcardEditor -> {
                FlashcardEditorScreen(
                    deckId = screen.deckId,
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onSaveAndFinished = {
                        navigateBack()
                    }
                )
            }

            is Screen.TemplateLibrary -> {
                TemplateLibraryScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onDeckCreatedFromTemplate = { newDeckId ->
                        popToDashboard()
                        navigateTo(Screen.DeckDetail(newDeckId))
                    }
                )
            }

            is Screen.StartQuiz -> {
                StartQuizScreen(
                    viewModel = viewModel,
                    preselectedDeckId = screen.preselectedDeckId,
                    onBack = { navigateBack() },
                    onLaunchQuiz = {
                        navigateTo(Screen.ActiveQuiz)
                    }
                )
            }

            is Screen.ActiveQuiz -> {
                QuizScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onQuizCompleted = { sessionId ->
                        backStack.removeAt(backStack.lastIndex)
                        navigateTo(Screen.QuizResult(sessionId))
                    }
                )
            }

            is Screen.QuizResult -> {
                QuizResultScreen(
                    sessionId = screen.sessionId,
                    viewModel = viewModel,
                    onBackToDashboard = { popToDashboard() },
                    onRetakeQuiz = {
                        backStack.removeAt(backStack.lastIndex)
                        navigateTo(Screen.ActiveQuiz)
                    }
                )
            }

            is Screen.QuizHistory -> {
                QuizHistoryScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onSelectSession = { sessionId -> navigateTo(Screen.QuizResult(sessionId)) }
                )
            }

            is Screen.OverallStats -> {
                OverallStatsScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() }
                )
            }

            is Screen.DeckStats -> {
                DeckStatsScreen(
                    deckId = screen.deckId,
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onStartQuiz = { dId ->
                        viewModel.selectSingleDeckForQuiz(dId)
                        viewModel.startSingleDeckQuiz(dId) {
                            navigateTo(Screen.ActiveQuiz)
                        }
                    }
                )
            }

            is Screen.CustomImport -> {
                CustomImportScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onDeckCreated = { newDeckId ->
                        popToDashboard()
                        navigateTo(Screen.DeckDetail(newDeckId))
                    }
                )
            }

            is Screen.CreateFocusSession -> {
                CreateFocusSessionScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onFinalized = {
                        while (viewModel.navigationBackStack.size > 1) {
                            viewModel.navigationBackStack.removeLastOrNull()
                        }
                    }
                )
            }

            is Screen.LockedQuiz -> {
                LockedQuizScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onQuizCompleted = { sessionId, maxPts, earnedPts ->
                        backStack.removeAt(backStack.lastIndex)
                        navigateTo(Screen.Evaluation(screen.scheduleId, maxPts, earnedPts))
                    }
                )
            }

            is Screen.Evaluation -> {
                EvaluationScreen(
                    viewModel = viewModel,
                    scheduleId = screen.scheduleId,
                    maxPoints = screen.maxPoints,
                    earnedPoints = screen.earnedPoints,
                    onBackToDashboard = { popToDashboard() }
                )
            }

            is Screen.FailedHomeworks -> {
                FailedHomeworksScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() }
                )
            }

            is Screen.FocusLockScreen -> {
                FocusLockScreen(
                    viewModel = viewModel,
                    scheduleId = screen.scheduleId,
                    onTakeQuizNow = { sched ->
                        viewModel.startFocusSessionQuiz(sched.selectedDeckIds) {
                            navigateTo(Screen.LockedQuiz(sched.id))
                        }
                    },
                    onTimeout = { sched ->
                        viewModel.startFocusSessionQuiz(sched.selectedDeckIds) {
                            navigateTo(Screen.LockedQuiz(sched.id))
                        }
                    }
                )
            }

            else -> {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToMyDecks = { navigateTo(Screen.DeckList) },
                    onNavigateToDeckDetail = { deckId -> navigateTo(Screen.DeckDetail(deckId)) },
                    onNavigateToCreateDeck = { navigateTo(Screen.DeckList) },
                    onNavigateToStartQuiz = { navigateTo(Screen.StartQuiz()) },
                    onNavigateToOverallStats = { navigateTo(Screen.OverallStats) },
                    onNavigateToRecentQuizzes = { navigateTo(Screen.QuizHistory) },
                    onNavigateToQuizResult = { sessionId -> navigateTo(Screen.QuizResult(sessionId)) },
                    onNavigateToSettings = { navigateTo(Screen.Settings) },
                    onNavigateToCreateFocusSession = { navigateTo(Screen.CreateFocusSession) },
                    onNavigateToFailedHomeworks = { navigateTo(Screen.FailedHomeworks) }
                )
            }
        }
    }
}
}
