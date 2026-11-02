package com.uncode.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uncode.app.viewmodel.DailyActivity
import com.uncode.app.viewmodel.DeckPerformanceSummary
import com.uncode.app.viewmodel.ForecastItem
import com.uncode.app.viewmodel.MainViewModel
import com.uncode.app.viewmodel.ScoreTrendPoint

data class HighlightDeckModalState(
    val category: String,
    val deckName: String,
    val isStrongest: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverallStatsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.overallStats.collectAsStateWithLifecycle()
    var activeHighlightModal by remember { mutableStateOf<HighlightDeckModalState?>(null) }

    activeHighlightModal?.let { modal ->
        Dialog(onDismissRequest = { activeHighlightModal = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("highlight_deck_modal")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (modal.isStrongest) Icons.Default.EmojiEvents else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (modal.isStrongest) Color(0xFF2E7D32) else Color(0xFFFB8C00),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = modal.category,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (modal.isStrongest) Color(0xFF2E7D32) else Color(0xFFFB8C00)
                            )
                        }
                        IconButton(
                            onClick = { activeHighlightModal = null },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("close_highlight_modal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = modal.deckName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 88.dp
            ),
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
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-12).dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Overall Deck Statistics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }



            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("overall_readiness_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Exam Readiness Index",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val readiness = stats.readinessScore
                        val gaugeColor = when {
                            readiness >= 80 -> Color(0xFF2E7D32)
                            readiness >= 50 -> Color(0xFFFB8C00)
                            else -> Color(0xFFE53935)
                        }

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 14.dp.toPx()
                                val arcSize = size.width - strokeWidth
                                val arcOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)

                                drawArc(
                                    color = Color.LightGray.copy(alpha = 0.25f),
                                    startAngle = 140f,
                                    sweepAngle = 260f,
                                    useCenter = false,
                                    topLeft = arcOffset,
                                    size = Size(arcSize, arcSize),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                val sweep = (readiness / 100f) * 260f
                                if (sweep > 0f) {
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            listOf(gaugeColor.copy(alpha = 0.7f), gaugeColor)
                                        ),
                                        startAngle = 140f,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        topLeft = arcOffset,
                                        size = Size(arcSize, arcSize),
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$readiness%",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = gaugeColor
                                )
                                Text(
                                    text = when {
                                        readiness >= 85 -> "Mastered"
                                        readiness >= 70 -> "Exam Ready"
                                        readiness >= 50 -> "On Track"
                                        else -> "Learning"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (stats.strongestDeck != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            activeHighlightModal = HighlightDeckModalState("Strongest", stats.strongestDeck ?: "", true)
                                        }
                                        .testTag("highlight_strongest_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("Strongest", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                            Text(
                                                text = stats.strongestDeck ?: "",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            if (stats.weakestDeck != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFB8C00).copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            activeHighlightModal = HighlightDeckModalState("Needs Practice", stats.weakestDeck ?: "", false)
                                        }
                                        .testTag("highlight_needs_practice_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFB8C00), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("Needs Practice", fontSize = 10.sp, color = Color(0xFFFB8C00), fontWeight = FontWeight.Bold)
                                            Text(
                                                text = stats.weakestDeck ?: "",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }



            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Overall Accuracy",
                            value = "${stats.overallAccuracy}%",
                            color = Color(0xFF43A047),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Average Score",
                            value = "${stats.avgScore}%",
                            color = Color(0xFF3F51B5),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Quizzes Completed",
                            value = "${stats.totalQuizzes}",
                            color = Color(0xFF7E57C2),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Questions Answered",
                            value = "${stats.totalQuestionsAnswered}",
                            color = Color(0xFF00897B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("overall_memory_dynamics_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Memory Dynamics & Fluency",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = try { Color(android.graphics.Color.parseColor(stats.retentionZoneColorHex)).copy(alpha = 0.15f) } catch (e: Exception) { MaterialTheme.colorScheme.primaryContainer }
                            ) {
                                Text(
                                    text = stats.retentionZoneLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = try { Color(android.graphics.Color.parseColor(stats.retentionZoneColorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary },
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Recall Speed", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            text = "${String.format(java.util.Locale.US, "%.1f", stats.averageSecondsPerCard)}s / card",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (stats.leechCardsCount > 0) Icons.Default.PriorityHigh else Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (stats.leechCardsCount > 0) Color(0xFFE53935) else Color(0xFF2E7D32),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Friction Cards", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            text = if (stats.leechCardsCount > 0) "${stats.leechCardsCount} Leeches" else "0 (Clean)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (stats.leechCardsCount > 0) Color(0xFFE53935) else Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }



            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("overall_score_trend_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quiz Score Trend", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            text = "Historical test retention and score progression",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        ScoreTrendChart(points = stats.scoreTrend)
                    }
                }
            }



            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("overall_mastery_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Card Mastery Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            text = "${stats.totalCards} Total Cards Across ${stats.totalDecks} Study Decks",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            MasteryDonutChart(
                                mastered = stats.masteredCards,
                                learning = stats.learningCards,
                                newCount = stats.newCards,
                                total = stats.totalCards.coerceAtLeast(1),
                                modifier = Modifier.size(110.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            val totalCards = stats.totalCards.coerceAtLeast(1)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MasteryBarItem(
                                    label = "Mastered (>=80%)",
                                    count = stats.masteredCards,
                                    total = totalCards,
                                    color = Color(0xFF2E7D32)
                                )

                                MasteryBarItem(
                                    label = "Learning (50-79%)",
                                    count = stats.learningCards,
                                    total = totalCards,
                                    color = Color(0xFFFB8C00)
                                )

                                MasteryBarItem(
                                    label = "New / Unreviewed",
                                    count = stats.newCards,
                                    total = totalCards,
                                    color = Color(0xFF757575)
                                )
                            }
                        }
                    }
                }
            }



            if (stats.deckPerformances.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("overall_deck_comparison_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Per-Deck Performance", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Text("Accuracy %", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Comparative retention ranking across study decks",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            stats.deckPerformances.forEach { deckPerf ->
                                DeckComparisonItem(deckPerf = deckPerf)
                            }
                        }
                    }
                }
            }



            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("overall_weekly_activity_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("7-Day Study Volume", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            text = "Daily quiz questions practiced over the past week",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        WeeklyActivityChart(days = stats.weeklyActivity)
                    }
                }
            }



            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Content & Practice Diversity", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Visual Diagram Cards: ${stats.visualCardsCount}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text("Text Cards: ${stats.textCardsCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val visualFraction = if (stats.totalCards > 0) (stats.visualCardsCount.toFloat() / stats.totalCards.toFloat()).coerceIn(0f, 1f) else 0f
                            LinearProgressIndicator(
                                progress = { visualFraction },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Single Deck", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1, softWrap = false)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${stats.modeBreakdown.singleCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Combined", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1, softWrap = false)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${stats.modeBreakdown.combinedPoolCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Stitched", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1, softWrap = false)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${stats.modeBreakdown.stitchedCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }



            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("overall_forecast_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("5-Day Review Forecast", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            text = "Anticipated review load based on memory stability & spaced retrieval",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        ForecastChart(forecast = stats.workloadForecast)
                    }
                }
            }



            val urgentDecks = stats.atRiskDecks.filter { it.riskLevel == "HIGH" || it.riskLevel == "MEDIUM" }
            if (urgentDecks.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("overall_decay_risk_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Color(0xFFFB8C00))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Memory Decay Risk Alert", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text(
                                text = "Decks unstudied recently risk crossing below the 80% retention threshold",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )

                            urgentDecks.take(4).forEach { riskDeck ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (riskDeck.riskLevel == "HIGH") Color(0xFFE53935).copy(alpha = 0.08f) else Color(0xFFFB8C00).copy(alpha = 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (riskDeck.riskLevel == "HIGH") Color(0xFFE53935).copy(alpha = 0.3f) else Color(0xFFFB8C00).copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(riskDeck.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(
                                                text = if (riskDeck.daysSinceLastStudied == -1) "Never practiced" else "Last studied ${riskDeck.daysSinceLastStudied} days ago",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (riskDeck.riskLevel == "HIGH") Color(0xFFE53935) else Color(0xFFFB8C00)
                                        ) {
                                            Text(
                                                text = if (riskDeck.riskLevel == "HIGH") "High Decay Risk" else "Due Soon",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }



            if (stats.topLeechCards.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("overall_leech_inspector_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = Color(0xFFE53935))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("High-Friction Concepts (Leeches)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text(
                                text = "Cards with repeated mistakes that require conceptual review or rephrasing",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )

                            stats.topLeechCards.forEach { leech ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(leech.term, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 2)
                                            Text(leech.deckName, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFE53935).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${leech.failureCount} mistakes",
                                                color = Color(0xFFE53935),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
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




@Composable
private fun ScoreTrendChart(
    points: List<ScoreTrendPoint>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        Box(
            modifier = modifier.fillMaxWidth().height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (points.isEmpty()) "Complete quiz sessions to generate trend curve" else "Complete 1 more quiz to unlock trend curve",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val targetColor = Color(0xFF2E7D32)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(primaryColor, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Quiz Score %", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(targetColor, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("80% Mastery Goal", fontSize = 11.sp, color = targetColor, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            val width = size.width
            val height = size.height
            val paddingStart = 28f
            val paddingEnd = 16f
            val paddingTop = 16f
            val paddingBottom = 20f
            val chartW = width - paddingStart - paddingEnd
            val chartH = height - paddingTop - paddingBottom

            val yLines = listOf(0f, 50f, 80f, 100f)
            yLines.forEach { yVal ->
                val yPos = paddingTop + chartH * (1f - (yVal / 100f))
                val isTarget = yVal == 80f
                drawLine(
                    color = if (isTarget) targetColor.copy(alpha = 0.5f) else gridColor,
                    start = Offset(paddingStart, yPos),
                    end = Offset(width - paddingEnd, yPos),
                    strokeWidth = if (isTarget) 2f else 1f
                )
            }

            val pointCoords = points.mapIndexed { idx, pt ->
                val x = paddingStart + (idx.toFloat() / (points.size - 1).toFloat()) * chartW
                val y = paddingTop + chartH * (1f - (pt.scorePercent.toFloat().coerceIn(0f, 100f) / 100f))
                Offset(x, y)
            }

            if (pointCoords.isNotEmpty()) {

                val fillPath = Path().apply {
                    moveTo(pointCoords.first().x, paddingTop + chartH)
                    lineTo(pointCoords.first().x, pointCoords.first().y)
                    for (i in 0 until pointCoords.size - 1) {
                        val p0 = pointCoords[i]
                        val p1 = pointCoords[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                    lineTo(pointCoords.last().x, paddingTop + chartH)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.35f), primaryColor.copy(alpha = 0.02f)),
                        startY = paddingTop,
                        endY = paddingTop + chartH
                    )
                )

                val strokePath = Path().apply {
                    moveTo(pointCoords.first().x, pointCoords.first().y)
                    for (i in 0 until pointCoords.size - 1) {
                        val p0 = pointCoords[i]
                        val p1 = pointCoords[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }
                drawPath(
                    path = strokePath,
                    color = primaryColor,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                pointCoords.forEach { coord ->
                    drawCircle(color = Color.White, radius = 5.5f, center = coord)
                    drawCircle(color = primaryColor, radius = 3.5f, center = coord)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = "Q${pt.sessionIndex}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MasteryDonutChart(
    mastered: Int,
    learning: Int,
    newCount: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val mAngle = (mastered.toFloat() / total.toFloat()) * 360f
    val lAngle = (learning.toFloat() / total.toFloat()) * 360f
    val nAngle = 360f - mAngle - lAngle

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = size.width - strokeWidth
            val arcOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)

            var start = -90f

            if (mAngle > 0f) {
                drawArc(
                    color = Color(0xFF2E7D32),
                    startAngle = start,
                    sweepAngle = mAngle,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth)
                )
                start += mAngle
            }

            if (lAngle > 0f) {
                drawArc(
                    color = Color(0xFFFB8C00),
                    startAngle = start,
                    sweepAngle = lAngle,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth)
                )
                start += lAngle
            }

            if (nAngle > 0f) {
                drawArc(
                    color = Color(0xFF757575),
                    startAngle = start,
                    sweepAngle = nAngle,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$total",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Cards",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DeckComparisonItem(
    deckPerf: DeckPerformanceSummary,
    modifier: Modifier = Modifier
) {
    val accColor = when {
        deckPerf.accuracy >= 80 -> Color(0xFF2E7D32)
        deckPerf.accuracy >= 50 -> Color(0xFF1976D2)
        deckPerf.accuracy > 0 -> Color(0xFFFB8C00)
        else -> MaterialTheme.colorScheme.outline
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = try { Color(android.graphics.Color.parseColor(deckPerf.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary },
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = deckPerf.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${deckPerf.cardCount} cards",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = if (deckPerf.timesReviewed > 0) "${deckPerf.accuracy}%" else "Untested",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accColor,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        val fraction = (deckPerf.accuracy / 100f).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun WeeklyActivityChart(
    days: List<DailyActivity>,
    modifier: Modifier = Modifier
) {
    val maxQuestions = days.maxOfOrNull { it.questionCount }?.coerceAtLeast(1) ?: 1

    Row(
        modifier = modifier.fillMaxWidth().height(110.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val fraction = (day.questionCount.toFloat() / maxQuestions.toFloat()).coerceIn(0f, 1f)
            val barHeight = (60.dp * fraction).coerceAtLeast(8.dp)
            val barColor = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                if (day.questionCount > 0) {
                    Text(
                        text = "${day.questionCount}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                } else {
                    Spacer(modifier = Modifier.height(11.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(barColor)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = day.dayLabel.take(2),
                    fontSize = 10.sp,
                    fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun ForecastChart(
    forecast: List<ForecastItem>,
    modifier: Modifier = Modifier
) {
    val maxDue = forecast.maxOfOrNull { it.projectedDueCards }?.coerceAtLeast(1) ?: 1

    Row(
        modifier = modifier.fillMaxWidth().height(100.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        forecast.forEach { item ->
            val fraction = (item.projectedDueCards.toFloat() / maxDue.toFloat()).coerceIn(0f, 1f)
            val barHeight = (54.dp * fraction).coerceAtLeast(6.dp)
            val barColor = if (item.isToday) Color(0xFFFB8C00) else MaterialTheme.colorScheme.tertiary

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${item.projectedDueCards}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isToday) Color(0xFFFB8C00) else MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(barColor)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.dayLabel,
                    fontSize = 10.sp,
                    fontWeight = if (item.isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (item.isToday) Color(0xFFFB8C00) else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckStatsScreen(
    deckId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onStartQuiz: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val decks by viewModel.allDecks.collectAsStateWithLifecycle()
    val allCards by viewModel.allFlashcards.collectAsStateWithLifecycle()

    val deck = decks.find { it.id == deckId }
    val cards = allCards.filter { it.deckId == deckId }

    val mastered = cards.count { it.masteryLevel == 2 }
    val learning = cards.count { it.masteryLevel == 1 }
    val newCount = cards.count { it.masteryLevel == 0 }

    val totalReviewed = cards.sumOf { it.timesReviewed }
    val totalCorrect = cards.sumOf { it.timesCorrect }
    val acc = if (totalReviewed > 0) ((totalCorrect.toFloat() / totalReviewed) * 100).toInt() else 0

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 88.dp
            ),
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
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-12).dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${deck?.title ?: "Deck"} Stats",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Accuracy",
                        value = "$acc%",
                        color = Color(0xFF43A047),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Cards",
                        value = "${cards.size}",
                        color = Color(0xFF3F51B5),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Mastered",
                        value = "$mastered",
                        color = Color(0xFF7E57C2),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Retention & Mastery Levels", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        val total = cards.size.coerceAtLeast(1)
                        MasteryBarItem("Mastered", mastered, total, Color(0xFF2E7D32))
                        MasteryBarItem("Learning", learning, total, Color(0xFFFB8C00))
                        MasteryBarItem("New", newCount, total, Color(0xFF757575))
                    }
                }
            }

            item {
                Button(
                    onClick = { onStartQuiz(deckId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("stats_start_quiz_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Quiz This Deck", fontWeight = FontWeight.Bold)
                }
            }

            item {
                Text("Card Performance List", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            items(cards) { card ->
                val cardAcc = if (card.timesReviewed > 0) ((card.timesCorrect.toFloat() / card.timesReviewed) * 100).toInt() else 0
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(card.termOrQuestion, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                if (card.timesReviewed > 0) "${card.timesCorrect}/${card.timesReviewed} correct ($cardAcc%)" else "Not yet reviewed",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = when (card.masteryLevel) {
                                2 -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                                1 -> Color(0xFFFB8C00).copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = when (card.masteryLevel) {
                                    2 -> "Mastered"
                                    1 -> "Learning"
                                    else -> "New"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (card.masteryLevel) {
                                    2 -> Color(0xFF2E7D32)
                                    1 -> Color(0xFFFB8C00)
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MasteryBarItem(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val fraction = (count.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("$count (${(fraction * 100).toInt()}%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(3.5.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

