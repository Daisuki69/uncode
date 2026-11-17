package com.uncode.app.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uncode.app.data.Deck
import com.uncode.app.viewmodel.MainViewModel

enum class DeckSortType(val label: String) {
    DATE_CREATED("Date Created"),
    NAME("Name"),
    CARD_COUNT("Card Count"),
    ACCURACY("Accuracy"),
    LAST_STUDIED("Last Studied")
}

enum class SortOrder(val label: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
}

private data class DeckItemMetrics(
    val totalCards: Int,
    val masteredCards: Int,
    val accuracy: Int,
    val hasReviewed: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSelectDeck: (Long) -> Unit,
    onNavigateToOverallStats: () -> Unit,
    onNavigateToTemplateLibrary: () -> Unit,
    onNavigateToManualEditor: (Long) -> Unit,
    onNavigateToCustomImport: () -> Unit,
    isMathOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val allDecks by viewModel.allDecks.collectAsStateWithLifecycle()
    val decks = remember(allDecks, isMathOnly) {
        if (isMathOnly) allDecks.filter { it.category == "math" } else allDecks.filter { it.category != "math" }
    }
    val allCards by viewModel.allFlashcards.collectAsStateWithLifecycle()
    val focusSchedules by viewModel.focusSchedules.collectAsStateWithLifecycle()
    val failedSessions by viewModel.failedFocusSessions.collectAsStateWithLifecycle()

    var sortType by rememberSaveable { mutableStateOf(DeckSortType.DATE_CREATED) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.DESCENDING) }
    var sortTypeMenuExpanded by remember { mutableStateOf(false) }
    var sortOrderMenuExpanded by remember { mutableStateOf(false) }

    val lockedDeckIds = remember(focusSchedules, failedSessions) {
        val set = mutableSetOf<Long>()
        focusSchedules.forEach { set.addAll(it.selectedDeckIds) }
        failedSessions.forEach { set.addAll(it.selectedDeckIds) }
        set
    }

    val deckStatsById = remember(allCards) {
        allCards.groupBy { it.deckId }.mapValues { (_, cards) ->
            val total = cards.size
            val mastered = cards.count { it.masteryLevel == 2 }
            val reviewed = cards.sumOf { it.timesReviewed }
            val correct = cards.sumOf { it.timesCorrect }
            val acc = if (reviewed > 0) ((correct.toFloat() / reviewed) * 100).toInt() else 0
            DeckItemMetrics(
                totalCards = total,
                masteredCards = mastered,
                accuracy = acc,
                hasReviewed = reviewed > 0
            )
        }
    }

    val sortedDecks = remember(decks, deckStatsById, sortType, sortOrder) {
        val comparator: Comparator<Deck> = when (sortType) {
            DeckSortType.DATE_CREATED -> compareBy { it.createdAt }
            DeckSortType.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            DeckSortType.CARD_COUNT -> compareBy { deckStatsById[it.id]?.totalCards ?: 0 }
            DeckSortType.ACCURACY -> compareBy { deckStatsById[it.id]?.accuracy ?: 0 }
            DeckSortType.LAST_STUDIED -> compareBy { it.lastStudiedAt ?: 0L }
        }
        val orderedComparator = if (sortOrder == SortOrder.DESCENDING) {
            comparator.reversed().thenByDescending { it.id }
        } else {
            comparator.thenBy { it.id }
        }
        decks.sortedWith(orderedComparator)
    }

    var showCreateMethodDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isMathOnly) {
                        onNavigateToManualEditor(0L)
                    } else {
                        showCreateMethodDialog = true
                    }
                },
                modifier = Modifier.testTag("deck_list_create_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Deck")
            }
        },
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
                        text = if (isMathOnly) "Math Decks" else "Standard Decks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sortTypeMenuExpanded = true }
                                .testTag("deck_sort_by_dropdown"),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Sort by",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = sortType.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select sort type",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sortTypeMenuExpanded,
                            onDismissRequest = { sortTypeMenuExpanded = false }
                        ) {
                            DeckSortType.entries.forEach { type ->
                                val isSelected = type == sortType
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = type.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        sortType = type
                                        sortTypeMenuExpanded = false
                                    },
                                    modifier = Modifier.testTag("deck_sort_type_${type.name}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sortOrderMenuExpanded = true }
                                .testTag("deck_sort_order_dropdown"),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Order",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = sortOrder.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select sort order",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sortOrderMenuExpanded,
                            onDismissRequest = { sortOrderMenuExpanded = false }
                        ) {
                            SortOrder.entries.forEach { order ->
                                val isSelected = order == sortOrder
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = order.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (order == SortOrder.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        sortOrder = order
                                        sortOrderMenuExpanded = false
                                    },
                                    modifier = Modifier.testTag("deck_sort_order_${order.name}")
                                )
                            }
                        }
                    }
                }
            }

            items(sortedDecks, key = { it.id }) { deck ->
                val stats = deckStatsById[deck.id]
                val totalCards = stats?.totalCards ?: 0
                val statsText = when {
                    totalCards == 0 -> "0 cards"
                    stats != null && stats.hasReviewed -> "$totalCards cards \u2022 ${stats.masteredCards} mastered \u2022 ${stats.accuracy}% accuracy"
                    else -> "$totalCards cards \u2022 Not practiced yet"
                }
                val isLocked = deck.id in lockedDeckIds

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDeck(deck.id) }
                        .testTag("select_deck_${deck.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = deck.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isLocked) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Locked",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statsText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (showCreateMethodDialog) {
            AlertDialog(
                onDismissRequest = { showCreateMethodDialog = false },
                title = { Text("Create Deck", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Choose how you would like to create your new deck:")

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCreateMethodDialog = false
                                    onNavigateToManualEditor(0L)
                                }
                                .testTag("create_method_manual"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Manual", fontWeight = FontWeight.Bold)
                                    Text("Write custom questions, add photos & diagram annotations", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCreateMethodDialog = false
                                    onNavigateToTemplateLibrary()
                                }
                                .testTag("create_method_ai_import"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("AI Import", fontWeight = FontWeight.Bold)
                                    Text("Upload a document to generate structured flashcards", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCreateMethodDialog = false
                                    onNavigateToCustomImport()
                                }
                                .testTag("create_method_custom_import"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFFFF8533))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Custom Import", fontWeight = FontWeight.Bold)
                                    Text("Import text or files with custom delimiters", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showCreateMethodDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

