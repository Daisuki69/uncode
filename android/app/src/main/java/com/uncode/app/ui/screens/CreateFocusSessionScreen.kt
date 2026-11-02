package com.uncode.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uncode.app.data.FocusSessionMessages
import com.uncode.app.viewmodel.MainViewModel
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ActivePickerField {
    NONE, HOUR, MINUTE, DURATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFocusSessionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onFinalized: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val decks by viewModel.allDecks.collectAsStateWithLifecycle()
    val focusSchedules by viewModel.focusSchedules.collectAsStateWithLifecycle()
    val operatingMode by viewModel.operatingMode.collectAsStateWithLifecycle()
    val selectedDeckIds = remember { mutableStateListOf<Long>() }

    var showAbandonConfirmDialog by remember { mutableStateOf(false) }
    var abandonQuote by remember { mutableStateOf("") }
    var isNavigatingAway by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val isHardcore = operatingMode.equals("hardcore", ignoreCase = true)
    val hours = remember(isHardcore) {
        if (isHardcore) (1..12).toList() else listOf(7, 8, 9, 10, 11, 12, 1, 2)
    }

    val nowCalendar = remember { Calendar.getInstance() }
    val currentH24 = remember { nowCalendar.get(Calendar.HOUR_OF_DAY) }
    val currentM = remember { nowCalendar.get(Calendar.MINUTE) }
    val (defaultH24, defaultM) = remember(isHardcore) {
        if (isHardcore || (currentH24 >= 19 || currentH24 < 3)) {
            currentH24 to currentM
        } else {
            19 to 0
        }
    }
    val defaultH12 = remember(defaultH24) { if (defaultH24 % 12 == 0) 12 else defaultH24 % 12 }
    val defaultPeriod = remember(defaultH24) { if (defaultH24 in 12..23) "PM" else "AM" }

    var selectedHour12 by remember(defaultH12, hours) {
        mutableIntStateOf(if (defaultH12 in hours) defaultH12 else hours.first())
    }
    var selectedMinute by remember(defaultM) { mutableIntStateOf(defaultM) }
    var selectedPeriod by remember(defaultPeriod, isHardcore, selectedHour12) {
        mutableStateOf(if (isHardcore) defaultPeriod else (if (selectedHour12 in 7..11) "PM" else "AM"))
    }

    val hour24 = when {
        selectedPeriod == "AM" && selectedHour12 == 12 -> 0
        selectedPeriod == "PM" && selectedHour12 != 12 -> selectedHour12 + 12
        else -> selectedHour12
    }
    val isInOperatingHours = isHardcore || (hour24 >= 19 || hour24 < 3)
    val normalizedStart = if (hour24 >= 19) hour24 * 60 + selectedMinute else 1440 + hour24 * 60 + selectedMinute
    val maxDuration = if (isHardcore) 90 else minOf(90, (1620 - normalizedStart).coerceAtLeast(1))
    val durationRange = remember(maxDuration) { (1..maxDuration).toList() }

    var selectedDurationMinutes by remember { mutableIntStateOf(25) }
    val effectiveDuration = selectedDurationMinutes.coerceIn(1, maxDuration)
    val normalizedEnd = normalizedStart + effectiveDuration
    val isCurfewExceeded = !isHardcore && (normalizedEnd > 1620)

    val isScheduleOverlapping = remember(selectedHour12, selectedMinute, selectedPeriod, effectiveDuration, focusSchedules) {
        val proposedStart = hour24 * 60 + selectedMinute
        val proposedEnd = proposedStart + effectiveDuration

        focusSchedules.any { existing ->
            if (!existing.isActive) return@any false
            val parts = existing.activationTime.split(":")
            val exH = parts.getOrNull(0)?.toIntOrNull() ?: return@any false
            val exM = parts.getOrNull(1)?.toIntOrNull() ?: return@any false
            val exStart = exH * 60 + exM
            val exEnd = exStart + existing.durationMinutes

            listOf(-1440, 0, 1440).any { offset ->
                val exStartOff = exStart + offset
                val exEndOff = exEnd + offset
                proposedStart < exEndOff + 30 && proposedEnd + 30 > exStartOff
            }
        }
    }

    val isScheduleValid = isInOperatingHours && !isCurfewExceeded && !isScheduleOverlapping

    var activeField by remember { mutableStateOf(ActivePickerField.NONE) }
    val minutes = remember { (0..59).toList() }

    val hourListState = rememberLazyListState(
        initialFirstVisibleItemIndex = hours.indexOf(selectedHour12).coerceAtLeast(0)
    )
    val minuteListState = rememberLazyListState(
        initialFirstVisibleItemIndex = minutes.indexOf(selectedMinute).coerceAtLeast(0)
    )
    val durationListState = rememberLazyListState(
        initialFirstVisibleItemIndex = durationRange.indexOf(effectiveDuration).coerceAtLeast(0)
    )

    val centerAnchorProvider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                val y = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
                return IntOffset(x, y)
            }
        }
    }

    val handleBackAttempt = {
        if (!isNavigatingAway) {
            val selectedOrRandomDeck = decks.find { selectedDeckIds.contains(it.id) }?.title
                ?: decks.filter { it.title.isNotBlank() }.randomOrNull()?.title?.trim()
                ?: "your flashcards"
            abandonQuote = FocusSessionMessages.getRandomQuote(selectedOrRandomDeck)
            showAbandonConfirmDialog = true
        }
    }

    BackHandler(enabled = !isNavigatingAway) {
        if (currentStep == 2) {
            currentStep = 1
        } else {
            handleBackAttempt()
        }
    }

    if (showAbandonConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isNavigatingAway) {
                    showAbandonConfirmDialog = false
                }
            },
            text = {
                Text(abandonQuote + "\n\nAre you sure this is what you want?")
            },
            confirmButton = {
                Button(
                    enabled = !isNavigatingAway,
                    onClick = {
                        if (isNavigatingAway) return@Button
                        isNavigatingAway = true
                        showAbandonConfirmDialog = false
                        coroutineScope.launch {
                            delay(180L)
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !isNavigatingAway,
                    onClick = { showAbandonConfirmDialog = false }
                ) {
                    Text("No")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (currentStep == 1) "Step 1: Select Deck" else "Step 2: Schedule",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            enabled = !isNavigatingAway,
                            onClick = {
                                if (currentStep == 2) {
                                    currentStep = 1
                                } else {
                                    handleBackAttempt()
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                if (currentStep == 1) {
                    AnimatedVisibility(
                        visible = selectedDeckIds.isNotEmpty(),
                        enter = fadeIn(animationSpec = tween(180)) + scaleIn(animationSpec = tween(180)),
                        exit = fadeOut(animationSpec = tween(140)) + scaleOut(animationSpec = tween(140))
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = { currentStep = 2 },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("focus_session_next_fab")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Next", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    AnimatedVisibility(
                        visible = isScheduleValid,
                        enter = fadeIn(animationSpec = tween(150)) + scaleIn(animationSpec = tween(150)),
                        exit = fadeOut(animationSpec = tween(150)) + scaleOut(animationSpec = tween(150))
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                if (!isScheduleValid) return@ExtendedFloatingActionButton
                                val hour24 = when {
                                    selectedPeriod == "AM" && selectedHour12 == 12 -> 0
                                    selectedPeriod == "PM" && selectedHour12 != 12 -> selectedHour12 + 12
                                    else -> selectedHour12
                                }
                                val activationTime = String.format(Locale.getDefault(), "%02d:%02d", hour24, selectedMinute)
                                val selectedDecks = decks.filter { selectedDeckIds.contains(it.id) }
                                val title = selectedDecks.joinToString(", ") { it.title.trim() }.ifBlank { "Study Session" }
                                val deckNames = selectedDecks.joinToString(", ") { it.title.trim() }
                                val success = viewModel.createFocusSchedule(
                                    title = title,
                                    selectedDeckIds = selectedDeckIds.toList(),
                                    deckNames = deckNames,
                                    activationTime = activationTime,
                                    durationMinutes = effectiveDuration
                                )
                                if (success) {
                                    onFinalized()
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("focus_session_finalize_fab")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Finalize")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Finalize", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (currentStep == 1) {
                if (decks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No study decks available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = innerPadding.calculateTopPadding() + 8.dp,
                            bottom = innerPadding.calculateBottomPadding() + 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = decks,
                            key = { it.id }
                        ) { deck ->
                            val isSelected = selectedDeckIds.contains(deck.id)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) {
                                            selectedDeckIds.remove(deck.id)
                                        } else {
                                            selectedDeckIds.add(deck.id)
                                        }
                                    }
                                    .testTag("focus_deck_card_${deck.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = deck.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                selectedDeckIds.add(deck.id)
                                            } else {
                                                selectedDeckIds.remove(deck.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("activation_time_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Time",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (activeField == ActivePickerField.HOUR) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            },
                                            modifier = Modifier.wheelAnchorGesture(
                                                onTap = {
                                                    if (activeField == ActivePickerField.HOUR) {
                                                        activeField = ActivePickerField.NONE
                                                    } else {
                                                        coroutineScope.launch {
                                                            hourListState.scrollToItem(hours.indexOf(selectedHour12).coerceAtLeast(0))
                                                        }
                                                        activeField = ActivePickerField.HOUR
                                                    }
                                                },
                                                onDragStart = {
                                                    activeField = ActivePickerField.HOUR
                                                },
                                                onDrag = { dragAmountY ->
                                                    hourListState.dispatchRawDelta(-dragAmountY)
                                                    val layoutInfo = hourListState.layoutInfo
                                                    val visibleItems = layoutInfo.visibleItemsInfo
                                                    if (visibleItems.isNotEmpty()) {
                                                        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                                        val closest = visibleItems.minByOrNull { item ->
                                                            val itemCenter = item.offset + item.size / 2
                                                            abs(itemCenter - viewportCenter)
                                                        }
                                                        if (closest != null) {
                                                            selectedHour12 = hours[closest.index.coerceIn(0, hours.lastIndex)]
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    val layoutInfo = hourListState.layoutInfo
                                                    val visibleItems = layoutInfo.visibleItemsInfo
                                                    val targetIndex = if (visibleItems.isNotEmpty()) {
                                                        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                                        val closest = visibleItems.minByOrNull { item ->
                                                            val itemCenter = item.offset + item.size / 2
                                                            abs(itemCenter - viewportCenter)
                                                        }
                                                        closest?.index?.coerceIn(0, hours.lastIndex) ?: hourListState.firstVisibleItemIndex.coerceIn(0, hours.lastIndex)
                                                    } else {
                                                        hourListState.firstVisibleItemIndex.coerceIn(0, hours.lastIndex)
                                                    }
                                                    selectedHour12 = hours[targetIndex]
                                                    if (!isHardcore) {
                                                        selectedPeriod = if (selectedHour12 in 7..11) "PM" else "AM"
                                                    }
                                                    coroutineScope.launch {
                                                        hourListState.scrollToItem(targetIndex)
                                                    }
                                                    activeField = ActivePickerField.NONE
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = selectedHour12.toString(),
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }

                                        if (activeField == ActivePickerField.HOUR) {
                                            Popup(
                                                popupPositionProvider = centerAnchorProvider,
                                                onDismissRequest = { activeField = ActivePickerField.NONE },
                                                properties = PopupProperties(focusable = true)
                                            ) {
                                                Card(
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                                ) {
                                                    ScrollWheelPicker(
                                                        items = hours,
                                                        listState = hourListState,
                                                        onItemSelected = {
                                                            selectedHour12 = it
                                                            if (!isHardcore) {
                                                                selectedPeriod = if (it in 7..11) "PM" else "AM"
                                                            }
                                                        },
                                                        onCenterClicked = { activeField = ActivePickerField.NONE },
                                                        modifier = Modifier.width(56.dp),
                                                        itemHeight = 32.dp,
                                                        visibleItemCount = 5
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = ":",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )

                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (activeField == ActivePickerField.MINUTE) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            },
                                            modifier = Modifier.wheelAnchorGesture(
                                                onTap = {
                                                    if (activeField == ActivePickerField.MINUTE) {
                                                        activeField = ActivePickerField.NONE
                                                    } else {
                                                        coroutineScope.launch {
                                                            minuteListState.scrollToItem(minutes.indexOf(selectedMinute).coerceAtLeast(0))
                                                        }
                                                        activeField = ActivePickerField.MINUTE
                                                    }
                                                },
                                                onDragStart = {
                                                    activeField = ActivePickerField.MINUTE
                                                },
                                                onDrag = { dragAmountY ->
                                                    minuteListState.dispatchRawDelta(-dragAmountY)
                                                    val layoutInfo = minuteListState.layoutInfo
                                                    val visibleItems = layoutInfo.visibleItemsInfo
                                                    if (visibleItems.isNotEmpty()) {
                                                        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                                        val closest = visibleItems.minByOrNull { item ->
                                                            val itemCenter = item.offset + item.size / 2
                                                            abs(itemCenter - viewportCenter)
                                                        }
                                                        if (closest != null) {
                                                            selectedMinute = minutes[closest.index.coerceIn(0, minutes.lastIndex)]
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    val layoutInfo = minuteListState.layoutInfo
                                                    val visibleItems = layoutInfo.visibleItemsInfo
                                                    val targetIndex = if (visibleItems.isNotEmpty()) {
                                                        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                                        val closest = visibleItems.minByOrNull { item ->
                                                            val itemCenter = item.offset + item.size / 2
                                                            abs(itemCenter - viewportCenter)
                                                        }
                                                        closest?.index?.coerceIn(0, minutes.lastIndex) ?: minuteListState.firstVisibleItemIndex.coerceIn(0, minutes.lastIndex)
                                                    } else {
                                                        minuteListState.firstVisibleItemIndex.coerceIn(0, minutes.lastIndex)
                                                    }
                                                    selectedMinute = minutes[targetIndex]
                                                    coroutineScope.launch {
                                                        minuteListState.scrollToItem(targetIndex)
                                                    }
                                                    activeField = ActivePickerField.NONE
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = String.format(Locale.getDefault(), "%02d", selectedMinute),
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }

                                        if (activeField == ActivePickerField.MINUTE) {
                                            Popup(
                                                popupPositionProvider = centerAnchorProvider,
                                                onDismissRequest = { activeField = ActivePickerField.NONE },
                                                properties = PopupProperties(focusable = true)
                                            ) {
                                                Card(
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                                ) {
                                                    ScrollWheelPicker(
                                                        items = minutes,
                                                        listState = minuteListState,
                                                        onItemSelected = { selectedMinute = it },
                                                        onCenterClicked = { activeField = ActivePickerField.NONE },
                                                        labelFormatter = { String.format(Locale.getDefault(), "%02d", it) },
                                                        modifier = Modifier.width(56.dp),
                                                        itemHeight = 32.dp,
                                                        visibleItemCount = 5
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.clickable(enabled = isHardcore) {
                                            if (isHardcore) {
                                                selectedPeriod = if (selectedPeriod == "AM") "PM" else "AM"
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = selectedPeriod,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("duration_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Duration",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (activeField == ActivePickerField.DURATION) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            },
                                            modifier = Modifier.wheelAnchorGesture(
                                                onTap = {
                                                    if (activeField == ActivePickerField.DURATION) {
                                                        activeField = ActivePickerField.NONE
                                                    } else {
                                                        coroutineScope.launch {
                                                            durationListState.scrollToItem(durationRange.indexOf(selectedDurationMinutes).coerceAtLeast(0))
                                                        }
                                                        activeField = ActivePickerField.DURATION
                                                    }
                                                },
                                                onDragStart = {
                                                    activeField = ActivePickerField.DURATION
                                                },
                                                onDrag = { dragAmountY ->
                                                    durationListState.dispatchRawDelta(-dragAmountY)
                                                    val layoutInfo = durationListState.layoutInfo
                                                    val visibleItems = layoutInfo.visibleItemsInfo
                                                    if (visibleItems.isNotEmpty()) {
                                                        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                                        val closest = visibleItems.minByOrNull { item ->
                                                            val itemCenter = item.offset + item.size / 2
                                                            abs(itemCenter - viewportCenter)
                                                        }
                                                        if (closest != null) {
                                                            selectedDurationMinutes = durationRange[closest.index.coerceIn(0, durationRange.lastIndex)]
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    val layoutInfo = durationListState.layoutInfo
                                                    val visibleItems = layoutInfo.visibleItemsInfo
                                                    val targetIndex = if (visibleItems.isNotEmpty()) {
                                                        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                                        val closest = visibleItems.minByOrNull { item ->
                                                            val itemCenter = item.offset + item.size / 2
                                                            abs(itemCenter - viewportCenter)
                                                        }
                                                        closest?.index?.coerceIn(0, durationRange.lastIndex) ?: durationListState.firstVisibleItemIndex.coerceIn(0, durationRange.lastIndex)
                                                    } else {
                                                        durationListState.firstVisibleItemIndex.coerceIn(0, durationRange.lastIndex)
                                                    }
                                                    selectedDurationMinutes = durationRange[targetIndex]
                                                    coroutineScope.launch {
                                                        durationListState.scrollToItem(targetIndex)
                                                    }
                                                    activeField = ActivePickerField.NONE
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = effectiveDuration.toString(),
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }

                                        if (activeField == ActivePickerField.DURATION) {
                                            Popup(
                                                popupPositionProvider = centerAnchorProvider,
                                                onDismissRequest = { activeField = ActivePickerField.NONE },
                                                properties = PopupProperties(focusable = true)
                                            ) {
                                                Card(
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                                ) {
                                                    ScrollWheelPicker(
                                                        items = durationRange,
                                                        listState = durationListState,
                                                        onItemSelected = { selectedDurationMinutes = it },
                                                        onCenterClicked = { activeField = ActivePickerField.NONE },
                                                        modifier = Modifier.width(56.dp),
                                                        itemHeight = 32.dp,
                                                        visibleItemCount = 5
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = "min",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    if (!isInOperatingHours) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("safemode_hours_warning_card"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Operating Hours Warning",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Selected time is prohibited in Safemode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    } else if (isCurfewExceeded) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("curfew_warning_card"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Curfew Warning",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "ERROR: Session exceeds 3:00 AM curfew. Please reduce duration.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    } else if (isScheduleOverlapping) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("overlap_warning_card"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Overlap Warning",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Schedule is Overlapping",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    } else if (isHardcore) {
                        item {
                            Text(
                                text = "🔥 Hardcore 24/7 Mode: Can schedule at any time of day.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "Safemode: Must be between 7:00 PM and 3:00 AM",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isNavigatingAway) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> ScrollWheelPicker(
    items: List<T>,
    listState: LazyListState,
    onItemSelected: (T) -> Unit,
    onCenterClicked: () -> Unit,
    modifier: Modifier = Modifier,
    visibleItemCount: Int = 5,
    itemHeight: Dp = 32.dp,
    labelFormatter: (T) -> String = { it.toString() }
) {
    val coroutineScope = rememberCoroutineScope()
    val wheelHeight = itemHeight * visibleItemCount

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || items.isEmpty()) return@derivedStateOf listState.firstVisibleItemIndex.coerceIn(0, items.lastIndex)
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            val closest = visibleItems.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                abs(itemCenter - viewportCenter)
            }
            closest?.index?.coerceIn(0, items.lastIndex) ?: listState.firstVisibleItemIndex.coerceIn(0, items.lastIndex)
        }
    }

    LaunchedEffect(centeredIndex) {
        if (centeredIndex in items.indices) {
            onItemSelected(items[centeredIndex])
        }
    }

    Box(
        modifier = modifier.height(wheelHeight),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ) {}

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemCount / 2))
        ) {
            items(items.size) { index ->
                val item = items[index]
                val distance = abs(index - centeredIndex)
                val alpha = when (distance) {
                    0 -> 1f
                    1 -> 0.4f
                    else -> 0.18f
                }
                val scale = when (distance) {
                    0 -> 1.1f
                    1 -> 0.95f
                    else -> 0.8f
                }
                val fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal
                val textColor = if (distance == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (distance == 0) {
                                onCenterClicked()
                            } else {
                                onItemSelected(items[index])
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelFormatter(item),
                        fontSize = (16 * scale).sp,
                        fontWeight = fontWeight,
                        color = textColor.copy(alpha = alpha),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun Modifier.wheelAnchorGesture(
    onTap: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
): Modifier = pointerInput(Unit) {
    val touchSlop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var pointer = down
        var dragged = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointer.id }
            if (change == null || !change.pressed) {
                if (!dragged) {
                    onTap()
                } else {
                    onDragEnd()
                }
                break
            }
            val dragAmount = change.positionChange()
            if (!dragged && abs(dragAmount.y) > touchSlop) {
                dragged = true
                onDragStart()
            }
            if (dragged) {
                change.consume()
                onDrag(dragAmount.y)
            }
            pointer = change
        }
    }
}
