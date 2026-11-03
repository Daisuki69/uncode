package com.uncode.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.uncode.app.UnifiedPolicyRegistry
import com.uncode.app.UnifiedService
import com.uncode.app.data.Deck
import com.uncode.app.viewmodel.AppThemeMode
import com.uncode.app.viewmodel.MainViewModel
import kotlin.math.abs
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Casino
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange

private enum class ExtremePickerField {
    NONE, DECK, DURATION, AUTO_QUIZ_MODE, AUTO_QUIZ_DECK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()
    val operatingMode by viewModel.operatingMode.collectAsStateWithLifecycle()
    val webProtectionMode by viewModel.webProtectionMode.collectAsStateWithLifecycle()
    val activeUnifiedServices by viewModel.activeUnifiedServices.collectAsStateWithLifecycle()
    val isLockdownActive by viewModel.isLockdownActive.collectAsStateWithLifecycle()
    val isConsequenceActive by viewModel.isConsequenceActive.collectAsStateWithLifecycle()
    val focusSchedules by viewModel.focusSchedules.collectAsStateWithLifecycle()
    val failedFocusSessions by viewModel.failedFocusSessions.collectAsStateWithLifecycle()
    val isPolicyLocked = remember(isLockdownActive, isConsequenceActive, focusSchedules, failedFocusSessions) {
        isLockdownActive || isConsequenceActive || failedFocusSessions.isNotEmpty() || focusSchedules.any { it.isRescheduled }
    }
    val autoScheduleEnabled by viewModel.autoScheduleEnabled.collectAsStateWithLifecycle()
    val autoScheduleSelectedDeckId by viewModel.autoScheduleSelectedDeckId.collectAsStateWithLifecycle()
    val autoScheduleSelectedDuration by viewModel.autoScheduleSelectedDuration.collectAsStateWithLifecycle()
    val autoQuizEnabled by viewModel.autoQuizEnabled.collectAsStateWithLifecycle()
    val autoQuizMode by viewModel.autoQuizMode.collectAsStateWithLifecycle()
    val autoQuizSelectedDeckId by viewModel.autoQuizSelectedDeckId.collectAsStateWithLifecycle()
    val allDecks by viewModel.allDecks.collectAsStateWithLifecycle()
    var activeExtremeField by remember { mutableStateOf(ExtremePickerField.NONE) }
    var showIncompatibleModeDialog by remember { mutableStateOf(false) }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.setWebProtectionMode("dual_hybrid")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshLockdownAndServiceState()
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
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Some settings may not allow changes during active sessions, failed session, or pending reschedules.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ThemeOptionRow(
                            title = "System Default",
                            subtitle = "Follow system theme settings",
                            icon = Icons.Default.BrightnessAuto,
                            isSelected = currentTheme == AppThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(AppThemeMode.SYSTEM) },
                            testTag = "theme_system"
                        )

                        ThemeOptionRow(
                            title = "Light Mode",
                            subtitle = "Clean bright theme",
                            icon = Icons.Default.LightMode,
                            isSelected = currentTheme == AppThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) },
                            testTag = "theme_light"
                        )

                        ThemeOptionRow(
                            title = "Dark Mode",
                            subtitle = "Easy on the eyes in low light",
                            icon = Icons.Default.DarkMode,
                            isSelected = currentTheme == AppThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(AppThemeMode.DARK) },
                            testTag = "theme_dark"
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Study Enforcement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Operating Mode",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                        )

                        SettingsSelectionRow(
                            title = "Safemode",
                            subtitle = "Enforcement runs at 7:00 PM - 3:00 AM",
                            isSelected = operatingMode.equals("safemode", ignoreCase = true),
                            onClick = {
                                val success = viewModel.setOperatingMode("safemode")
                                if (!success) {
                                    showIncompatibleModeDialog = true
                                }
                            },
                            testTag = "operating_mode_safemode",
                            enabled = !isPolicyLocked
                        )

                        SettingsSelectionRow(
                            title = "Hardcore",
                            subtitle = "Enforcement runs 24/7",
                            isSelected = operatingMode.equals("hardcore", ignoreCase = true),
                            onClick = { viewModel.setOperatingMode("hardcore") },
                            testTag = "operating_mode_hardcore",
                            enabled = !isPolicyLocked
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )

                        Text(
                            text = "Internet Lock",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                        )

                        SettingsSelectionRow(
                            title = "Accessibility",
                            subtitle = null,
                            isSelected = webProtectionMode.equals("accessibility", ignoreCase = true),
                            onClick = { viewModel.setWebProtectionMode("accessibility") },
                            testTag = "internet_lock_accessibility",
                            enabled = !isPolicyLocked
                        )

                        SettingsSelectionRow(
                            title = "VPN",
                            subtitle = null,
                            isSelected = webProtectionMode.equals("dual_hybrid", ignoreCase = true),
                            onClick = {
                                viewModel.setWebProtectionMode("dual_hybrid") { intent ->
                                    vpnLauncher.launch(intent)
                                }
                            },
                            testTag = "internet_lock_vpn",
                            enabled = !isPolicyLocked
                        )
                    }
                }
            }

            item {
                Text(
                    text = "EXTREME",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                ) {
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

                    val deckItems = remember(allDecks) {
                        listOf<Long?>(null) + allDecks.map { it.id }
                    }
                    val deckWheelListState = rememberLazyListState(
                        initialFirstVisibleItemIndex = deckItems.indexOf(autoScheduleSelectedDeckId).coerceAtLeast(0)
                    )

                    val durationOptions = remember {
                        listOf<Int?>(null) + (10..90 step 5).toList()
                    }
                    val durWheelListState = rememberLazyListState(
                        initialFirstVisibleItemIndex = durationOptions.indexOf(autoScheduleSelectedDuration).coerceAtLeast(0)
                    )

                    val autoQuizModeOptions = remember {
                        listOf("usage_15", "usage_30", "usage_45", "usage_60", "daily_random")
                    }
                    val autoQuizModeWheelListState = rememberLazyListState(
                        initialFirstVisibleItemIndex = autoQuizModeOptions.indexOf(autoQuizMode).coerceAtLeast(0)
                    )
                    val autoQuizDeckWheelListState = rememberLazyListState(
                        initialFirstVisibleItemIndex = deckItems.indexOf(autoQuizSelectedDeckId).coerceAtLeast(0)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "AutoSchedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "1 session / day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (activeExtremeField == ExtremePickerField.DECK) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    },
                                    modifier = Modifier.wheelAnchorGesture(
                                        onTap = {
                                            if (activeExtremeField == ExtremePickerField.DECK) {
                                                activeExtremeField = ExtremePickerField.NONE
                                            } else {
                                                coroutineScope.launch {
                                                    deckWheelListState.scrollToItem(deckItems.indexOf(autoScheduleSelectedDeckId).coerceAtLeast(0))
                                                }
                                                activeExtremeField = ExtremePickerField.DECK
                                            }
                                        },
                                        onDragStart = {
                                            activeExtremeField = ExtremePickerField.DECK
                                        },
                                        onDrag = { dragAmountY ->
                                            deckWheelListState.dispatchRawDelta(-dragAmountY)
                                            val layoutInfo = deckWheelListState.layoutInfo
                                            val visibleItems = layoutInfo.visibleItemsInfo
                                            if (visibleItems.isNotEmpty()) {
                                                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                                val closest = visibleItems.minByOrNull { item ->
                                                    val itemCenter = item.offset + item.size / 2
                                                    abs(itemCenter - viewportCenter)
                                                }
                                                if (closest != null) {
                                                    viewModel.setAutoScheduleDeckId(deckItems[closest.index.coerceIn(0, deckItems.lastIndex)])
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val layoutInfo = deckWheelListState.layoutInfo
                                            val visibleItems = layoutInfo.visibleItemsInfo
                                            val targetIndex = if (visibleItems.isNotEmpty()) {
                                                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                                val closest = visibleItems.minByOrNull { item ->
                                                    val itemCenter = item.offset + item.size / 2
                                                    abs(itemCenter - viewportCenter)
                                                }
                                                closest?.index?.coerceIn(0, deckItems.lastIndex) ?: deckWheelListState.firstVisibleItemIndex.coerceIn(0, deckItems.lastIndex)
                                            } else {
                                                deckWheelListState.firstVisibleItemIndex.coerceIn(0, deckItems.lastIndex)
                                            }
                                            viewModel.setAutoScheduleDeckId(deckItems[targetIndex])
                                            coroutineScope.launch {
                                                deckWheelListState.scrollToItem(targetIndex)
                                            }
                                            activeExtremeField = ExtremePickerField.NONE
                                        }
                                    )
                                ) {
                                    if (autoScheduleSelectedDeckId == null) {
                                        Icon(
                                            imageVector = Icons.Default.Casino,
                                            contentDescription = "Random Deck",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                                .size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = allDecks.find { it.id == autoScheduleSelectedDeckId }?.title ?: "Deck",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (activeExtremeField == ExtremePickerField.DECK) {
                                    Popup(
                                        popupPositionProvider = centerAnchorProvider,
                                        onDismissRequest = { activeExtremeField = ExtremePickerField.NONE },
                                        properties = PopupProperties(focusable = true)
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                        ) {
                                            ScrollWheelPicker(
                                                items = deckItems,
                                                listState = deckWheelListState,
                                                onItemSelected = { selectedId ->
                                                    viewModel.setAutoScheduleDeckId(selectedId)
                                                },
                                                onCenterClicked = { activeExtremeField = ExtremePickerField.NONE },
                                                modifier = Modifier.width(140.dp),
                                                itemHeight = 32.dp,
                                                visibleItemCount = 5,
                                                labelFormatter = { id ->
                                                    if (id == null) "🎲" else allDecks.find { it.id == id }?.title ?: "Deck"
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (activeExtremeField == ExtremePickerField.DURATION) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    },
                                    modifier = Modifier.wheelAnchorGesture(
                                        onTap = {
                                            if (activeExtremeField == ExtremePickerField.DURATION) {
                                                activeExtremeField = ExtremePickerField.NONE
                                            } else {
                                                coroutineScope.launch {
                                                    durWheelListState.scrollToItem(durationOptions.indexOf(autoScheduleSelectedDuration).coerceAtLeast(0))
                                                }
                                                activeExtremeField = ExtremePickerField.DURATION
                                            }
                                        },
                                        onDragStart = {
                                            activeExtremeField = ExtremePickerField.DURATION
                                        },
                                        onDrag = { dragAmountY ->
                                            durWheelListState.dispatchRawDelta(-dragAmountY)
                                            val layoutInfo = durWheelListState.layoutInfo
                                            val visibleItems = layoutInfo.visibleItemsInfo
                                            if (visibleItems.isNotEmpty()) {
                                                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                                val closest = visibleItems.minByOrNull { item ->
                                                    val itemCenter = item.offset + item.size / 2
                                                    abs(itemCenter - viewportCenter)
                                                }
                                                if (closest != null) {
                                                    viewModel.setAutoScheduleDuration(durationOptions[closest.index.coerceIn(0, durationOptions.lastIndex)])
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val layoutInfo = durWheelListState.layoutInfo
                                            val visibleItems = layoutInfo.visibleItemsInfo
                                            val targetIndex = if (visibleItems.isNotEmpty()) {
                                                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                                val closest = visibleItems.minByOrNull { item ->
                                                    val itemCenter = item.offset + item.size / 2
                                                    abs(itemCenter - viewportCenter)
                                                }
                                                closest?.index?.coerceIn(0, durationOptions.lastIndex) ?: durWheelListState.firstVisibleItemIndex.coerceIn(0, durationOptions.lastIndex)
                                            } else {
                                                durWheelListState.firstVisibleItemIndex.coerceIn(0, durationOptions.lastIndex)
                                            }
                                            viewModel.setAutoScheduleDuration(durationOptions[targetIndex])
                                            coroutineScope.launch {
                                                durWheelListState.scrollToItem(targetIndex)
                                            }
                                            activeExtremeField = ExtremePickerField.NONE
                                        }
                                    )
                                ) {
                                    if (autoScheduleSelectedDuration == null) {
                                        Icon(
                                            imageVector = Icons.Default.Casino,
                                            contentDescription = "Random Duration",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                                .size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${autoScheduleSelectedDuration}m",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (activeExtremeField == ExtremePickerField.DURATION) {
                                    Popup(
                                        popupPositionProvider = centerAnchorProvider,
                                        onDismissRequest = { activeExtremeField = ExtremePickerField.NONE },
                                        properties = PopupProperties(focusable = true)
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                        ) {
                                            ScrollWheelPicker(
                                                items = durationOptions,
                                                listState = durWheelListState,
                                                onItemSelected = { selectedMinutes ->
                                                    viewModel.setAutoScheduleDuration(selectedMinutes)
                                                },
                                                onCenterClicked = { activeExtremeField = ExtremePickerField.NONE },
                                                modifier = Modifier.width(110.dp),
                                                itemHeight = 32.dp,
                                                visibleItemCount = 5,
                                                labelFormatter = { mins ->
                                                    if (mins == null) "🎲" else "${mins}m"
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Switch(
                                checked = autoScheduleEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setAutoScheduleEnabled(enabled)
                                    if (enabled) {
                                        val armed = viewModel.armAutoSchedule()
                                        if (armed) {
                                            Toast.makeText(context, "AutoSchedule armed for today", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Daily session already scheduled today", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isPolicyLocked
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "AutoQuiz",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "based on usage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (activeExtremeField == ExtremePickerField.AUTO_QUIZ_MODE) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    },
                                    modifier = Modifier.wheelAnchorGesture(
                                        onTap = {
                                            if (activeExtremeField == ExtremePickerField.AUTO_QUIZ_MODE) {
                                                activeExtremeField = ExtremePickerField.NONE
                                            } else {
                                                coroutineScope.launch {
                                                    autoQuizModeWheelListState.scrollToItem(autoQuizModeOptions.indexOf(autoQuizMode).coerceAtLeast(0))
                                                }
                                                activeExtremeField = ExtremePickerField.AUTO_QUIZ_MODE
                                            }
                                        },
                                        onDragStart = {
                                            activeExtremeField = ExtremePickerField.AUTO_QUIZ_MODE
                                        },
                                        onDrag = { dragAmountY ->
                                            autoQuizModeWheelListState.dispatchRawDelta(-dragAmountY)
                                            val layoutInfo = autoQuizModeWheelListState.layoutInfo
                                            val visibleItems = layoutInfo.visibleItemsInfo
                                            if (visibleItems.isNotEmpty()) {
                                                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                                val closest = visibleItems.minByOrNull { item ->
                                                    val itemCenter = item.offset + item.size / 2
                                                    abs(itemCenter - viewportCenter)
                                                }
                                                if (closest != null) {
                                                    viewModel.setAutoQuizMode(autoQuizModeOptions[closest.index.coerceIn(0, autoQuizModeOptions.lastIndex)])
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val layoutInfo = autoQuizModeWheelListState.layoutInfo
                                            val visibleItems = layoutInfo.visibleItemsInfo
                                            val targetIndex = if (visibleItems.isNotEmpty()) {
                                                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                                val closest = visibleItems.minByOrNull { item ->
                                                    val itemCenter = item.offset + item.size / 2
                                                    abs(itemCenter - viewportCenter)
                                                }
                                                closest?.index?.coerceIn(0, autoQuizModeOptions.lastIndex) ?: autoQuizModeWheelListState.firstVisibleItemIndex.coerceIn(0, autoQuizModeOptions.lastIndex)
                                            } else {
                                                autoQuizModeWheelListState.firstVisibleItemIndex.coerceIn(0, autoQuizModeOptions.lastIndex)
                                            }
                                            viewModel.setAutoQuizMode(autoQuizModeOptions[targetIndex])
                                            coroutineScope.launch {
                                                autoQuizModeWheelListState.scrollToItem(targetIndex)
                                            }
                                            activeExtremeField = ExtremePickerField.NONE
                                        }
                                    )
                                ) {
                                    val modeLabel = when (autoQuizMode) {
                                        "usage_15" -> "15m"
                                        "usage_30" -> "30m"
                                        "usage_45" -> "45m"
                                        "usage_60" -> "60m"
                                        "daily_random" -> "Daily"
                                        else -> "30m"
                                    }
                                    Text(
                                        text = modeLabel,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        maxLines = 1
                                    )
                                }

                                if (activeExtremeField == ExtremePickerField.AUTO_QUIZ_MODE) {
                                    Popup(
                                        popupPositionProvider = centerAnchorProvider,
                                        onDismissRequest = { activeExtremeField = ExtremePickerField.NONE },
                                        properties = PopupProperties(focusable = true)
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                        ) {
                                            ScrollWheelPicker(
                                                items = autoQuizModeOptions,
                                                listState = autoQuizModeWheelListState,
                                                onItemSelected = { selectedMode ->
                                                    viewModel.setAutoQuizMode(selectedMode)
                                                },
                                                onCenterClicked = { activeExtremeField = ExtremePickerField.NONE },
                                                modifier = Modifier.width(130.dp),
                                                itemHeight = 32.dp,
                                                visibleItemCount = 5,
                                                labelFormatter = { mode ->
                                                    when (mode) {
                                                        "usage_15" -> "15m"
                                                        "usage_30" -> "30m"
                                                        "usage_45" -> "45m"
                                                        "usage_60" -> "60m"
                                                        "daily_random" -> "Daily"
                                                        else -> mode
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (activeExtremeField == ExtremePickerField.AUTO_QUIZ_DECK) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    },
                                    modifier = Modifier.wheelAnchorGesture(
                                        onTap = {
                                            if (activeExtremeField == ExtremePickerField.AUTO_QUIZ_DECK) {
                                                activeExtremeField = ExtremePickerField.NONE
                                            } else {
                                                coroutineScope.launch {
                                                    autoQuizDeckWheelListState.scrollToItem(deckItems.indexOf(autoQuizSelectedDeckId).coerceAtLeast(0))
                                                }
                                                activeExtremeField = ExtremePickerField.AUTO_QUIZ_DECK
                                            }
                                        },
                                        onDragStart = {
                                            activeExtremeField = ExtremePickerField.AUTO_QUIZ_DECK
                                        },
                                        onDrag = { dragAmountY ->
                                            autoQuizDeckWheelListState.dispatchRawDelta(-dragAmountY)
                                            val layoutInfo = autoQuizDeckWheelListState.layoutInfo
                                            val visibleItems = layoutInfo.visibleItemsInfo
                                            if (visibleItems.isNotEmpty()) {
                                                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                                val closest = visibleItems.minByOrNull { item ->
                                                    val itemCenter = item.offset + item.size / 2
                                                    abs(itemCenter - viewportCenter)
                                                }
                                                if (closest != null) {
                                                    viewModel.setAutoQuizDeckId(deckItems[closest.index.coerceIn(0, deckItems.lastIndex)])
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val layoutInfo = autoQuizDeckWheelListState.layoutInfo
                                            val visibleItems = layoutInfo.visibleItemsInfo
                                            val targetIndex = if (visibleItems.isNotEmpty()) {
                                                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                                val closest = visibleItems.minByOrNull { item ->
                                                    val itemCenter = item.offset + item.size / 2
                                                    abs(itemCenter - viewportCenter)
                                                }
                                                closest?.index?.coerceIn(0, deckItems.lastIndex) ?: autoQuizDeckWheelListState.firstVisibleItemIndex.coerceIn(0, deckItems.lastIndex)
                                            } else {
                                                autoQuizDeckWheelListState.firstVisibleItemIndex.coerceIn(0, deckItems.lastIndex)
                                            }
                                            viewModel.setAutoQuizDeckId(deckItems[targetIndex])
                                            coroutineScope.launch {
                                                autoQuizDeckWheelListState.scrollToItem(targetIndex)
                                            }
                                            activeExtremeField = ExtremePickerField.NONE
                                        }
                                    )
                                ) {
                                    if (autoQuizSelectedDeckId == null) {
                                        Icon(
                                            imageVector = Icons.Default.Casino,
                                            contentDescription = "Random Deck",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                                .size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = allDecks.find { it.id == autoQuizSelectedDeckId }?.title ?: "Deck",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (activeExtremeField == ExtremePickerField.AUTO_QUIZ_DECK) {
                                    Popup(
                                        popupPositionProvider = centerAnchorProvider,
                                        onDismissRequest = { activeExtremeField = ExtremePickerField.NONE },
                                        properties = PopupProperties(focusable = true)
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                        ) {
                                            ScrollWheelPicker(
                                                items = deckItems,
                                                listState = autoQuizDeckWheelListState,
                                                onItemSelected = { selectedId ->
                                                    viewModel.setAutoQuizDeckId(selectedId)
                                                },
                                                onCenterClicked = { activeExtremeField = ExtremePickerField.NONE },
                                                modifier = Modifier.width(140.dp),
                                                itemHeight = 32.dp,
                                                visibleItemCount = 5,
                                                labelFormatter = { id ->
                                                    if (id == null) "🎲" else allDecks.find { it.id == id }?.title ?: "Deck"
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Switch(
                                checked = autoQuizEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setAutoQuizEnabled(enabled)
                                },
                                enabled = !isPolicyLocked
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Policy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPolicyLocked) {
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
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        val services = UnifiedPolicyRegistry.SERVICES.values.toList()
                        services.forEachIndexed { index, svc ->
                            val isAllowed = activeUnifiedServices.contains(svc.id)
                            PolicyServiceItem(
                                service = svc,
                                isAllowed = isAllowed,
                                onToggle = { viewModel.toggleUnifiedService(svc.id, !isAllowed) },
                                enabled = !isPolicyLocked
                            )
                            if (index < services.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("export_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Export", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("import_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Import", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SIREN Quiz App",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        if (showIncompatibleModeDialog) {
            AlertDialog(
                onDismissRequest = { showIncompatibleModeDialog = false },
                title = {
                    Text(
                        text = "Cannot Switch to Safemode",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("Cannot switch to Safemode: Some existing schedule is incompatible. Delete or reschedule it first.")
                },
                confirmButton = {
                    Button(
                        onClick = { showIncompatibleModeDialog = false }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSelectionRow(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 14.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            )
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
        RadioButton(
            selected = isSelected,
            onClick = { if (enabled) onClick() },
            enabled = enabled
        )
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
    }
}

@Composable
private fun PolicyServiceItem(
    service: UnifiedService,
    isAllowed: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("policy_service_${service.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = service.displayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
                if (service.badge.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (enabled) 0.6f else 0.3f)
                    ) {
                        Text(
                            text = service.badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            if (service.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = service.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    lineHeight = 16.sp
                )
            }
        }
        Switch(
            checked = isAllowed,
            onCheckedChange = { if (enabled) onToggle() },
            enabled = enabled,
            modifier = Modifier.testTag("policy_switch_${service.id}")
        )
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
                        color = textColor.copy(alpha = alpha)
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

