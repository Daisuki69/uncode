package com.uncode.app.math.ui.math

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.uncode.app.math.ui.keyboard.SciCalcKey

class MathInputController {

    private val knownFunctions = listOf(
        "integrate(",
        "defint(", "derivn(", "derivv(", "deriv(", "matrix(",
        "arcsin(", "arccos(", "arctan(", "arccot(", "arcsec(", "arccsc(",
        "arsinh(", "arcosh(", "artanh(", "arcoth(", "arsech(", "arcsch(",
        "sin⁻¹(", "cos⁻¹(", "tan⁻¹(", "cot⁻¹(",
        "mixed(", "binom(",
        "sinh(", "cosh(", "tanh(", "coth(", "sech(", "csch(",
        "cbrt(", "root(", "sqrt(", "perm(", "comb(", "conj(",
        "logb(", "dms2(", "dms3(", "intx(", "intv(",
        "lim+(", "lim-(", "limp(", "limm(",
        "asin(", "acos(", "atan(", "acot(",
        "d/dx(", "log2(", "log₂(",
        "sin(", "cos(", "tan(", "cot(", "sec(", "csc(",
        "log(", "exp(", "lim(", "sum(", "mod(", "abs(", "sgn(", "det(", "rad(",
        "sub(", "fn1(", "fn2(", "deg(", "sys(",
        "ln(", "√(", "∛("
    )

    private val structuralPrefixes = knownFunctions + listOf("^(", "/(")

    private val multiCharSymbols = listOf("==", "!=", "<=", ">=")
    private var preGestureSnapshot: TextFieldValue? = null
    private var lastNewLineOrigin: Pair<String, Int>? = null

    fun prepareKeyGesture(current: TextFieldValue) {
        preGestureSnapshot = current
    }

    fun selectAlternate(
        current: TextFieldValue,
        baseKey: SciCalcKey,
        altKey: SciCalcKey,
        onOpenMatrixDialog: (Boolean) -> Unit = {}
    ): TextFieldValue {
        val base = preGestureSnapshot ?: current
        preGestureSnapshot = null
        return onKeyClick(base, altKey, onOpenMatrixDialog)
    }

    fun insertMatrix(
        current: TextFieldValue,
        rows: Int,
        cols: Int,
        isDet: Boolean = false
    ): TextFieldValue {
        val r = rows.coerceIn(1, 5)
        val c = cols.coerceIn(1, 5)
        val fnName = if (isDet) "det" else "matrix"
        val cellSemis = ";".repeat((r * c - 1).coerceAtLeast(0))
        val header = "$fnName($r,$c;"
        val insertText = "$header$cellSemis)"

        val text = current.text
        val start = current.selection.min
        val end = current.selection.max

        val newText = text.substring(0, start) + insertText + text.substring(end)
        val newCursor = (start + header.length).coerceIn(0, newText.length)
        return makeTfv(newText, newCursor)
    }

    fun onKeyClick(
        current: TextFieldValue,
        key: SciCalcKey,
        onOpenMatrixDialog: (Boolean) -> Unit = {}
    ): TextFieldValue {
        if (key.label.isEmpty() && key.insertText.isEmpty()) return current

        if (key.label == "[□]") {
            onOpenMatrixDialog(false)
            return current
        }
        if (key.label == "det") {
            onOpenMatrixDialog(true)
            return current
        }

        val text = current.text
        val selection = current.selection
        val start = selection.min
        val end = selection.max

        when (key.label) {
            "(□)", "(" -> return handleOpenParenInput(text, start, end)
            ")" -> return handleCloseParenInput(current, text, start, end)
            "a/b", "÷", "/" -> return handleFractionInput(text, start, end)
            "x²" -> return handlePowerInput(text, start, end, "^(2)")
            "x³" -> return handlePowerInput(text, start, end, "^(3)")
            "x⁴" -> return handlePowerInput(text, start, end, "^(4)")
            "xⁿ", "^" -> return handleExponentInput(text, start, end)
            "10ⁿ" -> return handleDirectExponentInput(text, start, end, "10^()")
            "2ⁿ" -> return handleDirectExponentInput(text, start, end, "2^()")
            "eⁿ", "eˣ" -> return handleDirectExponentInput(text, start, end, "e^()")
            "√x", "√" -> return handleSquareRootInput(text, start, end, "√()")
            "∛" -> return handleSquareRootInput(text, start, end, "∛()")
            "ⁿ√" -> return handleNthRootInput(text, start, end)
            "|x|" -> return handleAbsoluteValueInput(text, start, end)
        }

        val insertText = key.insertText
        val cursorOffset = key.cursorOffset

        val newText = buildString {
            append(text.substring(0, start))
            append(insertText)
            append(text.substring(end))
        }

        val rawNewCursor = start + insertText.length + cursorOffset
        val newCursor = rawNewCursor.coerceIn(0, newText.length)

        return makeTfv(newText, newCursor)
    }

    private fun handleOpenParenInput(text: String, start: Int, end: Int): TextFieldValue {
        if (start != end) {
            val selected = text.substring(start, end)
            val wrapped = "($selected)"
            val newText = text.substring(0, start) + wrapped + text.substring(end)
            return makeTfv(newText, start + wrapped.length)
        }
        val newText = text.substring(0, start) + "()" + text.substring(start)
        return makeTfv(newText, start + 1)
    }

    private fun handleCloseParenInput(current: TextFieldValue, text: String, start: Int, end: Int): TextFieldValue {
        if (start == end && start < text.length && text[start] == ')') {
            return current.copy(selection = TextRange(start + 1))
        }
        val openCount = text.count { it == '(' }
        val closeCount = text.count { it == ')' }
        if (openCount > closeCount) {
            val newText = text.substring(0, start) + ")" + text.substring(end)
            return makeTfv(newText, start + 1)
        } else if (start < text.length) {
            val closeIdx = text.indexOf(')', start)
            if (closeIdx >= 0) {
                return current.copy(selection = TextRange(closeIdx + 1))
            }
        }
        return current
    }

    private fun handleFractionInput(text: String, start: Int, end: Int): TextFieldValue {
        if (start != end) {
            val selected = text.substring(start, end)
            val wrapped = "($selected)/()"
            val newText = text.substring(0, start) + wrapped + text.substring(end)
            return makeTfv(newText, start + wrapped.length - 1)
        }

        if (start == 0 || isOperatorOrDelimiter(text[start - 1])) {
            val insert = "()/()"
            val newText = text.substring(0, start) + insert + text.substring(start)
            return makeTfv(newText, start + 1)
        }

        val termStart = findPrecedingTermStart(text, start)
        val term = text.substring(termStart, start).trim()
        val wrappedNumerator = if (term.startsWith("(") && term.endsWith(")")) term else "($term)"
        val insert = "$wrappedNumerator/()"
        val newText = text.substring(0, termStart) + insert + text.substring(start)
        return makeTfv(newText, termStart + insert.length - 1)
    }

    private fun handlePowerInput(text: String, start: Int, end: Int, powerStr: String): TextFieldValue {
        if (start != end) {
            val selected = text.substring(start, end)
            val wrapped = "($selected)$powerStr"
            val newText = text.substring(0, start) + wrapped + text.substring(end)
            return makeTfv(newText, start + wrapped.length)
        }

        if (start == 0 || isOperatorOrDelimiter(text[start - 1])) {
            val newText = text.substring(0, start) + powerStr + text.substring(end)
            return makeTfv(newText, start)
        }

        val newText = text.substring(0, start) + powerStr + text.substring(end)
        return makeTfv(newText, start + powerStr.length)
    }

    private fun handleExponentInput(text: String, start: Int, end: Int): TextFieldValue {
        if (start != end) {
            val selected = text.substring(start, end)
            val wrapped = "($selected)^()"
            val newText = text.substring(0, start) + wrapped + text.substring(end)
            return makeTfv(newText, start + wrapped.length - 1)
        }

        if (start == 0 || isOperatorOrDelimiter(text[start - 1])) {
            val insert = "^()"
            val newText = text.substring(0, start) + insert + text.substring(start)
            return makeTfv(newText, start)
        }

        val insert = "^()"
        val newText = text.substring(0, start) + insert + text.substring(start)
        return makeTfv(newText, start + insert.length - 1)
    }

    private fun handleDirectExponentInput(text: String, start: Int, end: Int, pattern: String): TextFieldValue {
        val newText = text.substring(0, start) + pattern + text.substring(end)
        return makeTfv(newText, start + pattern.length - 1)
    }

    private fun handleSquareRootInput(text: String, start: Int, end: Int, template: String): TextFieldValue {
        if (start != end) {
            val selected = text.substring(start, end)
            val fnPrefix = template.substringBeforeLast(")")
            val wrapped = "$fnPrefix$selected)"
            val newText = text.substring(0, start) + wrapped + text.substring(end)
            return makeTfv(newText, start + wrapped.length)
        }

        val newText = text.substring(0, start) + template + text.substring(start)
        return makeTfv(newText, start + template.length - 1)
    }

    private fun handleNthRootInput(text: String, start: Int, end: Int): TextFieldValue {
        if (start != end) {
            val selected = text.substring(start, end)
            val wrapped = "root(;$selected)"
            val newText = text.substring(0, start) + wrapped + text.substring(end)
            return makeTfv(newText, start + 5)
        }
        val insert = "root(;)"
        val newText = text.substring(0, start) + insert + text.substring(start)
        return makeTfv(newText, start + 5)
    }

    private fun handleAbsoluteValueInput(text: String, start: Int, end: Int): TextFieldValue {
        if (start != end) {
            val selected = text.substring(start, end)
            val wrapped = "|$selected|"
            val newText = text.substring(0, start) + wrapped + text.substring(end)
            return makeTfv(newText, start + wrapped.length)
        }

        val insert = "||"
        val newText = text.substring(0, start) + insert + text.substring(start)
        return makeTfv(newText, start + 1)
    }

    private fun findPrecedingTermStart(text: String, end: Int): Int {
        if (end <= 0) return 0
        if (text[end - 1] == ')') {
            val openIdx = findMatchingOpenParen(text, end - 1)
            if (openIdx >= 0) {
                var fnStart = openIdx
                while (fnStart > 0 && (text[fnStart - 1].isLetterOrDigit() || text[fnStart - 1] in "√∛⁻¹₂+-/")) {
                    fnStart--
                }
                return fnStart
            }
            return 0
        }

        var i = end - 1
        while (i >= 0) {
            val c = text[i]
            if (isOperatorOrDelimiter(c)) {
                return i + 1
            }
            i--
        }
        return 0
    }

    private fun isOperatorOrDelimiter(c: Char): Boolean {
        return c == '+' || c == '-' || c == '−' || c == '*' || c == '·' || c == '/' || c == '=' || c == '<' || c == '>' || c == '(' || c == ',' || c == ';' || c == '^' || c == '|' || c == '!' || c == '%'
    }

    fun onNextStep(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val pos = current.selection.min

        if (text.startsWith("sys(") && text.endsWith(")")) {
            val openParenIdx = 3
            val closeParenIdx = text.length - 1
            val slotRanges = mutableListOf<IntRange>()
            var depth = 0
            var slotStart = openParenIdx + 1
            for (i in (openParenIdx + 1) until closeParenIdx) {
                when (text[i]) {
                    '(' -> depth++
                    ')' -> if (depth > 0) depth--
                    ';' -> if (depth == 0) {
                        slotRanges.add(slotStart..i)
                        slotStart = i + 1
                    }
                }
            }
            slotRanges.add(slotStart..closeParenIdx)

            val currSlotIdx = slotRanges.indexOfFirst { pos in it }.let { if (it >= 0) it else slotRanges.lastIndex }
            val currRange = slotRanges[currSlotIdx]
            val insertIdx = currRange.last
            val newText = text.substring(0, insertIdx) + ";" + text.substring(insertIdx)
            lastNewLineOrigin = newText to pos
            return makeTfv(newText, insertIdx + 1)
        }

        val newText = "sys($text;)"
        val newCursor = 4 + text.length + 1
        lastNewLineOrigin = newText to pos
        return makeTfv(newText, newCursor)
    }

    fun onCursorSet(current: TextFieldValue, index: Int): TextFieldValue {
        val clamped = index.coerceIn(0, current.text.length)
        return current.copy(selection = TextRange(clamped))
    }

    private data class EnclosingConstruct(
        val prefix: String,
        val blockStart: Int,
        val openParenIdx: Int,
        val closeParenIdx: Int,
        val isGridHeader: Boolean,
        val firstUserSlotStart: Int
    )

    private fun findEnclosingConstruct(text: String, pos: Int): EnclosingConstruct? {
        var depth = 0
        var i = pos - 1
        while (i >= 0) {
            when (text[i]) {
                ')' -> depth++
                '(' -> {
                    if (depth == 0) {
                        val openIdx = i
                        val closeIdx = findMatchingCloseParen(text, openIdx)
                        if (closeIdx < pos) return null
                        for (prefix in structuralPrefixes) {
                            val pStart = openIdx - (prefix.length - 1)
                            if (pStart >= 0 && text.startsWith(prefix, pStart)) {
                                val isGrid = prefix == "matrix(" || prefix == "det("
                                val firstSemi = if (isGrid) text.indexOf(';', openIdx + 1) else -1
                                val firstSlotStart = if (isGrid && firstSemi in (openIdx + 1) until closeIdx) {
                                    firstSemi + 1
                                } else {
                                    openIdx + 1
                                }
                                return EnclosingConstruct(
                                    prefix = prefix,
                                    blockStart = pStart,
                                    openParenIdx = openIdx,
                                    closeParenIdx = closeIdx,
                                    isGridHeader = isGrid,
                                    firstUserSlotStart = firstSlotStart
                                )
                            }
                        }
                        return null
                    }
                    depth--
                }
            }
            i--
        }
        return null
    }

    private fun splitTopLevelSlots(inner: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var lastStart = 0
        for (i in inner.indices) {
            when (inner[i]) {
                '(' -> depth++
                ')' -> if (depth > 0) depth--
                ';' -> if (depth == 0) {
                    result.add(inner.substring(lastStart, i))
                    lastStart = i + 1
                }
            }
        }
        result.add(inner.substring(lastStart))
        return result
    }

    fun onBackspace(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val selection = current.selection

        if (selection.min != selection.max) {
            val newText = text.removeRange(selection.min, selection.max)
            return makeTfv(newText, selection.min)
        }

        val pos = selection.min

        if (text.startsWith("^()", pos) || text.startsWith("/()", pos)) {
            val newText = text.removeRange(pos, pos + 3)
            return makeTfv(newText, pos)
        }

        if (pos == 0 && (text.startsWith("^(") || text.startsWith("/("))) {
            val closeIdx = findMatchingCloseParen(text, 1)
            val endRemove = if (closeIdx in 1 until text.length) closeIdx + 1 else text.length
            val newText = text.removeRange(0, endRemove)
            return makeTfv(newText, 0)
        }

        if (pos <= 0) return current

        if (pos >= 1 && text.startsWith("()/(", pos - 1)) {
            val denOpen = pos + 2
            val denClose = findMatchingCloseParen(text, denOpen)
            if (denClose == denOpen + 1) {
                val newText = text.removeRange(pos - 1, denClose + 1)
                return makeTfv(newText, pos - 1)
            } else if (denClose > denOpen + 1 && denClose < text.length) {
                val withoutClose = text.removeRange(denClose, denClose + 1)
                val newText = withoutClose.removeRange(pos - 1, denOpen + 1)
                return makeTfv(newText, pos - 1)
            }
        }

        val enclosing = findEnclosingConstruct(text, pos)
        if (enclosing != null && enclosing.closeParenIdx <= text.length) {
            val innerStart = enclosing.firstUserSlotStart
            val innerEnd = enclosing.closeParenIdx
            if (innerStart <= innerEnd) {
                val userInner = text.substring(innerStart, innerEnd)
                val slots = splitTopLevelSlots(userInner)
                val isMultiSlot = slots.size > 1 || enclosing.isGridHeader
                if (isMultiSlot) {
                    val atSlotStart = pos == enclosing.firstUserSlotStart || text[pos - 1] == ';'
                    if (atSlotStart) {
                        if (enclosing.prefix == "sys(") {
                            val nonEmptySlots = slots.filter { it.isNotEmpty() }
                            if (nonEmptySlots.isEmpty()) {
                                if (slots.size > 2 && pos > innerStart && text[pos - 1] == ';') {
                                    val newText = text.removeRange(pos - 1, pos)
                                    return makeTfv(newText, pos - 1)
                                }
                                val removeEnd = (enclosing.closeParenIdx + 1).coerceAtMost(text.length)
                                val newText = text.removeRange(enclosing.blockStart, removeEnd)
                                return makeTfv(newText, enclosing.blockStart)
                            }
                            if (pos > innerStart && text[pos - 1] == ';') {
                                val atSlotEnd = pos == innerEnd || text[pos] == ';'
                                if (atSlotEnd) {
                                    val originCursor = if (lastNewLineOrigin?.first == text) lastNewLineOrigin?.second else null
                                    lastNewLineOrigin = null
                                    val withoutSemi = text.removeRange(pos - 1, pos)
                                    val newClose = enclosing.closeParenIdx - 1
                                    val newInner = withoutSemi.substring(innerStart, newClose)
                                    val remSlots = splitTopLevelSlots(newInner)
                                    return if (remSlots.size <= 1) {
                                        val unwrapped = withoutSemi.substring(0, enclosing.blockStart) +
                                            newInner +
                                            withoutSemi.substring((newClose + 1).coerceAtMost(withoutSemi.length))
                                        val targetCursor = originCursor?.coerceIn(0, unwrapped.length)
                                            ?: (enclosing.blockStart + newInner.length).coerceIn(0, unwrapped.length)
                                        makeTfv(unwrapped, targetCursor)
                                    } else {
                                        val targetCursor = originCursor?.coerceIn(0, withoutSemi.length) ?: (pos - 1)
                                        makeTfv(withoutSemi, targetCursor)
                                    }
                                } else {
                                    return current.copy(selection = TextRange(pos - 1))
                                }
                            }
                            if (pos == innerStart && slots.first().isEmpty() && slots.size > 1) {
                                val withoutFirst = text.removeRange(innerStart, innerStart + 1)
                                val newClose = enclosing.closeParenIdx - 1
                                val newInner = withoutFirst.substring(innerStart, newClose)
                                val remSlots = splitTopLevelSlots(newInner)
                                return if (remSlots.size <= 1) {
                                    val unwrapped = withoutFirst.substring(0, enclosing.blockStart) +
                                        newInner +
                                        withoutFirst.substring((newClose + 1).coerceAtMost(withoutFirst.length))
                                    makeTfv(unwrapped, enclosing.blockStart)
                                } else {
                                    makeTfv(withoutFirst, innerStart)
                                }
                            }
                        }
                        val nonEmptySlots = slots.filter { it.isNotEmpty() }
                        if (nonEmptySlots.isEmpty()) {
                            val removeEnd = (enclosing.closeParenIdx + 1).coerceAtMost(text.length)
                            val newText = text.removeRange(enclosing.blockStart, removeEnd)
                            return makeTfv(newText, enclosing.blockStart)
                        } else if (pos == enclosing.firstUserSlotStart && nonEmptySlots.size == 1 && !enclosing.isGridHeader) {
                            val kept = nonEmptySlots.first()
                            val removeEnd = (enclosing.closeParenIdx + 1).coerceAtMost(text.length)
                            val newText = text.substring(0, enclosing.blockStart) + kept + text.substring(removeEnd)
                            return makeTfv(newText, enclosing.blockStart)
                        } else if (pos > enclosing.firstUserSlotStart && text[pos - 1] == ';') {
                            return current.copy(selection = TextRange(pos - 1))
                        } else if (pos == enclosing.firstUserSlotStart) {
                            val removeEnd = (enclosing.closeParenIdx + 1).coerceAtMost(text.length)
                            val newText = text.removeRange(enclosing.blockStart, removeEnd)
                            return makeTfv(newText, enclosing.blockStart)
                        }
                    }
                }
            }
        }

        for (prefix in structuralPrefixes) {
            val pLen = prefix.length
            val emptyBlock = "$prefix)"
            val emptyLen = emptyBlock.length

            val insideEmpty = pos >= pLen && pos < text.length && text.startsWith(emptyBlock, pos - pLen)
            val afterEmpty = pos >= emptyLen && text.startsWith(emptyBlock, pos - emptyLen)

            if (insideEmpty || afterEmpty) {
                val blockStart = if (insideEmpty) pos - pLen else pos - emptyLen
                val blockEnd = blockStart + emptyLen
                var newText = text.removeRange(blockStart, blockEnd)
                var newCursor = blockStart
                if (prefix == "^(" || prefix == "/(") {
                    val unwrapped = unwrapPrecedingSingleGroup(newText, newCursor)
                    newText = unwrapped.first
                    newCursor = unwrapped.second
                }
                return makeTfv(newText, newCursor)
            }
        }

        for (prefix in structuralPrefixes) {
            val pLen = prefix.length
            for (offset in 1..pLen) {
                val pStart = pos - offset
                if (pStart >= 0 && text.startsWith(prefix, pStart)) {
                    val openParenIdx = pStart + pLen - 1
                    val closeParenIdx = findMatchingCloseParen(text, openParenIdx)
                    val innerContent = if (closeParenIdx in (openParenIdx + 1) until text.length) {
                        text.substring(openParenIdx + 1, closeParenIdx)
                    } else ""
                    if (innerContent.contains(';')) {
                        val endRemove = if (closeParenIdx in (openParenIdx + 1) until text.length) closeParenIdx + 1 else text.length
                        val newText = text.removeRange(pStart, endRemove)
                        return makeTfv(newText, pStart)
                    }
                    var newText = if (closeParenIdx in (openParenIdx + 1) until text.length && text[closeParenIdx] == ')') {
                        text.removeRange(closeParenIdx, closeParenIdx + 1)
                    } else {
                        text
                    }
                    newText = newText.removeRange(pStart, openParenIdx + 1)
                    var newCursor = pStart
                    if (prefix == "^(" || prefix == "/(") {
                        val unwrapped = unwrapPrecedingSingleGroup(newText, newCursor)
                        newText = unwrapped.first
                        newCursor = unwrapped.second
                    }
                    return makeTfv(newText, newCursor)
                }
            }
        }

        if (text[pos - 1] == ')') {
            val closeIdx = pos - 1
            val openIdx = findMatchingOpenParen(text, closeIdx)
            if (openIdx >= 0) {
                val matchedPrefix = structuralPrefixes.firstOrNull { prefix ->
                    val pStart = openIdx - (prefix.length - 1)
                    pStart >= 0 && text.startsWith(prefix, pStart)
                }
                if (matchedPrefix != null) {
                    val pStart = openIdx - (matchedPrefix.length - 1)
                    val inner = text.substring(openIdx + 1, closeIdx)
                    val isGrid = matchedPrefix == "matrix(" || matchedPrefix == "det("
                    val userInner = if (isGrid && inner.contains(';')) inner.substringAfter(';') else inner
                    val allSlotsEmpty = userInner.all { it == ';' }
                    if (allSlotsEmpty) {
                        val newText = text.removeRange(pStart, closeIdx + 1)
                        return makeTfv(newText, pStart)
                    }
                    if (closeIdx > openIdx + 1) {
                        val innerDelPos = closeIdx - 1
                        if (text[innerDelPos] == ';') {
                            return current.copy(selection = TextRange(innerDelPos))
                        }
                        val newText = text.removeRange(innerDelPos, closeIdx)
                        return makeTfv(newText, innerDelPos)
                    }
                } else {
                    return if (closeIdx == openIdx + 1) {
                        val newText = text.removeRange(openIdx, closeIdx + 1)
                        makeTfv(newText, openIdx)
                    } else {
                        val newText = text.removeRange(closeIdx, closeIdx + 1)
                        makeTfv(newText, closeIdx)
                    }
                }
            }
        }

        if (text[pos - 1] == '(') {
            val openIdx = pos - 1
            val newText = if (pos < text.length && text[pos] == ')') {
                text.removeRange(openIdx, pos + 1)
            } else {
                text.removeRange(openIdx, openIdx + 1)
            }
            return makeTfv(newText, openIdx)
        }

        if (text[pos - 1] == '|') {
            val barIdx = pos - 1
            if (pos < text.length && text[pos] == '|') {
                val newText = text.removeRange(barIdx, pos + 1)
                return makeTfv(newText, barIdx)
            }
            val prevBar = text.lastIndexOf('|', barIdx - 1)
            val nextBar = text.indexOf('|', pos)
            if (prevBar >= 0) {
                val withoutSecond = text.removeRange(barIdx, barIdx + 1)
                val newText = withoutSecond.removeRange(prevBar, prevBar + 1)
                return makeTfv(newText, (pos - 2).coerceAtLeast(0))
            } else if (nextBar >= 0) {
                val withoutSecond = text.removeRange(nextBar, nextBar + 1)
                val newText = withoutSecond.removeRange(barIdx, barIdx + 1)
                return makeTfv(newText, barIdx)
            }
        }

        for (sym in multiCharSymbols) {
            if (pos >= sym.length && text.startsWith(sym, pos - sym.length)) {
                val startDel = pos - sym.length
                val newText = text.removeRange(startDel, pos)
                return makeTfv(newText, startDel)
            }
        }

        val removeIdx = pos - 1
        val newText = text.removeRange(removeIdx, removeIdx + 1)
        return makeTfv(newText, removeIdx)
    }

    fun onCursorLeft(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val pos = current.selection.min
        if (pos <= 0) return current

        val enclosing = findEnclosingConstruct(text, pos)
        if (enclosing != null && enclosing.isGridHeader && pos == enclosing.firstUserSlotStart) {
            return current.copy(selection = TextRange(enclosing.blockStart))
        }

        if (pos >= 3 && text.startsWith(")/(", pos - 3)) {
            return current.copy(selection = TextRange((pos - 3).coerceAtLeast(0)))
        }

        if (pos >= 3 && text.startsWith(")^(", pos - 3)) {
            return current.copy(selection = TextRange((pos - 3).coerceAtLeast(0)))
        }

        if (pos >= 2 && text.startsWith("/(", pos - 2)) {
            return current.copy(selection = TextRange((pos - 2).coerceAtLeast(0)))
        }

        if (pos >= 2 && text.startsWith("^(", pos - 2)) {
            return current.copy(selection = TextRange((pos - 2).coerceAtLeast(0)))
        }

        for (fn in knownFunctions) {
            val fnLen = fn.length
            if (pos in 1..text.length) {
                for (offset in 1..fnLen) {
                    val candidateStart = pos - offset
                    if (candidateStart >= 0 && text.startsWith(fn, candidateStart)) {
                        return current.copy(selection = TextRange(candidateStart))
                    }
                }
            }
        }

        for (sym in multiCharSymbols) {
            if (pos >= sym.length && text.startsWith(sym, pos - sym.length)) {
                return current.copy(selection = TextRange(pos - sym.length))
            }
        }

        return current.copy(selection = TextRange((pos - 1).coerceAtLeast(0)))
    }

    fun onCursorRight(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val pos = current.selection.max
        if (pos >= text.length) return current

        if (text.startsWith(")/(", pos)) {
            return current.copy(selection = TextRange((pos + 3).coerceAtMost(text.length)))
        }

        if (text.startsWith(")^(", pos)) {
            return current.copy(selection = TextRange((pos + 3).coerceAtMost(text.length)))
        }

        if (text.startsWith("/(", pos)) {
            return current.copy(selection = TextRange((pos + 2).coerceAtMost(text.length)))
        }

        if (text.startsWith("^(", pos)) {
            return current.copy(selection = TextRange((pos + 2).coerceAtMost(text.length)))
        }

        for (fn in knownFunctions) {
            if (text.startsWith(fn, pos)) {
                var target = (pos + fn.length).coerceAtMost(text.length)
                if (fn == "matrix(" || fn == "det(") {
                    val firstSemi = text.indexOf(';', target)
                    if (firstSemi >= target) {
                        target = (firstSemi + 1).coerceAtMost(text.length)
                    }
                }
                return current.copy(selection = TextRange(target))
            }
        }

        for (sym in multiCharSymbols) {
            if (text.startsWith(sym, pos)) {
                return current.copy(selection = TextRange((pos + sym.length).coerceAtMost(text.length)))
            }
        }

        if (text[pos] == ')' || text[pos] == '|' || text[pos] == ';') {
            return current.copy(selection = TextRange((pos + 1).coerceAtMost(text.length)))
        }

        return current.copy(selection = TextRange((pos + 1).coerceAtMost(text.length)))
    }

    private fun unwrapPrecedingSingleGroup(text: String, endIdx: Int): Pair<String, Int> {
        if (endIdx < 2 || endIdx > text.length || text[endIdx - 1] != ')') {
            return text to endIdx
        }
        val closeIdx = endIdx - 1
        val openIdx = findMatchingOpenParen(text, closeIdx)
        if (openIdx < 0) return text to endIdx
        if (openIdx > 0 && (text[openIdx - 1].isLetter() || text[openIdx - 1] in "√∛⁻¹₂+-")) {
            return text to endIdx
        }
        val inner = text.substring(openIdx + 1, closeIdx)
        val hasTopLevelBinaryOp = hasTopLevelOperator(inner)
        if (!hasTopLevelBinaryOp) {
            val withoutClose = text.removeRange(closeIdx, closeIdx + 1)
            val withoutOpen = withoutClose.removeRange(openIdx, openIdx + 1)
            return withoutOpen to (endIdx - 2).coerceAtLeast(0)
        }
        return text to endIdx
    }

    private fun hasTopLevelOperator(expr: String): Boolean {
        var depth = 0
        for (i in expr.indices) {
            val c = expr[i]
            when (c) {
                '(' -> depth++
                ')' -> depth--
                '+', '*', '/', '=', '<', '>', ';' -> if (depth == 0) return true
                '-', '−' -> if (depth == 0 && i > 0) return true
            }
        }
        return false
    }

    private fun findMatchingCloseParen(text: String, openIdx: Int): Int {
        if (openIdx < 0 || openIdx >= text.length || text[openIdx] != '(') return -1
        var depth = 1
        var i = openIdx + 1
        while (i < text.length) {
            when (text[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        return text.length
    }

    private fun findMatchingOpenParen(text: String, closeIdx: Int): Int {
        if (closeIdx <= 0 || closeIdx >= text.length || text[closeIdx] != ')') return -1
        var depth = 1
        var i = closeIdx - 1
        while (i >= 0) {
            when (text[i]) {
                ')' -> depth++
                '(' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
            i--
        }
        return -1
    }

    private fun makeTfv(newText: String, newCursor: Int): TextFieldValue {
        val clamped = newCursor.coerceIn(0, newText.length)
        return TextFieldValue(newText, selection = TextRange(clamped))
    }
}
