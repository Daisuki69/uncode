package com.uncode.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Speed
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uncode.app.ui.components.DiagramCanvas
import com.uncode.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onQuizCompleted: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val questions by viewModel.activeQuizQuestions.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val score by viewModel.quizScore.collectAsStateWithLifecycle()
    val streak by viewModel.quizStreak.collectAsStateWithLifecycle()
    val finishedSessionId by viewModel.quizFinishedSessionId.collectAsStateWithLifecycle()

    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isAnswerSubmitted by remember { mutableStateOf(false) }
    var revealedAnnotationIds by remember { mutableStateOf(emptySet<String>()) }
    var lastCardId by remember { mutableStateOf<Long?>(null) }
    var questionStartTime by remember(currentIndex) { mutableStateOf(System.currentTimeMillis()) }
    var responseLatencyMs by remember { mutableStateOf(0L) }
    var remainingTimeMs by remember { mutableStateOf(0L) }
    var animTrigger by remember(currentIndex) { mutableStateOf(false) }
    var firstAttemptWrongAnswer by remember(currentIndex) { mutableStateOf<String?>(null) }
    var lockedTierTitle by remember { mutableStateOf("") }
    var lockedTierColor by remember { mutableStateOf(Color.Unspecified) }
    var showSpellAnimation by remember(currentIndex) { mutableStateOf(false) }
    var submittedQuestionIndices by remember { mutableStateOf(setOf<Int>()) }
    var wrongOptionsByQuestion by remember { mutableStateOf(mapOf<Int, Set<String>>()) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }

    LaunchedEffect(finishedSessionId) {
        if (finishedSessionId != null) {
            onQuizCompleted(finishedSessionId!!)
        }
    }

    LaunchedEffect(currentIndex) {
        val curCardId = questions.getOrNull(currentIndex)?.cardId
        if (curCardId != lastCardId) {
            revealedAnnotationIds = emptySet()
            lastCardId = curCardId
        }
        selectedOption = null
        isAnswerSubmitted = false
        questionStartTime = System.currentTimeMillis()
        responseLatencyMs = 0L
        remainingTimeMs = 0L
        firstAttemptWrongAnswer = null
        showSpellAnimation = false
    }

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No questions loaded")
        }
        return
    }

    val currentQ = questions.getOrNull(currentIndex) ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Quiz Finished")
        }
        return
    }

    val total = questions.size
    val progress = (currentIndex.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    LaunchedEffect(isAnswerSubmitted) {
        if (isAnswerSubmitted) {
            if (showSpellAnimation) {
                animTrigger = true
                val startTick = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTick
                    val rem = (2000L - elapsed).coerceAtLeast(0L)
                    remainingTimeMs = rem
                    if (rem == 0L) break
                    delay(16L)
                }
                viewModel.submitAnswer(selectedOption ?: "", isSelfRatedCorrect = true)
            } else {
                delay(2000L)
                val answerToSubmit = firstAttemptWrongAnswer ?: selectedOption ?: ""
                viewModel.submitAnswer(answerToSubmit, isSelfRatedCorrect = false)
            }
        } else {
            animTrigger = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isAnswerSubmitted && showSpellAnimation) Modifier.blur(16.dp) else Modifier)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-12).dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Quiz")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Question ${currentIndex + 1} of $total",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = currentQ.deckName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (streak > 1) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFF9800).copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("$streak Streak", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                            }
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )

                Spacer(modifier = Modifier.height(12.dp))

                val topTargetKey = if (currentQ.isAnnotated) currentQ.cardId else currentIndex.toLong()

                AnimatedContent(
                    targetState = topTargetKey,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(durationMillis = 320))) togetherWith
                            (slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(durationMillis = 260)))
                        } else {
                            (slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(durationMillis = 320))) togetherWith
                            (slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(durationMillis = 260)))
                        }.using(null)
                    },
                    label = "quiz_top_transition",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { _ ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Top
                    ) {
                        if (currentQ.isAnnotated && currentQ.photoUri != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                DiagramCanvas(
                                    diagramType = currentQ.photoUri,
                                    modifier = Modifier.fillMaxSize(),
                                    annotations = currentQ.annotations,
                                    highlightedAnnotationId = currentQ.targetAnnotationId,
                                    revealedAnnotationIds = revealedAnnotationIds,
                                    maskLabels = true
                                )
                            }

                            if (currentQ.prompt.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentQ.prompt,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else if (!currentQ.isAnnotated && currentQ.photoUri != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                DiagramCanvas(
                                    diagramType = currentQ.photoUri,
                                    modifier = Modifier.fillMaxSize(),
                                    annotations = emptyList(),
                                    maskLabels = false
                                )
                            }

                            if (currentQ.prompt.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentQ.prompt,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 120.dp)
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 120.dp)
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "QUESTION",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = currentQ.prompt,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(durationMillis = 320))) togetherWith
                            (slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(durationMillis = 260)))
                        } else {
                            (slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(durationMillis = 320))) togetherWith
                            (slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(durationMillis = 260)))
                        }.using(null)
                    },
                    label = "quiz_choices_transition",
                    modifier = Modifier
                        .fillMaxWidth()
                        .disableSplitMotionEvents()
                ) { targetIndex ->
                    val targetQ = questions.getOrNull(targetIndex) ?: return@AnimatedContent
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        targetQ.options.forEachIndexed { optIndex, optionText ->
                            val isCardSubmitted = targetIndex in submittedQuestionIndices
                            val cardWrongs = wrongOptionsByQuestion.getOrElse(targetIndex) { emptySet() }
                            val isCorrectOption = optionText.trim().equals(targetQ.answer.trim(), ignoreCase = true)
                            val isWrongChoice = optionText in cardWrongs
                            val isHighlightedAnswer = isCardSubmitted && isCorrectOption

                            val optionBgColor = when {
                                isHighlightedAnswer -> Color(0xFF2E7D32)
                                isWrongChoice -> Color(0xFFC62828)
                                else -> MaterialTheme.colorScheme.surface
                            }

                            val textColor = when {
                                isHighlightedAnswer || isWrongChoice -> Color.White
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = targetIndex == currentIndex && !isAnswerSubmitted && optionText !in cardWrongs) {
                                        val now = System.currentTimeMillis()
                                        if (now - lastTapTimestamp < 350L || targetIndex != currentIndex || isAnswerSubmitted || optionText in cardWrongs) return@clickable
                                        lastTapTimestamp = now

                                        if (isCorrectOption) {
                                            selectedOption = optionText
                                            submittedQuestionIndices = submittedQuestionIndices + currentIndex
                                            if (targetQ.targetAnnotationId != null) {
                                                revealedAnnotationIds = revealedAnnotationIds + targetQ.targetAnnotationId
                                            }
                                            if (cardWrongs.isEmpty()) {
                                                val latencyMs = (System.currentTimeMillis() - questionStartTime).coerceAtLeast(0L)
                                                val latencySec = latencyMs / 1000f
                                                val (title, color) = when {
                                                    latencySec < 2.5f -> "INSTANT" to Color(0xFF00E676)
                                                    latencySec < 3.5f -> "SHARP" to Color(0xFF00E5FF)
                                                    latencySec < 4.5f -> "DELIBERATE" to Color(0xFF448AFF)
                                                    else -> "HESITANT" to Color(0xFFFFAB00)
                                                }
                                                lockedTierTitle = title
                                                lockedTierColor = color
                                                showSpellAnimation = true
                                            } else {
                                                showSpellAnimation = false
                                            }
                                            isAnswerSubmitted = true
                                        } else {
                                            if (firstAttemptWrongAnswer == null) {
                                                firstAttemptWrongAnswer = optionText
                                            }
                                            val updatedWrongs = cardWrongs + optionText
                                            wrongOptionsByQuestion = wrongOptionsByQuestion + (currentIndex to updatedWrongs)
                                            val remainingOptions = targetQ.options.filter { it !in updatedWrongs }
                                            if (remainingOptions.size == 1) {
                                                val lastChoice = remainingOptions.first()
                                                selectedOption = lastChoice
                                                submittedQuestionIndices = submittedQuestionIndices + currentIndex
                                                if (targetQ.targetAnnotationId != null) {
                                                    revealedAnnotationIds = revealedAnnotationIds + targetQ.targetAnnotationId
                                                }
                                                showSpellAnimation = false
                                                isAnswerSubmitted = true
                                            }
                                        }
                                    }
                                    .testTag("quiz_option_$optIndex"),
                                colors = CardDefaults.cardColors(containerColor = optionBgColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = optionText,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = textColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isHighlightedAnswer) {
                                        Icon(Icons.Default.Check, contentDescription = "Correct", tint = Color.White)
                                    } else if (isWrongChoice) {
                                        Icon(Icons.Default.Close, contentDescription = "Wrong", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isAnswerSubmitted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = true
                    ) {}
            )
        }

        AnimatedVisibility(
            visible = isAnswerSubmitted && showSpellAnimation,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF030712).copy(alpha = 0.85f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = true
                    ) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "\u2727  \u2726  \u2727",
                        color = lockedTierColor.copy(alpha = 0.75f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 6.sp,
                        style = TextStyle(
                            shadow = Shadow(
                                color = lockedTierColor.copy(alpha = 0.8f),
                                offset = Offset(0f, 0f),
                                blurRadius = 20f
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        lockedTierTitle.forEachIndexed { index, char ->
                            SpellLetter(
                                char = char,
                                index = index,
                                trigger = animTrigger,
                                color = lockedTierColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val countdownSec = remainingTimeMs / 1000f
                    Text(
                        text = String.format(java.util.Locale.US, "%.2f", countdownSec),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = lockedTierColor.copy(alpha = 0.9f),
                        letterSpacing = 2.sp,
                        style = TextStyle(
                            shadow = Shadow(
                                color = lockedTierColor.copy(alpha = 0.7f),
                                offset = Offset(0f, 0f),
                                blurRadius = 18f
                            )
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SpellLetter(
    char: Char,
    index: Int,
    trigger: Boolean,
    color: Color
) {
    val progress by animateFloatAsState(
        targetValue = if (trigger) 1f else 0f,
        animationSpec = tween(
            durationMillis = 350,
            delayMillis = index * 45,
            easing = FastOutSlowInEasing
        ),
        label = "SpellLetterAnim"
    )

    Text(
        text = char.toString(),
        color = color.copy(alpha = progress),
        fontSize = 34.sp,
        fontWeight = FontWeight.Black,
        style = TextStyle(
            shadow = Shadow(
                color = color.copy(alpha = 0.9f * progress),
                offset = Offset(0f, 0f),
                blurRadius = 28f * progress
            )
        ),
        modifier = Modifier
            .graphicsLayer {
                val s = 1.7f - 0.7f * progress
                scaleX = s
                scaleY = s
                translationY = -24f * (1f - progress)
                alpha = progress
            }
            .padding(horizontal = 2.dp)
    )
}

private fun Modifier.disableSplitMotionEvents(): Modifier = pointerInput(Unit) {
    var currentId = -1L
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            event.changes.forEach { pointerInfo ->
                when {
                    pointerInfo.pressed && currentId == -1L -> {
                        currentId = pointerInfo.id.value
                    }
                    !pointerInfo.pressed && currentId == pointerInfo.id.value -> {
                        currentId = -1L
                    }
                    pointerInfo.id.value != currentId && currentId != -1L -> {
                        pointerInfo.consume()
                    }
                }
            }
        }
    }
}


