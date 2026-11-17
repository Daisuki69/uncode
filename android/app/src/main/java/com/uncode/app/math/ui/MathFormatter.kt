package com.uncode.app.math.ui

object MathFormatter {
    private val greekToAscii = listOf(
        "α" to "alpha",
        "β" to "beta",
        "γ" to "gamma",
        "δ" to "delta",
        "ε" to "epsilon",
        "θ" to "theta",
        "λ" to "lambda",
        "μ" to "mu",
        "π" to "pi",
        "ρ" to "rho",
        "σ" to "sigma",
        "τ" to "tau",
        "φ" to "phi",
        "ω" to "omega",
        "Δ" to "Delta"
    )

    private val greekNames = setOf(
        "alpha", "beta", "gamma", "delta", "epsilon",
        "theta", "lambda", "mu", "rho", "sigma",
        "tau", "phi", "omega", "Delta"
    )

    private val knownFunctions = listOf(
        "integrate", "defint", "derivn", "derivv", "deriv",
        "arcsin", "arccos", "arctan", "arccot", "arcsec", "arccsc",
        "arsinh", "arcosh", "artanh", "arcoth", "arsech", "arcsch",
        "matrix", "mixed", "binom",
        "asin", "acos", "atan", "acot",
        "sinh", "cosh", "tanh", "coth", "sech", "csch",
        "intx", "intv", "limp", "limm", "logb", "dms2", "dms3",
        "sin", "cos", "tan",
        "sec", "csc", "cot",
        "sqrt", "cbrt", "root", "perm", "comb", "conj",
        "log2", "log", "ln", "lim", "sum",
        "abs", "exp", "mod", "sgn", "det", "rad", "deg", "sub", "fn1", "fn2", "sys"
    )

    private val semicolonFunctions = setOf(
        "root", "logb", "sub", "mixed", "perm", "comb", "binom",
        "fn1", "fn2", "dms2", "dms3",
        "lim", "lim+", "lim-", "limp", "limm",
        "derivv", "derivn", "intv", "defint", "sum",
        "matrix", "det", "sys"
    )

    fun toDisplayString(rawFormula: String): String {
        var result = rawFormula
            .replace("limp(", "lim+(")
            .replace("limm(", "lim-(")
            .replace("deriv(", "d/dx(")
            .replace("==", " = ")
            .replace("<=", " ≤ ")
            .replace(">=", " ≥ ")
            .replace("!=", " ≠ ")
            .replace(" * ", " · ")
            .replace("*", " · ")
            .replace("oo", "∞")
            .replace("arcsin", "sin⁻¹")
            .replace("arccos", "cos⁻¹")
            .replace("arctan", "tan⁻¹")
            .replace("arccot", "cot⁻¹")

        result = restoreMultiSlotSemicolons(result)

        result = result.replace(Regex("""\^\(?([0-9nxy])\)?(?![a-zA-Z0-9.])""")) { match ->
            when (match.groupValues[1]) {
                "0" -> "⁰"
                "1" -> "¹"
                "2" -> "²"
                "3" -> "³"
                "4" -> "⁴"
                "5" -> "⁵"
                "6" -> "⁶"
                "7" -> "⁷"
                "8" -> "⁸"
                "9" -> "⁹"
                "n" -> "ⁿ"
                "x" -> "ˣ"
                "y" -> "ʸ"
                else -> match.value
            }
        }

        for ((unicode, ascii) in greekToAscii) {
            result = result.replace(Regex("""\b$ascii\b"""), unicode)
        }

        result = result.replace(Regex("""cbrt\(([^)]*)\)"""), "∛($1)")
        result = result.replace(Regex("""sqrt\(([^)]+)\)"""), "√($1)")
        result = result.replace(Regex("""abs\(([^)]+)\)"""), "|$1|")
        result = result.replace(Regex("""\s*/\s*"""), "/")
        result = result.replace(Regex("""\s*;\s*"""), ";")
        result = result.replace(Regex("""\s+"""), " ").trim()
        return result
    }

    private fun restoreMultiSlotSemicolons(input: String): String {
        val sb = StringBuilder()
        var i = 0
        val n = input.length
        while (i < n) {
            val matchedFn = semicolonFunctions.firstOrNull { fn ->
                input.startsWith(fn, i) &&
                    i + fn.length < n &&
                    input[i + fn.length] == '(' &&
                    (i == 0 || !input[i - 1].isLetterOrDigit())
            }
            if (matchedFn != null) {
                val openIdx = i + matchedFn.length
                val closeIdx = findCloseParen(input, openIdx)
                val innerRaw = input.substring(openIdx + 1, closeIdx)
                val isGrid = matchedFn == "matrix" || matchedFn == "det"
                val convertedInner = convertTopLevelCommasToSemicolons(innerRaw, isGrid)
                sb.append(matchedFn).append('(').append(convertedInner)
                if (closeIdx < n) {
                    sb.append(')')
                    i = closeIdx + 1
                } else {
                    i = n
                }
            } else {
                sb.append(input[i])
                i++
            }
        }
        return sb.toString()
    }

    private fun convertTopLevelCommasToSemicolons(inner: String, keepFirstComma: Boolean): String {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        for (idx in inner.indices) {
            when (inner[idx]) {
                '(' -> depth++
                ')' -> if (depth > 0) depth--
                ',', ';' -> if (depth == 0) {
                    parts.add(restoreMultiSlotSemicolons(inner.substring(start, idx).trim()))
                    start = idx + 1
                }
            }
        }
        parts.add(restoreMultiSlotSemicolons(inner.substring(start).trim()))
        if (parts.size <= 1) return parts.firstOrNull() ?: ""
        return if (keepFirstComma && parts.size >= 2) {
            val header = "${parts[0]},${parts[1]}"
            val cells = parts.drop(2).joinToString(";")
            if (cells.isEmpty()) header else "$header;$cells"
        } else {
            parts.joinToString(";")
        }
    }

    private fun findCloseParen(s: String, openIdx: Int): Int {
        var depth = 1
        var i = openIdx + 1
        while (i < s.length) {
            when (s[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        return s.length
    }

    fun toParseableString(displayFormula: String): String {
        var result = displayFormula
            .replace("lim+(", "limp(")
            .replace("lim-(", "limm(")
            .replace("d/dx(", "deriv(")
            .replace("y'", "dy")
            .replace(";", ", ")
            .replace("·", "*")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("≤", "<=")
            .replace("≥", ">=")
            .replace("≠", "!=")
            .replace("²", "^2")
            .replace("³", "^3")
            .replace("⁴", "^4")
            .replace("⁵", "^5")
            .replace("⁶", "^6")
            .replace("⁷", "^7")
            .replace("⁸", "^8")
            .replace("⁹", "^9")
            .replace("⁰", "^0")
            .replace("¹", "^1")
            .replace("ⁿ", "^n")
            .replace("ˣ", "^x")
            .replace("ʸ", "^y")
            .replace("∞", "oo")
            .replace("sin⁻¹", "arcsin")
            .replace("cos⁻¹", "arccos")
            .replace("tan⁻¹", "arctan")
            .replace("cot⁻¹", "arccot")
            .replace("∛", "cbrt")

        result = result.replace(Regex("""(?<![!=<>])=(?!=)"""), "==")
        result = result.replace(Regex("""√\(([^)]*)\)"""), "sqrt($1)")
        result = result.replace(Regex("""√([a-zA-Z0-9]+)"""), "sqrt($1)")
        result = result.replace(Regex("""\|([^|]+)\|"""), "abs($1)")

        result = insertImplicitMultiplication(result)

        for ((unicode, ascii) in greekToAscii) {
            result = result.replace(unicode, ascii)
        }

        return result
    }

    private fun insertImplicitMultiplication(expr: String): String {
        val tokens = tokenize(expr)
        if (tokens.isEmpty()) return expr

        val sb = StringBuilder()
        var i = 0
        while (i < tokens.size) {
            val curr = tokens[i]
            sb.append(curr.text)

            var nextNonWsIdx = i + 1
            while (nextNonWsIdx < tokens.size && tokens[nextNonWsIdx] is Token.Whitespace) {
                nextNonWsIdx++
            }

            if (curr !is Token.Whitespace && nextNonWsIdx < tokens.size) {
                val next = tokens[nextNonWsIdx]
                if (shouldMultiply(curr, next)) {
                    val hasWhitespaceBetween = nextNonWsIdx > i + 1
                    if (hasWhitespaceBetween) {
                        sb.append(" * ")
                        i = nextNonWsIdx - 1
                    } else {
                        sb.append(" * ")
                    }
                }
            }
            i++
        }
        return sb.toString()
    }

    private sealed class Token(val text: String) {
        class Number(text: String) : Token(text)
        class Identifier(text: String, val isFunction: Boolean) : Token(text)
        class Operator(text: String) : Token(text)
        class OpenParen(text: String) : Token(text)
        class CloseParen(text: String) : Token(text)
        class Pipe(text: String) : Token(text)
        class Whitespace(text: String) : Token(text)
    }

    private fun tokenize(expr: String): List<Token> {
        val list = mutableListOf<Token>()
        var i = 0
        val n = expr.length

        while (i < n) {
            val c = expr[i]
            if (c.isWhitespace()) {
                val start = i
                while (i < n && expr[i].isWhitespace()) {
                    i++
                }
                list.add(Token.Whitespace(expr.substring(start, i)))
                continue
            }

            if (c.isDigit()) {
                val start = i
                while (i < n && (expr[i].isDigit() || expr[i] == '.')) {
                    i++
                }
                list.add(Token.Number(expr.substring(start, i)))
                continue
            }

            var matchedFn: String? = null
            for (fn in knownFunctions) {
                if (expr.startsWith(fn, i)) {
                    val nextIdx = i + fn.length
                    if (nextIdx >= n || !expr[nextIdx].isLetter()) {
                        matchedFn = fn
                        break
                    }
                }
            }

            if (matchedFn != null) {
                list.add(Token.Identifier(matchedFn, isFunction = true))
                i += matchedFn.length
                continue
            }

            var matchedGreek: String? = null
            for (g in greekNames) {
                if (expr.startsWith(g, i)) {
                    val nextIdx = i + g.length
                    if (nextIdx >= n || !expr[nextIdx].isLetter()) {
                        matchedGreek = g
                        break
                    }
                }
            }

            if (matchedGreek != null) {
                list.add(Token.Identifier(matchedGreek, isFunction = false))
                i += matchedGreek.length
                continue
            }

            if (expr.startsWith("pi", i)) {
                val nextIdx = i + 2
                if (nextIdx >= n || !expr[nextIdx].isLetter()) {
                    list.add(Token.Identifier("pi", isFunction = false))
                    i += 2
                    continue
                }
            }

            if (expr.startsWith("oo", i)) {
                val nextIdx = i + 2
                if (nextIdx >= n || !expr[nextIdx].isLetter()) {
                    list.add(Token.Identifier("oo", isFunction = false))
                    i += 2
                    continue
                }
            }

            if (expr.startsWith("dx", i) || expr.startsWith("dy", i)) {
                val nextIdx = i + 2
                if (nextIdx >= n || !expr[nextIdx].isLetter()) {
                    list.add(Token.Identifier(expr.substring(i, i + 2), isFunction = false))
                    i += 2
                    continue
                }
            }

            if (c.isLetter() || isGreek(c)) {
                list.add(Token.Identifier(c.toString(), isFunction = false))
                i++
                continue
            }

            if (expr.startsWith("==", i) || expr.startsWith("!=", i) || expr.startsWith("<=", i) || expr.startsWith(">=", i)) {
                list.add(Token.Operator(expr.substring(i, i + 2)))
                i += 2
                continue
            }

            when (c) {
                '(' -> {
                    list.add(Token.OpenParen("("))
                    i++
                }
                ')' -> {
                    list.add(Token.CloseParen(")"))
                    i++
                }
                '|' -> {
                    list.add(Token.Pipe("|"))
                    i++
                }
                '+', '-', '*', '/', '^', '<', '>', '=', ',' -> {
                    list.add(Token.Operator(c.toString()))
                    i++
                }
                else -> {
                    list.add(Token.Operator(c.toString()))
                    i++
                }
            }
        }
        return list
    }

    private fun isGreek(c: Char): Boolean =
        c in "αβγδεθλμπρστφωΔ"

    private fun shouldMultiply(left: Token, right: Token): Boolean {
        val leftCanEndFactor = when (left) {
            is Token.Number -> true
            is Token.Identifier -> !left.isFunction
            is Token.CloseParen -> true
            is Token.Pipe -> true
            else -> false
        }

        val rightCanStartFactor = when (right) {
            is Token.Number -> left is Token.CloseParen || left is Token.Pipe || (left is Token.Identifier && !left.isFunction)
            is Token.Identifier -> true
            is Token.OpenParen -> true
            is Token.Pipe -> true
            else -> false
        }

        return leftCanEndFactor && rightCanStartFactor
    }
}
