package com.uncode.app.ui.components

import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.util.PriorityQueue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.uncode.app.data.AnnotationItem
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private fun getImageIntrinsicDimensions(context: android.content.Context, uriString: String): Size? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        var rotationDegrees = 0
        if (uriString.startsWith("content://")) {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                rotationDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } else {
            val cleanPath = if (uriString.startsWith("file://")) Uri.parse(uriString).path ?: uriString.removePrefix("file://") else uriString
            BitmapFactory.decodeFile(cleanPath, options)
            val exif = ExifInterface(cleanPath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }
        val rawW = options.outWidth.toFloat()
        val rawH = options.outHeight.toFloat()
        if (rawW <= 0f || rawH <= 0f) null
        else if (rotationDegrees == 90 || rotationDegrees == 270) Size(rawH, rawW)
        else Size(rawW, rawH)
    } catch (_: Exception) {
        null
    }
}

@Composable
fun DiagramCanvas(
    diagramType: String?,
    modifier: Modifier = Modifier,
    annotations: List<AnnotationItem> = emptyList(),
    selectedAnnotationId: String? = null,
    highlightedAnnotationId: String? = null,
    revealedAnnotationIds: Set<String> = emptySet(),
    maskLabels: Boolean = false,
    showConnectorLines: Boolean = true,
    isInteractiveEditor: Boolean = false,
    externalTrashCoordinates: LayoutCoordinates? = null,
    isHoveringTrash: Boolean = false,
    onHoverTrashChanged: ((Boolean) -> Unit)? = null,
    onCanvasTapped: ((Offset, Float, Float) -> Unit)? = null,
    onImageBoundsChanged: ((Float, Float, Float, Float) -> Unit)? = null,
    onAnnotationTapped: ((AnnotationItem) -> Unit)? = null,
    onAutoAddAnswerBox: ((AnnotationItem) -> Unit)? = null,
    onAnswerChanged: ((AnnotationItem, String) -> Unit)? = null,
    onDeleteAnnotation: ((AnnotationItem) -> Unit)? = null,
    onBoxDragged: ((AnnotationItem, Float, Float) -> Unit)? = null,
    onTargetDragged: ((AnnotationItem, Float, Float) -> Unit)? = null,
    onCornerInvertedChanged: ((AnnotationItem, Boolean) -> Unit)? = null,
    onLineMoved: ((AnnotationItem, Float, Float, Float, Float) -> Unit)? = null
) {
    var canvasLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val density = LocalDensity.current
    val densityScale = density.density
    val measuredBoxSizes = remember { androidx.compose.runtime.mutableStateMapOf<String, androidx.compose.ui.unit.IntSize>() }

    val currentAnnotations by rememberUpdatedState(annotations)
    val currentExternalTrashCoordinates by rememberUpdatedState(externalTrashCoordinates)
    val currentIsHoveringTrash by rememberUpdatedState(isHoveringTrash)
    val currentOnHoverTrashChanged by rememberUpdatedState(onHoverTrashChanged)
    val currentOnLineMoved by rememberUpdatedState(onLineMoved)
    val currentOnDeleteAnnotation by rememberUpdatedState(onDeleteAnnotation)

    val context = LocalContext.current
    val isCustomImage = diagramType != null && !diagramType.startsWith("diagram_")
    val intrinsicDimensions = remember(diagramType) {
        if (isCustomImage && diagramType != null) {
            getImageIntrinsicDimensions(context, diagramType)
        } else null
    }

    BoxWithConstraints(
        modifier = modifier
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
            .onGloballyPositioned { coordinates ->
                canvasLayoutCoordinates = coordinates
            }
            .then(
                if (onCanvasTapped != null) {
                    Modifier.pointerInput(onCanvasTapped, intrinsicDimensions) {
                        val scale = if (intrinsicDimensions != null && intrinsicDimensions.width > 0f && intrinsicDimensions.height > 0f) {
                            minOf(size.width.toFloat() / intrinsicDimensions.width, size.height.toFloat() / intrinsicDimensions.height)
                        } else 1f
                        val rw = if (intrinsicDimensions != null && intrinsicDimensions.width > 0f) intrinsicDimensions.width * scale else size.width.toFloat()
                        val rh = if (intrinsicDimensions != null && intrinsicDimensions.height > 0f) intrinsicDimensions.height * scale else size.height.toFloat()
                        val ox = (size.width.toFloat() - rw) / 2f
                        val oy = (size.height.toFloat() - rh) / 2f
                        detectTapGestures { offset ->
                            val normX = if (rw > 0f) ((offset.x - ox) / rw).coerceIn(0f, 1f) else 0.5f
                            val normY = if (rh > 0f) ((offset.y - oy) / rh).coerceIn(0f, 1f) else 0.5f
                            onCanvasTapped.invoke(offset, normX, normY)
                        }
                    }
                } else Modifier
            )
            .then(
                if (isInteractiveEditor) {
                    Modifier.pointerInput(isInteractiveEditor, densityScale, intrinsicDimensions) {
                        var activeItem: AnnotationItem? = null
                        var fingerX = 0f
                        var fingerY = 0f
                        var curTx = 0f
                        var curTy = 0f
                        var curBx = 0f
                        var curBy = 0f

                        val scale = if (intrinsicDimensions != null && intrinsicDimensions.width > 0f && intrinsicDimensions.height > 0f) {
                            minOf(size.width.toFloat() / intrinsicDimensions.width, size.height.toFloat() / intrinsicDimensions.height)
                        } else 1f
                        val rw = if (intrinsicDimensions != null && intrinsicDimensions.width > 0f) intrinsicDimensions.width * scale else size.width.toFloat()
                        val rh = if (intrinsicDimensions != null && intrinsicDimensions.height > 0f) intrinsicDimensions.height * scale else size.height.toFloat()
                        val ox = (size.width.toFloat() - rw) / 2f
                        val oy = (size.height.toFloat() - rh) / 2f

                        detectDragGestures(
                            onDragStart = { offset ->
                                fingerX = offset.x
                                fingerY = offset.y
                                val width = size.width.toFloat()
                                val height = size.height.toFloat()
                                val liveAnnotations = currentAnnotations
                                val resolvedRects = resolveNonOverlappingBoxRects(
                                    annotations = liveAnnotations,
                                    measuredSizes = measuredBoxSizes,
                                    widthPx = width,
                                    heightPx = height,
                                    densityScale = densityScale,
                                    imgOffsetX = ox,
                                    imgOffsetY = oy,
                                    renderedW = rw,
                                    renderedH = rh
                                )
                                val routeMap = computeAllAnnotationRoutes(
                                    annotations = liveAnnotations,
                                    resolvedBoxRects = resolvedRects,
                                    widthPx = width,
                                    heightPx = height,
                                    densityScale = densityScale,
                                    imgOffsetX = ox,
                                    imgOffsetY = oy,
                                    renderedW = rw,
                                    renderedH = rh
                                )

                                for (item in liveAnnotations.asReversed()) {
                                    val tPos = Offset(ox + item.targetX * rw, oy + item.targetY * rh)
                                    val bPos = resolvedRects[item.id]?.center ?: Offset(item.boxX * width, item.boxY * height)
                                    val points = routeMap[item.id] ?: listOf(tPos, bPos)

                                    val distTarget = (offset - tPos).getDistance()
                                    val distBox = (offset - bPos).getDistance()

                                    if (distTarget > 36f && distBox > 44f) {
                                        var minDistLine = Float.MAX_VALUE
                                        for (i in 0 until points.size - 1) {
                                            minDistLine = minOf(
                                                minDistLine,
                                                distanceToSegment(offset, points[i], points[i + 1])
                                            )
                                        }

                                        if (minDistLine <= 38f) {
                                            activeItem = item
                                            curTx = item.targetX
                                            curTy = item.targetY
                                            curBx = (bPos.x / width).coerceIn(0.02f, 0.98f)
                                            curBy = (bPos.y / height).coerceIn(0.02f, 0.98f)
                                            break
                                        }
                                    }
                                }

                                if (activeItem != null) {
                                    val trash = currentExternalTrashCoordinates
                                    val canvas = canvasLayoutCoordinates
                                    val overTrash = if (trash != null && canvas != null && trash.isAttached && canvas.isAttached) {
                                        val trashPosInCanvas = canvas.localPositionOf(trash, Offset.Zero)
                                        val trashRect = Rect(trashPosInCanvas, Size(trash.size.width.toFloat(), trash.size.height.toFloat()))
                                        Rect(trashRect.left - 24f, trashRect.top - 24f, trashRect.right + 24f, trashRect.bottom + 24f).contains(Offset(fingerX, fingerY))
                                    } else false
                                    currentOnHoverTrashChanged?.invoke(overTrash)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                val item = activeItem
                                if (item != null) {
                                    change.consume()
                                    val width = size.width.toFloat()
                                    val height = size.height.toFloat()
                                    val dx = if (rw > 0f) dragAmount.x / rw else 0f
                                    val dy = if (rh > 0f) dragAmount.y / rh else 0f
                                    curTx = (curTx + dx).coerceIn(0.01f, 0.99f)
                                    curTy = (curTy + dy).coerceIn(0.01f, 0.99f)
                                    curBx = (curBx + dragAmount.x / width).coerceIn(0.02f, 0.98f)
                                    curBy = (curBy + dragAmount.y / height).coerceIn(0.02f, 0.98f)
                                    fingerX += dragAmount.x
                                    fingerY += dragAmount.y

                                    val trash = currentExternalTrashCoordinates
                                    val canvas = canvasLayoutCoordinates
                                    val overTrash = if (trash != null && canvas != null && trash.isAttached && canvas.isAttached) {
                                        val trashPosInCanvas = canvas.localPositionOf(trash, Offset.Zero)
                                        val trashRect = Rect(trashPosInCanvas, Size(trash.size.width.toFloat(), trash.size.height.toFloat()))
                                        Rect(trashRect.left - 24f, trashRect.top - 24f, trashRect.right + 24f, trashRect.bottom + 24f).contains(Offset(fingerX, fingerY))
                                    } else false
                                    currentOnHoverTrashChanged?.invoke(overTrash)

                                    currentOnLineMoved?.invoke(
                                        item,
                                        curTx, curTy,
                                        curBx, curBy
                                    )
                                }
                            },
                            onDragEnd = {
                                val item = activeItem
                                if (item != null) {
                                    val trash = currentExternalTrashCoordinates
                                    val canvas = canvasLayoutCoordinates
                                    val overTrash = if (trash != null && canvas != null && trash.isAttached && canvas.isAttached) {
                                        val trashPosInCanvas = canvas.localPositionOf(trash, Offset.Zero)
                                        val trashRect = Rect(trashPosInCanvas, Size(trash.size.width.toFloat(), trash.size.height.toFloat()))
                                        Rect(trashRect.left - 24f, trashRect.top - 24f, trashRect.right + 24f, trashRect.bottom + 24f).contains(Offset(fingerX, fingerY))
                                    } else false
                                    if (currentIsHoveringTrash || overTrash) {
                                        currentOnDeleteAnnotation?.invoke(item)
                                    }
                                    currentOnHoverTrashChanged?.invoke(false)
                                    activeItem = null
                                }
                            },
                            onDragCancel = {
                                activeItem = null
                                currentOnHoverTrashChanged?.invoke(false)
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        val (imgOffsetX, imgOffsetY, renderedW, renderedH) = remember(widthPx, heightPx, intrinsicDimensions) {
            if (intrinsicDimensions != null && intrinsicDimensions.width > 0f && intrinsicDimensions.height > 0f) {
                val scale = minOf(widthPx / intrinsicDimensions.width, heightPx / intrinsicDimensions.height)
                val rw = intrinsicDimensions.width * scale
                val rh = intrinsicDimensions.height * scale
                val ox = (widthPx - rw) / 2f
                val oy = (heightPx - rh) / 2f
                floatArrayOf(ox, oy, rw, rh)
            } else {
                floatArrayOf(0f, 0f, widthPx, heightPx)
            }
        }

        LaunchedEffect(imgOffsetX, imgOffsetY, renderedW, renderedH) {
            onImageBoundsChanged?.invoke(imgOffsetX, imgOffsetY, renderedW, renderedH)
        }

        val checkHoveringTrash: (Float, Float) -> Boolean = remember(externalTrashCoordinates, canvasLayoutCoordinates) {
            { xPx: Float, yPx: Float ->
                val trash = externalTrashCoordinates
                val canvas = canvasLayoutCoordinates
                if (trash != null && canvas != null && trash.isAttached && canvas.isAttached) {
                    val trashPosInCanvas = canvas.localPositionOf(trash, Offset.Zero)
                    val trashRect = Rect(trashPosInCanvas, Size(trash.size.width.toFloat(), trash.size.height.toFloat()))
                    val hitRect = Rect(
                        left = trashRect.left - 24f,
                        top = trashRect.top - 24f,
                        right = trashRect.right + 24f,
                        bottom = trashRect.bottom + 24f
                    )
                    hitRect.contains(Offset(xPx, yPx))
                } else {
                    false
                }
            }
        }

        if (isCustomImage && diagramType != null) {
            val imageModel = remember(diagramType) {
                if (diagramType.startsWith("content://") || diagramType.startsWith("file://")) {
                    Uri.parse(diagramType)
                } else {
                    File(diagramType)
                }
            }
            var imageLoadFailed by remember(diagramType) { mutableStateOf(false) }
            if (imageLoadFailed) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Image unavailable",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            } else {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    onError = { imageLoadFailed = true }
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (diagramType) {
                    "diagram_heart" -> drawHeartDiagram(size)
                    "diagram_brain" -> drawBrainDiagram(size)
                    "diagram_cell" -> drawCellDiagram(size)
                    "diagram_map" -> drawMapDiagram(size)
                    else -> drawGenericDiagram(size)
                }
            }
        }

        val annotationsSnapshot = annotations.toList()
        val measuredSizesSnapshot = measuredBoxSizes.toMap()

        val resolvedBoxRects = remember(annotationsSnapshot, measuredSizesSnapshot, widthPx, heightPx, densityScale, imgOffsetX, imgOffsetY, renderedW, renderedH) {
            resolveNonOverlappingBoxRects(
                annotations = annotationsSnapshot,
                measuredSizes = measuredSizesSnapshot,
                widthPx = widthPx,
                heightPx = heightPx,
                densityScale = densityScale,
                imgOffsetX = imgOffsetX,
                imgOffsetY = imgOffsetY,
                renderedW = renderedW,
                renderedH = renderedH
            )
        }

        val allRoutes = remember(annotationsSnapshot, resolvedBoxRects, widthPx, heightPx, densityScale, imgOffsetX, imgOffsetY, renderedW, renderedH) {
            computeAllAnnotationRoutes(
                annotations = annotationsSnapshot,
                resolvedBoxRects = resolvedBoxRects,
                widthPx = widthPx,
                heightPx = heightPx,
                densityScale = densityScale,
                imgOffsetX = imgOffsetX,
                imgOffsetY = imgOffsetY,
                renderedW = renderedW,
                renderedH = renderedH
            )
        }

        val allMoveHandles = remember(annotationsSnapshot, allRoutes, resolvedBoxRects, widthPx, heightPx, densityScale, imgOffsetX, imgOffsetY, renderedW, renderedH) {
            computeAllSafeMoveHandles(
                annotations = annotationsSnapshot,
                allRoutes = allRoutes,
                resolvedBoxRects = resolvedBoxRects,
                widthPx = widthPx,
                heightPx = heightPx,
                densityScale = densityScale,
                imgOffsetX = imgOffsetX,
                imgOffsetY = imgOffsetY,
                renderedW = renderedW,
                renderedH = renderedH
            )
        }

        if (showConnectorLines) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                annotations.forEach { item ->
                    val isSelected = item.id == selectedAnnotationId
                    val isHighlighted = item.id == highlightedAnnotationId

                    val targetPos = Offset(imgOffsetX + item.targetX * renderedW, imgOffsetY + item.targetY * renderedH)
                    val boxPos = resolvedBoxRects[item.id]?.center ?: Offset(item.boxX * size.width, item.boxY * size.height)

                    val lineColor = when {
                        isHighlighted -> Color(0xFFFFD54F)
                        isSelected -> Color(0xFF64B5F6)
                        else -> Color(0xFFF43F5E)
                    }

                    val points = allRoutes[item.id] ?: listOf(targetPos, boxPos)
                    val path = Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                    }

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(
                            width = if (isSelected || isHighlighted) 3.5f else 2.5f,
                            cap = StrokeCap.Round
                        )
                    )

                    if (points.size >= 2) {
                        val p0 = points[0]
                        val p1 = points[1]
                        val angle = atan2(p0.y - p1.y, p0.x - p1.x)
                        val arrowLen = 24f
                        val arrowAngle = Math.PI / 6

                        val x1 = p0.x - arrowLen * cos(angle - arrowAngle).toFloat()
                        val y1 = p0.y - arrowLen * sin(angle - arrowAngle).toFloat()
                        val x2 = p0.x - arrowLen * cos(angle + arrowAngle).toFloat()
                        val y2 = p0.y - arrowLen * sin(angle + arrowAngle).toFloat()

                        val arrowPath = Path().apply {
                            moveTo(p0.x, p0.y)
                            lineTo(x1, y1)
                            lineTo(x2, y2)
                            close()
                        }
                        drawPath(arrowPath, lineColor)
                    }
                }
            }
        }

        annotations.forEachIndexed { index, item ->
            key(item.id) {
                val resolvedCenter = resolvedBoxRects[item.id]?.center ?: Offset(item.boxX * widthPx, item.boxY * heightPx)
                val fallbackPoints = listOf(
                    Offset(imgOffsetX + item.targetX * renderedW, imgOffsetY + item.targetY * renderedH),
                    resolvedCenter
                )
                val defaultHandle = Offset(
                    (imgOffsetX + item.targetX * renderedW + resolvedCenter.x) / 2f,
                    (imgOffsetY + item.targetY * renderedH + resolvedCenter.y) / 2f
                )
                DiagramAnnotationItemOverlay(
                    item = item,
                    index = index,
                    resolvedBoxCenter = resolvedCenter,
                    routedPoints = allRoutes[item.id] ?: fallbackPoints,
                    safeMoveHandlePos = allMoveHandles[item.id] ?: defaultHandle,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    imgOffsetX = imgOffsetX,
                    imgOffsetY = imgOffsetY,
                    renderedW = renderedW,
                    renderedH = renderedH,
                    isSelected = item.id == selectedAnnotationId,
                    isHighlighted = item.id == highlightedAnnotationId,
                    isRevealed = revealedAnnotationIds.contains(item.id) || !maskLabels,
                    maskLabels = maskLabels,
                    isInteractiveEditor = isInteractiveEditor,
                    isHoveringTrash = isHoveringTrash,
                    checkHoveringTrash = checkHoveringTrash,
                    onHoverTrashStateChanged = { onHoverTrashChanged?.invoke(it) },
                    onBoxSizeMeasured = { id, sz ->
                        if (measuredBoxSizes[id] != sz) {
                            measuredBoxSizes[id] = sz
                        }
                    },
                    onAnnotationTapped = onAnnotationTapped,
                    onAutoAddAnswerBox = onAutoAddAnswerBox,
                    onAnswerChanged = onAnswerChanged,
                    onDeleteAnnotation = onDeleteAnnotation,
                    onBoxDragged = onBoxDragged,
                    onTargetDragged = onTargetDragged,
                    onCornerInvertedChanged = onCornerInvertedChanged,
                    onLineMoved = onLineMoved
                )
            }
        }
    }
}

@Composable
private fun DiagramAnnotationItemOverlay(
    item: AnnotationItem,
    index: Int,
    resolvedBoxCenter: Offset,
    routedPoints: List<Offset>,
    safeMoveHandlePos: Offset,
    widthPx: Float,
    heightPx: Float,
    imgOffsetX: Float = 0f,
    imgOffsetY: Float = 0f,
    renderedW: Float = widthPx,
    renderedH: Float = heightPx,
    isSelected: Boolean,
    isHighlighted: Boolean,
    isRevealed: Boolean,
    maskLabels: Boolean,
    isInteractiveEditor: Boolean,
    isHoveringTrash: Boolean,
    checkHoveringTrash: (Float, Float) -> Boolean,
    onHoverTrashStateChanged: (Boolean) -> Unit,
    onBoxSizeMeasured: (String, androidx.compose.ui.unit.IntSize) -> Unit,
    onAnnotationTapped: ((AnnotationItem) -> Unit)?,
    onAutoAddAnswerBox: ((AnnotationItem) -> Unit)?,
    onAnswerChanged: ((AnnotationItem, String) -> Unit)?,
    onDeleteAnnotation: ((AnnotationItem) -> Unit)?,
    onBoxDragged: ((AnnotationItem, Float, Float) -> Unit)?,
    onTargetDragged: ((AnnotationItem, Float, Float) -> Unit)?,
    onCornerInvertedChanged: ((AnnotationItem, Boolean) -> Unit)?,
    onLineMoved: ((AnnotationItem, Float, Float, Float, Float) -> Unit)?
) {
    val currentItem by rememberUpdatedState(item)
    val currentResolvedBoxCenter by rememberUpdatedState(resolvedBoxCenter)
    val currentSafeMoveHandlePos by rememberUpdatedState(safeMoveHandlePos)
    val currentOnBoxDragged by rememberUpdatedState(onBoxDragged)
    val currentOnTargetDragged by rememberUpdatedState(onTargetDragged)
    val currentOnCornerInvertedChanged by rememberUpdatedState(onCornerInvertedChanged)
    val currentOnLineMoved by rememberUpdatedState(onLineMoved)

    val posX = resolvedBoxCenter.x.roundToInt()
    val posY = resolvedBoxCenter.y.roundToInt()

    val density = LocalDensity.current
    val plusRadiusPx = with(density) { 16.dp.roundToPx() }
    val targetHalfPx = with(density) { 24.dp.roundToPx() }
    val handleHalfPx = with(density) { 18.dp.roundToPx() }
    var boxWidthPx by remember { mutableIntStateOf(0) }
    var boxHeightPx by remember { mutableIntStateOf(0) }

    val hasBoxAttached = item.isBoxAttached
    val boxOffsetX = if (!hasBoxAttached) posX - plusRadiusPx else posX - (boxWidthPx / 2)
    val boxOffsetY = if (!hasBoxAttached) posY - plusRadiusPx else posY - (boxHeightPx / 2)

    Box(
        modifier = Modifier
            .offset { IntOffset(boxOffsetX, boxOffsetY) }
            .onSizeChanged { size ->
                boxWidthPx = size.width
                boxHeightPx = size.height
                onBoxSizeMeasured(currentItem.id, size)
            }
            .pointerInput(item.id) {
                detectTapGestures {
                    onAnnotationTapped?.invoke(currentItem)
                }
            }
            .then(
                if (isInteractiveEditor) {
                    Modifier.pointerInput("box_drag_${item.id}") {
                        var accBx = currentResolvedBoxCenter.x / widthPx
                        var accBy = currentResolvedBoxCenter.y / heightPx
                        var fingerX = currentResolvedBoxCenter.x
                        var fingerY = currentResolvedBoxCenter.y
                        detectDragGestures(
                            onDragStart = {
                                accBx = (currentResolvedBoxCenter.x / widthPx).coerceIn(0.02f, 0.98f)
                                accBy = (currentResolvedBoxCenter.y / heightPx).coerceIn(0.02f, 0.98f)
                                fingerX = currentResolvedBoxCenter.x
                                fingerY = currentResolvedBoxCenter.y
                                val overTrash = checkHoveringTrash(fingerX, fingerY)
                                onHoverTrashStateChanged(overTrash)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accBx = (accBx + dragAmount.x / widthPx).coerceIn(0.02f, 0.98f)
                                accBy = (accBy + dragAmount.y / heightPx).coerceIn(0.02f, 0.98f)
                                fingerX += dragAmount.x
                                fingerY += dragAmount.y
                                val overTrash = checkHoveringTrash(fingerX, fingerY)
                                onHoverTrashStateChanged(overTrash)
                                currentOnBoxDragged?.invoke(currentItem, accBx, accBy)
                            },
                            onDragEnd = {
                                val overTrash = checkHoveringTrash(fingerX, fingerY)
                                if (isHoveringTrash || overTrash) {
                                    onDeleteAnnotation?.invoke(currentItem)
                                }
                                onHoverTrashStateChanged(false)
                            },
                            onDragCancel = {
                                onHoverTrashStateChanged(false)
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        if (!hasBoxAttached) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF16A34A),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        onAutoAddAnswerBox?.invoke(currentItem)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Answer Box",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    isHighlighted -> Color(0xFFFFB300)
                    isSelected -> Color(0xFF2563EB)
                    else -> Color(0xFF1E293B)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when {
                        isHighlighted -> Color.White
                        isSelected -> Color(0xFF93C5FD)
                        else -> Color(0xFF475569)
                    }
                ),
                shadowElevation = if (isSelected || isHighlighted) 6.dp else 2.dp
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isHighlighted || isSelected) Color.White.copy(alpha = 0.3f) else Color(0xFF334155),
                        modifier = Modifier.size(18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))

                    if (isInteractiveEditor && !maskLabels) {
                        BasicTextField(
                            value = item.labelAnswer,
                            onValueChange = { newText ->
                                onAnswerChanged?.invoke(currentItem, newText)
                            },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (item.labelAnswer.isEmpty()) {
                                        Text(
                                            text = "Answer ${index + 1}",
                                            color = Color.White.copy(alpha = 0.45f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.widthIn(min = 55.dp, max = 150.dp)
                        )
                    } else {
                        val displayAnswer = if (item.labelAnswer.isNotBlank()) item.labelAnswer else "Answer ${index + 1}"
                        Text(
                            text = if (isRevealed) displayAnswer else "???",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (isInteractiveEditor) {
        val targetPxX = (imgOffsetX + item.targetX * renderedW).roundToInt()
        val targetPxY = (imgOffsetY + item.targetY * renderedH).roundToInt()
        Box(
            modifier = Modifier
                .offset { IntOffset(targetPxX - targetHalfPx, targetPxY - targetHalfPx) }
                .size(48.dp)
                .pointerInput("target_drag_${item.id}", imgOffsetX, imgOffsetY, renderedW, renderedH) {
                    var accTx = currentItem.targetX
                    var accTy = currentItem.targetY
                    var fingerX = imgOffsetX + accTx * renderedW
                    var fingerY = imgOffsetY + accTy * renderedH
                    detectDragGestures(
                        onDragStart = {
                            accTx = currentItem.targetX
                            accTy = currentItem.targetY
                            fingerX = imgOffsetX + accTx * renderedW
                            fingerY = imgOffsetY + accTy * renderedH
                            val overTrash = checkHoveringTrash(fingerX, fingerY)
                            onHoverTrashStateChanged(overTrash)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accTx = (accTx + dragAmount.x / renderedW).coerceIn(0.01f, 0.99f)
                            accTy = (accTy + dragAmount.y / renderedH).coerceIn(0.01f, 0.99f)
                            fingerX += dragAmount.x
                            fingerY += dragAmount.y
                            val overTrash = checkHoveringTrash(fingerX, fingerY)
                            onHoverTrashStateChanged(overTrash)
                            currentOnTargetDragged?.invoke(currentItem, accTx, accTy)
                        },
                        onDragEnd = {
                            val overTrash = checkHoveringTrash(fingerX, fingerY)
                            if (isHoveringTrash || overTrash) {
                                onDeleteAnnotation?.invoke(currentItem)
                            }
                            onHoverTrashStateChanged(false)
                        },
                        onDragCancel = {
                            onHoverTrashStateChanged(false)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF64B5F6).copy(alpha = 0.8f)),
                    modifier = Modifier.size(24.dp)
                ) {}
            }
        }
    }

    if (isInteractiveEditor && !maskLabels) {
        val midPos = safeMoveHandlePos
        val midPxX = midPos.x.roundToInt()
        val midPxY = midPos.y.roundToInt()

        Box(
            modifier = Modifier
                .offset { IntOffset(midPxX - handleHalfPx, midPxY - handleHalfPx) }
                .size(36.dp)
                .pointerInput("line_body_drag_${item.id}", imgOffsetX, imgOffsetY, renderedW, renderedH) {
                    var accTx = currentItem.targetX
                    var accTy = currentItem.targetY
                    var accBx = (currentResolvedBoxCenter.x / widthPx).coerceIn(0.02f, 0.98f)
                    var accBy = (currentResolvedBoxCenter.y / heightPx).coerceIn(0.02f, 0.98f)
                    var fingerX = currentSafeMoveHandlePos.x
                    var fingerY = currentSafeMoveHandlePos.y

                    detectDragGestures(
                        onDragStart = {
                            accTx = currentItem.targetX
                            accTy = currentItem.targetY
                            accBx = (currentResolvedBoxCenter.x / widthPx).coerceIn(0.02f, 0.98f)
                            accBy = (currentResolvedBoxCenter.y / heightPx).coerceIn(0.02f, 0.98f)
                            fingerX = currentSafeMoveHandlePos.x
                            fingerY = currentSafeMoveHandlePos.y
                            val overTrash = checkHoveringTrash(fingerX, fingerY)
                            onHoverTrashStateChanged(overTrash)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val dx = dragAmount.x / renderedW
                            val dy = dragAmount.y / renderedH
                            accTx = (accTx + dx).coerceIn(0.01f, 0.99f)
                            accTy = (accTy + dy).coerceIn(0.01f, 0.99f)
                            accBx = (accBx + dragAmount.x / widthPx).coerceIn(0.02f, 0.98f)
                            accBy = (accBy + dragAmount.y / heightPx).coerceIn(0.02f, 0.98f)
                            fingerX += dragAmount.x
                            fingerY += dragAmount.y

                            val overTrash = checkHoveringTrash(fingerX, fingerY)
                            onHoverTrashStateChanged(overTrash)

                            currentOnLineMoved?.invoke(
                                currentItem,
                                accTx, accTy,
                                accBx, accBy
                            )
                        },
                        onDragEnd = {
                            val overTrash = checkHoveringTrash(fingerX, fingerY)
                            if (isHoveringTrash || overTrash) {
                                onDeleteAnnotation?.invoke(currentItem)
                            }
                            onHoverTrashStateChanged(false)
                        },
                        onDragCancel = {
                            onHoverTrashStateChanged(false)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF94A3B8)),
                shadowElevation = 3.dp,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.OpenWith,
                        contentDescription = "Move Entire Object",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawHeartDiagram(size: Size) {
    val w = size.width
    val h = size.height

    drawRect(Color(0xFF0F172A))

    val svcPath = Path().apply {
        moveTo(w * 0.28f, h * 0.12f)
        lineTo(w * 0.36f, h * 0.12f)
        lineTo(w * 0.38f, h * 0.42f)
        lineTo(w * 0.28f, h * 0.40f)
        close()
    }
    drawPath(svcPath, Color(0xFF1E88E5))

    val aortaPath = Path().apply {
        moveTo(w * 0.44f, h * 0.34f)
        cubicTo(w * 0.42f, h * 0.10f, w * 0.62f, h * 0.10f, w * 0.62f, h * 0.35f)
        lineTo(w * 0.54f, h * 0.35f)
        cubicTo(w * 0.54f, h * 0.20f, w * 0.48f, h * 0.20f, w * 0.48f, h * 0.34f)
        close()
    }
    drawPath(aortaPath, Color(0xFFE53935))

    val paPath = Path().apply {
        moveTo(w * 0.48f, h * 0.32f)
        lineTo(w * 0.70f, h * 0.26f)
        lineTo(w * 0.72f, h * 0.34f)
        lineTo(w * 0.50f, h * 0.40f)
        close()
    }
    drawPath(paPath, Color(0xFF3949AB))

    val heartMuscles = Path().apply {
        moveTo(w * 0.50f, h * 0.36f)
        cubicTo(w * 0.22f, h * 0.34f, w * 0.20f, h * 0.65f, w * 0.50f, h * 0.88f)
        cubicTo(w * 0.80f, h * 0.65f, w * 0.78f, h * 0.34f, w * 0.50f, h * 0.36f)
        close()
    }
    drawPath(heartMuscles, Color(0xFFB71C1C))
    drawPath(heartMuscles, Color(0xFFFF8A80), style = Stroke(width = 3f))

    drawLine(
        color = Color(0xFF880E4F),
        start = Offset(w * 0.50f, h * 0.42f),
        end = Offset(w * 0.50f, h * 0.82f),
        strokeWidth = 6f
    )

    drawCircle(Color(0xFF1565C0).copy(alpha = 0.7f), radius = w * 0.11f, center = Offset(w * 0.35f, h * 0.48f))

    drawCircle(Color(0xFFC62828).copy(alpha = 0.7f), radius = w * 0.13f, center = Offset(w * 0.62f, h * 0.64f))
}

private fun DrawScope.drawBrainDiagram(size: Size) {
    val w = size.width
    val h = size.height

    val brainPath = Path().apply {
        moveTo(w * 0.22f, h * 0.55f)
        cubicTo(w * 0.15f, h * 0.30f, w * 0.40f, h * 0.16f, w * 0.65f, h * 0.18f)
        cubicTo(w * 0.88f, h * 0.20f, w * 0.88f, h * 0.50f, w * 0.82f, h * 0.58f)
        cubicTo(w * 0.75f, h * 0.62f, w * 0.65f, h * 0.58f, w * 0.50f, h * 0.62f)
        cubicTo(w * 0.35f, h * 0.65f, w * 0.25f, h * 0.65f, w * 0.22f, h * 0.55f)
        close()
    }
    drawPath(brainPath, Color(0xFFBA68C8))
    drawPath(brainPath, Color(0xFFE1BEE7), style = Stroke(width = 3.5f))

    drawArc(
        color = Color(0xFF4CAF50).copy(alpha = 0.4f),
        startAngle = 140f,
        sweepAngle = 120f,
        useCenter = true,
        topLeft = Offset(w * 0.20f, h * 0.22f),
        size = Size(w * 0.38f, h * 0.35f)
    )

    drawCircle(
        color = Color(0xFFFF9800).copy(alpha = 0.4f),
        radius = w * 0.10f,
        center = Offset(w * 0.75f, h * 0.44f)
    )

    val cerebellumPath = Path().apply {
        moveTo(w * 0.62f, h * 0.60f)
        cubicTo(w * 0.82f, h * 0.60f, w * 0.82f, h * 0.80f, w * 0.66f, h * 0.82f)
        cubicTo(w * 0.58f, h * 0.80f, w * 0.56f, h * 0.66f, w * 0.62f, h * 0.60f)
        close()
    }
    drawPath(cerebellumPath, Color(0xFF7E57C2))
    drawPath(cerebellumPath, Color(0xFFD1C4E9), style = Stroke(width = 2.5f))

    val stem = Path().apply {
        moveTo(w * 0.50f, h * 0.62f)
        lineTo(w * 0.56f, h * 0.62f)
        lineTo(w * 0.54f, h * 0.88f)
        lineTo(w * 0.46f, h * 0.88f)
        close()
    }
    drawPath(stem, Color(0xFFFFF176).copy(alpha = 0.8f))
}

private fun DrawScope.drawCellDiagram(size: Size) {
    val w = size.width
    val h = size.height

    drawOval(
        color = Color(0xFF26A69A),
        topLeft = Offset(w * 0.10f, h * 0.15f),
        size = Size(w * 0.80f, h * 0.70f),
        style = Stroke(width = 5f)
    )
    drawOval(
        color = Color(0xFF004D40).copy(alpha = 0.5f),
        topLeft = Offset(w * 0.10f, h * 0.15f),
        size = Size(w * 0.80f, h * 0.70f)
    )

    drawCircle(
        color = Color(0xFF8E24AA),
        radius = w * 0.18f,
        center = Offset(w * 0.48f, h * 0.46f)
    )
    drawCircle(
        color = Color(0xFFBA68C8),
        radius = w * 0.08f,
        center = Offset(w * 0.48f, h * 0.46f)
    )

    drawOval(
        color = Color(0xFFE65100),
        topLeft = Offset(w * 0.20f, h * 0.56f),
        size = Size(w * 0.15f, h * 0.10f)
    )

    for (i in 0..3) {
        drawLine(
            color = Color(0xFFFFB300),
            start = Offset(w * 0.64f, h * (0.62f + i * 0.025f)),
            end = Offset(w * 0.78f, h * (0.64f + i * 0.025f)),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }

    drawArc(
        color = Color(0xFF42A5F5),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.32f, h * 0.24f),
        size = Size(w * 0.25f, h * 0.12f),
        style = Stroke(width = 3.5f)
    )
}

private fun DrawScope.drawMapDiagram(size: Size) {
    val w = size.width
    val h = size.height

    for (i in 1..4) {
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(0f, h * (i * 0.2f)),
            end = Offset(w, h * (i * 0.2f)),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(w * (i * 0.2f), 0f),
            end = Offset(w * (i * 0.2f), h),
            strokeWidth = 1f
        )
    }

    drawOval(Color(0xFF2E7D32), topLeft = Offset(w * 0.16f, h * 0.22f), size = Size(w * 0.22f, h * 0.22f))
    drawOval(Color(0xFF388E3C), topLeft = Offset(w * 0.26f, h * 0.52f), size = Size(w * 0.16f, h * 0.28f))
    drawOval(Color(0xFF1565C0), topLeft = Offset(w * 0.46f, h * 0.22f), size = Size(w * 0.14f, h * 0.15f))
    drawOval(Color(0xFFF57F17), topLeft = Offset(w * 0.46f, h * 0.42f), size = Size(w * 0.18f, h * 0.26f))
    drawOval(Color(0xFFC2185B), topLeft = Offset(w * 0.62f, h * 0.20f), size = Size(w * 0.28f, h * 0.32f))
    drawOval(Color(0xFF00838F), topLeft = Offset(w * 0.74f, h * 0.65f), size = Size(w * 0.14f, h * 0.14f))
}

private fun DrawScope.drawGenericDiagram(size: Size) {
    val w = size.width
    val h = size.height

    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(w * 0.15f, h * 0.2f),
        size = Size(w * 0.7f, h * 0.6f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
    )

    drawCircle(Color(0xFF3B82F6), radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.5f))
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val l2 = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
    if (l2 == 0f) return (p - a).getDistance()
    val t = (((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2).coerceIn(0f, 1f)
    val proj = Offset(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
    return (p - proj).getDistance()
}

private fun ccw(a: Offset, b: Offset, c: Offset): Boolean {
    return (c.y - a.y) * (b.x - a.x) > (b.y - a.y) * (c.x - a.x)
}

private fun segmentsIntersect(p1: Offset, p2: Offset, p3: Offset, p4: Offset): Boolean {
    return (ccw(p1, p3, p4) != ccw(p2, p3, p4)) && (ccw(p1, p2, p3) != ccw(p1, p2, p4))
}

private fun segmentToSegmentDistance(p1: Offset, p2: Offset, q1: Offset, q2: Offset): Float {
    if (segmentsIntersect(p1, p2, q1, q2)) return 0f
    return minOf(
        minOf(distanceToSegment(p1, q1, q2), distanceToSegment(p2, q1, q2)),
        minOf(distanceToSegment(q1, p1, p2), distanceToSegment(q2, p1, p2))
    )
}

private fun segmentIntersectsRect(p1: Offset, p2: Offset, rect: Rect): Boolean {
    if (p1.x > rect.left && p1.x < rect.right && p1.y > rect.top && p1.y < rect.bottom) return true
    if (p2.x > rect.left && p2.x < rect.right && p2.y > rect.top && p2.y < rect.bottom) return true
    if (kotlin.math.abs(p1.y - p2.y) < 0.5f) {
        val y = 0.5f * (p1.y + p2.y)
        val minX = minOf(p1.x, p2.x)
        val maxX = maxOf(p1.x, p2.x)
        if (rect.top < y && y < rect.bottom) {
            if (!(maxX <= rect.left || minX >= rect.right)) return true
        }
        return false
    }
    if (kotlin.math.abs(p1.x - p2.x) < 0.5f) {
        val x = 0.5f * (p1.x + p2.x)
        val minY = minOf(p1.y, p2.y)
        val maxY = maxOf(p1.y, p2.y)
        if (rect.left < x && x < rect.right) {
            if (!(maxY <= rect.top || minY >= rect.bottom)) return true
        }
        return false
    }
    val tl = rect.topLeft
    val tr = rect.topRight
    val bl = rect.bottomLeft
    val br = rect.bottomRight
    return segmentsIntersect(p1, p2, tl, tr) ||
           segmentsIntersect(p1, p2, tr, br) ||
           segmentsIntersect(p1, p2, br, bl) ||
           segmentsIntersect(p1, p2, bl, tl)
}

private fun resolveAnnotationBoxHalfExtents(
    item: AnnotationItem,
    index: Int,
    densityScale: Float,
    measuredSizes: Map<String, androidx.compose.ui.unit.IntSize>
): Pair<Float, Float> {
    val measured = measuredSizes[item.id]
    if (item.isBoxAttached && measured != null && measured.width > 0 && measured.height > 0) {
        return (measured.width / 2f) to (measured.height / 2f)
    }
    if (!item.isBoxAttached) {
        return (18f * densityScale) to (18f * densityScale)
    }
    val textLen = if (item.labelAnswer.isNotEmpty()) item.labelAnswer.length else "Answer ${index + 1}".length
    val estWidthDp = (40f + textLen * 7f).coerceIn(96f, 190f)
    return ((estWidthDp * densityScale) / 2f) to (18f * densityScale)
}

private fun resolveNonOverlappingBoxRects(
    annotations: List<AnnotationItem>,
    measuredSizes: Map<String, androidx.compose.ui.unit.IntSize>,
    widthPx: Float,
    heightPx: Float,
    densityScale: Float,
    imgOffsetX: Float = 0f,
    imgOffsetY: Float = 0f,
    renderedW: Float = widthPx,
    renderedH: Float = heightPx
): Map<String, Rect> {
    if (annotations.isEmpty() || widthPx <= 0f || heightPx <= 0f) return emptyMap()
    val n = annotations.size
    val cx = FloatArray(n)
    val cy = FloatArray(n)
    val hw = FloatArray(n)
    val hh = FloatArray(n)
    val edgePad = 8f * densityScale
    val boxGap = 8f * densityScale
    val pinRad = 14f * densityScale

    for (i in 0 until n) {
        val item = annotations[i]
        val extents = resolveAnnotationBoxHalfExtents(item, i, densityScale, measuredSizes)
        hw[i] = extents.first
        hh[i] = extents.second
        val minX = minOf(hw[i] + edgePad, widthPx * 0.5f)
        val maxX = maxOf(widthPx - hw[i] - edgePad, widthPx * 0.5f)
        val minY = minOf(hh[i] + edgePad, heightPx * 0.5f)
        val maxY = maxOf(heightPx - hh[i] - edgePad, heightPx * 0.5f)
        cx[i] = (item.boxX * widthPx).coerceIn(minX, maxX)
        cy[i] = (item.boxY * heightPx).coerceIn(minY, maxY)
    }

    val pinX = FloatArray(n) { imgOffsetX + annotations[it].targetX * renderedW }
    val pinY = FloatArray(n) { imgOffsetY + annotations[it].targetY * renderedH }

    for (iter in 0 until 24) {
        var moved = false
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val reqDx = hw[i] + hw[j] + boxGap
                val reqDy = hh[i] + hh[j] + boxGap
                var dx = cx[j] - cx[i]
                var dy = cy[j] - cy[i]
                if (kotlin.math.abs(dx) < 0.5f && kotlin.math.abs(dy) < 0.5f) {
                    val angle = (j - i) * 1.1f
                    dx = kotlin.math.cos(angle) * 2f
                    dy = kotlin.math.sin(angle) * 2f
                }
                val overlapX = reqDx - kotlin.math.abs(dx)
                val overlapY = reqDy - kotlin.math.abs(dy)
                if (overlapX > 0f && overlapY > 0f) {
                    moved = true
                    if (overlapX < overlapY * 1.35f) {
                        val sign = if (dx >= 0f) 1f else -1f
                        val push = overlapX * 0.52f + 0.5f
                        cx[i] -= sign * push
                        cx[j] += sign * push
                    } else {
                        val sign = if (dy >= 0f) 1f else -1f
                        val push = overlapY * 0.52f + 0.5f
                        cy[i] -= sign * push
                        cy[j] += sign * push
                    }
                }
            }

            for (p in 0 until n) {
                val reqDx = hw[i] + pinRad
                val reqDy = hh[i] + pinRad
                var dx = cx[i] - pinX[p]
                var dy = cy[i] - pinY[p]
                if (kotlin.math.abs(dx) < 0.5f && kotlin.math.abs(dy) < 0.5f) {
                    dx = 2f
                    dy = -2f
                }
                val overlapX = reqDx - kotlin.math.abs(dx)
                val overlapY = reqDy - kotlin.math.abs(dy)
                if (overlapX > 0f && overlapY > 0f) {
                    moved = true
                    if (overlapX < overlapY) {
                        val sign = if (dx >= 0f) 1f else -1f
                        cx[i] += sign * (overlapX + 1f)
                    } else {
                        val sign = if (dy >= 0f) 1f else -1f
                        cy[i] += sign * (overlapY + 1f)
                    }
                }
            }

            val minX = minOf(hw[i] + edgePad, widthPx * 0.5f)
            val maxX = maxOf(widthPx - hw[i] - edgePad, widthPx * 0.5f)
            val minY = minOf(hh[i] + edgePad, heightPx * 0.5f)
            val maxY = maxOf(heightPx - hh[i] - edgePad, heightPx * 0.5f)
            cx[i] = cx[i].coerceIn(minX, maxX)
            cy[i] = cy[i].coerceIn(minY, maxY)
        }
        if (!moved) break
    }

    val out = HashMap<String, Rect>(n)
    for (i in 0 until n) {
        out[annotations[i].id] = Rect(
            cx[i] - hw[i],
            cy[i] - hh[i],
            cx[i] + hw[i],
            cy[i] + hh[i]
        )
    }
    return out
}

private fun buildUniqueAxisCoords(
    rawCoords: List<Float>,
    mustKeepA: Float,
    mustKeepB: Float,
    limitPx: Float,
    edgeMargin: Float
): FloatArray {
    if (limitPx <= 16f) return floatArrayOf(mustKeepA, mustKeepB)
    val safeMargin = edgeMargin.coerceAtMost(limitPx * 0.25f)
    val sorted = FloatArray(rawCoords.size + 2)
    var idx = 0
    for (c in rawCoords) {
        sorted[idx++] = c.coerceIn(safeMargin, limitPx - safeMargin)
    }
    sorted[idx++] = mustKeepA.coerceIn(2f, limitPx - 2f)
    sorted[idx++] = mustKeepB.coerceIn(2f, limitPx - 2f)
    sorted.sort()

    val out = FloatArray(sorted.size)
    var outSize = 0
    for (c in sorted) {
        val isMust = kotlin.math.abs(c - mustKeepA) < 0.5f || kotlin.math.abs(c - mustKeepB) < 0.5f
        if (outSize == 0) {
            out[outSize++] = c
        } else if (kotlin.math.abs(c - out[outSize - 1]) >= 8f) {
            out[outSize++] = c
        } else if (isMust) {
            out[outSize - 1] = c
        }
    }
    return out.copyOf(outSize)
}

private class OrthogonalPathNode(
    val xi: Int,
    val yi: Int,
    val dir: Int,
    val g: Float,
    val f: Float
) : Comparable<OrthogonalPathNode> {
    override fun compareTo(other: OrthogonalPathNode): Int = f.compareTo(other.f)
}

private fun computeAllAnnotationRoutes(
    annotations: List<AnnotationItem>,
    resolvedBoxRects: Map<String, Rect>,
    widthPx: Float,
    heightPx: Float,
    densityScale: Float,
    imgOffsetX: Float = 0f,
    imgOffsetY: Float = 0f,
    renderedW: Float = widthPx,
    renderedH: Float = heightPx
): Map<String, List<Offset>> {
    val n = annotations.size
    if (n == 0 || widthPx <= 0f || heightPx <= 0f) return emptyMap()
    val result = HashMap<String, List<Offset>>(n)
    val boxRects = Array(n) { idx ->
        val item = annotations[idx]
        resolvedBoxRects[item.id] ?: Rect(
            item.boxX * widthPx - 18f * densityScale,
            item.boxY * heightPx - 18f * densityScale,
            item.boxX * widthPx + 18f * densityScale,
            item.boxY * heightPx + 18f * densityScale
        )
    }
    val pinRadius = 11f * densityScale
    val pinRects = Array(n) { idx ->
        val tx = imgOffsetX + annotations[idx].targetX * renderedW
        val ty = imgOffsetY + annotations[idx].targetY * renderedH
        Rect(tx - pinRadius, ty - pinRadius, tx + pinRadius, ty + pinRadius)
    }
    val starts = Array(n) { idx ->
        Offset(imgOffsetX + annotations[idx].targetX * renderedW, imgOffsetY + annotations[idx].targetY * renderedH)
    }
    val ends = Array(n) { idx ->
        boxRects[idx].center
    }

    val order = IntArray(n) { it }
    for (i in 0 until n - 1) {
        var minIdx = i
        var minDist = (ends[order[i]] - starts[order[i]]).getDistanceSquared()
        for (j in i + 1 until n) {
            val d = (ends[order[j]] - starts[order[j]]).getDistanceSquared()
            if (d < minDist) {
                minDist = d
                minIdx = j
            }
        }
        if (minIdx != i) {
            val tmp = order[i]
            order[i] = order[minIdx]
            order[minIdx] = tmp
        }
    }

    val numPasses = if (n > 1) 2 else 1
    for (pass in 0 until numPasses) {
        for (ordIdx in 0 until n) {
            val i = order[ordIdx]
            val item = annotations[i]
            val coreObstacles = ArrayList<Rect>(n * 2)
            val otherSegments = ArrayList<Pair<Offset, Offset>>(n * 4)
            for (j in 0 until n) {
                if (j == i) continue
                coreObstacles.add(boxRects[j])
                coreObstacles.add(pinRects[j])
                val otherPoly = result[annotations[j].id]
                if (otherPoly != null) {
                    for (k in 0 until otherPoly.size - 1) {
                        otherSegments.add(otherPoly[k] to otherPoly[k + 1])
                    }
                }
            }
            val poly = computeSingleAnnotationRoute(
                start = starts[i],
                end = ends[i],
                coreObstacles = coreObstacles,
                routedSegments = otherSegments,
                widthPx = widthPx,
                heightPx = heightPx,
                densityScale = densityScale
            )
            result[item.id] = poly
        }
    }
    return result
}

private fun computeSingleAnnotationRoute(
    start: Offset,
    end: Offset,
    coreObstacles: List<Rect>,
    routedSegments: List<Pair<Offset, Offset>>,
    widthPx: Float,
    heightPx: Float,
    densityScale: Float
): List<Offset> {
    val pad1 = 6f * densityScale
    val pad2 = 16f * densityScale
    val lineClearance = 10f * densityScale
    val edgeMargin = 18f * densityScale

    var straightClear = true
    for (r in coreObstacles) {
        val pr = Rect(r.left - pad1, r.top - pad1, r.right + pad1, r.bottom + pad1)
        if (segmentIntersectsRect(start, end, pr)) {
            straightClear = false
            break
        }
    }
    if (straightClear) {
        for (seg in routedSegments) {
            if (segmentToSegmentDistance(start, end, seg.first, seg.second) < lineClearance) {
                straightClear = false
                break
            }
        }
    }
    if (straightClear) {
        return listOf(start, end)
    }

    val lCorners = arrayOf(Offset(end.x, start.y), Offset(start.x, end.y))
    for (corner in lCorners) {
        var cornerClear = true
        val segA = start to corner
        val segB = corner to end
        for (leg in arrayOf(segA, segB)) {
            if ((leg.second - leg.first).getDistanceSquared() < 1f) continue
            for (r in coreObstacles) {
                val pr = Rect(r.left - pad1, r.top - pad1, r.right + pad1, r.bottom + pad1)
                if (segmentIntersectsRect(leg.first, leg.second, pr)) {
                    cornerClear = false
                    break
                }
            }
            if (!cornerClear) break
            for (seg in routedSegments) {
                if (segmentToSegmentDistance(leg.first, leg.second, seg.first, seg.second) < lineClearance) {
                    cornerClear = false
                    break
                }
            }
            if (!cornerClear) break
        }
        if (cornerClear) {
            return listOf(start, corner, end)
        }
    }

    val xRaw = ArrayList<Float>(coreObstacles.size * 5 + routedSegments.size * 2 + 6)
    val yRaw = ArrayList<Float>(coreObstacles.size * 5 + routedSegments.size * 2 + 6)
    xRaw.add(18f * densityScale)
    xRaw.add(widthPx - 18f * densityScale)
    xRaw.add(32f * densityScale)
    xRaw.add(widthPx - 32f * densityScale)
    yRaw.add(18f * densityScale)
    yRaw.add(heightPx - 18f * densityScale)
    yRaw.add(32f * densityScale)
    yRaw.add(heightPx - 32f * densityScale)

    for (r in coreObstacles) {
        xRaw.add(r.left - pad1)
        xRaw.add(r.right + pad1)
        xRaw.add(r.left - pad2)
        xRaw.add(r.right + pad2)
        yRaw.add(r.top - pad1)
        yRaw.add(r.bottom + pad1)
        yRaw.add(r.top - pad2)
        yRaw.add(r.bottom + pad2)
    }

    for (i in coreObstacles.indices) {
        val r1 = coreObstacles[i]
        for (j in i + 1 until coreObstacles.size) {
            val r2 = coreObstacles[j]
            if (r2.left > r1.right + 4f) {
                xRaw.add(0.5f * (r1.right + r2.left))
            } else if (r1.left > r2.right + 4f) {
                xRaw.add(0.5f * (r2.right + r1.left))
            }
            if (r2.top > r1.bottom + 4f) {
                yRaw.add(0.5f * (r1.bottom + r2.top))
            } else if (r1.top > r2.bottom + 4f) {
                yRaw.add(0.5f * (r2.bottom + r1.top))
            }
        }
    }

    var hCount = 0
    var vCount = 0
    var dCount = 0
    for (seg in routedSegments) {
        val q1 = seg.first
        val q2 = seg.second
        if (kotlin.math.abs(q1.y - q2.y) < 1f) {
            val ym = 0.5f * (q1.y + q2.y)
            yRaw.add(ym - lineClearance)
            yRaw.add(ym + lineClearance)
            hCount++
        } else if (kotlin.math.abs(q1.x - q2.x) < 1f) {
            val xm = 0.5f * (q1.x + q2.x)
            xRaw.add(xm - lineClearance)
            xRaw.add(xm + lineClearance)
            vCount++
        } else {
            xRaw.add(q1.x - lineClearance)
            xRaw.add(q1.x + lineClearance)
            yRaw.add(q1.y - lineClearance)
            yRaw.add(q1.y + lineClearance)
            dCount++
        }
    }

    val hSegY = FloatArray(hCount)
    val hSegMinX = FloatArray(hCount)
    val hSegMaxX = FloatArray(hCount)
    val vSegX = FloatArray(vCount)
    val vSegMinY = FloatArray(vCount)
    val vSegMaxY = FloatArray(vCount)
    val dSegs = ArrayList<Pair<Offset, Offset>>(dCount)

    var hi = 0
    var vi = 0
    for (seg in routedSegments) {
        val q1 = seg.first
        val q2 = seg.second
        if (kotlin.math.abs(q1.y - q2.y) < 1f) {
            hSegY[hi] = 0.5f * (q1.y + q2.y)
            hSegMinX[hi] = minOf(q1.x, q2.x)
            hSegMaxX[hi] = maxOf(q1.x, q2.x)
            hi++
        } else if (kotlin.math.abs(q1.x - q2.x) < 1f) {
            vSegX[vi] = 0.5f * (q1.x + q2.x)
            vSegMinY[vi] = minOf(q1.y, q2.y)
            vSegMaxY[vi] = maxOf(q1.y, q2.y)
            vi++
        } else {
            dSegs.add(seg)
        }
    }

    val uniqueX = buildUniqueAxisCoords(xRaw, start.x, end.x, widthPx, edgeMargin)
    val uniqueY = buildUniqueAxisCoords(yRaw, start.y, end.y, heightPx, edgeMargin)

    if (uniqueX.size < 2 || uniqueY.size < 2) {
        return listOf(start, end)
    }

    var startXi = 0
    var bestDistSX = Float.MAX_VALUE
    for (i in uniqueX.indices) {
        val d = kotlin.math.abs(uniqueX[i] - start.x)
        if (d < bestDistSX) {
            bestDistSX = d
            startXi = i
        }
    }

    var startYi = 0
    var bestDistSY = Float.MAX_VALUE
    for (i in uniqueY.indices) {
        val d = kotlin.math.abs(uniqueY[i] - start.y)
        if (d < bestDistSY) {
            bestDistSY = d
            startYi = i
        }
    }

    var endXi = 0
    var bestDistEX = Float.MAX_VALUE
    for (i in uniqueX.indices) {
        val d = kotlin.math.abs(uniqueX[i] - end.x)
        if (d < bestDistEX) {
            bestDistEX = d
            endXi = i
        }
    }

    var endYi = 0
    var bestDistEY = Float.MAX_VALUE
    for (i in uniqueY.indices) {
        val d = kotlin.math.abs(uniqueY[i] - end.y)
        if (d < bestDistEY) {
            bestDistEY = d
            endYi = i
        }
    }

    val W = uniqueX.size
    val H = uniqueY.size
    val totalStates = W * H * 5
    val dist = FloatArray(totalStates) { Float.MAX_VALUE }
    val parent = IntArray(totalStates) { -1 }

    val openSet = PriorityQueue<OrthogonalPathNode>()

    val initH = kotlin.math.abs(uniqueX[startXi] - end.x) + kotlin.math.abs(uniqueY[startYi] - end.y)
    for (d in 1..4) {
        val state = (startXi * H + startYi) * 5 + d
        dist[state] = 0f
        openSet.add(OrthogonalPathNode(startXi, startYi, d, 0f, initH))
    }

    var finalState = -1

    while (!openSet.isEmpty()) {
        val curr = openSet.poll() ?: break
        val currState = (curr.xi * H + curr.yi) * 5 + curr.dir

        if (curr.g > dist[currState]) continue

        if (curr.xi == endXi && curr.yi == endYi) {
            finalState = currState
            break
        }

        val p1 = Offset(uniqueX[curr.xi], uniqueY[curr.yi])

        for (dirStep in 1..4) {
            val nxi: Int
            val nyi: Int
            val isHoriz: Boolean
            when (dirStep) {
                1 -> { nxi = curr.xi + 1; nyi = curr.yi; isHoriz = true }
                2 -> { nxi = curr.xi - 1; nyi = curr.yi; isHoriz = true }
                3 -> { nxi = curr.xi; nyi = curr.yi + 1; isHoriz = false }
                else -> { nxi = curr.xi; nyi = curr.yi - 1; isHoriz = false }
            }

            if (nxi !in 0 until W || nyi !in 0 until H) continue

            val p2 = Offset(uniqueX[nxi], uniqueY[nyi])
            val isGoalStep = (nxi == endXi && nyi == endYi)
            var blocked = false
            var extraPenalty = 0f

            if (isHoriz) {
                val y = p1.y
                val minX = minOf(p1.x, p2.x)
                val maxX = maxOf(p1.x, p2.x)
                for (r in coreObstacles) {
                    val p1In = p1.x > r.left && p1.x < r.right && y > r.top && y < r.bottom
                    val p2In = p2.x > r.left && p2.x < r.right && y > r.top && y < r.bottom
                    if (!p1In && !p2In) {
                        if (y > r.top && y < r.bottom && maxX > r.left && minX < r.right) {
                            blocked = true
                            break
                        }
                        if (y > r.top - pad1 + 1f && y < r.bottom + pad1 - 1f && maxX > r.left - pad1 + 1f && minX < r.right + pad1 - 1f) {
                            extraPenalty += 800f
                        }
                    } else if (p2In && !p1In) {
                        if (!isGoalStep) {
                            blocked = true
                            break
                        } else {
                            extraPenalty += 800f
                        }
                    }
                }
                if (blocked) continue

                for (k in 0 until hCount) {
                    if (kotlin.math.abs(y - hSegY[k]) < lineClearance) {
                        val ov = minOf(maxX, hSegMaxX[k]) - maxOf(minX, hSegMinX[k])
                        if (ov > 0f) {
                            extraPenalty += 25000f + ov * 50f
                        } else if (maxOf(minX - hSegMaxX[k], hSegMinX[k] - maxX) < lineClearance) {
                            extraPenalty += 3500f
                        }
                    }
                }
                for (k in 0 until vCount) {
                    val vx = vSegX[k]
                    val vmin = vSegMinY[k]
                    val vmax = vSegMaxY[k]
                    if (vx in minX..maxX && y in vmin..vmax) {
                        extraPenalty += 14000f
                    } else {
                        val dx = if (vx in minX..maxX) 0f else minOf(kotlin.math.abs(minX - vx), kotlin.math.abs(maxX - vx))
                        val dy = if (y in vmin..vmax) 0f else minOf(kotlin.math.abs(y - vmin), kotlin.math.abs(y - vmax))
                        if (dx < lineClearance && dy < lineClearance && dx * dx + dy * dy < lineClearance * lineClearance) {
                            extraPenalty += 3500f
                        }
                    }
                }
            } else {
                val x = p1.x
                val minY = minOf(p1.y, p2.y)
                val maxY = maxOf(p1.y, p2.y)
                for (r in coreObstacles) {
                    val p1In = x > r.left && x < r.right && p1.y > r.top && p1.y < r.bottom
                    val p2In = x > r.left && x < r.right && p2.y > r.top && p2.y < r.bottom
                    if (!p1In && !p2In) {
                        if (x > r.left && x < r.right && maxY > r.top && minY < r.bottom) {
                            blocked = true
                            break
                        }
                        if (x > r.left - pad1 + 1f && x < r.right + pad1 - 1f && maxY > r.top - pad1 + 1f && minY < r.bottom + pad1 - 1f) {
                            extraPenalty += 800f
                        }
                    } else if (p2In && !p1In) {
                        if (!isGoalStep) {
                            blocked = true
                            break
                        } else {
                            extraPenalty += 800f
                        }
                    }
                }
                if (blocked) continue

                for (k in 0 until vCount) {
                    if (kotlin.math.abs(x - vSegX[k]) < lineClearance) {
                        val ov = minOf(maxY, vSegMaxY[k]) - maxOf(minY, vSegMinY[k])
                        if (ov > 0f) {
                            extraPenalty += 25000f + ov * 50f
                        } else if (maxOf(minY - vSegMaxY[k], vSegMinY[k] - maxY) < lineClearance) {
                            extraPenalty += 3500f
                        }
                    }
                }
                for (k in 0 until hCount) {
                    val hy = hSegY[k]
                    val hmin = hSegMinX[k]
                    val hmax = hSegMaxX[k]
                    if (x in hmin..hmax && hy in minY..maxY) {
                        extraPenalty += 14000f
                    } else {
                        val dx = if (x in hmin..hmax) 0f else minOf(kotlin.math.abs(x - hmin), kotlin.math.abs(x - hmax))
                        val dy = if (hy in minY..maxY) 0f else minOf(kotlin.math.abs(minY - hy), kotlin.math.abs(maxY - hy))
                        if (dx < lineClearance && dy < lineClearance && dx * dx + dy * dy < lineClearance * lineClearance) {
                            extraPenalty += 3500f
                        }
                    }
                }
            }

            for (seg in dSegs) {
                val sDist = segmentToSegmentDistance(p1, p2, seg.first, seg.second)
                if (sDist == 0f) {
                    extraPenalty += 14000f
                } else if (sDist < lineClearance) {
                    extraPenalty += 3500f
                }
            }

            val stepDist = kotlin.math.abs(p2.x - p1.x) + kotlin.math.abs(p2.y - p1.y)
            val isBend = curr.dir != 0 && curr.dir != dirStep
            val bendPenalty = if (isBend) 85f else 0f
            val newG = curr.g + stepDist + bendPenalty + extraPenalty

            val nState = (nxi * H + nyi) * 5 + dirStep
            if (newG < dist[nState]) {
                dist[nState] = newG
                parent[nState] = currState
                val h = kotlin.math.abs(uniqueX[nxi] - end.x) + kotlin.math.abs(uniqueY[nyi] - end.y)
                openSet.add(OrthogonalPathNode(nxi, nyi, dirStep, newG, newG + h))
            }
        }
    }

    if (finalState == -1) {
        return listOf(start, end)
    }

    val rawPath = mutableListOf<Offset>()
    var trace = finalState
    while (trace != -1) {
        val combined = trace / 5
        val xi = combined / H
        val yi = combined % H
        rawPath.add(Offset(uniqueX[xi], uniqueY[yi]))
        trace = parent[trace]
    }
    rawPath.reverse()

    val deduped = mutableListOf<Offset>()
    for (p in rawPath) {
        if (deduped.isEmpty() || (p - deduped.last()).getDistance() >= 1.5f) {
            deduped.add(p)
        }
    }

    if (deduped.size <= 2) {
        return listOf(start, end)
    }

    val compressed = mutableListOf<Offset>()
    compressed.add(deduped.first())
    for (i in 1 until deduped.size - 1) {
        val prev = compressed.last()
        val curr = deduped[i]
        val next = deduped[i + 1]

        val isCollinearX = kotlin.math.abs(prev.x - curr.x) < 1f && kotlin.math.abs(curr.x - next.x) < 1f
        val isCollinearY = kotlin.math.abs(prev.y - curr.y) < 1f && kotlin.math.abs(curr.y - next.y) < 1f

        if (!isCollinearX && !isCollinearY) {
            compressed.add(curr)
        }
    }
    compressed.add(deduped.last())
    return compressed
}

private fun computeAllSafeMoveHandles(
    annotations: List<AnnotationItem>,
    allRoutes: Map<String, List<Offset>>,
    resolvedBoxRects: Map<String, Rect>,
    widthPx: Float,
    heightPx: Float,
    densityScale: Float,
    imgOffsetX: Float = 0f,
    imgOffsetY: Float = 0f,
    renderedW: Float = widthPx,
    renderedH: Float = heightPx
): Map<String, Offset> {
    if (annotations.isEmpty() || widthPx <= 0f || heightPx <= 0f) return emptyMap()
    val handleHalf = 19f * densityScale
    val pinRad = 13f * densityScale
    val placedHandles = ArrayList<Rect>(annotations.size)
    val out = HashMap<String, Offset>(annotations.size)

    val allBoxObstacles = ArrayList<Rect>(annotations.size * 2)
    for (item in annotations) {
        val br = resolvedBoxRects[item.id]
        if (br != null) {
            allBoxObstacles.add(Rect(br.left - 3f * densityScale, br.top - 3f * densityScale, br.right + 3f * densityScale, br.bottom + 3f * densityScale))
        }
        val tx = imgOffsetX + item.targetX * renderedW
        val ty = imgOffsetY + item.targetY * renderedH
        allBoxObstacles.add(Rect(tx - pinRad, ty - pinRad, tx + pinRad, ty + pinRad))
    }

    val sampleFracs = floatArrayOf(0.5f, 0.4f, 0.6f, 0.3f, 0.7f, 0.22f, 0.78f, 0.15f, 0.85f)

    for (item in annotations) {
        val poly = allRoutes[item.id] ?: listOf(
            Offset(imgOffsetX + item.targetX * renderedW, imgOffsetY + item.targetY * renderedH),
            resolvedBoxRects[item.id]?.center ?: Offset(item.boxX * widthPx, item.boxY * heightPx)
        )
        var bestPt: Offset? = null
        var bestScore = -Float.MAX_VALUE

        for (s in 0 until poly.size - 1) {
            val a = poly[s]
            val b = poly[s + 1]
            val segLen = (b - a).getDistance()
            if (segLen < 4f) continue

            for (t in sampleFracs) {
                val rawX = a.x + (b.x - a.x) * t
                val rawY = a.y + (b.y - a.y) * t
                val clampedX = rawX.coerceIn(handleHalf + 4f, (widthPx - handleHalf - 4f).coerceAtLeast(handleHalf + 4f))
                val clampedY = rawY.coerceIn(handleHalf + 4f, (heightPx - handleHalf - 4f).coerceAtLeast(handleHalf + 4f))
                val hr = Rect(clampedX - handleHalf, clampedY - handleHalf, clampedX + handleHalf, clampedY + handleHalf)

                var minClearance = Float.MAX_VALUE
                var collides = false

                for (obs in allBoxObstacles) {
                    if (hr.overlaps(obs)) {
                        collides = true
                        val ox = minOf(hr.right, obs.right) - maxOf(hr.left, obs.left)
                        val oy = minOf(hr.bottom, obs.bottom) - maxOf(hr.top, obs.top)
                        minClearance = minOf(minClearance, -maxOf(ox, oy))
                    } else {
                        val dx = maxOf(0f, maxOf(obs.left - hr.right, hr.left - obs.right))
                        val dy = maxOf(0f, maxOf(obs.top - hr.bottom, hr.top - obs.bottom))
                        minClearance = minOf(minClearance, kotlin.math.hypot(dx, dy))
                    }
                }

                for (ph in placedHandles) {
                    if (hr.overlaps(ph)) {
                        collides = true
                        val ox = minOf(hr.right, ph.right) - maxOf(hr.left, ph.left)
                        val oy = minOf(hr.bottom, ph.bottom) - maxOf(hr.top, ph.top)
                        minClearance = minOf(minClearance, -maxOf(ox, oy) - 20f)
                    } else {
                        val dx = maxOf(0f, maxOf(ph.left - hr.right, hr.left - ph.right))
                        val dy = maxOf(0f, maxOf(ph.top - hr.bottom, hr.top - ph.bottom))
                        minClearance = minOf(minClearance, kotlin.math.hypot(dx, dy))
                    }
                }

                for (otherItem in annotations) {
                    if (otherItem.id == item.id) continue
                    val otherPoly = allRoutes[otherItem.id] ?: continue
                    for (os in 0 until otherPoly.size - 1) {
                        val distOtherSeg = distanceToSegment(Offset(clampedX, clampedY), otherPoly[os], otherPoly[os + 1])
                        if (distOtherSeg < handleHalf + 2f) {
                            collides = true
                            minClearance = minOf(minClearance, distOtherSeg - (handleHalf + 2f))
                        } else {
                            minClearance = minOf(minClearance, distOtherSeg - handleHalf)
                        }
                    }
                }

                val centerPref = 1f - kotlin.math.abs(t - 0.5f)
                val clampPenalty = kotlin.math.hypot(rawX - clampedX, rawY - clampedY) * 3f
                val score = (if (collides) minClearance * 50f else minClearance.coerceAtMost(24f * densityScale)) +
                    segLen * 0.15f + centerPref * 12f - clampPenalty

                if (score > bestScore) {
                    bestScore = score
                    bestPt = Offset(clampedX, clampedY)
                }
            }
        }

        val chosen = bestPt ?: Offset(
            ((poly.first().x + poly.last().x) * 0.5f).coerceIn(handleHalf + 4f, (widthPx - handleHalf - 4f).coerceAtLeast(handleHalf + 4f)),
            ((poly.first().y + poly.last().y) * 0.5f).coerceIn(handleHalf + 4f, (heightPx - handleHalf - 4f).coerceAtLeast(handleHalf + 4f))
        )
        out[item.id] = chosen
        placedHandles.add(Rect(chosen.x - handleHalf, chosen.y - handleHalf, chosen.x + handleHalf, chosen.y + handleHalf))
    }

    return out
}

