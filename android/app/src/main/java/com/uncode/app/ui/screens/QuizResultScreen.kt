package com.uncode.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uncode.app.data.QuestionReviewLog
import com.uncode.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultScreen(
    sessionId: Long,
    viewModel: MainViewModel,
    onBackToDashboard: () -> Unit,
    onRetakeQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastSession by viewModel.lastFinishedSession.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) {
        if (lastSession == null || lastSession?.id != sessionId) {
            viewModel.loadSessionForReview(sessionId) {}
        }
    }

    val session = lastSession

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        if (session == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading quiz result...")
            }
            return@Scaffold
        }

        val reviewLogs = remember(session.answerLogsJson) {
            QuestionReviewLog.parseList(session.answerLogsJson)
        }
        val scoreColor = when {
            session.scorePercent >= 80 -> Color(0xFF43A047)
            session.scorePercent >= 50 -> Color(0xFFFB8C00)
            else -> Color(0xFFE53935)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackToDashboard,
                        modifier = Modifier.offset(x = (-12).dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Quiz Result",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = scoreColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(3.dp, scoreColor),
                            modifier = Modifier.size(100.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${session.scorePercent}%",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 28.sp,
                                        color = scoreColor
                                    )
                                    Text(
                                        text = "SCORE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = scoreColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = when {
                                session.scorePercent >= 90 -> "Mastery Achieved! ðŸ†"
                                session.scorePercent >= 70 -> "Great Work! Keep It Up! ðŸ‘"
                                else -> "Needs Practice! You'll get it! ðŸ’ª"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${session.correctCount} of ${session.totalQuestions} Questions Correct \u2022 ${session.deckNames}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Question-by-Question Breakdown:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(
                items = reviewLogs,
                key = { index, log -> "${log.cardId}_$index" }
            ) { index, log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_answer_item_$index"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (log.isCorrect) Color(0xFF2E7D32).copy(alpha = 0.5f) else Color(0xFFC62828).copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (log.isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (log.isCorrect) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${log.question}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row {
                                Text("Correct Answer: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                Text(log.correctAnswer, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }

                            if (!log.isCorrect && log.userAnswer.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row {
                                    Text("Your Answer: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(log.userAnswer, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

