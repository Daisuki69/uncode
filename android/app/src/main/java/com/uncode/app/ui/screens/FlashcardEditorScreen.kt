package com.uncode.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.uncode.app.data.AnnotationItem
import com.uncode.app.data.Deck
import com.uncode.app.ui.components.DiagramCanvas
import com.uncode.app.viewmodel.EditableCardDraft
import com.uncode.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardEditorScreen(
    deckId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSaveAndFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val decks by viewModel.allDecks.collectAsStateWithLifecycle()
    val allCards by viewModel.allFlashcards.collectAsStateWithLifecycle()
    val deck = decks.find { it.id == deckId }

    var editableDeckTitle by remember { mutableStateOf("") }
    var editableDeckCategory by remember { mutableStateOf("") }
    var editableDeckDescription by remember { mutableStateOf("") }
    var initialDeckSnapshot by remember { mutableStateOf<Deck?>(null) }

    LaunchedEffect(deck) {
        if (deck != null && initialDeckSnapshot == null) {
            editableDeckTitle = deck.title
            editableDeckCategory = deck.category
            editableDeckDescription = deck.description
            initialDeckSnapshot = deck
        } else if (deckId <= 0L && initialDeckSnapshot == null && editableDeckTitle.isBlank()) {
            editableDeckCategory = ""
            editableDeckDescription = ""
        }
    }

    val cardDrafts = remember { mutableStateListOf<EditableCardDraft>() }
    val deletedCardIds = remember { mutableStateListOf<Long>() }
    var initialDraftsSnapshot by remember { mutableStateOf<List<EditableCardDraft>>(emptyList()) }
    var isInitialized by remember { mutableStateOf(false) }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    var activePickingCardIndex by remember { mutableIntStateOf(-1) }

    var activeAnnotatingIndex by remember { mutableIntStateOf(-1) }
    var isPreviewMode by remember { mutableStateOf(false) }

    var showValidationDialog by remember { mutableStateOf(false) }
    var validationErrorTitle by remember { mutableStateOf("") }
    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }

    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && activePickingCardIndex in cardDrafts.indices) {
            val current = cardDrafts[activePickingCardIndex]
            cardDrafts[activePickingCardIndex] = current.copy(
                photoUri = uri.toString(),
                isAnnotated = false
            )
        }
        activePickingCardIndex = -1
    }

    LaunchedEffect(deckId, allCards) {
        if (!isInitialized) {
            val existing = allCards.filter { it.deckId == deckId }
            cardDrafts.clear()
            val initialList = if (existing.isNotEmpty()) {
                existing.map { c ->
                    EditableCardDraft(
                        id = c.id,
                        term = c.termOrQuestion,
                        definition = c.definitionOrAnswer,
                        photoUri = c.photoUri,
                        isAnnotated = c.isAnnotated,
                        annotations = AnnotationItem.parseList(c.annotationsJson),
                        masteryLevel = c.masteryLevel,
                        timesReviewed = c.timesReviewed,
                        timesCorrect = c.timesCorrect
                    )
                }
            } else {

                listOf(
                    EditableCardDraft(),
                    EditableCardDraft(),
                    EditableCardDraft()
                )
            }
            cardDrafts.addAll(initialList)
            initialDraftsSnapshot = initialList.map { it.copy() }
            isInitialized = true
        }
    }

    val hasUnsavedChanges = remember(
        cardDrafts.toList(),
        deletedCardIds.toList(),
        initialDraftsSnapshot,
        editableDeckTitle,
        editableDeckCategory,
        editableDeckDescription,
        initialDeckSnapshot
    ) {
        if (!isInitialized) return@remember false
        if (initialDeckSnapshot != null) {
            if (editableDeckTitle != initialDeckSnapshot?.title ||
                editableDeckCategory != initialDeckSnapshot?.category ||
                editableDeckDescription != initialDeckSnapshot?.description
            ) return@remember true
        }
        if (deletedCardIds.isNotEmpty()) return@remember true
        if (cardDrafts.size != initialDraftsSnapshot.size) return@remember true
        cardDrafts.indices.any { i ->
            val cur = cardDrafts[i]
            val init = initialDraftsSnapshot.getOrNull(i) ?: return@remember true
            cur.id != init.id ||
            cur.term != init.term ||
            cur.definition != init.definition ||
            cur.photoUri != init.photoUri ||
            cur.isAnnotated != init.isAnnotated ||
            cur.annotations != init.annotations
        }
    }

    val handleBack = {
        if (activeAnnotatingIndex != -1) {
            activeAnnotatingIndex = -1
        } else if (hasUnsavedChanges) {
            showDiscardConfirmDialog = true
        } else {
            onBack()
        }
    }

    BackHandler {
        handleBack()
    }

    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            title = { Text("Discard Unsaved Edits?") },
            text = { Text("You have unsaved changes in this deck. Leaving will discard your modifications.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard Changes")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDiscardConfirmDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    if (showValidationDialog) {
        AlertDialog(
            onDismissRequest = { showValidationDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(validationErrorTitle) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    validationErrors.forEach { issue ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("\u2022", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Text(issue, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showValidationDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }

    fun performSave() {
        if (!isSaving) {
            val trimmedTitle = editableDeckTitle.trim()
            val issues = mutableListOf<String>()
            if (trimmedTitle.isBlank()) {
                issues.add("Missing title")
            }
            if (cardDrafts.any { it.annotations.isEmpty() && it.term.trim().isBlank() }) {
                issues.add("Missing Question")
            }
            if (cardDrafts.any { it.annotations.isEmpty() && it.definition.trim().isBlank() }) {
                issues.add("Missing Answer")
            }
            if (cardDrafts.any { it.annotations.isNotEmpty() && it.annotations.any { ann -> !ann.isBoxAttached } }) {
                issues.add("Leader line missing an attached answer box")
            }
            if (cardDrafts.any { it.annotations.isNotEmpty() && it.annotations.any { ann -> ann.labelAnswer.trim().isBlank() } }) {
                issues.add("Answer box missing an answer")
            }

            if (issues.isNotEmpty()) {
                validationErrorTitle = "There are issues with the deck while saving"
                validationErrors = issues
                showValidationDialog = true
                return
            }

            isSaving = true
            if (deckId <= 0L) {
                viewModel.createDeck(
                    title = trimmedTitle,
                    description = editableDeckDescription.trim(),
                    category = editableDeckCategory.trim(),
                    colorHex = "#3F51B5"
                ) { newDeckId ->
                    viewModel.saveAllCardsForDeck(
                        deckId = newDeckId,
                        cardsToSave = cardDrafts.toList(),
                        deletedCardIds = emptyList()
                    ) {
                        isSaving = false
                        onSaveAndFinished()
                    }
                }
            } else {
                if (deck != null) {
                    val updatedDeck = deck.copy(
                        title = trimmedTitle,
                        category = editableDeckCategory.trim(),
                        description = editableDeckDescription.trim()
                    )
                    viewModel.updateDeck(updatedDeck)
                }
                viewModel.saveAllCardsForDeck(
                    deckId = deckId,
                    cardsToSave = cardDrafts.toList(),
                    deletedCardIds = deletedCardIds.toList()
                ) {
                    isSaving = false
                    onSaveAndFinished()
                }
            }
        }
    }

    val saveAction = { performSave() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (activeAnnotatingIndex != -1) "Annotation Editor" else editableDeckTitle.ifBlank { "Edit Deck" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (activeAnnotatingIndex == -1) {
                            Text(
                                text = "${cardDrafts.size} flashcards",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (activeAnnotatingIndex == -1) {
                        Button(
                            onClick = saveAction,
                            enabled = !isSaving,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("save_deck_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Deck", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (activeAnnotatingIndex != -1 && activeAnnotatingIndex in cardDrafts.indices) {
            val activeCard = cardDrafts[activeAnnotatingIndex]
            val cardAnnotations = remember(activeCard.annotations) {
                mutableStateListOf<AnnotationItem>().apply { addAll(activeCard.annotations) }
            }

            var rootLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
            var canvasLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
            var lineCardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
            var trashAreaCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
            var isHoveringTrash by remember { mutableStateOf(false) }

            var isDraggingFromPalette by remember { mutableStateOf(false) }
            var draggedPaletteTool by remember { mutableStateOf<String?>(null) }
            var currentFingerInRoot by remember { mutableStateOf(Offset.Zero) }

            fun validateAndFinishAnnotation() {
                val issues = mutableListOf<String>()
                if (cardAnnotations.any { !it.isBoxAttached }) {
                    issues.add("Leader line missing an attached answer box")
                }
                if (cardAnnotations.any { it.labelAnswer.trim().isBlank() }) {
                    issues.add("Answer box missing an answer")
                }
                if (issues.isNotEmpty()) {
                    validationErrorTitle = "There are issues with the annotation while saving"
                    validationErrors = issues
                    showValidationDialog = true
                    return
                }
                val hasAnnotations = cardAnnotations.isNotEmpty()
                cardDrafts[activeAnnotatingIndex] = activeCard.copy(
                    annotations = cardAnnotations.toList(),
                    isAnnotated = hasAnnotations,
                    term = activeCard.term,
                    definition = if (hasAnnotations) "" else activeCard.definition
                )
                activeAnnotatingIndex = -1
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .onGloballyPositioned { coordinates ->
                        rootLayoutCoordinates = coordinates
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { validateAndFinishAnnotation() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Done", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .onGloballyPositioned { coordinates ->
                                canvasLayoutCoordinates = coordinates
                            }
                    ) {
                        DiagramCanvas(
                            diagramType = activeCard.photoUri,
                            modifier = Modifier.fillMaxSize(),
                            annotations = cardAnnotations,
                            maskLabels = false,
                            isInteractiveEditor = true,
                            externalTrashCoordinates = trashAreaCoordinates,
                            isHoveringTrash = isHoveringTrash,
                            onHoverTrashChanged = { isHoveringTrash = it },
                            onCanvasTapped = null, // DRAG ONLY! NO TAP TO CREATE!
                            onAutoAddAnswerBox = { item ->
                                val idx = cardAnnotations.indexOfFirst { it.id == item.id }
                                if (idx != -1) {

                                    cardAnnotations[idx] = item.copy(
                                        isBoxAttached = true,
                                        labelAnswer = ""
                                    )
                                }
                            },
                            onAnswerChanged = { item, newText ->
                                val idx = cardAnnotations.indexOfFirst { it.id == item.id }
                                if (idx != -1) {
                                    cardAnnotations[idx] = item.copy(labelAnswer = newText)
                                }
                            },
                            onDeleteAnnotation = { item ->
                                cardAnnotations.remove(item)
                            },
                            onBoxDragged = { item, newX, newY ->
                                val idx = cardAnnotations.indexOfFirst { it.id == item.id }
                                if (idx != -1) {
                                    cardAnnotations[idx] = item.copy(boxX = newX, boxY = newY)
                                }
                            },
                            onTargetDragged = { item, newX, newY ->
                                val idx = cardAnnotations.indexOfFirst { it.id == item.id }
                                if (idx != -1) {
                                    cardAnnotations[idx] = item.copy(targetX = newX, targetY = newY)
                                }
                            },
                            onCornerInvertedChanged = { item, isInverted ->
                                val idx = cardAnnotations.indexOfFirst { it.id == item.id }
                                if (idx != -1) {
                                    cardAnnotations[idx] = item.copy(isCornerInverted = isInverted)
                                }
                            },
                            onLineMoved = { item, newTx, newTy, newBx, newBy ->
                                val idx = cardAnnotations.indexOfFirst { it.id == item.id }
                                if (idx != -1) {
                                    cardAnnotations[idx] = item.copy(
                                        targetX = newTx,
                                        targetY = newTy,
                                        boxX = newBx,
                                        boxY = newBy
                                    )
                                }
                            }
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DraggablePaletteItem(
                                    title = "Line",
                                    subtitle = "",
                                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                                    accentColor = Color(0xFFE11D48),
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { coordinates ->
                                            lineCardCoordinates = coordinates
                                        }
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragStart = { localOffset ->
                                                    isDraggingFromPalette = true
                                                    draggedPaletteTool = "LINE"
                                                    val root = rootLayoutCoordinates
                                                    val itemCoords = lineCardCoordinates
                                                    if (root != null && itemCoords != null && root.isAttached && itemCoords.isAttached) {
                                                        currentFingerInRoot = root.localPositionOf(itemCoords, localOffset)
                                                    } else {
                                                        currentFingerInRoot = localOffset
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    currentFingerInRoot += dragAmount
                                                },
                                                onDragEnd = {
                                                    val root = rootLayoutCoordinates
                                                    val canvasCoords = canvasLayoutCoordinates
                                                    if (root != null && canvasCoords != null && root.isAttached && canvasCoords.isAttached) {
                                                        val canvasTopLeft = root.localPositionOf(canvasCoords, Offset.Zero)
                                                        val canvasWidth = canvasCoords.size.width.toFloat()
                                                        val canvasHeight = canvasCoords.size.height.toFloat()
                                                        val canvasBounds = Rect(canvasTopLeft, Size(canvasWidth, canvasHeight))

                                                        if (canvasBounds.contains(currentFingerInRoot)) {
                                                            val normX = ((currentFingerInRoot.x - canvasBounds.left) / canvasWidth).coerceIn(0.06f, 0.94f)
                                                            val normY = ((currentFingerInRoot.y - canvasBounds.top) / canvasHeight).coerceIn(0.06f, 0.94f)
                                                            val candidateOffsets = arrayOf(
                                                                0.18f to -0.10f,
                                                                0.18f to 0.10f,
                                                                -0.18f to -0.10f,
                                                                -0.18f to 0.10f,
                                                                0.0f to -0.16f,
                                                                0.0f to 0.16f,
                                                                0.24f to 0.0f,
                                                                -0.24f to 0.0f
                                                            )
                                                            var chosenBx = (normX + 0.18f).coerceIn(0.08f, 0.92f)
                                                            var chosenBy = (normY - 0.10f).coerceIn(0.08f, 0.92f)
                                                            var bestClearance = -1f
                                                            for (off in candidateOffsets) {
                                                                val cx = (normX + off.first).coerceIn(0.08f, 0.92f)
                                                                val cy = (normY + off.second).coerceIn(0.08f, 0.92f)
                                                                var minDist = Float.MAX_VALUE
                                                                for (existing in cardAnnotations) {
                                                                    val db = kotlin.math.hypot(cx - existing.boxX, cy - existing.boxY)
                                                                    val dt = kotlin.math.hypot(cx - existing.targetX, cy - existing.targetY)
                                                                    minDist = minOf(minDist, db, dt)
                                                                }
                                                                if (minDist > bestClearance) {
                                                                    bestClearance = minDist
                                                                    chosenBx = cx
                                                                    chosenBy = cy
                                                                }
                                                            }
                                                            cardAnnotations.add(
                                                                AnnotationItem(
                                                                    id = "line_${System.currentTimeMillis()}",
                                                                    targetX = normX,
                                                                    targetY = normY,
                                                                    boxX = chosenBx,
                                                                    boxY = chosenBy,
                                                                    labelAnswer = "",
                                                                    connectorStyle = "ARROW",
                                                                    isBoxAttached = false
                                                                )
                                                            )
                                                        }
                                                    }
                                                    isDraggingFromPalette = false
                                                    draggedPaletteTool = null
                                                },
                                                onDragCancel = {
                                                    isDraggingFromPalette = false
                                                    draggedPaletteTool = null
                                                }
                                            )
                                        }
                                )

                                TrashPaletteItem(
                                    isHoveringTrash = isHoveringTrash,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { coordinates ->
                                            trashAreaCoordinates = coordinates
                                        }
                                        .testTag("palette_trash_item")
                                )
                            }
                        }
                    }
                }

                if (isDraggingFromPalette && draggedPaletteTool == "LINE") {
                    val fingerX = currentFingerInRoot.x
                    val fingerY = currentFingerInRoot.y

                    Box(
                        modifier = Modifier
                            .offset { IntOffset((fingerX - 18).roundToInt(), (fingerY - 18).roundToInt()) }
                            .zIndex(100f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(2.dp, Color(0xFFF43F5E)),
                                shadowElevation = 16.dp,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Arrow Tip",
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "Arrow Tip",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = try { Color(android.graphics.Color.parseColor(deck?.colorHex ?: "#1976D2")) } catch (e: Exception) { MaterialTheme.colorScheme.primary },
                                    modifier = Modifier.size(12.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Deck Details",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedTextField(
                                value = editableDeckTitle,
                                onValueChange = { editableDeckTitle = it },
                                label = { Text("Deck Title *") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("deck_title_input"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = editableDeckDescription,
                                onValueChange = { editableDeckDescription = it },
                                label = { Text("Description") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("deck_description_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                itemsIndexed(
                    items = cardDrafts,
                    key = { _, draft -> draft.tempKey }
                ) { index, draft ->
                    val isDraggingThis = draggedIndex == index

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDraggingThis) 10f else 1f)
                    ) {

                        if (index > 0) {
                            InlineAddCardBetweenButton(
                                cardIndex = index - 1,
                                onAdd = {
                                    cardDrafts.add(index, EditableCardDraft())
                                },
                                modifier = Modifier.zIndex(5f)
                            )
                        }

                        FlashcardEditorRowItem(
                            index = index,
                            totalCount = cardDrafts.size,
                            draft = draft,
                            isDragging = isDraggingThis,
                            dragOffsetY = if (isDraggingThis) dragOffsetY else 0f,
                            onDragStart = {
                                draggedIndex = index
                                dragOffsetY = 0f
                            },
                            onDragDelta = { deltaY, cardHeight ->
                                if (draggedIndex == null) return@FlashcardEditorRowItem
                                dragOffsetY += deltaY
                                val currentIdx = draggedIndex ?: return@FlashcardEditorRowItem
                                val effectiveHeight = if (cardHeight > 50f) cardHeight else 300f
                                val threshold = effectiveHeight * 0.5f
                                if (dragOffsetY > threshold && currentIdx < cardDrafts.size - 1) {
                                    val item = cardDrafts.removeAt(currentIdx)
                                    cardDrafts.add(currentIdx + 1, item)
                                    draggedIndex = currentIdx + 1
                                    dragOffsetY -= effectiveHeight
                                } else if (dragOffsetY < -threshold && currentIdx > 0) {
                                    val item = cardDrafts.removeAt(currentIdx)
                                    cardDrafts.add(currentIdx - 1, item)
                                    draggedIndex = currentIdx - 1
                                    dragOffsetY += effectiveHeight
                                }
                            },
                            onDragEnd = {
                                draggedIndex = null
                                dragOffsetY = 0f
                            },
                            onTermChanged = { newTerm ->
                                cardDrafts[index] = draft.copy(term = newTerm)
                            },
                            onDefChanged = { newDef ->
                                cardDrafts[index] = draft.copy(definition = newDef)
                            },
                            onPickPhoto = {
                                activePickingCardIndex = index
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onRemovePhoto = {
                                cardDrafts[index] = draft.copy(
                                    photoUri = null,
                                    isAnnotated = false,
                                    annotations = emptyList()
                                )
                            },
                            onOpenAnnotationEditor = {
                                activeAnnotatingIndex = index
                            },
                            onDeleteCard = {
                                if (index in cardDrafts.indices) {
                                    val draftToDelete = cardDrafts[index]
                                    if (draftToDelete.id > 0L) {
                                        deletedCardIds.add(draftToDelete.id)
                                    }
                                    cardDrafts.removeAt(index)
                                    if (cardDrafts.isEmpty()) {
                                        cardDrafts.add(EditableCardDraft())
                                    }
                                }
                            }
                        )
                    }
                }

                item {
                    if (cardDrafts.isNotEmpty()) {
                        InlineAddCardBetweenButton(
                            cardIndex = cardDrafts.size - 1,
                            onAdd = {
                                cardDrafts.add(EditableCardDraft())
                            },
                            modifier = Modifier.zIndex(5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun DraggablePaletteItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun TrashPaletteItem(
    isHoveringTrash: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isHoveringTrash) Color(0xFFDC2626) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isHoveringTrash) 2.dp else 1.dp,
            color = if (isHoveringTrash) Color.White else Color(0xFFEF4444).copy(alpha = 0.5f)
        ),
        shadowElevation = if (isHoveringTrash) 8.dp else 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = if (isHoveringTrash) Color.White.copy(alpha = 0.25f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Trash / Delete Object",
                        tint = if (isHoveringTrash) Color.White else Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isHoveringTrash) "Release" else "Delete",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isHoveringTrash) Color.White else Color(0xFFEF4444)
            )
        }
    }
}

@Composable
private fun InlineAddCardBetweenButton(
    cardIndex: Int,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.Center
    ) {

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp,
            modifier = Modifier
                .requiredSize(32.dp)
                .clickable { onAdd() }
                .testTag("insert_card_below_$cardIndex")
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Insert flashcard here",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FlashcardEditorRowItem(
    index: Int,
    totalCount: Int,
    draft: EditableCardDraft,
    isDragging: Boolean,
    dragOffsetY: Float,
    onDragStart: () -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onTermChanged: (String) -> Unit,
    onDefChanged: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onOpenAnnotationEditor: () -> Unit,
    onDeleteCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cardHeightPx by remember { mutableFloatStateOf(0f) }
    var cardWidthPx by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    val swipeOffsetX = remember(draft.tempKey) { Animatable(0f) }
    var showDeleteConfirm by remember(draft.tempKey) { mutableStateOf(false) }
    var termBounds by remember { mutableStateOf(Rect.Zero) }
    var defBounds by remember { mutableStateOf(Rect.Zero) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                coroutineScope.launch {
                    swipeOffsetX.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
                }
            },
            title = { Text("Delete Card?") },
            text = { Text("Are you sure you want to delete this card?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        coroutineScope.launch {
                            val effectiveWidth = if (cardWidthPx > 0f) cardWidthPx else 600f
                            val fullSlideTarget = if (swipeOffsetX.value >= 0f) effectiveWidth * 1.15f else -effectiveWidth * 1.15f
                            swipeOffsetX.animateTo(fullSlideTarget, tween(240, easing = FastOutSlowInEasing))
                            onDeleteCard()
                            swipeOffsetX.snapTo(0f)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteConfirm = false
                        coroutineScope.launch {
                            swipeOffsetX.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
                        }
                    }
                ) {
                    Text("No")
                }
            }
        )
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        if (kotlin.math.abs(swipeOffsetX.value) > 8f) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = (kotlin.math.abs(swipeOffsetX.value) / (if (cardWidthPx > 0f) cardWidthPx * 0.45f else 300f)).coerceIn(0.15f, 0.9f)),
                modifier = Modifier.matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = if (swipeOffsetX.value < 0f) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Slide to Delete",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_editor_item_$index")
                .onGloballyPositioned { coords ->
                    cardHeightPx = coords.size.height.toFloat()
                    cardWidthPx = coords.size.width.toFloat()
                }
                .graphicsLayer {
                    translationX = swipeOffsetX.value
                    if (isDragging) {
                        translationY = dragOffsetY
                        scaleX = 1.02f
                        scaleY = 1.02f
                        shadowElevation = 14f
                    }
                }
                .pointerInput("swipe_${draft.tempKey}") {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (!showDeleteConfirm) {
                                val centerLimit = size.width * 0.5f
                                val nextOffset = (swipeOffsetX.value + dragAmount).coerceIn(-centerLimit, centerLimit)
                                coroutineScope.launch {
                                    swipeOffsetX.snapTo(nextOffset)
                                }
                            }
                        },
                        onDragEnd = {
                            if (!showDeleteConfirm) {
                                val centerLimit = if (size.width > 0) size.width * 0.5f else 300f
                                val centerThreshold = centerLimit * 0.92f
                                if (kotlin.math.abs(swipeOffsetX.value) >= centerThreshold) {
                                    val holdOffset = if (swipeOffsetX.value >= 0f) centerLimit else -centerLimit
                                    showDeleteConfirm = true
                                    coroutineScope.launch {
                                        swipeOffsetX.animateTo(holdOffset, tween(100, easing = FastOutSlowInEasing))
                                    }
                                } else {
                                    coroutineScope.launch {
                                        swipeOffsetX.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            if (!showDeleteConfirm) {
                                coroutineScope.launch {
                                    swipeOffsetX.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
                                }
                            }
                        }
                    )
                }
                .pointerInput(draft.tempKey) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { startOffset ->
                            if (termBounds.contains(startOffset) || defBounds.contains(startOffset)) {
                                return@detectDragGesturesAfterLongPress
                            }
                            onDragStart()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.y, cardHeightPx)
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            ),
            border = if (isDragging) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            termBounds = coords.boundsInParent()
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (draft.photoUri != null && draft.annotations.isNotEmpty()) {
                                    Text(
                                        text = "${draft.annotations.size} Targets",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (draft.photoUri == null) {
                                    IconButton(
                                        onClick = onPickPhoto,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("add_photo_card_$index")
                                    ) {
                                        Icon(
                                            Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Add Photo",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        BasicTextField(
                            value = draft.term,
                            onValueChange = onTermChanged,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("term_input_$index")
                        )

                        if (draft.photoUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .clickable { onOpenAnnotationEditor() },
                                contentAlignment = Alignment.Center
                            ) {
                                DiagramCanvas(
                                    diagramType = draft.photoUri,
                                    modifier = Modifier.fillMaxSize(),
                                    annotations = draft.annotations,
                                    maskLabels = false,
                                    isInteractiveEditor = false
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onPickPhoto,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Change Photo", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = onRemovePhoto,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Remove Photo", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                if (draft.annotations.isEmpty()) {
                    OutlinedTextField(
                        value = draft.definition,
                        onValueChange = onDefChanged,
                        label = { Text("Answer") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                defBounds = coords.boundsInParent()
                            }
                            .testTag("def_input_$index"),
                        minLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }
}

