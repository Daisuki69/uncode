package com.uncode.app.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uncode.app.data.AnnotationItem
import com.uncode.app.viewmodel.MainViewModel
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun FocusLockScreen(
    viewModel: MainViewModel,
    scheduleId: String? = null,
    onTakeQuizNow: (com.uncode.app.viewmodel.FocusScheduleItem) -> Unit = {},
    onTimeout: (com.uncode.app.viewmodel.FocusScheduleItem) -> Unit = {}
) {
    val focusSchedules by viewModel.focusSchedules.collectAsStateWithLifecycle()
    val activeSchedule = remember(focusSchedules, scheduleId) {
        focusSchedules.find { it.id == scheduleId } ?: focusSchedules.firstOrNull { it.isActive }
    }
    val allowedApps by viewModel.allowedApps.collectAsStateWithLifecycle()
    val decks by viewModel.allDecks.collectAsStateWithLifecycle()
    val allCards by viewModel.allFlashcards.collectAsStateWithLifecycle()
    val assignedDecks = remember(decks, activeSchedule) {
        decks.filter { activeSchedule?.selectedDeckIds?.contains(it.id) == true }
    }

    val primaryDeckId = assignedDecks.firstOrNull()?.id ?: activeSchedule?.selectedDeckIds?.firstOrNull()
    var viewingDeckId by remember { mutableStateOf<Long?>(null) }

    BackHandler(enabled = viewingDeckId != null) {
        viewingDeckId = null
    }

    val durationMinutes = activeSchedule?.durationMinutes ?: 25
    val totalDurationMs = durationMinutes * 60L * 1000L

    val targetEndMs = remember(activeSchedule) {
        val context = viewModel.getApplication<android.app.Application>()
        val lockPrefs = context.getSharedPreferences("uncode_lock", Context.MODE_PRIVATE)
        val storedLockEnd = lockPrefs.getLong("lock_end_time", 0L)
        if (storedLockEnd > System.currentTimeMillis()) {
            storedLockEnd
        } else {
            val parts = (activeSchedule?.activationTime ?: "00:00").split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis + totalDurationMs
        }
    }

    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val updatedOnTimeout by rememberUpdatedState(onTimeout)
    val updatedOnTakeQuizNow by rememberUpdatedState(onTakeQuizNow)

    LaunchedEffect(targetEndMs, activeSchedule) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            if (currentTimeMillis >= targetEndMs) {
                activeSchedule?.let { updatedOnTimeout(it) }
                break
            }
            delay(250L)
        }
    }

    val remainingSeconds = ((targetEndMs - currentTimeMillis) / 1000L).coerceAtLeast(0L)
    val totalSeconds = (durationMinutes * 60L).coerceAtLeast(1L)
    val progress = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .statusBarsPadding()
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = Color(0xFFEF4444),
            trackColor = Color(0xFF1F2937)
        )

        val currentViewingDeckId = viewingDeckId
        if (currentViewingDeckId != null) {
            val viewingDeck = decks.find { it.id == currentViewingDeckId }
            val viewingCards = remember(currentViewingDeckId, allCards) {
                allCards.filter { it.deckId == currentViewingDeckId }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
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
                            onClick = { viewingDeckId = null },
                            modifier = Modifier.offset(x = (-12).dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewingDeck?.title ?: (activeSchedule?.title ?: "Deck Details"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = Color.White
                            )
                            Text(
                                text = "${viewingCards.size} Cards",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                        TimerCompactSection(remainingSeconds = remainingSeconds)
                    }
                }

                item {
                    Text(
                        text = "Flashcards in Deck (${viewingCards.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (viewingCards.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No cards in this deck",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }
                    }
                } else {
                    items(viewingCards, key = { it.id }) { card ->
                        val subtitleText = remember(card.id, card.annotationsJson, card.definitionOrAnswer) {
                            if (card.isAnnotated) {
                                val ann = AnnotationItem.parseList(card.annotationsJson)
                                "${ann.size} targets: " + ann.joinToString(", ") { it.labelAnswer }
                            } else {
                                card.definitionOrAnswer
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = card.termOrQuestion.ifBlank {
                                        if (card.isAnnotated) "Annotated Diagram" else if (!card.photoUri.isNullOrBlank()) "Image Card" else "Untitled Card"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF9CA3AF),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimerDisplaySection(remainingSeconds = remainingSeconds)

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = primaryDeckId != null) {
                            if (primaryDeckId != null) {
                                viewingDeckId = primaryDeckId
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F1D1D).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SELECTED DECK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = assignedDecks.firstOrNull()?.title ?: (activeSchedule?.title ?: "Focus Session"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val cardCount = allCards.count { it.deckId == primaryDeckId }
                            val cardInfo = if (primaryDeckId != null) "$cardCount cards \u2022 $durationMinutes min duration" else "$durationMinutes min duration"
                            Text(
                                text = cardInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                        if (primaryDeckId != null) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View Deck",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { activeSchedule?.let { updatedOnTakeQuizNow(it) } },
                    enabled = remainingSeconds > 5L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        disabledContainerColor = Color(0xFF7F1D1D).copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "Take Quiz Now",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingSeconds > 5L) Color.White else Color(0xFF9CA3AF)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Allowed Apps",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF),
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = allowedApps,
                        key = { it.packageName }
                    ) { app ->
                        AllowedAppGridTile(app = app)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerCompactSection(remainingSeconds: Long) {
    val h = remainingSeconds / 3600
    val m = (remainingSeconds % 3600) / 60
    val s = remainingSeconds % 60
    val timeString = if (h > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
    Text(
        text = timeString,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = Color(0xFFEF4444)
    )
}

@Composable
private fun TimerDisplaySection(remainingSeconds: Long) {
    val h = remainingSeconds / 3600
    val m = (remainingSeconds % 3600) / 60
    val s = remainingSeconds % 60
    val timeString = if (h > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
    Text(
        text = timeString,
        fontSize = 54.sp,
        fontWeight = FontWeight.Light,
        fontFamily = FontFamily.Monospace,
        color = Color(0xFFEF4444),
        textAlign = TextAlign.Center
    )
}
