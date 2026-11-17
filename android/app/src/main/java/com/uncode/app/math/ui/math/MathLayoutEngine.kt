package com.uncode.app.math.ui.math

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uncode.app.math.ui.theme.AccentCyan
import com.uncode.app.math.ui.theme.AccentIndigo
import com.uncode.app.math.ui.theme.JetBrainsMono
import kotlin.math.roundToInt

private fun isSupportedInJetBrainsMono(text: String): Boolean =
    text.all { c -> (c.code in 0x20..0x7E) || c in "²³·÷×−" }

sealed class MathAstNode {
    abstract val start: Int
    abstract val end: Int

    data class TextToken(
        val text: String,
        override val start: Int,
        override val end: Int,
        val isVariable: Boolean = false,
        val isOperator: Boolean = false
    ) : MathAstNode()

    data class FractionNode(
        val numerator: List<MathAstNode>,
        val denominator: List<MathAstNode>,
        val numRange: IntRange,
        val denRange: IntRange,
        override val start: Int,
        override val end: Int
    ) : MathAstNode()

    data class SqrtNode(
        val inner: List<MathAstNode>,
        val innerRange: IntRange,
        val rootDegree: String? = null,
        val degreeNodes: List<MathAstNode>? = null,
        val degreeRange: IntRange? = null,
        override val start: Int,
        override val end: Int
    ) : MathAstNode()

    data class PowerNode(
        val base: List<MathAstNode>,
        val exponent: List<MathAstNode>,
        val baseRange: IntRange,
        val expRange: IntRange,
        override val start: Int,
        override val end: Int
    ) : MathAstNode()

    data class AbsNode(
        val inner: List<MathAstNode>,
        val innerRange: IntRange,
        override val start: Int,
        override val end: Int
    ) : MathAstNode()

    data class ParenNode(
        val inner: List<MathAstNode>,
        override val start: Int,
        override val end: Int,
        val hasCloseParen: Boolean = true
    ) : MathAstNode()

    data class FunctionNode(
        val name: String,
        val arg: List<MathAstNode>,
        val argRange: IntRange,
        override val start: Int,
        override val end: Int
    ) : MathAstNode()

    data class TemplateNode(
        val kind: String,
        val slots: List<Pair<List<MathAstNode>, IntRange>>,
        override val start: Int,
        override val end: Int
    ) : MathAstNode()

    data class Grid2DNode(
        val isDeterminant: Boolean,
        val rows: Int,
        val cols: Int,
        val cells: List<Pair<List<MathAstNode>, IntRange>>,
        override val start: Int,
        override val end: Int
    ) : MathAstNode()

    data class EmptyPlaceholder(
        val slotIndex: Int,
        override val start: Int,
        override val end: Int
    ) : MathAstNode()
}

object AstParser {
    private val KNOWN_FUNCS = listOf(
        "integrate", "defint", "derivn", "derivv", "deriv",
        "arcsin", "arccos", "arctan", "arccot", "arcsec", "arccsc",
        "arsinh", "arcosh", "artanh", "arcoth", "arsech", "arcsch",
        "matrix",
        "sin⁻¹", "cos⁻¹", "tan⁻¹", "cot⁻¹",
        "mixed", "binom",
        "sinh", "cosh", "tanh", "coth", "sech", "csch",
        "cbrt", "root", "perm", "comb", "conj", "logb", "dms2", "dms3",
        "intx", "intv", "lim+", "lim-", "limp", "limm",
        "asin", "acos", "atan", "acot",
        "d/dx", "log2", "log₂",
        "sin", "cos", "tan", "cot", "sec", "csc",
        "log", "ln", "exp", "lim", "sum", "mod",
        "abs", "sqrt", "sgn", "det", "rad", "sub", "fn1", "fn2", "deg", "sys"
    )

    private val TEMPLATE_KINDS = setOf(
        "logb", "sub", "mixed", "perm", "comb", "binom",
        "fn1", "fn2", "deg", "dms2", "dms3",
        "lim", "lim+", "lim-", "limp", "limm",
        "d/dx", "deriv", "derivv", "derivn",
        "intx", "intv", "defint", "integrate", "sum", "sys"
    )

    fun parse(raw: String): List<MathAstNode> {
        return parseRange(raw, 0, raw.length)
    }

    private fun canPopAsOperand(nodes: List<MathAstNode>): Boolean {
        if (nodes.isEmpty()) return false
        val last = nodes.last()
        return !(last is MathAstNode.TextToken && last.isOperator)
    }

    private fun splitSlots(raw: String, from: Int, to: Int): List<Pair<List<MathAstNode>, IntRange>> {
        val result = mutableListOf<Pair<List<MathAstNode>, IntRange>>()
        var depth = 0
        var slotStart = from
        var k = from
        while (k < to) {
            when (raw[k]) {
                '(' -> depth++
                ')' -> if (depth > 0) depth--
                ';' -> if (depth == 0) {
                    result.add(parseRange(raw, slotStart, k) to (slotStart..k))
                    slotStart = k + 1
                }
            }
            k++
        }
        result.add(parseRange(raw, slotStart, to) to (slotStart..to))
        return result
    }

    private fun parseGrid2D(
        raw: String,
        isDet: Boolean,
        contentStart: Int,
        contentEnd: Int,
        fnStart: Int,
        nodeEnd: Int
    ): MathAstNode {
        val rawSlots = splitSlots(raw, contentStart, contentEnd)
        if (rawSlots.size >= 2) {
            val headerRange = rawSlots[0].second
            val headerStr = raw.substring(headerRange.first, headerRange.last).trim()
            val dims = headerStr.split(",")
            val r = dims.getOrNull(0)?.trim()?.toIntOrNull()?.coerceIn(1, 5)
            val c = dims.getOrNull(1)?.trim()?.toIntOrNull()?.coerceIn(1, 5)
            if (r != null && c != null) {
                val cellSlots = rawSlots.drop(1)
                return MathAstNode.Grid2DNode(
                    isDeterminant = isDet,
                    rows = r,
                    cols = c,
                    cells = cellSlots,
                    start = fnStart,
                    end = nodeEnd
                )
            }
        }
        val singleSlot = parseRange(raw, contentStart, contentEnd)
        return MathAstNode.FunctionNode(
            name = if (isDet) "det" else "matrix",
            arg = singleSlot,
            argRange = contentStart..contentEnd,
            start = fnStart,
            end = nodeEnd
        )
    }

    private fun ensureSlotCount(
        slots: List<Pair<List<MathAstNode>, IntRange>>,
        minCount: Int,
        fallbackPos: Int
    ): List<Pair<List<MathAstNode>, IntRange>> {
        if (slots.size >= minCount) return slots
        val padded = slots.toMutableList()
        while (padded.size < minCount) {
            padded.add(emptyList<MathAstNode>() to (fallbackPos..fallbackPos))
        }
        return padded
    }

    private fun parseRange(raw: String, from: Int, to: Int): List<MathAstNode> {
        val nodes = mutableListOf<MathAstNode>()
        var i = from

        while (i < to) {
            val c = raw[i]

            when {
                raw.startsWith("==", i) -> {
                    nodes.add(MathAstNode.TextToken("=", i, i + 2, isOperator = true))
                    i += 2
                }
                raw.startsWith("!=", i) -> {
                    nodes.add(MathAstNode.TextToken("≠", i, i + 2, isOperator = true))
                    i += 2
                }
                raw.startsWith("<=", i) -> {
                    nodes.add(MathAstNode.TextToken("≤", i, i + 2, isOperator = true))
                    i += 2
                }
                raw.startsWith(">=", i) -> {
                    nodes.add(MathAstNode.TextToken("≥", i, i + 2, isOperator = true))
                    i += 2
                }
                raw.startsWith("oo", i) && (i + 2 == to || !raw[i + 2].isLetter()) -> {
                    nodes.add(MathAstNode.TextToken("∞", i, i + 2, isVariable = true))
                    i += 2
                }
                c == '√' || c == '∛' -> {
                    val degree = if (c == '∛') "3" else null
                    val rootStart = i
                    i++
                    if (i < to && raw[i] == '(') {
                        val close = findMatchingParen(raw, i, to)
                        val hasClose = close < to && raw[close] == ')'
                        val inner = parseRange(raw, i + 1, close)
                        val nodeEnd = if (hasClose) close + 1 else close
                        nodes.add(MathAstNode.SqrtNode(inner, (i + 1)..close, degree, null, null, rootStart, nodeEnd))
                        i = nodeEnd
                    } else {
                        var end = i
                        while (end < to && raw[end].isLetterOrDigit()) end++
                        val inner = if (end > i) parseRange(raw, i, end) else emptyList()
                        nodes.add(MathAstNode.SqrtNode(inner, i..end, degree, null, null, rootStart, end))
                        i = end
                    }
                }
                c == '|' -> {
                    val close = raw.indexOf('|', i + 1)
                    if (close in (i + 1) until to) {
                        val inner = parseRange(raw, i + 1, close)
                        nodes.add(MathAstNode.AbsNode(inner, (i + 1)..close, i, close + 1))
                        i = close + 1
                    } else {
                        nodes.add(MathAstNode.TextToken("|", i, i + 1, isOperator = true))
                        i++
                    }
                }
                c == '/' -> {
                    val last = if (canPopAsOperand(nodes)) nodes.removeAt(nodes.size - 1) else null
                    val numNodes = if (last is MathAstNode.ParenNode) last.inner else if (last != null) listOf(last) else emptyList()
                    val numRange = if (last is MathAstNode.ParenNode) {
                        (last.start + 1)..(if (last.hasCloseParen) last.end - 1 else last.end)
                    } else if (last != null) {
                        last.start..last.end
                    } else {
                        i..i
                    }
                    val numStart = last?.start ?: i
                    i++

                    while (i < to && raw[i] == ' ') i++

                    if (i < to && raw[i] == '(') {
                        val close = findMatchingParen(raw, i, to)
                        val hasClose = close < to && raw[close] == ')'
                        val den = parseRange(raw, i + 1, close)
                        val denRange = (i + 1)..close
                        val nodeEnd = if (hasClose) close + 1 else close
                        nodes.add(MathAstNode.FractionNode(numNodes, den, numRange, denRange, numStart, nodeEnd))
                        i = nodeEnd
                    } else {
                        var end = i
                        while (end < to && (raw[end].isLetterOrDigit() || raw[end] in "αβγδεθλμπρστφωΔ")) end++
                        val den = if (end > i) parseRange(raw, i, end) else emptyList()
                        nodes.add(MathAstNode.FractionNode(numNodes, den, numRange, i..end, numStart, end))
                        i = end
                    }
                }
                c == '^' -> {
                    val last = if (canPopAsOperand(nodes)) nodes.removeAt(nodes.size - 1) else null
                    val baseNodes = if (last is MathAstNode.ParenNode) last.inner else if (last != null) listOf(last) else emptyList()
                    val baseRange = if (last is MathAstNode.ParenNode) {
                        (last.start + 1)..(if (last.hasCloseParen) last.end - 1 else last.end)
                    } else if (last != null) {
                        last.start..last.end
                    } else {
                        i..i
                    }
                    val baseStart = last?.start ?: i
                    i++

                    while (i < to && raw[i] == ' ') i++

                    if (i < to && raw[i] == '(') {
                        val close = findMatchingParen(raw, i, to)
                        val hasClose = close < to && raw[close] == ')'
                        val exp = parseRange(raw, i + 1, close)
                        val expRange = (i + 1)..close
                        val nodeEnd = if (hasClose) close + 1 else close
                        nodes.add(MathAstNode.PowerNode(baseNodes, exp, baseRange, expRange, baseStart, nodeEnd))
                        i = nodeEnd
                    } else {
                        var end = i
                        if (end < to && (raw[end] == '-' || raw[end] == '−')) {
                            end++
                            while (end < to && raw[end] == ' ') end++
                        }
                        while (end < to && (raw[end].isLetterOrDigit() || raw[end] in "αβγδεθλμπρστφωΔ")) end++
                        val exp = if (end > i) parseRange(raw, i, end) else emptyList()
                        nodes.add(MathAstNode.PowerNode(baseNodes, exp, baseRange, i..end, baseStart, end))
                        i = end
                    }
                }
                c in "⁰¹²³⁴⁵⁶⁷⁸⁹ⁿˣʸ" -> {
                    val last = if (canPopAsOperand(nodes)) nodes.removeAt(nodes.size - 1) else null
                    val baseNodes = if (last is MathAstNode.ParenNode) last.inner else if (last != null) listOf(last) else emptyList()
                    val baseRange = if (last is MathAstNode.ParenNode) {
                        (last.start + 1)..(if (last.hasCloseParen) last.end - 1 else last.end)
                    } else if (last != null) {
                        last.start..last.end
                    } else {
                        i..i
                    }
                    val baseStart = last?.start ?: i

                    val expVal = when (c) {
                        '⁰' -> "0"
                        '¹' -> "1"
                        '²' -> "2"
                        '³' -> "3"
                        '⁴' -> "4"
                        '⁵' -> "5"
                        '⁶' -> "6"
                        '⁷' -> "7"
                        '⁸' -> "8"
                        '⁹' -> "9"
                        'ⁿ' -> "n"
                        'ˣ' -> "x"
                        'ʸ' -> "y"
                        else -> c.toString()
                    }
                    val isVarExp = c in "ⁿˣʸ"
                    val expNode = listOf(MathAstNode.TextToken(expVal, i, i + 1, isVariable = isVarExp))
                    nodes.add(MathAstNode.PowerNode(baseNodes, expNode, baseRange, i..(i + 1), baseStart, i + 1))
                    i++
                }
                c == '(' -> {
                    val close = findMatchingParen(raw, i, to)
                    val hasClose = close < to && raw[close] == ')'
                    val inner = parseRange(raw, i + 1, close)
                    val nodeEnd = if (hasClose) close + 1 else close
                    nodes.add(MathAstNode.ParenNode(inner, i, nodeEnd, hasCloseParen = hasClose))
                    i = nodeEnd
                }
                c.isDigit() -> {
                    var end = i
                    while (end < to && (raw[end].isDigit() || raw[end] == '.')) end++
                    nodes.add(MathAstNode.TextToken(raw.substring(i, end), i, end))
                    i = end
                }
                c == '+' || c == '-' || c == '−' || c == '*' || c == '·' || c == '=' || c == '<' || c == '>' || c == ',' || c == '!' || c == '%' || c == '\'' -> {
                    val sym = if (c == '*') "·" else c.toString()
                    nodes.add(MathAstNode.TextToken(sym, i, i + 1, isOperator = c != '\''))
                    i++
                }
                c in "αβγδεθλμπρστφωΔ" -> {
                    nodes.add(MathAstNode.TextToken(c.toString(), i, i + 1, isVariable = true))
                    i++
                }
                else -> {
                    val matchedFunc = KNOWN_FUNCS.firstOrNull { raw.startsWith(it, i) && i + it.length < to && raw[i + it.length] == '(' }
                    if (matchedFunc != null) {
                        val fnStart = i
                        val parenStart = i + matchedFunc.length
                        val close = findMatchingParen(raw, parenStart, to)
                        val hasClose = close < to && raw[close] == ')'
                        val contentStart = parenStart + 1
                        val nodeEnd = if (hasClose) close + 1 else close

                        when (matchedFunc) {
                            "cbrt" -> {
                                val arg = parseRange(raw, contentStart, close)
                                nodes.add(MathAstNode.SqrtNode(arg, contentStart..close, "3", null, null, fnStart, nodeEnd))
                            }
                            "sqrt" -> {
                                val arg = parseRange(raw, contentStart, close)
                                nodes.add(MathAstNode.SqrtNode(arg, contentStart..close, null, null, null, fnStart, nodeEnd))
                            }
                            "root" -> {
                                val slots = splitSlots(raw, contentStart, close)
                                if (slots.size >= 2) {
                                    nodes.add(
                                        MathAstNode.SqrtNode(
                                            inner = slots[1].first,
                                            innerRange = slots[1].second,
                                            rootDegree = null,
                                            degreeNodes = slots[0].first,
                                            degreeRange = slots[0].second,
                                            start = fnStart,
                                            end = nodeEnd
                                        )
                                    )
                                } else {
                                    nodes.add(
                                        MathAstNode.SqrtNode(
                                            inner = slots[0].first,
                                            innerRange = slots[0].second,
                                            rootDegree = "n",
                                            degreeNodes = null,
                                            degreeRange = null,
                                            start = fnStart,
                                            end = nodeEnd
                                        )
                                    )
                                }
                            }
                            "abs" -> {
                                val arg = parseRange(raw, contentStart, close)
                                nodes.add(MathAstNode.AbsNode(arg, contentStart..close, fnStart, nodeEnd))
                            }
                            "matrix" -> {
                                nodes.add(parseGrid2D(raw, isDet = false, contentStart = contentStart, contentEnd = close, fnStart = fnStart, nodeEnd = nodeEnd))
                            }
                            "det" -> {
                                nodes.add(parseGrid2D(raw, isDet = true, contentStart = contentStart, contentEnd = close, fnStart = fnStart, nodeEnd = nodeEnd))
                            }
                            in TEMPLATE_KINDS -> {
                                val rawSlots = splitSlots(raw, contentStart, close)
                                val minSlots = when (matchedFunc) {
                                    "defint", "sum" -> 4
                                    "mixed", "fn2", "dms3", "lim", "lim+", "lim-", "limp", "limm", "derivn" -> 3
                                    "logb", "sub", "perm", "comb", "binom", "fn1", "dms2", "derivv", "intv", "sys" -> 2
                                    else -> 1
                                }
                                val slots = ensureSlotCount(rawSlots, minSlots, close)
                                nodes.add(MathAstNode.TemplateNode(matchedFunc, slots, fnStart, nodeEnd))
                            }
                            else -> {
                                val arg = parseRange(raw, contentStart, close)
                                nodes.add(MathAstNode.FunctionNode(matchedFunc, arg, contentStart..close, fnStart, nodeEnd))
                            }
                        }
                        i = nodeEnd
                    } else if (c.isLetter()) {
                        nodes.add(MathAstNode.TextToken(c.toString(), i, i + 1, isVariable = true))
                        i++
                    } else {
                        if (!c.isWhitespace()) {
                            nodes.add(MathAstNode.TextToken(c.toString(), i, i + 1))
                        }
                        i++
                    }
                }
            }
        }

        return nodes
    }

    private fun findMatchingParen(raw: String, openIdx: Int, maxIdx: Int): Int {
        var depth = 1
        var i = openIdx + 1
        while (i < maxIdx) {
            when (raw[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        return maxIdx
    }
}

private fun hasActiveEmptySlotInList(nodes: List<MathAstNode>, cursorIndex: Int): Boolean {
    if (cursorIndex < 0) return false
    return nodes.any { hasActiveEmptySlotInNode(it, cursorIndex) }
}

private fun hasActiveEmptySlotInNode(node: MathAstNode, cursorIndex: Int): Boolean {
    return when (node) {
        is MathAstNode.PowerNode -> {
            (node.base.isEmpty() && cursorIndex in node.baseRange) ||
                (node.exponent.isEmpty() && cursorIndex in node.expRange) ||
                hasActiveEmptySlotInList(node.base, cursorIndex) ||
                hasActiveEmptySlotInList(node.exponent, cursorIndex)
        }
        is MathAstNode.FractionNode -> {
            (node.numerator.isEmpty() && cursorIndex in node.numRange) ||
                (node.denominator.isEmpty() && cursorIndex in node.denRange) ||
                hasActiveEmptySlotInList(node.numerator, cursorIndex) ||
                hasActiveEmptySlotInList(node.denominator, cursorIndex)
        }
        is MathAstNode.SqrtNode -> {
            (node.degreeRange != null && node.degreeNodes.isNullOrEmpty() && cursorIndex in node.degreeRange) ||
                (node.degreeNodes != null && hasActiveEmptySlotInList(node.degreeNodes, cursorIndex)) ||
                (node.inner.isEmpty() && cursorIndex in node.innerRange) ||
                hasActiveEmptySlotInList(node.inner, cursorIndex)
        }
        is MathAstNode.AbsNode -> {
            (node.inner.isEmpty() && cursorIndex in node.innerRange) ||
                hasActiveEmptySlotInList(node.inner, cursorIndex)
        }
        is MathAstNode.FunctionNode -> {
            (node.arg.isEmpty() && cursorIndex in node.argRange) ||
                hasActiveEmptySlotInList(node.arg, cursorIndex)
        }
        is MathAstNode.ParenNode -> {
            val innerEnd = if (node.hasCloseParen) node.end - 1 else node.end
            (node.inner.isEmpty() && cursorIndex in (node.start + 1)..innerEnd) ||
                hasActiveEmptySlotInList(node.inner, cursorIndex)
        }
        is MathAstNode.TemplateNode -> {
            node.slots.any { (slotNodes, slotRange) ->
                (slotNodes.isEmpty() && cursorIndex in slotRange) ||
                    hasActiveEmptySlotInList(slotNodes, cursorIndex)
            }
        }
        is MathAstNode.Grid2DNode -> {
            node.cells.any { (cellNodes, cellRange) ->
                (cellNodes.isEmpty() && cursorIndex in cellRange) ||
                    hasActiveEmptySlotInList(cellNodes, cursorIndex)
            }
        }
        is MathAstNode.EmptyPlaceholder -> cursorIndex in node.start..node.end
        is MathAstNode.TextToken -> false
    }
}

private fun nodeDelegatesStartCursor(node: MathAstNode): Boolean {
    return when (node) {
        is MathAstNode.PowerNode -> node.baseRange.first == node.start
        is MathAstNode.FractionNode -> node.numRange.first == node.start
        else -> false
    }
}

@Composable
fun MathFormulaView(
    formula: String,
    modifier: Modifier = Modifier,
    cursorIndex: Int = -1,
    fontSize: TextUnit = 22.sp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onSlotClick: (Int) -> Unit = {}
) {
    val astNodes = remember(formula) { AstParser.parse(formula) }
    val activeSlotOwnsCursor = remember(astNodes, cursorIndex) {
        hasActiveEmptySlotInList(astNodes, cursorIndex)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RenderNodeList(
            nodes = astNodes,
            cursorIndex = cursorIndex,
            parentRange = 0..formula.length,
            fontSize = fontSize,
            color = color,
            onSlotClick = onSlotClick,
            suppressCursorBars = activeSlotOwnsCursor,
            allowEndCursor = true
        )
        if (!activeSlotOwnsCursor && astNodes.isNotEmpty() && cursorIndex > astNodes.last().end) {
            PulsingCursorBar(fontSize = fontSize)
        }
    }
}

@Composable
fun RenderNodeList(
    nodes: List<MathAstNode>,
    cursorIndex: Int,
    parentRange: IntRange,
    fontSize: TextUnit,
    color: Color,
    onSlotClick: (Int) -> Unit,
    suppressCursorBars: Boolean = false,
    allowEndCursor: Boolean = true
) {
    if (nodes.isEmpty()) {
        val slotIdx = parentRange.first
        val isActive = cursorIndex in parentRange
        PlaceholderSlot(
            isActive = isActive,
            fontSize = fontSize,
            onClick = { onSlotClick(slotIdx) }
        )
        return
    }

    nodes.forEachIndexed { index, node ->
        if (!suppressCursorBars && cursorIndex == node.start && index == 0 && parentRange.first == node.start && !nodeDelegatesStartCursor(node)) {
            PulsingCursorBar(fontSize = fontSize)
        }

        RenderAstNode(
            node = node,
            cursorIndex = cursorIndex,
            fontSize = fontSize,
            color = color,
            onSlotClick = onSlotClick,
            suppressCursorBars = suppressCursorBars
        )

        val isLastNode = index == nodes.lastIndex
        val canShowEnd = !suppressCursorBars && (allowEndCursor || !isLastNode)
        if (canShowEnd && cursorIndex == node.end) {
            PulsingCursorBar(fontSize = fontSize)
        }
    }
}

@Composable
fun RenderAstNode(
    node: MathAstNode,
    cursorIndex: Int,
    fontSize: TextUnit,
    color: Color,
    onSlotClick: (Int) -> Unit,
    suppressCursorBars: Boolean = false
) {
    when (node) {
        is MathAstNode.TextToken -> {
            RenderTextToken(
                node = node,
                cursorIndex = cursorIndex,
                fontSize = fontSize,
                color = color,
                onSlotClick = onSlotClick,
                suppressCursorBars = suppressCursorBars
            )
        }
        is MathAstNode.FractionNode -> {
            Column(
                modifier = Modifier.padding(horizontal = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    RenderNodeList(
                        nodes = node.numerator,
                        cursorIndex = cursorIndex,
                        parentRange = node.numRange,
                        fontSize = fontSize * 0.85f,
                        color = color,
                        onSlotClick = onSlotClick,
                        suppressCursorBars = suppressCursorBars,
                        allowEndCursor = true
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .widthIn(min = 18.dp)
                        .height(1.8.dp)
                        .background(color, RoundedCornerShape(1.dp))
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    RenderNodeList(
                        nodes = node.denominator,
                        cursorIndex = cursorIndex,
                        parentRange = node.denRange,
                        fontSize = fontSize * 0.85f,
                        color = color,
                        onSlotClick = onSlotClick,
                        suppressCursorBars = suppressCursorBars,
                        allowEndCursor = node.denRange.last < node.end
                    )
                }
            }
        }
        is MathAstNode.SqrtNode -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (node.degreeRange != null && node.degreeNodes != null) {
                    Box(modifier = Modifier.padding(bottom = (fontSize.value * 0.42f).dp, end = 1.dp)) {
                        RenderNodeList(
                            nodes = node.degreeNodes,
                            cursorIndex = cursorIndex,
                            parentRange = node.degreeRange,
                            fontSize = fontSize * 0.62f,
                            color = color,
                            onSlotClick = onSlotClick,
                            suppressCursorBars = suppressCursorBars,
                            allowEndCursor = true
                        )
                    }
                } else if (node.rootDegree != null) {
                    Text(
                        text = node.rootDegree,
                        color = AccentCyan,
                        fontSize = fontSize * 0.6f,
                        fontFamily = if (node.rootDegree == "n") FontFamily.Serif else JetBrainsMono,
                        fontStyle = if (node.rootDegree == "n") FontStyle.Italic else FontStyle.Normal,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp, end = 1.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .drawBehind {
                            val h = size.height
                            val stroke = 1.8.dp.toPx()
                            val tickW = 10.dp.toPx()

                            val path = Path().apply {
                                moveTo(1.dp.toPx(), h * 0.60f)
                                lineTo(4.dp.toPx(), h * 0.85f)
                                lineTo(tickW, 2.dp.toPx())
                                lineTo(size.width, 2.dp.toPx())
                            }

                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(
                                    width = stroke,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                        .padding(start = 12.dp, top = 5.dp, end = 3.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RenderNodeList(
                        nodes = node.inner,
                        cursorIndex = cursorIndex,
                        parentRange = node.innerRange,
                        fontSize = fontSize,
                        color = color,
                        onSlotClick = onSlotClick,
                        suppressCursorBars = suppressCursorBars,
                        allowEndCursor = node.innerRange.last < node.end
                    )
                }
            }
        }
        is MathAstNode.PowerNode -> {
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.Bottom
            ) {
                Box {
                    if (node.base.isEmpty()) {
                        PlaceholderSlot(
                            isActive = cursorIndex in node.baseRange,
                            fontSize = fontSize,
                            onClick = { onSlotClick(node.baseRange.first) }
                        )
                    } else {
                        RenderNodeList(
                            nodes = node.base,
                            cursorIndex = cursorIndex,
                            parentRange = node.baseRange,
                            fontSize = fontSize,
                            color = color,
                            onSlotClick = onSlotClick,
                            suppressCursorBars = suppressCursorBars,
                            allowEndCursor = true
                        )
                    }
                }
                Box(modifier = Modifier.padding(bottom = (fontSize.value * 0.40f).dp, start = 1.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RenderNodeList(
                            nodes = node.exponent,
                            cursorIndex = cursorIndex,
                            parentRange = node.expRange,
                            fontSize = fontSize * 0.72f,
                            color = color,
                            onSlotClick = onSlotClick,
                            suppressCursorBars = suppressCursorBars,
                            allowEndCursor = node.expRange.last < node.end
                        )
                    }
                }
            }
        }
        is MathAstNode.AbsNode -> {
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(color, RoundedCornerShape(1.dp))
                )
                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                    RenderNodeList(
                        nodes = node.inner,
                        cursorIndex = cursorIndex,
                        parentRange = node.innerRange,
                        fontSize = fontSize,
                        color = color,
                        onSlotClick = onSlotClick,
                        suppressCursorBars = suppressCursorBars,
                        allowEndCursor = true
                    )
                }
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(color, RoundedCornerShape(1.dp))
                )
            }
        }
        is MathAstNode.ParenNode -> {
            val innerEnd = if (node.hasCloseParen) node.end - 1 else node.end
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("(", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                RenderNodeList(
                    nodes = node.inner,
                    cursorIndex = cursorIndex,
                    parentRange = (node.start + 1)..innerEnd,
                    fontSize = fontSize,
                    color = color,
                    onSlotClick = onSlotClick,
                    suppressCursorBars = suppressCursorBars,
                    allowEndCursor = node.hasCloseParen
                )
                if (node.hasCloseParen) {
                    Text(")", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                }
            }
        }
        is MathAstNode.FunctionNode -> {
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    node.name.endsWith("⁻¹") -> {
                        val baseName = node.name.removeSuffix("⁻¹")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = baseName,
                                color = AccentCyan,
                                fontSize = fontSize,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "-1",
                                color = AccentCyan,
                                fontSize = fontSize * 0.58f,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = (fontSize.value * 0.35f).dp)
                            )
                        }
                    }
                    node.name == "log2" || node.name == "log₂" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "log",
                                color = AccentCyan,
                                fontSize = fontSize,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "2",
                                color = AccentCyan,
                                fontSize = fontSize * 0.62f,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = (fontSize.value * 0.32f).dp)
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = node.name,
                            color = AccentCyan,
                            fontSize = fontSize,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text("(", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                RenderNodeList(
                    nodes = node.arg,
                    cursorIndex = cursorIndex,
                    parentRange = node.argRange,
                    fontSize = fontSize,
                    color = color,
                    onSlotClick = onSlotClick,
                    suppressCursorBars = suppressCursorBars,
                    allowEndCursor = node.argRange.last < node.end
                )
                if (node.argRange.last < node.end) {
                    Text(")", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                }
            }
        }
        is MathAstNode.TemplateNode -> {
            RenderTemplateNode(
                node = node,
                cursorIndex = cursorIndex,
                fontSize = fontSize,
                color = color,
                onSlotClick = onSlotClick,
                suppressCursorBars = suppressCursorBars
            )
        }
        is MathAstNode.Grid2DNode -> {
            RenderGrid2DNode(
                node = node,
                cursorIndex = cursorIndex,
                fontSize = fontSize,
                color = color,
                onSlotClick = onSlotClick,
                suppressCursorBars = suppressCursorBars
            )
        }
        is MathAstNode.EmptyPlaceholder -> {
            PlaceholderSlot(
                isActive = cursorIndex in node.start..node.end,
                fontSize = fontSize,
                onClick = { onSlotClick(node.slotIndex) }
            )
        }
    }
}

@Composable
private fun RenderGrid2DNode(
    node: MathAstNode.Grid2DNode,
    cursorIndex: Int,
    fontSize: TextUnit,
    color: Color,
    onSlotClick: (Int) -> Unit,
    suppressCursorBars: Boolean
) {
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .drawBehind {
                    val stroke = 2.dp.toPx()
                    if (node.isDeterminant) {
                        drawLine(
                            color = color,
                            start = Offset(size.width * 0.5f, 0f),
                            end = Offset(size.width * 0.5f, size.height),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    } else {
                        drawLine(color, Offset(size.width, stroke / 2), Offset(stroke / 2, stroke / 2), stroke)
                        drawLine(color, Offset(stroke / 2, 0f), Offset(stroke / 2, size.height), stroke)
                        drawLine(color, Offset(stroke / 2, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
                    }
                }
        )

        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (r in 0 until node.rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (c in 0 until node.cols) {
                        val cellIdx = r * node.cols + c
                        val (cellNodes, cellRange) = node.cells.getOrElse(cellIdx) {
                            emptyList<MathAstNode>() to (node.end..node.end)
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onSlotClick(cellRange.last)
                                }
                                .padding(horizontal = 2.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RenderNodeList(
                                nodes = cellNodes,
                                cursorIndex = cursorIndex,
                                parentRange = cellRange,
                                fontSize = fontSize * 0.82f,
                                color = color,
                                onSlotClick = onSlotClick,
                                suppressCursorBars = suppressCursorBars,
                                allowEndCursor = true
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .drawBehind {
                    val stroke = 2.dp.toPx()
                    if (node.isDeterminant) {
                        drawLine(
                            color = color,
                            start = Offset(size.width * 0.5f, 0f),
                            end = Offset(size.width * 0.5f, size.height),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    } else {
                        val x = size.width - stroke / 2
                        drawLine(color, Offset(0f, stroke / 2), Offset(x, stroke / 2), stroke)
                        drawLine(color, Offset(x, 0f), Offset(x, size.height), stroke)
                        drawLine(color, Offset(0f, size.height - stroke / 2), Offset(x, size.height - stroke / 2), stroke)
                    }
                }
        )
    }
}

@Composable
private fun RenderTemplateNode(
    node: MathAstNode.TemplateNode,
    cursorIndex: Int,
    fontSize: TextUnit,
    color: Color,
    onSlotClick: (Int) -> Unit,
    suppressCursorBars: Boolean
) {
    @Composable
    fun Slot(idx: Int, scale: Float = 1f) {
        val (sNodes, sRange) = node.slots.getOrElse(idx) { emptyList<MathAstNode>() to (node.end..node.end) }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onSlotClick(sRange.last)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RenderNodeList(
                nodes = sNodes,
                cursorIndex = cursorIndex,
                parentRange = sRange,
                fontSize = fontSize * scale,
                color = color,
                onSlotClick = onSlotClick,
                suppressCursorBars = suppressCursorBars,
                allowEndCursor = true
            )
        }
    }

    when (node.kind) {
        "logb" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("log", color = AccentCyan, fontSize = fontSize, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold)
                Box(modifier = Modifier.padding(top = (fontSize.value * 0.35f).dp, end = 1.dp)) {
                    Slot(0, 0.65f)
                }
                Text("(", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                Slot(1, 1f)
                Text(")", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
            }
        }
        "sub" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slot(0, 1f)
                Box(modifier = Modifier.padding(top = (fontSize.value * 0.36f).dp, start = 1.dp)) {
                    Slot(1, 0.68f)
                }
            }
        }
        "mixed" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slot(0, 1f)
                Spacer(modifier = Modifier.width(2.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Slot(1, 0.80f)
                    Box(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .widthIn(min = 16.dp)
                            .height(1.6.dp)
                            .background(color, RoundedCornerShape(1.dp))
                    )
                    Slot(2, 0.80f)
                }
            }
        }
        "perm", "comb" -> {
            val midChar = if (node.kind == "perm") "P" else "C"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.padding(top = (fontSize.value * 0.32f).dp)) {
                    Slot(0, 0.68f)
                }
                Text(
                    text = midChar,
                    color = AccentCyan,
                    fontSize = fontSize,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 1.dp)
                )
                Box(modifier = Modifier.padding(top = (fontSize.value * 0.32f).dp)) {
                    Slot(1, 0.68f)
                }
            }
        }
        "binom" -> {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min).padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("(", color = color, fontSize = fontSize * 1.35f, fontFamily = JetBrainsMono)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Slot(0, 0.78f)
                    Slot(1, 0.78f)
                }
                Text(")", color = color, fontSize = fontSize * 1.35f, fontFamily = JetBrainsMono)
            }
        }
        "fn1" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slot(0, 1f)
                Text("(", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                Slot(1, 1f)
                Text(")", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
            }
        }
        "fn2" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slot(0, 1f)
                Text("(", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                Slot(1, 1f)
                Text(", ", color = AccentCyan, fontSize = fontSize, fontFamily = JetBrainsMono)
                Slot(2, 1f)
                Text(")", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
            }
        }
        "deg" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slot(0, 1f)
                Text("°", color = AccentCyan, fontSize = fontSize, fontFamily = FontFamily.SansSerif)
            }
        }
        "dms2" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slot(0, 1f)
                Text("°", color = AccentCyan, fontSize = fontSize, fontFamily = FontFamily.SansSerif)
                Spacer(modifier = Modifier.width(2.dp))
                Slot(1, 1f)
                Text("'", color = AccentCyan, fontSize = fontSize, fontFamily = JetBrainsMono)
            }
        }
        "dms3" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slot(0, 1f)
                Text("°", color = AccentCyan, fontSize = fontSize, fontFamily = FontFamily.SansSerif)
                Spacer(modifier = Modifier.width(2.dp))
                Slot(1, 1f)
                Text("'", color = AccentCyan, fontSize = fontSize, fontFamily = JetBrainsMono)
                Spacer(modifier = Modifier.width(2.dp))
                Slot(2, 1f)
                Text("\"", color = AccentCyan, fontSize = fontSize, fontFamily = JetBrainsMono)
            }
        }
        "lim", "lim+", "lim-", "limp", "limm" -> {
            val suffix = when (node.kind) {
                "lim+", "limp" -> "+"
                "lim-", "limm" -> "−"
                else -> ""
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 3.dp)
                ) {
                    Text(
                        text = "lim",
                        color = AccentCyan,
                        fontSize = fontSize * 0.85f,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slot(0, 0.58f)
                        Text(
                            text = "→",
                            color = AccentCyan,
                            fontSize = fontSize * 0.62f,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.padding(horizontal = 1.dp)
                        )
                        Slot(1, 0.58f)
                        if (suffix.isNotEmpty()) {
                            Text(
                                text = suffix,
                                color = AccentCyan,
                                fontSize = fontSize * 0.52f,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                        }
                    }
                }
                Text("(", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                Slot(2, 1f)
                Text(")", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
            }
        }
        "d/dx", "deriv" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(end = 3.dp)
                ) {
                    Text("d", color = AccentCyan, fontSize = fontSize * 0.72f, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.width(18.dp).height(1.5.dp).background(AccentCyan, RoundedCornerShape(1.dp)))
                    Text("dx", color = AccentIndigo, fontSize = fontSize * 0.72f, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                }
                Text("(", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                Slot(0, 1f)
                Text(")", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
            }
        }
        "derivv" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(end = 3.dp)
                ) {
                    Text("d", color = AccentCyan, fontSize = fontSize * 0.72f, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.width(24.dp).height(1.5.dp).background(AccentCyan, RoundedCornerShape(1.dp)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("d", color = AccentCyan, fontSize = fontSize * 0.72f, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                        Slot(0, 0.65f)
                    }
                }
                Text("(", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                Slot(1, 1f)
                Text(")", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
            }
        }
        "derivn" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(end = 3.dp)
                ) {
                    Text("d", color = AccentCyan, fontSize = fontSize * 0.72f, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.width(30.dp).height(1.5.dp).background(AccentCyan, RoundedCornerShape(1.dp)))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("d", color = AccentCyan, fontSize = fontSize * 0.72f, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                        Slot(0, 0.62f)
                        Box(modifier = Modifier.padding(bottom = (fontSize.value * 0.28f).dp)) {
                            Slot(1, 0.52f)
                        }
                    }
                }
                Text("(", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                Slot(2, 1f)
                Text(")", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
            }
        }
        "intx", "integrate" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("∫", color = AccentCyan, fontSize = fontSize * 1.35f, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(end = 2.dp))
                Text("(", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                Slot(0, 1f)
                Text(")", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                Text("dx", color = AccentIndigo, fontSize = fontSize * 0.85f, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, modifier = Modifier.padding(start = 2.dp))
            }
        }
        "intv" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("∫", color = AccentCyan, fontSize = fontSize * 1.35f, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(end = 2.dp))
                Text("(", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                Slot(0, 1f)
                Text(")", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                Text("d", color = AccentCyan, fontSize = fontSize * 0.85f, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, modifier = Modifier.padding(start = 2.dp))
                Slot(1, 0.75f)
            }
        }
        "defint" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(end = 2.dp)
                ) {
                    Slot(1, 0.56f)
                    Text("∫", color = AccentCyan, fontSize = fontSize * 1.30f, fontFamily = FontFamily.SansSerif)
                    Slot(0, 0.56f)
                }
                Text("(", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                Slot(2, 1f)
                Text(")", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                Text("d", color = AccentCyan, fontSize = fontSize * 0.85f, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, modifier = Modifier.padding(start = 2.dp))
                Slot(3, 0.75f)
            }
        }
        "sum" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(end = 3.dp)
                ) {
                    Slot(2, 0.56f)
                    Text("∑", color = AccentCyan, fontSize = fontSize * 1.25f, fontFamily = FontFamily.SansSerif)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slot(0, 0.56f)
                        Text("=", color = AccentCyan, fontSize = fontSize * 0.56f, fontFamily = JetBrainsMono)
                        Slot(1, 0.56f)
                    }
                }
                Text("(", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
                Slot(3, 1f)
                Text(")", color = color, fontSize = fontSize, fontFamily = JetBrainsMono)
            }
        }
        "sys" -> {
            Row(
                modifier = Modifier
                    .padding(horizontal = 2.dp, vertical = 2.dp)
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .fillMaxHeight()
                        .drawBehind {
                            val w = size.width
                            val h = size.height
                            val midY = h * 0.5f
                            val stroke = 2.2.dp.toPx()
                            val path = Path().apply {
                                moveTo(w * 0.92f, stroke)
                                cubicTo(
                                    w * 0.38f, stroke,
                                    w * 0.42f, h * 0.08f,
                                    w * 0.42f, h * 0.38f
                                )
                                cubicTo(
                                    w * 0.42f, h * 0.46f,
                                    w * 0.18f, midY,
                                    w * 0.08f, midY
                                )
                                cubicTo(
                                    w * 0.18f, midY,
                                    w * 0.42f, h * 0.54f,
                                    w * 0.42f, h * 0.62f
                                )
                                cubicTo(
                                    w * 0.42f, h * 0.92f,
                                    w * 0.38f, h - stroke,
                                    w * 0.92f, h - stroke
                                )
                            }
                            drawPath(
                                path = path,
                                color = AccentCyan,
                                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                )
                Column(
                    modifier = Modifier.padding(start = 5.dp, top = 2.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    for (idx in node.slots.indices) {
                        Slot(idx, 0.92f)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderTextToken(
    node: MathAstNode.TextToken,
    cursorIndex: Int,
    fontSize: TextUnit,
    color: Color,
    onSlotClick: (Int) -> Unit,
    suppressCursorBars: Boolean = false
) {
    val text = node.text
    val scheme = MaterialTheme.colorScheme
    val tokenColor = if (node.isVariable) scheme.secondary else if (node.isOperator) scheme.primary else color
    val fontFamily = when {
        node.isVariable -> FontFamily.Serif
        isSupportedInJetBrainsMono(text) -> JetBrainsMono
        else -> FontFamily.SansSerif
    }
    val fontStyle = if (node.isVariable && text != "∞") FontStyle.Italic else FontStyle.Normal
    val fontWeight = if (node.isOperator) FontWeight.Medium else FontWeight.SemiBold
    val opPadding = if (node.isOperator && text != "·" && text != "!" && text != "," && text != "%") 3.dp else 0.dp

    val clickModifier = Modifier
        .padding(horizontal = opPadding)
        .pointerInput(node) {
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                val clickX = down.position.x
                val w = size.width.toFloat().coerceAtLeast(1f)
                val charOffset = if (text.length <= 1) {
                    if (clickX < w * 0.35f) 0 else text.length
                } else {
                    ((clickX / w) * text.length).roundToInt().coerceIn(0, text.length)
                }
                onSlotClick(node.start + charOffset)
                val up = waitForUpOrCancellation()
                up?.consume()
            }
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = clickModifier
    ) {
        if (!suppressCursorBars && cursorIndex in (node.start + 1) until node.end) {
            val splitOffset = (cursorIndex - node.start).coerceIn(0, text.length)
            val left = text.substring(0, splitOffset)
            val right = text.substring(splitOffset)

            Text(
                text = left,
                color = tokenColor,
                fontSize = fontSize,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontStyle = fontStyle
            )
            PulsingCursorBar(fontSize = fontSize)
            Text(
                text = right,
                color = tokenColor,
                fontSize = fontSize,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontStyle = fontStyle
            )
        } else {
            Text(
                text = text,
                color = tokenColor,
                fontSize = fontSize,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontStyle = fontStyle
            )
        }
    }
}

@Composable
fun PulsingCursorBar(fontSize: TextUnit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    val barHeight = fontSize.value.dp * 1.15f

    Box(
        modifier = Modifier
            .padding(horizontal = 0.5.dp)
            .width(2.dp)
            .height(barHeight)
            .alpha(alpha)
            .background(primaryColor, RoundedCornerShape(1.dp))
    )
}

@Composable
fun PlaceholderSlot(
    isActive: Boolean,
    fontSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "slot")
    val glowColor by transition.animateColor(
        initialValue = scheme.primary,
        targetValue = scheme.primary.copy(alpha = 0.35f),
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "slotGlow"
    )

    val slotW = (fontSize.value * 0.85f).dp.coerceAtLeast(16.dp)
    val slotH = (fontSize.value * 0.95f).dp.coerceAtLeast(18.dp)

    Box(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .width(slotW)
            .height(slotH)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isActive) scheme.primary.copy(alpha = 0.14f) else scheme.surfaceVariant.copy(alpha = 0.45f))
            .border(
                BorderStroke(
                    width = if (isActive) 1.8.dp else 1.dp,
                    color = if (isActive) glowColor else scheme.outline
                ),
                RoundedCornerShape(3.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(1.8.dp)
                    .height(slotH * 0.65f)
                    .background(scheme.primary, RoundedCornerShape(1.dp))
            )
        }
    }
}
