package com.uncode.app.math.ui.keyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.uncode.app.math.ui.theme.JetBrainsMono
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun isSupportedInJetBrainsMono(text: String): Boolean =
    text.all { c -> (c.code in 0x20..0x7E) || c in "²³·÷×−" }

@Composable
fun SciCalcKeyboard(
    selectedTab: KeyboardTab,
    onTabSelected: (KeyboardTab) -> Unit,
    onKeyClick: (SciCalcKey) -> Unit,
    onSelectAlternate: (SciCalcKey, SciCalcKey) -> Unit = { _, alt -> onKeyClick(alt) },
    onPrepareGesture: () -> Unit = {},
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onCursorLeft: () -> Unit,
    onCursorRight: () -> Unit,
    onNextStep: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.surface.luminance() > 0.5f

    var previousMathTab by remember { mutableStateOf(KeyboardTab.BASIC) }
    if (selectedTab != KeyboardTab.ABC) {
        previousMathTab = selectedTab
    }

    val containerBg = if (isLight) scheme.surfaceVariant else Color(0xFF0F131C)
    val containerBorder = if (isLight) scheme.outline else Color(0xFF1E2433)
    val utilBg = if (isLight) scheme.surface else Color(0xFF1A202E)
    val utilBorder = if (isLight) scheme.outline else Color(0xFF29334A)
    val utilIconTint = if (isLight) scheme.onSurface else Color(0xFFCBD5E1)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("scicalc_keyboard_container"),
        color = containerBg,
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, containerBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAbcActive = selectedTab == KeyboardTab.ABC
                TouchDownButton(
                    onAction = {
                        if (isAbcActive) {
                            onTabSelected(previousMathTab)
                        } else {
                            onTabSelected(KeyboardTab.ABC)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "keyboard_tab_abc",
                    backgroundColor = if (isAbcActive) {
                        if (isLight) scheme.primaryContainer else Color(0xFF28354E)
                    } else utilBg,
                    borderColor = if (isAbcActive) scheme.primary else utilBorder
                ) {
                    Text(
                        text = "abc",
                        color = if (isAbcActive) scheme.onSurface else utilIconTint,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp
                    )
                }

                RepeatActionButton(
                    onAction = onCursorLeft,
                    modifier = Modifier.weight(1f),
                    testTag = "keyboard_btn_cursor_left",
                    backgroundColor = utilBg,
                    borderColor = utilBorder
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cursor Left",
                        tint = utilIconTint,
                        modifier = Modifier.size(17.dp)
                    )
                }

                RepeatActionButton(
                    onAction = onCursorRight,
                    modifier = Modifier.weight(1f),
                    testTag = "keyboard_btn_cursor_right",
                    backgroundColor = utilBg,
                    borderColor = utilBorder
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Cursor Right",
                        tint = utilIconTint,
                        modifier = Modifier.size(17.dp)
                    )
                }

                TouchDownButton(
                    onAction = onNextStep,
                    modifier = Modifier.weight(1f),
                    testTag = "keyboard_btn_next_step",
                    backgroundColor = if (isLight) scheme.primaryContainer else Color(0xFF1E2638),
                    borderColor = if (isLight) scheme.primary.copy(alpha = 0.5f) else Color(0xFF2E3B56)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = "Next Step",
                        tint = scheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                RepeatActionButton(
                    onAction = onBackspace,
                    modifier = Modifier.weight(1f),
                    testTag = "keyboard_btn_backspace",
                    backgroundColor = if (isLight) Color(0xFFFEF3C7) else Color(0xFF282015),
                    borderColor = if (isLight) Color(0xFFF59E0B) else Color(0xFF47361C)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = if (isLight) Color(0xFFD97706) else Color(0xFFFBBF24),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            val chipTabs = listOf(
                KeyboardTab.BASIC,
                KeyboardTab.FUNCTIONS,
                KeyboardTab.TRIG,
                KeyboardTab.CALCULUS
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                chipTabs.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val tabBg by animateColorAsState(
                        targetValue = if (isSelected) {
                            if (isLight) scheme.primaryContainer else Color(0xFF25324B)
                        } else {
                            if (isLight) scheme.surface else Color(0xFF141923)
                        },
                        label = "tabBg"
                    )
                    val tabBorder by animateColorAsState(
                        targetValue = if (isSelected) scheme.primary else utilBorder,
                        label = "tabBorder"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) scheme.onSurface else scheme.onSurfaceVariant,
                        label = "tabText"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(tabBg)
                            .border(BorderStroke(1.dp, tabBorder), RoundedCornerShape(19.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab)
                            }
                            .testTag("keyboard_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = tab.topLine,
                                color = textColor,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = tab.bottomLine,
                                color = textColor,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            val currentRows = when (selectedTab) {
                KeyboardTab.BASIC -> KeyboardLayouts.basicRows
                KeyboardTab.FUNCTIONS -> KeyboardLayouts.functionRows
                KeyboardTab.TRIG -> KeyboardLayouts.trigRows
                KeyboardTab.CALCULUS -> KeyboardLayouts.calculusRows
                KeyboardTab.ABC -> KeyboardLayouts.abcRows
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                currentRows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowKeys.forEach { key ->
                            KeyButton(
                                key = key,
                                onAction = { onKeyClick(key) },
                                onSelectAlternate = { altKey ->
                                    onKeyClick(altKey)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixDimensionDialog(
    isDeterminant: Boolean = false,
    initialRows: Int = 3,
    initialCols: Int = 3,
    onDismiss: () -> Unit,
    onInsert: (rows: Int, cols: Int) -> Unit
) {
    var selectedRows by remember { mutableIntStateOf(initialRows.coerceIn(1, 5)) }
    var selectedCols by remember { mutableIntStateOf(initialCols.coerceIn(1, 5)) }
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.surface.luminance() > 0.5f
    val crimsonColor = Color(0xFFC8102E)
    val bracketColor = scheme.onSurface
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("matrix_dimension_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = if (isLight) scheme.surface else Color(0xFF1A202E),
            border = BorderStroke(1.dp, if (isLight) scheme.outline else Color(0xFF2E3B56)),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Matrices",
                    color = scheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "$selectedRows × $selectedCols",
                    color = Color(0xFFFB7185),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("matrix_dimension_label")
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .fillMaxHeight()
                            .drawBehind {
                                val stroke = 2.8.dp.toPx()
                                drawLine(bracketColor, Offset(size.width, stroke / 2), Offset(stroke / 2, stroke / 2), stroke)
                                drawLine(bracketColor, Offset(stroke / 2, 0f), Offset(stroke / 2, size.height), stroke)
                                drawLine(bracketColor, Offset(stroke / 2, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
                            }
                    )

                    val cellPx = with(density) { 40.dp.toPx() }
                    val gapPx = with(density) { 8.dp.toPx() }
                    val stepPx = cellPx + gapPx
                    val padHPx = with(density) { 14.dp.toPx() }
                    val padVPx = with(density) { 6.dp.toPx() }

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    fun updateFromOffset(offset: Offset) {
                                        val relX = (offset.x - padHPx).coerceAtLeast(0f)
                                        val relY = (offset.y - padVPx).coerceAtLeast(0f)
                                        val c = ((relX / stepPx).toInt() + 1).coerceIn(1, 5)
                                        val r = ((relY / stepPx).toInt() + 1).coerceIn(1, 5)
                                        if (r != selectedRows || c != selectedCols) {
                                            selectedRows = r
                                            selectedCols = c
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                    updateFromOffset(down.position)
                                    do {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull()
                                        if (change != null && change.pressed) {
                                            updateFromOffset(change.position)
                                            change.consume()
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                            .testTag("matrix_drag_grid"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (r in 1..5) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (c in 1..5) {
                                    val isSelected = r <= selectedRows && c <= selectedCols
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(
                                                if (isSelected) crimsonColor
                                                else if (isLight) scheme.surfaceVariant
                                                else Color(0xFF141923)
                                            )
                                            .border(
                                                BorderStroke(
                                                    width = 1.5.dp,
                                                    color = if (isSelected) Color(0xFFFB7185) else if (isLight) scheme.outline else Color(0xFF38435C)
                                                ),
                                                RoundedCornerShape(7.dp)
                                            )
                                            .clickable {
                                                selectedRows = r
                                                selectedCols = c
                                            }
                                            .testTag("matrix_cell_${r}_${c}")
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .fillMaxHeight()
                            .drawBehind {
                                val stroke = 2.8.dp.toPx()
                                val x = size.width - stroke / 2
                                drawLine(bracketColor, Offset(0f, stroke / 2), Offset(x, stroke / 2), stroke)
                                drawLine(bracketColor, Offset(x, 0f), Offset(x, size.height), stroke)
                                drawLine(bracketColor, Offset(0f, size.height - stroke / 2), Offset(x, size.height - stroke / 2), stroke)
                            }
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                Button(
                    onClick = { onInsert(selectedRows, selectedCols) },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = crimsonColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .width(150.dp)
                        .height(48.dp)
                        .testTag("matrix_insert_button")
                ) {
                    Text(
                        text = "Insert",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

@Composable
private fun TouchDownButton(
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
    backgroundColor: Color = Color(0xFF1A202E),
    borderColor: Color = Color(0xFF29334A),
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val bg = if (isPressed) backgroundColor.copy(alpha = 0.6f) else backgroundColor

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .pointerInput(onAction) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAction()
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun RepeatActionButton(
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
    backgroundColor: Color = Color(0xFF1A202E),
    borderColor: Color = Color(0xFF29334A),
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val bg = if (isPressed) backgroundColor.copy(alpha = 0.6f) else backgroundColor

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .pointerInput(onAction) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAction()

                        val repeatJob = launch {
                            delay(380L)
                            while (down.pressed) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAction()
                                delay(45L)
                            }
                        }

                        waitForUpOrCancellation()
                        repeatJob.cancel()
                        isPressed = false
                    }
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun KeyButton(
    key: SciCalcKey,
    onAction: () -> Unit,
    onSelectAlternate: (SciCalcKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.surface.luminance() > 0.5f

    if (key.label.isEmpty()) {
        Box(
            modifier = modifier
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isLight) scheme.surfaceVariant.copy(alpha = 0.6f) else Color(0xFF131824))
                .border(BorderStroke(1.dp, if (isLight) scheme.outline.copy(alpha = 0.5f) else Color(0xFF1D2436)), RoundedCornerShape(10.dp))
        )
        return
    }

    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    var isPopupOpen by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val (keyBg, borderColor, textColor) = if (isLight) {
        when (key.category) {
            KeyCategory.NUMBER -> Triple(scheme.surface, scheme.outline, scheme.onSurface)
            KeyCategory.OPERATOR -> Triple(scheme.primaryContainer.copy(alpha = 0.45f), scheme.outline, scheme.primary)
            KeyCategory.VARIABLE -> Triple(scheme.surface, scheme.outline, scheme.secondary)
            KeyCategory.FUNCTION -> Triple(scheme.surface, scheme.outline, scheme.tertiary)
            KeyCategory.ACTION -> Triple(Color(0xFFE11D48), Color(0xFFFB7185), Color.White)
        }
    } else {
        when (key.category) {
            KeyCategory.NUMBER -> Triple(Color(0xFF202738), Color(0xFF303B52), Color(0xFFF8FAFC))
            KeyCategory.OPERATOR -> Triple(Color(0xFF172030), Color(0xFF26334D), Color(0xFF38BDF8))
            KeyCategory.VARIABLE -> Triple(Color(0xFF192338), Color(0xFF2A3A5E), Color(0xFF818CF8))
            KeyCategory.FUNCTION -> Triple(Color(0xFF181E2E), Color(0xFF27324B), Color(0xFFC084FC))
            KeyCategory.ACTION -> Triple(Color(0xFFE11D48), Color(0xFFFB7185), Color(0xFFFFFFFF))
        }
    }

    val displayBg = if (isPressed) keyBg.copy(alpha = 0.55f) else keyBg

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(displayBg)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
            .pointerInput(key) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                        if (key.alternates.isNotEmpty()) {
                            val pointerId = down.id
                            var popupActive = false

                            val holdJob = launch {
                                delay(220L)
                                popupActive = true
                                isPopupOpen = true
                                hoveredIndex = 0
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            val itemWidthPx = with(density) { 44.dp.toPx() }
                            val maxDeltaY = with(density) { 95.dp.toPx() }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.find { it.id == pointerId } ?: break

                                if (popupActive) {
                                    val deltaX = change.position.x - down.position.x
                                    val deltaY = change.position.y - down.position.y

                                    val calculatedIdx = (deltaX / itemWidthPx).toInt().coerceIn(0, key.alternates.lastIndex)
                                    if (calculatedIdx != hoveredIndex) {
                                        hoveredIndex = calculatedIdx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }

                                    if (change.changedToUp()) {
                                        change.consume()
                                        holdJob.cancel()
                                        isPopupOpen = false
                                        isPressed = false
                                        if (deltaY > -maxDeltaY * 2 && deltaY < maxDeltaY) {
                                            onSelectAlternate(key.alternates[hoveredIndex])
                                        }
                                        break
                                    }
                                } else {
                                    if (change.changedToUp()) {
                                        change.consume()
                                        holdJob.cancel()
                                        isPressed = false
                                        onAction()
                                        break
                                    }
                                }
                            }
                        } else {
                            onAction()
                            waitForUpOrCancellation()
                            isPressed = false
                        }
                    }
                }
            }
            .testTag("keyboard_key_${key.label}"),
        contentAlignment = Alignment.Center
    ) {
        RenderKeyLabel(
            key = key,
            textColor = textColor,
            compact = false
        )

        if (key.hasAlternate || key.alternates.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 4.dp)
                    .size(4.dp)
                    .background(Color(0xFFEF4444), CircleShape)
            )
        }

        if (isPopupOpen && key.alternates.isNotEmpty()) {
            val offsetYPx = with(density) { (-54).dp.roundToPx() }

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, offsetYPx),
                properties = PopupProperties(focusable = false)
            ) {
                KeyPopupSelector(
                    alternates = key.alternates,
                    hoveredIndex = hoveredIndex
                )
            }
        }
    }
}

@Composable
private fun RenderKeyLabel(
    key: SciCalcKey,
    textColor: Color,
    compact: Boolean
) {
    when (key.label) {
        "(□)" -> ParenSlotKeyIcon(color = textColor)
        "a/b" -> FractionKeyIcon(color = textColor)
        "mixed_frac" -> MixedFractionKeyIcon(color = textColor)
        "√x", "√" -> SquareRootKeyIcon(color = textColor)
        "∛" -> IndexedRootKeyIcon(degree = "3", color = textColor)
        "ⁿ√" -> IndexedRootKeyIcon(degree = "□", color = textColor)
        "xⁿ" -> PowerEmptyKeyIcon(color = textColor)
        "x²" -> PowerBoxKeyIcon(exp = "2", color = textColor)
        "x³" -> PowerBoxKeyIcon(exp = "3", color = textColor)
        "x⁴" -> PowerBoxKeyIcon(exp = "4", color = textColor)
        "π/2" -> StackedTextFractionIcon(num = "π", den = "2", color = textColor)
        "π/3" -> StackedTextFractionIcon(num = "π", den = "3", color = textColor)
        "|x|" -> AbsValKeyIcon(color = textColor)
        "log₁₀" -> SubscriptKeyText(base = "log", sub = "10", color = textColor, compact = compact)
        "log₂" -> SubscriptKeyText(base = "log", sub = "2", color = textColor, compact = compact)
        "log_□" -> LogBaseSlotKeyIcon(color = textColor)
        "□_□" -> SubscriptBoxesKeyIcon(color = textColor)
        "□(□)" -> FnCallBoxKeyIcon(twoArgs = false, color = textColor)
        "□(□,□)" -> FnCallBoxKeyIcon(twoArgs = true, color = textColor)
        "□P□" -> InfixBoxesKeyIcon(mid = "P", color = textColor)
        "□C□" -> InfixBoxesKeyIcon(mid = "C", color = textColor)
        "(□/□)" -> BinomialKeyIcon(color = textColor)
        "[□]" -> MatrixKeyIcon(isDet = false, color = textColor)
        "det" -> MatrixKeyIcon(isDet = true, color = textColor)
        "□,□,□" -> CommaBoxesKeyIcon(color = textColor)
        "□°" -> DegreeBoxKeyIcon(mode = 1, color = textColor)
        "□°□'" -> DegreeBoxKeyIcon(mode = 2, color = textColor)
        "□°□'□''" -> DegreeBoxKeyIcon(mode = 3, color = textColor)
        "d/dx" -> DerivativeKeyIcon(mode = 1, color = textColor)
        "dy/dx" -> DerivativeKeyIcon(mode = 0, color = textColor)
        "d/d□" -> DerivativeKeyIcon(mode = 2, color = textColor)
        "d²/dx²" -> DerivativeKeyIcon(mode = 3, color = textColor)
        "lim" -> LimitKeyIcon(suffix = "", color = textColor)
        "lim⁺" -> LimitKeyIcon(suffix = "+", color = textColor)
        "lim⁻" -> LimitKeyIcon(suffix = "−", color = textColor)
        "∫" -> IntegralKeyIcon(mode = 1, color = textColor)
        "∫d□" -> IntegralKeyIcon(mode = 2, color = textColor)
        "∫_a^b" -> IntegralKeyIcon(mode = 3, color = textColor)
        "∑" -> SigmaKeyIcon(color = textColor)
        "aₙ" -> SubscriptKeyText(base = "a", sub = "n", color = textColor, compact = compact)
        else -> {
            val isSingleLetterVar = key.category == KeyCategory.VARIABLE && key.label.length == 1 && (key.label[0].isLetter() || key.label[0] in "αβγδεθλμπρστφωΔΦ")
            val fontFamily = when {
                isSingleLetterVar -> FontFamily.SansSerif
                isSupportedInJetBrainsMono(key.label) -> JetBrainsMono
                else -> FontFamily.SansSerif
            }
            Text(
                text = key.label,
                color = textColor,
                fontSize = when {
                    compact && key.label.length >= 4 -> 11.sp
                    compact -> 14.sp
                    key.category == KeyCategory.ACTION -> 19.sp
                    key.category == KeyCategory.NUMBER -> 18.sp
                    key.label.length >= 6 -> 11.sp
                    key.label.length >= 4 -> 12.sp
                    key.label.length == 3 -> 13.sp
                    else -> 16.sp
                },
                fontWeight = when (key.category) {
                    KeyCategory.ACTION -> FontWeight.Bold
                    KeyCategory.NUMBER -> FontWeight.SemiBold
                    else -> FontWeight.Medium
                },
                fontStyle = FontStyle.Normal,
                fontFamily = fontFamily,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

private fun DrawScope.drawDottedBox(
    topLeft: Offset,
    boxSize: Size,
    color: Color
) {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(2.5.dp.toPx(), 2.dp.toPx()), 0f)
    drawRoundRect(
        color = color.copy(alpha = 0.85f),
        topLeft = topLeft,
        size = boxSize,
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = Stroke(width = 1.3.dp.toPx(), pathEffect = dashEffect)
    )
}

@Composable
private fun DottedPlaceholderBox(
    color: Color,
    widthDp: Float = 10f,
    heightDp: Float = 11f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(widthDp.dp, heightDp.dp)) {
        drawDottedBox(
            topLeft = Offset.Zero,
            boxSize = size,
            color = color
        )
    }
}

@Composable
private fun ParenSlotKeyIcon(color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text("(", color = color, fontSize = 17.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium)
        DottedPlaceholderBox(color = color, widthDp = 11f, heightDp = 12f)
        Text(")", color = color, fontSize = 17.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FractionKeyIcon(
    color: Color,
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawDottedBox(
            topLeft = Offset(w * 0.30f, h * 0.06f),
            boxSize = Size(w * 0.40f, h * 0.32f),
            color = color
        )
        drawLine(
            color = color,
            start = Offset(w * 0.16f, h * 0.50f),
            end = Offset(w * 0.84f, h * 0.50f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawDottedBox(
            topLeft = Offset(w * 0.30f, h * 0.62f),
            boxSize = Size(w * 0.40f, h * 0.32f),
            color = color
        )
    }
}

@Composable
private fun MixedFractionKeyIcon(
    color: Color,
    modifier: Modifier = Modifier.size(28.dp, 24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawDottedBox(
            topLeft = Offset(w * 0.05f, h * 0.32f),
            boxSize = Size(w * 0.30f, h * 0.36f),
            color = color
        )
        drawDottedBox(
            topLeft = Offset(w * 0.50f, h * 0.06f),
            boxSize = Size(w * 0.34f, h * 0.32f),
            color = color
        )
        drawLine(
            color = color,
            start = Offset(w * 0.44f, h * 0.50f),
            end = Offset(w * 0.92f, h * 0.50f),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawDottedBox(
            topLeft = Offset(w * 0.50f, h * 0.62f),
            boxSize = Size(w * 0.34f, h * 0.32f),
            color = color
        )
    }
}

@Composable
private fun StackedTextFractionIcon(
    num: String,
    den: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(num, color = color, fontSize = 10.sp, lineHeight = 10.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.width(14.dp).height(1.2.dp).background(color))
        Text(den, color = color, fontSize = 10.sp, lineHeight = 10.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SquareRootKeyIcon(
    color: Color,
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val sqrtPath = Path().apply {
            moveTo(w * 0.06f, h * 0.58f)
            lineTo(w * 0.22f, h * 0.80f)
            lineTo(w * 0.38f, h * 0.16f)
            lineTo(w * 0.94f, h * 0.16f)
        }

        drawPath(
            path = sqrtPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawDottedBox(
            topLeft = Offset(w * 0.44f, h * 0.32f),
            boxSize = Size(w * 0.42f, h * 0.46f),
            color = color
        )
    }
}

@Composable
private fun IndexedRootKeyIcon(
    degree: String,
    color: Color
) {
    Box(
        modifier = Modifier.size(28.dp, 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (degree == "□") {
            DottedPlaceholderBox(
                color = color,
                widthDp = 6.5f,
                heightDp = 6.5f,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 1.dp, start = 1.dp)
            )
        } else {
            Text(
                text = degree,
                color = color,
                fontSize = 8.5.sp,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 1.dp)
            )
        }
        SquareRootKeyIcon(
            color = color,
            modifier = Modifier
                .size(22.dp)
                .align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun PowerBoxKeyIcon(
    exp: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        DottedPlaceholderBox(color = color, widthDp = 11f, heightDp = 12f, modifier = Modifier.padding(top = 3.dp))
        Text(
            text = exp,
            color = color,
            fontSize = 10.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, start = 1.dp)
        )
    }
}

@Composable
private fun PowerEmptyKeyIcon(color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        DottedPlaceholderBox(color = color, widthDp = 11f, heightDp = 12f, modifier = Modifier.padding(top = 3.dp))
        Spacer(modifier = Modifier.width(1.5.dp))
        DottedPlaceholderBox(color = color, widthDp = 7.5f, heightDp = 8f, modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun SubscriptBoxesKeyIcon(color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        DottedPlaceholderBox(color = color, widthDp = 11f, heightDp = 12f, modifier = Modifier.padding(bottom = 3.dp))
        Spacer(modifier = Modifier.width(1.5.dp))
        DottedPlaceholderBox(color = color, widthDp = 7.5f, heightDp = 8f, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun LogBaseSlotKeyIcon(color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text("log", color = color, fontSize = 12.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium)
        DottedPlaceholderBox(color = color, widthDp = 6.5f, heightDp = 7f, modifier = Modifier.padding(top = 7.dp, start = 1.dp))
    }
}

@Composable
private fun FnCallBoxKeyIcon(twoArgs: Boolean, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        DottedPlaceholderBox(color = color, widthDp = 7.5f, heightDp = 9f)
        Text("(", color = color, fontSize = 12.sp, fontFamily = JetBrainsMono)
        DottedPlaceholderBox(color = color, widthDp = 7f, heightDp = 8.5f)
        if (twoArgs) {
            Text(",", color = color, fontSize = 11.sp, fontFamily = JetBrainsMono)
            DottedPlaceholderBox(color = color, widthDp = 7f, heightDp = 8.5f)
        }
        Text(")", color = color, fontSize = 12.sp, fontFamily = JetBrainsMono)
    }
}

@Composable
private fun InfixBoxesKeyIcon(mid: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        DottedPlaceholderBox(color = color, widthDp = 7f, heightDp = 7.5f, modifier = Modifier.padding(top = 6.dp))
        Text(mid, color = color, fontSize = 13.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 1.dp))
        DottedPlaceholderBox(color = color, widthDp = 7f, heightDp = 7.5f, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun BinomialKeyIcon(color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text("(", color = color, fontSize = 18.sp, fontFamily = JetBrainsMono)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DottedPlaceholderBox(color = color, widthDp = 8f, heightDp = 7.5f)
            DottedPlaceholderBox(color = color, widthDp = 8f, heightDp = 7.5f)
        }
        Text(")", color = color, fontSize = 18.sp, fontFamily = JetBrainsMono)
    }
}

@Composable
private fun MatrixKeyIcon(isDet: Boolean, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(if (isDet) "|" else "[", color = color, fontSize = 17.sp, fontFamily = JetBrainsMono)
        Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                DottedPlaceholderBox(color = color, widthDp = 6f, heightDp = 6f)
                DottedPlaceholderBox(color = color, widthDp = 6f, heightDp = 6f)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                DottedPlaceholderBox(color = color, widthDp = 6f, heightDp = 6f)
                DottedPlaceholderBox(color = color, widthDp = 6f, heightDp = 6f)
            }
        }
        Text(if (isDet) "|" else "]", color = color, fontSize = 17.sp, fontFamily = JetBrainsMono)
    }
}

@Composable
private fun CommaBoxesKeyIcon(color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        DottedPlaceholderBox(color = color, widthDp = 6.5f, heightDp = 7.5f)
        Text(",", color = color, fontSize = 11.sp, fontFamily = JetBrainsMono)
        DottedPlaceholderBox(color = color, widthDp = 6.5f, heightDp = 7.5f)
        Text(",", color = color, fontSize = 11.sp, fontFamily = JetBrainsMono)
        DottedPlaceholderBox(color = color, widthDp = 6.5f, heightDp = 7.5f)
        Text("…", color = color, fontSize = 10.sp, fontFamily = FontFamily.SansSerif)
    }
}

@Composable
private fun DegreeBoxKeyIcon(mode: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        DottedPlaceholderBox(color = color, widthDp = 7.5f, heightDp = 8.5f, modifier = Modifier.padding(top = 2.dp))
        Text("°", color = color, fontSize = 11.sp, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(bottom = 5.dp))
        if (mode >= 2) {
            DottedPlaceholderBox(color = color, widthDp = 7.5f, heightDp = 8.5f, modifier = Modifier.padding(top = 2.dp))
            Text("'", color = color, fontSize = 11.sp, fontFamily = JetBrainsMono, modifier = Modifier.padding(bottom = 5.dp))
        }
        if (mode >= 3) {
            DottedPlaceholderBox(color = color, widthDp = 7.5f, heightDp = 8.5f, modifier = Modifier.padding(top = 2.dp))
            Text("\"", color = color, fontSize = 11.sp, fontFamily = JetBrainsMono, modifier = Modifier.padding(bottom = 5.dp))
        }
    }
}

@Composable
private fun AbsValKeyIcon(color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(1.8.dp)
                .height(17.dp)
                .background(color, RoundedCornerShape(1.dp))
        )
        Spacer(modifier = Modifier.width(2.5.dp))
        DottedPlaceholderBox(color = color, widthDp = 10f, heightDp = 11f)
        Spacer(modifier = Modifier.width(2.5.dp))
        Box(
            modifier = Modifier
                .width(1.8.dp)
                .height(17.dp)
                .background(color, RoundedCornerShape(1.dp))
        )
    }
}

@Composable
private fun SubscriptKeyText(
    base: String,
    sub: String,
    color: Color,
    compact: Boolean
) {
    val baseSize = if (compact) 11.sp else 12.5.sp
    val subSize = if (compact) 8.5.sp else 9.5.sp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = base,
            color = color,
            fontSize = baseSize,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = sub,
            color = color,
            fontSize = subSize,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp, start = 0.5.dp)
        )
    }
}

@Composable
private fun KeyPopupSelector(
    alternates: List<SciCalcKey>,
    hoveredIndex: Int
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E2638),
        border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            alternates.forEachIndexed { idx, alt ->
                val isHovered = idx == hoveredIndex
                val bg = if (isHovered) Color(0xFF0284C7) else Color(0xFF28344E)
                val border = if (isHovered) BorderStroke(1.5.dp, Color.White) else BorderStroke(1.dp, Color(0xFF334155))

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .border(border, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    RenderKeyLabel(
                        key = alt,
                        textColor = Color.White,
                        compact = true
                    )
                }
            }
        }
    }
}

@Composable
private fun DerivativeKeyIcon(
    mode: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (mode == 0) "dy" else "d",
                color = color,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.SansSerif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                lineHeight = 9.5.sp
            )
            Box(
                modifier = Modifier
                    .width(if (mode == 3) 20.dp else 16.dp)
                    .height(1.2.dp)
                    .background(color, RoundedCornerShape(1.dp))
            )
            when (mode) {
                0, 1 -> {
                    Text(
                        text = "dx",
                        color = color,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 9.5.sp
                    )
                }
                2 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "d",
                            color = color,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 9.5.sp
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        DottedPlaceholderBox(color = color, widthDp = 6f, heightDp = 6.5f)
                    }
                }
                else -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "d",
                            color = color,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 9.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        DottedPlaceholderBox(
                            color = color,
                            widthDp = 5.5f,
                            heightDp = 6f,
                            modifier = Modifier.padding(top = 2.5.dp)
                        )
                        Spacer(modifier = Modifier.width(0.8.dp))
                        DottedPlaceholderBox(color = color, widthDp = 4.2f, heightDp = 4.5f)
                    }
                }
            }
        }
        if (mode >= 1) {
            Spacer(modifier = Modifier.width(2.dp))
            DottedPlaceholderBox(color = color, widthDp = 7.5f, heightDp = 8.5f)
        }
    }
}

@Composable
private fun LimitKeyIcon(
    suffix: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "lim",
            color = color,
            fontSize = 10.5.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            lineHeight = 10.5.sp
        )
        Spacer(modifier = Modifier.height(1.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            DottedPlaceholderBox(color = color, widthDp = 6.5f, heightDp = 6.5f)
            Text(
                text = "→",
                color = color,
                fontSize = 8.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 8.sp,
                modifier = Modifier.padding(horizontal = 0.5.dp)
            )
            DottedPlaceholderBox(color = color, widthDp = 6.5f, heightDp = 6.5f)
            if (suffix.isNotEmpty()) {
                Text(
                    text = suffix,
                    color = color,
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 5.dp, start = 0.5.dp)
                )
            }
        }
    }
}

@Composable
private fun IntegralKeyIcon(
    mode: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "∫",
            color = color,
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold
        )
        if (mode == 3) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.padding(horizontal = 1.dp)
            ) {
                DottedPlaceholderBox(color = color, widthDp = 5f, heightDp = 5f)
                DottedPlaceholderBox(color = color, widthDp = 7f, heightDp = 7.5f)
                DottedPlaceholderBox(color = color, widthDp = 5f, heightDp = 5f)
            }
        } else {
            DottedPlaceholderBox(color = color, widthDp = 7f, heightDp = 8f, modifier = Modifier.padding(horizontal = 1.5.dp))
        }
        Text(
            text = if (mode == 1) "dx" else "d",
            color = color,
            fontSize = 9.5.sp,
            fontFamily = FontFamily.SansSerif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium
        )
        if (mode >= 2) {
            Spacer(modifier = Modifier.width(1.dp))
            DottedPlaceholderBox(color = color, widthDp = 6f, heightDp = 6.5f)
        }
    }
}

@Composable
private fun SigmaKeyIcon(color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DottedPlaceholderBox(color = color, widthDp = 5f, heightDp = 5f)
            Text(
                text = "∑",
                color = color,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                DottedPlaceholderBox(color = color, widthDp = 4.5f, heightDp = 4.5f)
                Text("=", color = color, fontSize = 6.5.sp, lineHeight = 6.5.sp, fontFamily = JetBrainsMono)
                DottedPlaceholderBox(color = color, widthDp = 4.5f, heightDp = 4.5f)
            }
        }
        Spacer(modifier = Modifier.width(1.5.dp))
        DottedPlaceholderBox(color = color, widthDp = 7f, heightDp = 8f)
    }
}
