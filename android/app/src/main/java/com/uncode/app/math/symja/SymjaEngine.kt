package com.uncode.app.math.symja

import org.matheclipse.parser.client.Parser
import org.matheclipse.parser.client.SyntaxError
import org.matheclipse.parser.client.ast.ASTNode
import org.matheclipse.parser.client.ast.FloatNode
import org.matheclipse.parser.client.ast.FractionNode
import org.matheclipse.parser.client.ast.FunctionNode
import org.matheclipse.parser.client.ast.IntegerNode
import org.matheclipse.parser.client.ast.SymbolNode
import org.matheclipse.parser.client.eval.DoubleEvaluator
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh
import kotlin.random.Random

data class MutationRecord(
    val original: String,
    val mutated: String,
    val type: MutationType
)

enum class MutationType {
    VARIABLE,
    CONSTANT,
    OPERATOR
}

data class MutationResult(
    val mutatedFormula: String,
    val records: List<MutationRecord>,
    val variableValues: Map<String, Double> = emptyMap()
)

data class SymjaValidation(
    val isValid: Boolean,
    val astNode: ASTNode?,
    val variables: List<String>,
    val constants: List<String>,
    val operators: List<String>,
    val errorMessage: String? = null,
    val errorPosition: Int = 0
)

class SymjaEngine(
    private val random: Random = Random.Default
) {
    private val unknownVariablePool = listOf("x", "y", "z")
    private val preferredUnknowns = listOf("x", "y", "z", "t", "u", "v", "w", "n", "m", "k", "theta", "alpha", "beta", "lambda", "phi", "a", "b", "c", "p", "q")
    private val exponentPool = listOf("2", "3", "4")
    private val distinctConstPool = listOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 15, 16, 18)
    private val distinctValuePool = listOf(2, 3, 4, 5, -2, -3, 6, 7, -1, 8, 9, -4, -5, 10, 11, 12)
    private val safeExponentValues = listOf(2, 3, 4)

    fun warmup() {
        try {
            val parser = Parser(true)
            val node = parser.parse("2 * x + 1 == 5")
            val evaluator = DoubleEvaluator()
            evaluator.evaluateNodeLogical(parser.parse("1 + 1 == 2"))
            validate("x + 1 = 2")
            if (node != null) {
                evaluateAnswer("x + 1 == 2")
            }
        } catch (_: Throwable) {
        }
    }

    fun validate(formula: String): SymjaValidation {
        val trimmed = formula.trim()
        if (trimmed.isEmpty()) {
            return SymjaValidation(
                isValid = false,
                astNode = null,
                variables = emptyList(),
                constants = emptyList(),
                operators = emptyList()
            )
        }

        return try {
            val normalized = com.uncode.app.math.ui.MathFormatter.toParseableString(trimmed)
            val parser = Parser(true)
            val root = parser.parse(normalized)
            val detectedVars = LinkedHashSet<String>()
            val detectedConsts = LinkedHashSet<String>()
            val detectedOps = LinkedHashSet<String>()

            collectTokens(root, detectedVars, detectedConsts, detectedOps, emptySet())

            SymjaValidation(
                isValid = true,
                astNode = root,
                variables = detectedVars.toList(),
                constants = detectedConsts.toList(),
                operators = detectedOps.toList()
            )
        } catch (e: SyntaxError) {
            SymjaValidation(
                isValid = false,
                astNode = null,
                variables = emptyList(),
                constants = emptyList(),
                operators = emptyList(),
                errorMessage = e.message ?: "Syntax error",
                errorPosition = e.startOffset
            )
        } catch (e: Throwable) {
            SymjaValidation(
                isValid = false,
                astNode = null,
                variables = emptyList(),
                constants = emptyList(),
                operators = emptyList(),
                errorMessage = e.message ?: "Invalid formula",
                errorPosition = 0
            )
        }
    }

    private fun collectTokens(
        node: ASTNode,
        vars: MutableSet<String>,
        consts: MutableSet<String>,
        ops: MutableSet<String>,
        boundVars: Set<String>
    ) {
        when (node) {
            is FunctionNode -> {
                if (node.isNotEmpty()) {
                    val head = node[0].toString()
                    val headLower = head.lowercase()
                    val mappedOp = mapHeadToOperator(head)
                    if (mappedOp != null) {
                        ops.add(mappedOp)
                    }
                    when {
                        headLower in setOf("lim", "limp", "limm") && node.size >= 4 -> {
                            val bVar = node[1].toString()
                            collectTokens(node[2], vars, consts, ops, boundVars)
                            collectTokens(node[3], vars, consts, ops, boundVars + bVar)
                        }
                        headLower == "sum" && node.size >= 5 -> {
                            val bVar = node[1].toString()
                            collectTokens(node[2], vars, consts, ops, boundVars)
                            collectTokens(node[3], vars, consts, ops, boundVars)
                            collectTokens(node[4], vars, consts, ops, boundVars + bVar)
                        }
                        headLower == "defint" && node.size >= 5 -> {
                            val bVar = node[4].toString()
                            collectTokens(node[1], vars, consts, ops, boundVars)
                            collectTokens(node[2], vars, consts, ops, boundVars)
                            collectTokens(node[3], vars, consts, ops, boundVars + bVar)
                        }
                        (headLower == "matrix" || headLower == "det") && node.size >= 3 -> {
                            for (i in 3 until node.size) {
                                collectTokens(node[i], vars, consts, ops, boundVars)
                            }
                        }
                        else -> {
                            for (i in 1 until node.size) {
                                collectTokens(node[i], vars, consts, ops, boundVars)
                            }
                        }
                    }
                }
            }
            is IntegerNode -> consts.add(node.toString())
            is FloatNode -> consts.add(node.toString())
            is FractionNode -> consts.add(node.toString())
            is SymbolNode -> {
                val name = node.toString()
                if (isConstantSymbol(name)) {
                    consts.add(name)
                } else if (name !in boundVars && !isBuiltInFunction(name)) {
                    vars.add(name)
                }
            }
        }
    }

    private fun collectBoundVariablesInFormula(formula: String): Set<String> {
        val bound = mutableSetOf<String>()
        try {
            val normalized = com.uncode.app.math.ui.MathFormatter.toParseableString(formula)
            val root = Parser(true).parse(normalized)
            fun walk(node: ASTNode) {
                if (node is FunctionNode && node.isNotEmpty()) {
                    val h = node[0].toString().lowercase()
                    when {
                        h in setOf("lim", "limp", "limm", "sum", "derivv", "derivn") && node.size >= 2 -> {
                            bound.add(node[1].toString())
                        }
                        h == "intv" && node.size >= 3 -> {
                            bound.add(node[2].toString())
                        }
                        h == "defint" && node.size >= 5 -> {
                            bound.add(node[4].toString())
                        }
                        h == "deriv" || h == "intx" || h == "integrate" -> {
                            bound.add("x")
                        }
                    }
                    for (i in 1 until node.size) walk(node[i])
                }
            }
            walk(root)
        } catch (_: Throwable) {}
        return bound
    }

    private fun realNthRoot(rad: Double, deg: Double): Double {
        if (abs(deg) < 1e-12) return Double.NaN
        if (rad < 0.0) {
            val nInt = deg.roundToInt()
            if (abs(deg - nInt) < 1e-9 && nInt % 2 != 0) {
                return -((-rad).pow(1.0 / deg))
            }
            return Double.NaN
        }
        return rad.pow(1.0 / deg)
    }

    private fun permutation(nVal: Double, rVal: Double): Double {
        val n = nVal.roundToInt()
        val r = rVal.roundToInt()
        if (abs(nVal - n) > 1e-7 || abs(rVal - r) > 1e-7 || n < 0 || r < 0 || r > n) return Double.NaN
        var res = 1.0
        for (i in (n - r + 1)..n) res *= i.toDouble()
        return res
    }

    private fun combination(nVal: Double, rVal: Double): Double {
        val n = nVal.roundToInt()
        val r = rVal.roundToInt()
        if (abs(nVal - n) > 1e-7 || abs(rVal - r) > 1e-7 || n < 0 || r < 0 || r > n) return Double.NaN
        val k = minOf(r, n - r)
        var res = 1.0
        for (i in 1..k) {
            res = res * (n - k + i) / i
        }
        return res
    }

    private fun computeDeterminant(n: Int, elements: List<Double>): Double {
        if (n <= 0 || elements.size < n * n) return Double.NaN
        if (n == 1) return elements[0]
        if (n == 2) return elements[0] * elements[3] - elements[1] * elements[2]
        val mat = Array(n) { r -> DoubleArray(n) { c -> elements[r * n + c] } }
        var detSign = 1.0
        for (i in 0 until n) {
            var pivot = i
            var maxVal = abs(mat[i][i])
            for (r in (i + 1) until n) {
                val v = abs(mat[r][i])
                if (v > maxVal) {
                    maxVal = v
                    pivot = r
                }
            }
            if (maxVal < 1e-12) return 0.0
            if (pivot != i) {
                val tmp = mat[i]
                mat[i] = mat[pivot]
                mat[pivot] = tmp
                detSign = -detSign
            }
            val pivVal = mat[i][i]
            for (r in (i + 1) until n) {
                val factor = mat[r][i] / pivVal
                for (c in i until n) {
                    mat[r][c] -= factor * mat[i][c]
                }
            }
        }
        var prod = detSign
        for (i in 0 until n) prod *= mat[i][i]
        return prod
    }

    private fun evaluateLimitNode(
        engine: DoubleEvaluator,
        fnNode: FunctionNode,
        mode: String
    ): Double {
        if (fnNode.size < 4) return Double.NaN
        val varName = fnNode[1].toString()
        val targetNode = fnNode[2]
        val bodyNode = fnNode[3]
        val targetStr = targetNode.toString().lowercase()
        val saved: Double = try {
            engine.getVariable(varName)?.value ?: Double.NaN
        } catch (_: Throwable) {
            Double.NaN
        }

        fun evalAt(x: Double): Double = try {
            engine.defineVariable(varName, x)
            engine.evaluateNode(bodyNode)
        } catch (_: Throwable) {
            Double.NaN
        }

        val result = when {
            targetStr == "oo" || targetStr == "infinity" -> {
                val v1 = evalAt(1e5)
                val v2 = evalAt(1e6)
                if (!v2.isNaN() && !v2.isInfinite() && abs(v2 - v1) < 1e-2) v2 else v2
            }
            targetStr == "-oo" || targetStr == "-infinity" -> {
                val v1 = evalAt(-1e5)
                val v2 = evalAt(-1e6)
                if (!v2.isNaN() && !v2.isInfinite() && abs(v2 - v1) < 1e-2) v2 else v2
            }
            else -> {
                val t = try {
                    engine.evaluateNode(targetNode)
                } catch (_: Throwable) {
                    0.0
                }
                val h1 = 1e-5
                val h2 = 1e-7
                when (mode) {
                    "limp" -> {
                        val r2 = evalAt(t + h2)
                        if (!r2.isNaN() && !r2.isInfinite()) r2 else evalAt(t + h1)
                    }
                    "limm" -> {
                        val l2 = evalAt(t - h2)
                        if (!l2.isNaN() && !l2.isInfinite()) l2 else evalAt(t - h1)
                    }
                    else -> {
                        val direct = evalAt(t)
                        if (!direct.isNaN() && !direct.isInfinite()) {
                            val r = evalAt(t + h1)
                            val l = evalAt(t - h1)
                            if (!r.isNaN() && !l.isNaN() && abs(r - l) < 1e-3) {
                                (r + l) * 0.5
                            } else {
                                direct
                            }
                        } else {
                            val r = evalAt(t + h2)
                            val l = evalAt(t - h2)
                            if (!r.isNaN() && !l.isNaN() && abs(r - l) < 1e-3) {
                                (r + l) * 0.5
                            } else {
                                Double.NaN
                            }
                        }
                    }
                }
            }
        }

        if (!saved.isNaN()) {
            engine.defineVariable(varName, saved)
        }
        return result
    }

    private fun evaluateDerivativeNode(
        engine: DoubleEvaluator,
        varName: String,
        order: Int,
        bodyNode: ASTNode
    ): Double {
        val center: Double = try {
            val v = engine.getVariable(varName)?.value ?: Double.NaN
            if (v.isNaN()) 1.0 else v
        } catch (_: Throwable) {
            1.0
        }

        fun evalAt(x: Double): Double {
            engine.defineVariable(varName, x)
            return engine.evaluateNode(bodyNode)
        }

        val h = if (order == 1) 1e-5 else 1e-3
        val res = when (order.coerceIn(1, 4)) {
            1 -> (evalAt(center + h) - evalAt(center - h)) / (2.0 * h)
            2 -> (evalAt(center + h) - 2.0 * evalAt(center) + evalAt(center - h)) / (h * h)
            3 -> (evalAt(center + 2 * h) - 2.0 * evalAt(center + h) + 2.0 * evalAt(center - h) - evalAt(center - 2 * h)) / (2.0 * h * h * h)
            else -> (evalAt(center + 2 * h) - 4.0 * evalAt(center + h) + 6.0 * evalAt(center) - 4.0 * evalAt(center - h) + evalAt(center - 2 * h)) / (h * h * h * h)
        }
        engine.defineVariable(varName, center)
        return res
    }

    private fun evaluateDefiniteIntegral(
        engine: DoubleEvaluator,
        varName: String,
        low: Double,
        high: Double,
        bodyNode: ASTNode
    ): Double {
        if (low.isNaN() || high.isNaN()) return Double.NaN
        if (abs(high - low) < 1e-12) return 0.0
        val saved: Double = try {
            engine.getVariable(varName)?.value ?: Double.NaN
        } catch (_: Throwable) {
            1.0
        }
        val steps = 100
        val h = (high - low) / steps
        fun evalAt(x: Double): Double {
            engine.defineVariable(varName, x)
            return engine.evaluateNode(bodyNode)
        }
        var sum = evalAt(low) + evalAt(high)
        for (i in 1 until steps) {
            val x = low + i * h
            val weight = if (i % 2 == 0) 2.0 else 4.0
            sum += weight * evalAt(x)
        }
        if (!saved.isNaN()) {
            engine.defineVariable(varName, saved)
        }
        return (h / 3.0) * sum
    }

    private fun createEvaluator(preBindVars: Set<String> = emptySet()): DoubleEvaluator {
        val evaluator = DoubleEvaluator(true)
        evaluator.defineVariable("pi", Math.PI)
        evaluator.defineVariable("e", Math.E)
        evaluator.defineVariable("oo", 1e8)
        evaluator.defineVariable("i", 1.0)
        evaluator.defineVariable("dx", 1.0)
        evaluator.defineVariable("dy", 1.0)
        for (bv in preBindVars) {
            if (!isConstantSymbol(bv)) {
                evaluator.defineVariable(bv, 1.0)
            }
        }
        evaluator.setCallbackFunction { engine, functionNode, args ->
            val head = if (functionNode.size > 0) functionNode[0].toString().lowercase() else ""
            when (head) {
                "cbrt" -> if (args.isNotEmpty()) cbrt(args[0]) else 0.0
                "root" -> when {
                    args.size >= 2 -> {
                        val r1 = realNthRoot(rad = args[1], deg = args[0])
                        val r2 = realNthRoot(rad = args[0], deg = args[1])
                        if (!r1.isNaN() && abs(r1 - r1.roundToLong()) < 1e-7) {
                            r1
                        } else if (!r2.isNaN() && abs(r2 - r2.roundToLong()) < 1e-7) {
                            r2
                        } else {
                            r1
                        }
                    }
                    args.isNotEmpty() -> sqrt(args[0])
                    else -> 0.0
                }
                "logb" -> if (args.size >= 2) ln(args[1]) / ln(args[0]) else 0.0
                "mixed" -> if (args.size >= 3) {
                    val w = args[0]
                    val s = if (w < 0.0) -1.0 else 1.0
                    w + s * (args[1] / args[2])
                } else 0.0
                "perm" -> if (args.size >= 2) permutation(args[0], args[1]) else 0.0
                "comb", "binom" -> if (args.size >= 2) combination(args[0], args[1]) else 0.0
                "deg" -> if (args.isNotEmpty()) Math.toRadians(args[0]) else 0.0
                "dms2" -> if (args.size >= 2) Math.toRadians(args[0] + args[1] / 60.0) else 0.0
                "dms3" -> if (args.size >= 3) Math.toRadians(args[0] + args[1] / 60.0 + args[2] / 3600.0) else 0.0
                "sub" -> if (args.isNotEmpty()) args[0] else 0.0
                "fn1" -> if (args.size >= 2) args[0] * args[1] else 0.0
                "fn2" -> if (args.size >= 3) args[0] * (args[1] + args[2]) else 0.0
                "conj" -> if (args.isNotEmpty()) args[0] else 0.0
                "det" -> if (args.size >= 3) {
                    val r = args[0].roundToInt()
                    val c = args[1].roundToInt()
                    if (r == c && r >= 1) {
                        computeDeterminant(r, args.drop(2))
                    } else 0.0
                } else if (args.isNotEmpty()) args[0] else 0.0
                "matrix" -> if (args.size >= 3) {
                    args.drop(2).sum()
                } else 0.0
                "lim", "limp", "limm" -> evaluateLimitNode(engine, functionNode, head)
                "deriv" -> if (functionNode.size >= 2) {
                    evaluateDerivativeNode(engine, "x", 1, functionNode[1])
                } else 0.0
                "derivv" -> if (functionNode.size >= 3) {
                    evaluateDerivativeNode(engine, functionNode[1].toString(), 1, functionNode[2])
                } else 0.0
                "derivn" -> if (functionNode.size >= 4) {
                    val ord = args.getOrElse(1) { 2.0 }.roundToInt().coerceIn(1, 4)
                    evaluateDerivativeNode(engine, functionNode[1].toString(), ord, functionNode[3])
                } else 0.0
                "intx", "integrate" -> if (functionNode.size >= 2) {
                    val upper: Double = try {
                        val v = engine.getVariable("x")?.value ?: Double.NaN
                        if (v.isNaN()) 1.0 else v
                    } catch (_: Throwable) {
                        1.0
                    }
                    evaluateDefiniteIntegral(engine, "x", 0.0, upper, functionNode[1])
                } else 0.0
                "intv" -> if (functionNode.size >= 3) {
                    val vName = functionNode[2].toString()
                    val upper: Double = try {
                        val v = engine.getVariable(vName)?.value ?: Double.NaN
                        if (v.isNaN()) 1.0 else v
                    } catch (_: Throwable) {
                        1.0
                    }
                    evaluateDefiniteIntegral(engine, vName, 0.0, upper, functionNode[1])
                } else 0.0
                "defint" -> if (functionNode.size >= 5) {
                    val low = args[0]
                    val high = args[1]
                    val vName = functionNode[4].toString()
                    evaluateDefiniteIntegral(engine, vName, low, high, functionNode[3])
                } else 0.0
                "sum" -> if (functionNode.size >= 5) {
                    val vName = functionNode[1].toString()
                    val low = args[1].roundToInt()
                    val high = args[2].roundToInt()
                    val body = functionNode[4]
                    var total = 0.0
                    if (high >= low && (high - low) <= 500) {
                        for (idx in low..high) {
                            engine.defineVariable(vName, idx.toDouble())
                            total += engine.evaluateNode(body)
                        }
                    }
                    total
                } else 0.0
                "sec" -> if (args.isNotEmpty()) 1.0 / cos(args[0]) else 0.0
                "csc" -> if (args.isNotEmpty()) 1.0 / sin(args[0]) else 0.0
                "cot" -> if (args.isNotEmpty()) 1.0 / tan(args[0]) else 0.0
                "coth" -> if (args.isNotEmpty()) 1.0 / tanh(args[0]) else 0.0
                "sech" -> if (args.isNotEmpty()) 1.0 / cosh(args[0]) else 0.0
                "csch" -> if (args.isNotEmpty()) 1.0 / sinh(args[0]) else 0.0
                "arccot", "acot" -> if (args.isNotEmpty()) atan(1.0 / args[0]) else 0.0
                "arcsec" -> if (args.isNotEmpty()) acos(1.0 / args[0]) else 0.0
                "arccsc" -> if (args.isNotEmpty()) asin(1.0 / args[0]) else 0.0
                "arsinh" -> if (args.isNotEmpty()) ln(args[0] + sqrt(args[0] * args[0] + 1.0)) else 0.0
                "arcosh" -> if (args.isNotEmpty()) ln(args[0] + sqrt(args[0] * args[0] - 1.0)) else 0.0
                "artanh" -> if (args.isNotEmpty()) 0.5 * ln((1.0 + args[0]) / (1.0 - args[0])) else 0.0
                "arcoth" -> if (args.isNotEmpty()) 0.5 * ln((args[0] + 1.0) / (args[0] - 1.0)) else 0.0
                "arsech" -> if (args.isNotEmpty()) ln((1.0 + sqrt(1.0 - args[0] * args[0])) / args[0]) else 0.0
                "arcsch" -> if (args.isNotEmpty()) ln(1.0 / args[0] + sqrt(1.0 / (args[0] * args[0]) + 1.0)) else 0.0
                "sgn" -> if (args.isNotEmpty()) sign(args[0]) else 0.0
                "rad" -> if (args.isNotEmpty()) Math.toRadians(args[0]) else 0.0
                "log2" -> if (args.isNotEmpty()) ln(args[0]) / ln(2.0) else 0.0
                "mod" -> if (args.size >= 2) args[0] % args[1] else 0.0
                "sys" -> if (args.isNotEmpty()) args.sum() else 0.0
                else -> throw org.matheclipse.parser.client.math.ArithmeticMathException("Unsupported function: $head")
            }
        }
        return evaluator
    }

    private fun astContainsPowerOfVar(node: ASTNode, varName: String): Boolean {
        if (node is FunctionNode && node.isNotEmpty()) {
            val h = node[0].toString()
            if (h == "Power" && node.size >= 3 && node[1].toString() == varName) return true
            for (i in 1 until node.size) {
                if (astContainsPowerOfVar(node[i], varName)) return true
            }
        }
        return false
    }

    private fun checkAllDivisionsExact(
        exprStr: String,
        varName: String?,
        varValue: Double,
        boundVars: Set<String>
    ): Boolean {
        return try {
            val norm = com.uncode.app.math.ui.MathFormatter.toParseableString(exprStr)
            val ast = Parser(true).parse(norm)
            val eval = createEvaluator(boundVars)
            if (varName != null) {
                eval.defineVariable(varName, varValue)
            }
            fun checkNode(n: ASTNode): Boolean {
                if (n is FunctionNode && n.isNotEmpty()) {
                    val h = n[0].toString()
                    if (h == "Divide" && n.size >= 3) {
                        val numVal = eval.evaluateNode(n[1])
                        val denVal = eval.evaluateNode(n[2])
                        if (abs(denVal) < 1e-9) return false
                        val q = numVal / denVal
                        if (q.isNaN() || q.isInfinite() || abs(q - q.roundToLong()) > 1e-7) return false
                    } else if (h == "Times" && n.size >= 3) {
                        val hasInv = (1 until n.size).any { idx ->
                            val c = n[idx]
                            c is FunctionNode && c.size == 3 && c[0].toString() == "Power" && c[2].toString() == "-1"
                        }
                        if (hasInv) {
                            val prodVal = eval.evaluateNode(n)
                            if (prodVal.isNaN() || prodVal.isInfinite() || abs(prodVal - prodVal.roundToLong()) > 1e-7) return false
                        }
                    }
                    for (i in 1 until n.size) {
                        if (!checkNode(n[i])) return false
                    }
                }
                return true
            }
            checkNode(ast)
        } catch (_: Throwable) {
            false
        }
    }

    private fun extractInverseDenom(n: ASTNode): ASTNode? {
        if (n is FunctionNode && n.size == 3 && n[0].toString() == "Power" && n[2] is IntegerNode && n[2].toString() == "-1") {
            return n[1]
        }
        return null
    }

    private fun isSimpleConstOrParamTerm(n: ASTNode, symMap: Map<String, String>, targetVar: String): Boolean {
        if (n is IntegerNode) return true
        if (n is SymbolNode) return symMap[n.toString()] != targetVar
        if (n is FunctionNode && n.size == 3 && n[0].toString() == "Times" && n[1].toString() == "-1") {
            return isSimpleConstOrParamTerm(n[2], symMap, targetVar)
        }
        return false
    }

    fun mutate(validation: SymjaValidation): MutationResult {
        val root = validation.astNode ?: return MutationResult("", emptyList())
        val records = mutableListOf<MutationRecord>()

        val boundVarsInAst = mutableSetOf<String>()
        fun findBoundInAst(node: ASTNode) {
            if (node is FunctionNode && node.isNotEmpty()) {
                val h = node[0].toString().lowercase()
                when {
                    h in setOf("lim", "limp", "limm", "sum", "derivv", "derivn") && node.size >= 2 -> boundVarsInAst.add(node[1].toString())
                    h == "intv" && node.size >= 3 -> boundVarsInAst.add(node[2].toString())
                    h == "defint" && node.size >= 5 -> boundVarsInAst.add(node[4].toString())
                    h == "deriv" || h == "intx" || h == "integrate" -> boundVarsInAst.add("x")
                }
                for (i in 1 until node.size) findBoundInAst(node[i])
            }
        }
        findBoundInAst(root)

        if (root is FunctionNode && root.size >= 2 && root[0].toString().lowercase() == "sys") {
            val sysResult = mutateSystemOfEquations(root, boundVarsInAst, records)
            if (sysResult != null) {
                return sysResult
            }
        }

        val quadraticInstance = tryGenerateFactoredQuadratic(root, validation.variables, records)
        if (quadraticInstance != null) {
            return quadraticInstance
        }

        val isEquation = root is FunctionNode && root.size >= 3 && root[0].toString() == "Equal"
        if (isEquation && !containsMatrixNode(root) && !SymbolicCalculus.containsSymbolicCalculusNode(root) && !containsComplexNode(root)) {
            val eqNode = root as FunctionNode
            val lhsNode = eqNode[1]
            val rhsNode = eqNode[2]
            val paramResult = generateParametricEquation(lhsNode, rhsNode, validation.variables, boundVarsInAst)
            if (paramResult != null) {
                return paramResult
            }
        }

        return generateGeneralInstance(root, validation.variables, boundVarsInAst, records)
    }

    private fun generateParametricEquation(
        lhsNode: ASTNode,
        rhsNode: ASTNode,
        allVars: List<String>,
        boundVars: Set<String>
    ): MutationResult? {
        val lhsVars = LinkedHashSet<String>()
        val rhsVars = LinkedHashSet<String>()
        collectTokens(lhsNode, lhsVars, LinkedHashSet(), LinkedHashSet(), boundVars)
        collectTokens(rhsNode, rhsVars, LinkedHashSet(), LinkedHashSet(), boundVars)

        val rhsIsZero = rhsNode is IntegerNode && rhsNode.toString() == "0"
        val rhsIsIsolatedAtom = rhsNode is SymbolNode || rhsNode is IntegerNode || rhsNode is FloatNode

        val primaryLhsVar = selectPrimaryVariable(lhsVars.toList(), lhsNode)
        val chosenUnknown = unknownVariablePool.filter { it !in boundVars }.random(random)
        val hasNonlinearPower = primaryLhsVar != null && astContainsPowerOfVar(lhsNode, primaryLhsVar)

        if (rhsIsIsolatedAtom && !rhsIsZero && !hasNonlinearPower && lhsNode is FunctionNode && random.nextInt(10) < 3) {
            for (trial in 0 until 40) {
                val trialRecords = mutableListOf<MutationRecord>()
                val usedConsts = mutableSetOf<Int>()
                val symMap = mutableMapOf<String, String>()
                for (v in lhsVars) {
                    val c = pickDistinctConst(usedConsts)
                    symMap[v] = c.toString()
                    trialRecords.add(MutationRecord(v, c.toString(), MutationType.CONSTANT))
                }
                val mutLhs = mutateSubtree(
                    node = lhsNode,
                    symMap = symMap,
                    targetVar = chosenUnknown,
                    targetVal = null,
                    activeBound = boundVars,
                    records = trialRecords,
                    promoteLeafId = null,
                    counter = intArrayOf(0)
                )
                if (!checkAllDivisionsExact(mutLhs, null, 0.0, boundVars)) continue
                val eval = createEvaluator(boundVars)
                val valLhs = try {
                    eval.evaluate(com.uncode.app.math.ui.MathFormatter.toParseableString(mutLhs))
                } catch (_: Throwable) {
                    Double.NaN
                }
                if (!valLhs.isNaN() && !valLhs.isInfinite() && abs(valLhs - valLhs.roundToLong()) < 1e-7 && abs(valLhs) in 1.0..200.0) {
                    val cleanLhs = sanitizeFormulaString(mutLhs)
                    trialRecords.add(MutationRecord(rhsNode.toString(), chosenUnknown, MutationType.VARIABLE))
                    return MutationResult(
                        mutatedFormula = "$cleanLhs == $chosenUnknown",
                        records = trialRecords,
                        variableValues = mapOf(chosenUnknown to valLhs.roundToLong().toDouble())
                    )
                }
            }
        }

        if (rhsIsZero && lhsNode is FunctionNode && lhsNode.size == 3 && lhsNode[0].toString() in setOf("Plus", "Subtract")) {
            val termA = lhsNode[1]
            val termB = lhsNode[2]
            val bVars = LinkedHashSet<String>()
            collectTokens(termB, bVars, LinkedHashSet(), LinkedHashSet(), boundVars)
            val bIsConstOrParam = primaryLhsVar != null && primaryLhsVar !in bVars && (termB is SymbolNode || termB is IntegerNode)
            if (bIsConstOrParam) {
                val targetPool = if (hasNonlinearPower) listOf(2, 3, 4, 5) else listOf(2, 3, 4, 5, 6, -2, -3, -4, -5)
                for (trial in 0 until 45) {
                    val targetX = targetPool.random(random).toDouble()
                    val trialRecords = mutableListOf<MutationRecord>()
                    val usedConsts = mutableSetOf<Int>()
                    val symMap = mutableMapOf<String, String>()
                    for (v in lhsVars) {
                        if (v == primaryLhsVar) {
                            symMap[v] = chosenUnknown
                            trialRecords.add(MutationRecord(v, chosenUnknown, MutationType.VARIABLE))
                        } else {
                            val c = pickDistinctConst(usedConsts)
                            symMap[v] = c.toString()
                            trialRecords.add(MutationRecord(v, c.toString(), MutationType.CONSTANT))
                        }
                    }
                    val mutTermA = mutateSubtree(
                        node = termA,
                        symMap = symMap,
                        targetVar = chosenUnknown,
                        targetVal = targetX,
                        activeBound = boundVars,
                        records = trialRecords,
                        promoteLeafId = null,
                        counter = intArrayOf(0)
                    )
                    if (!checkAllDivisionsExact(mutTermA, chosenUnknown, targetX, boundVars)) continue
                    val eval = createEvaluator(boundVars)
                    eval.defineVariable(chosenUnknown, targetX)
                    val valA = try {
                        eval.evaluate(com.uncode.app.math.ui.MathFormatter.toParseableString(mutTermA))
                    } catch (_: Throwable) {
                        Double.NaN
                    }
                    if (!valA.isNaN() && !valA.isInfinite() && abs(valA - valA.roundToLong()) < 1e-7 && abs(valA) in 1.0..150.0) {
                        val intA = valA.roundToLong()
                        val opSign = if (intA > 0) "-" else "+"
                        val absC = abs(intA)
                        val cleanTermA = sanitizeFormulaString(mutTermA)
                        return MutationResult(
                            mutatedFormula = "$cleanTermA $opSign $absC == 0",
                            records = trialRecords,
                            variableValues = mapOf(chosenUnknown to targetX)
                        )
                    }
                }
            }
        }

        if (rhsIsIsolatedAtom && !rhsIsZero) {
            val targetPool = if (hasNonlinearPower) listOf(2, 3, 4, 5) else listOf(2, 3, 4, 5, 6, 7, -2, -3, -4)
            val leafConstIds = collectMutableLeafIds(lhsNode)
            val needPromoteLeaf = primaryLhsVar == null && leafConstIds.isNotEmpty()

            for (trial in 0 until 60) {
                val targetX = targetPool.random(random).toDouble()
                val trialRecords = mutableListOf<MutationRecord>()
                val usedConsts = mutableSetOf<Int>()
                val symMap = mutableMapOf<String, String>()
                val promotedId = if (needPromoteLeaf) leafConstIds.random(random) else null

                for (v in lhsVars) {
                    if (v == primaryLhsVar && promotedId == null) {
                        symMap[v] = chosenUnknown
                        trialRecords.add(MutationRecord(v, chosenUnknown, MutationType.VARIABLE))
                    } else {
                        val c = pickDistinctConst(usedConsts)
                        symMap[v] = c.toString()
                        trialRecords.add(MutationRecord(v, c.toString(), MutationType.CONSTANT))
                    }
                }

                val mutLhs = mutateSubtree(
                    node = lhsNode,
                    symMap = symMap,
                    targetVar = chosenUnknown,
                    targetVal = targetX,
                    activeBound = boundVars,
                    records = trialRecords,
                    promoteLeafId = promotedId,
                    counter = intArrayOf(0)
                )

                if (!checkAllDivisionsExact(mutLhs, chosenUnknown, targetX, boundVars)) continue

                val eval = createEvaluator(boundVars)
                eval.defineVariable(chosenUnknown, targetX)
                val rhsVal = try {
                    eval.evaluate(com.uncode.app.math.ui.MathFormatter.toParseableString(mutLhs))
                } catch (_: Throwable) {
                    Double.NaN
                }

                if (!rhsVal.isNaN() && !rhsVal.isInfinite() && abs(rhsVal - rhsVal.roundToLong()) < 1e-7 && abs(rhsVal) in 1.0..150.0) {
                    val rhsInt = rhsVal.roundToLong()
                    val cleanLhs = sanitizeFormulaString(mutLhs)
                    trialRecords.add(MutationRecord(rhsNode.toString(), rhsInt.toString(), MutationType.CONSTANT))
                    return MutationResult(
                        mutatedFormula = "$cleanLhs == $rhsInt",
                        records = trialRecords,
                        variableValues = mapOf(chosenUnknown to targetX)
                    )
                }
            }
        }

        val sharedPrimary = primaryLhsVar ?: selectPrimaryVariable(allVars, rhsNode)
        if (sharedPrimary != null && !rhsIsIsolatedAtom) {
            val targetPool = listOf(2, 3, 4, 5, -2, -3, -4)
            for (trial in 0 until 50) {
                val targetX = targetPool.random(random).toDouble()
                val trialRecords = mutableListOf<MutationRecord>()
                val usedConsts = mutableSetOf<Int>()
                val symMap = mutableMapOf<String, String>()
                for (v in allVars) {
                    if (v == sharedPrimary) {
                        symMap[v] = chosenUnknown
                        trialRecords.add(MutationRecord(v, chosenUnknown, MutationType.VARIABLE))
                    } else {
                        val c = pickDistinctConst(usedConsts)
                        symMap[v] = c.toString()
                        trialRecords.add(MutationRecord(v, c.toString(), MutationType.CONSTANT))
                    }
                }
                val mutLhs = mutateSubtree(lhsNode, symMap, chosenUnknown, targetX, boundVars, trialRecords, null, intArrayOf(0))
                val mutRhs = mutateSubtree(rhsNode, symMap, chosenUnknown, targetX, boundVars, trialRecords, null, intArrayOf(0))
                if (!checkAllDivisionsExact(mutLhs, chosenUnknown, targetX, boundVars)) continue
                if (!checkAllDivisionsExact(mutRhs, chosenUnknown, targetX, boundVars)) continue

                val eval = createEvaluator(boundVars)
                eval.defineVariable(chosenUnknown, targetX)
                val lVal = try {
                    eval.evaluate(com.uncode.app.math.ui.MathFormatter.toParseableString(mutLhs))
                } catch (_: Throwable) {
                    Double.NaN
                }
                val rVal = try {
                    eval.evaluate(com.uncode.app.math.ui.MathFormatter.toParseableString(mutRhs))
                } catch (_: Throwable) {
                    Double.NaN
                }
                if (!lVal.isNaN() && !rVal.isNaN() && abs(lVal - lVal.roundToLong()) < 1e-7 && abs(rVal - rVal.roundToLong()) < 1e-7) {
                    val diff = (lVal - rVal).roundToLong()
                    val evalAtNext = try {
                        eval.defineVariable(chosenUnknown, targetX + 1.0)
                        val l2 = eval.evaluate(com.uncode.app.math.ui.MathFormatter.toParseableString(mutLhs))
                        val r2 = eval.evaluate(com.uncode.app.math.ui.MathFormatter.toParseableString(mutRhs))
                        abs((l2 - r2) - (lVal - rVal)) > 1e-5
                    } catch (_: Throwable) {
                        false
                    }
                    if (!evalAtNext) continue
                    val balancedRhs = when {
                        diff == 0L -> mutRhs
                        diff > 0L -> "$mutRhs + $diff"
                        else -> "$mutRhs - ${-diff}"
                    }
                    val cleanLhs = sanitizeFormulaString(mutLhs)
                    val cleanRhs = sanitizeFormulaString(balancedRhs)
                    return MutationResult(
                        mutatedFormula = "$cleanLhs == $cleanRhs",
                        records = trialRecords,
                        variableValues = mapOf(chosenUnknown to targetX)
                    )
                }
            }
        }

        return null
    }

    private fun collectMutableLeafIds(root: ASTNode): List<Int> {
        val ids = mutableListOf<Int>()
        var counter = 0
        fun walk(n: ASTNode, inExp: Boolean, lockConst: Boolean) {
            val myId = counter++
            when (n) {
                is IntegerNode, is FloatNode, is FractionNode -> {
                    if (!inExp && !lockConst) ids.add(myId)
                }
                is FunctionNode -> {
                    if (n.isNotEmpty()) {
                        val h = n[0].toString().lowercase()
                        when {
                            h == "power" && n.size == 3 -> {
                                walk(n[1], false, lockConst)
                                walk(n[2], true, true)
                            }
                            h in setOf("matrix", "det", "sum", "defint", "lim", "limp", "limm", "perm", "comb", "binom") -> {
                                for (i in 1 until n.size) walk(n[i], inExp, true)
                            }
                            else -> {
                                for (i in 1 until n.size) walk(n[i], inExp, lockConst)
                            }
                        }
                    }
                }
            }
        }
        walk(root, false, false)
        return ids
    }

    private fun pickDistinctConst(used: MutableSet<Int>): Int {
        val cands = distinctConstPool.filter { it !in used }
        val picked = if (cands.isNotEmpty()) cands.random(random) else random.nextInt(2, 12)
        used.add(picked)
        return picked
    }

    private fun mutateSubtree(
        node: ASTNode,
        symMap: Map<String, String>,
        targetVar: String,
        targetVal: Double?,
        activeBound: Set<String>,
        records: MutableList<MutationRecord>,
        promoteLeafId: Int?,
        counter: IntArray,
        inExp: Boolean = false,
        keepExactNumber: Boolean = false
    ): String {
        val myId = counter[0]++
        return when (node) {
            is SymbolNode -> {
                val name = node.toString()
                if (name in activeBound) {
                    name
                } else if (symMap.containsKey(name)) {
                    symMap[name]!!
                } else if (isConstantSymbol(name)) {
                    name
                } else {
                    name
                }
            }
            is IntegerNode -> {
                val orig = node.toString()
                if (keepExactNumber || inExp) {
                    orig
                } else if (myId == promoteLeafId) {
                    records.add(MutationRecord(orig, targetVar, MutationType.VARIABLE))
                    targetVar
                } else {
                    val origInt = orig.toIntOrNull() ?: 2
                    val newConst = if (origInt == 0) {
                        "0"
                    } else {
                        random.nextInt(2, 10).toString()
                    }
                    records.add(MutationRecord(orig, newConst, MutationType.CONSTANT))
                    newConst
                }
            }
            is FloatNode -> {
                val orig = node.toString()
                if (keepExactNumber || inExp) {
                    orig
                } else if (myId == promoteLeafId) {
                    records.add(MutationRecord(orig, targetVar, MutationType.VARIABLE))
                    targetVar
                } else {
                    val newConst = ((random.nextInt(2, 40)) / 10.0).toString()
                    records.add(MutationRecord(orig, newConst, MutationType.CONSTANT))
                    newConst
                }
            }
            is FractionNode -> {
                val orig = node.toString()
                if (keepExactNumber || inExp) {
                    orig
                } else {
                    val den = random.nextInt(2, 10)
                    var num = random.nextInt(1, 10)
                    while (num % den == 0) {
                        num = random.nextInt(1, 10)
                    }
                    if (myId == promoteLeafId) {
                        records.add(MutationRecord(orig, targetVar, MutationType.VARIABLE))
                        if (random.nextBoolean()) "$targetVar/$den" else "$num/$targetVar"
                    } else {
                        "$num/$den"
                    }
                }
            }
            is FunctionNode -> {
                if (node.isEmpty()) ""
                else {
                    val head = node[0].toString()
                    val headLower = head.lowercase()
                    when {
                        head == "Power" && node.size == 3 -> {
                            val rawBase = mutateSubtree(node[1], symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter, inExp = false).ifBlank { "2" }
                            val safeBase = if (needsParentheses(rawBase)) "($rawBase)" else rawBase
                            val expNode = node[2]
                            val rawExp = if (expNode is IntegerNode) {
                                counter[0]++
                                expNode.toString()
                            } else if (expNode is SymbolNode && expNode.toString() !in activeBound && symMap[expNode.toString()] != targetVar) {
                                counter[0]++
                                exponentPool.random(random)
                            } else {
                                mutateSubtree(expNode, symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter, inExp = true)
                            }
                            val safeExp = if (rawExp.startsWith("-") || needsParentheses(rawExp)) "($rawExp)" else rawExp
                            "$safeBase^$safeExp"
                        }
                        head == "Divide" && node.size == 3 -> {
                            mutateDivisionPair(node[1], node[2], symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter, inExp)
                        }
                        head == "Times" -> {
                            if (node.size == 3 && node[1] is IntegerNode && node[1].toString() == "-1") {
                                counter[0]++
                                val sub = mutateSubtree(node[2], symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter, inExp)
                                val cleanSub = if (needsParentheses(sub)) "($sub)" else sub
                                "-$cleanSub"
                            } else {
                                val numNodes = mutableListOf<ASTNode>()
                                val denNodes = mutableListOf<ASTNode>()
                                for (idx in 1 until node.size) {
                                    val child = node[idx]
                                    val invDen = extractInverseDenom(child)
                                    if (invDen != null) {
                                        denNodes.add(invDen)
                                    } else {
                                        numNodes.add(child)
                                    }
                                }
                                if (denNodes.size == 1 && numNodes.size == 1) {
                                    mutateDivisionPair(numNodes[0], denNodes[0], symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter, inExp)
                                } else if (denNodes.isNotEmpty() && numNodes.isNotEmpty()) {
                                    val numPart = numNodes.joinToString(" * ") { childNode ->
                                        val sub = mutateSubtree(childNode, symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter, inExp)
                                        if (childNode is FunctionNode && childNode.size >= 3 && childNode[0].toString() in setOf("Plus", "Subtract")) "($sub)" else sub
                                    }
                                    val denPart = denNodes.joinToString(" * ") { childNode ->
                                        val sub = mutateSubtree(childNode, symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter, inExp)
                                        if (needsParentheses(sub)) "($sub)" else sub
                                    }
                                    val safeLeft = if (numPart.contains("+") || numPart.contains("-")) "($numPart)" else numPart
                                    val safeRight = if (denNodes.size > 1 || needsParentheses(denPart)) "($denPart)" else denPart
                                    "$safeLeft/$safeRight"
                                } else {
                                    val args = (1 until node.size).map {
                                        val childNode = node[it]
                                        val sub = mutateSubtree(childNode, symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter, inExp)
                                        if (childNode is FunctionNode && childNode.size >= 3 && childNode[0].toString() in setOf("Plus", "Subtract")) {
                                            "($sub)"
                                        } else {
                                            sub
                                        }
                                    }
                                    args.joinToString(" * ")
                                }
                            }
                        }
                        (headLower == "matrix" || headLower == "det") && node.size >= 3 -> {
                            val rStr = mutateSubtree(node[1], symMap, targetVar, targetVal, activeBound, records, null, counter, keepExactNumber = true)
                            val cStr = mutateSubtree(node[2], symMap, targetVar, targetVal, activeBound, records, null, counter, keepExactNumber = true)
                            val cells = (3 until node.size).map { mutateSubtree(node[it], symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter) }
                            "$headLower($rStr,$cStr;${cells.joinToString(";")})"
                        }
                        headLower in setOf("lim", "limp", "limm") && node.size >= 4 -> {
                            val bVar = mutateSubtree(node[1], symMap, targetVar, targetVal, activeBound + node[1].toString(), records, null, counter, keepExactNumber = true)
                            val target = mutateSubtree(node[2], symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter)
                            val body = mutateSubtree(node[3], symMap, targetVar, targetVal, activeBound + bVar, records, promoteLeafId, counter)
                            val dispName = when (headLower) {
                                "limp" -> "lim+"
                                "limm" -> "lim-"
                                else -> "lim"
                            }
                            "$dispName($bVar;$target;$body)"
                        }
                        headLower == "sum" && node.size >= 5 -> {
                            val bVar = mutateSubtree(node[1], symMap, targetVar, targetVal, activeBound + node[1].toString(), records, null, counter, keepExactNumber = true)
                            val low = mutateSubtree(node[2], symMap, targetVar, targetVal, activeBound, records, null, counter, keepExactNumber = true)
                            val highOrig = mutateSubtree(node[3], symMap, targetVar, targetVal, activeBound, records, null, counter, keepExactNumber = true)
                            val lowInt = low.toIntOrNull() ?: 1
                            val highMut = if (highOrig.toIntOrNull() != null) {
                                random.nextInt((lowInt + 2).coerceAtLeast(3), (lowInt + 6).coerceAtLeast(7)).toString()
                            } else highOrig
                            val body = mutateSubtree(node[4], symMap, targetVar, targetVal, activeBound + bVar, records, promoteLeafId, counter)
                            "sum($bVar;$low;$highMut;$body)"
                        }
                        headLower == "defint" && node.size >= 5 -> {
                            val lowOrig = mutateSubtree(node[1], symMap, targetVar, targetVal, activeBound, records, null, counter, keepExactNumber = true)
                            val highOrig = mutateSubtree(node[2], symMap, targetVar, targetVal, activeBound, records, null, counter, keepExactNumber = true)
                            val body = mutateSubtree(node[3], symMap, targetVar, targetVal, activeBound + node[4].toString(), records, promoteLeafId, counter)
                            val bVar = mutateSubtree(node[4], symMap, targetVar, targetVal, activeBound + node[4].toString(), records, null, counter, keepExactNumber = true)
                            val lowMut = if (lowOrig.toIntOrNull() != null) random.nextInt(0, 2).toString() else lowOrig
                            val highMut = if (highOrig.toIntOrNull() != null) random.nextInt(2, 5).toString() else highOrig
                            "defint($lowMut;$highMut;$body;$bVar)"
                        }
                        headLower == "intv" && node.size >= 3 -> {
                            val bVar = node[2].toString()
                            val body = mutateSubtree(node[1], symMap, targetVar, targetVal, activeBound + bVar, records, promoteLeafId, counter)
                            val vStr = mutateSubtree(node[2], symMap, targetVar, targetVal, activeBound + bVar, records, null, counter, keepExactNumber = true)
                            "intv($body;$vStr)"
                        }
                        headLower == "derivv" && node.size >= 3 -> {
                            val bVar = node[1].toString()
                            val vStr = mutateSubtree(node[1], symMap, targetVar, targetVal, activeBound + bVar, records, null, counter, keepExactNumber = true)
                            val body = mutateSubtree(node[2], symMap, targetVar, targetVal, activeBound + bVar, records, promoteLeafId, counter)
                            "derivv($vStr;$body)"
                        }
                        headLower == "derivn" && node.size >= 4 -> {
                            val bVar = node[1].toString()
                            val vStr = mutateSubtree(node[1], symMap, targetVar, targetVal, activeBound + bVar, records, null, counter, keepExactNumber = true)
                            val ordOrig = mutateSubtree(node[2], symMap, targetVar, targetVal, activeBound, records, null, counter, keepExactNumber = true)
                            val ordMut = if (ordOrig.toIntOrNull() != null) random.nextInt(1, 4).toString() else ordOrig
                            val body = mutateSubtree(node[3], symMap, targetVar, targetVal, activeBound + bVar, records, promoteLeafId, counter)
                            "derivn($vStr;$ordMut;$body)"
                        }
                        headLower in setOf("perm", "comb", "binom") && node.size >= 3 -> {
                            val a0 = mutateSubtree(node[1], symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter)
                            val a1 = mutateSubtree(node[2], symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter)
                            val i0 = a0.toIntOrNull()
                            val i1 = a1.toIntOrNull()
                            if (i0 != null && i1 != null) {
                                val nVal = random.nextInt(4, 9)
                                val rVal = random.nextInt(1, nVal)
                                "$headLower($nVal;$rVal)"
                            } else {
                                "$headLower($a0;$a1)"
                            }
                        }
                        head in setOf("Less", "Greater", "LessEqual", "GreaterEqual", "Unequal") && node.size == 3 -> {
                            val left = mutateSubtree(node[1], symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter, inExp)
                            val right = mutateSubtree(node[2], symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter, inExp)
                            "$left ${mapHeadToOperator(head)} $right"
                        }
                        else -> {
                            val args = (1 until node.size).map {
                                mutateSubtree(node[it], symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter, inExp)
                            }
                            formatMutatedFunction(head, args, records)
                        }
                    }
                }
            }
            else -> node.toString()
        }
    }

    private fun mutateDivisionPair(
        numAst: ASTNode,
        denAst: ASTNode,
        symMap: Map<String, String>,
        targetVar: String,
        targetVal: Double?,
        activeBound: Set<String>,
        records: MutableList<MutationRecord>,
        promoteLeafId: Int?,
        counter: IntArray,
        inExp: Boolean
    ): String {
        if (targetVal != null && numAst is FunctionNode && numAst.size == 3 && numAst[0].toString() in setOf("Plus", "Subtract") &&
            isSimpleConstOrParamTerm(numAst[2], symMap, targetVar) &&
            isSimpleConstOrParamTerm(denAst, symMap, targetVar)
        ) {
            counter[0]++
            val innerA = mutateSubtree(numAst[1], symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter, inExp = false)
            counter[0]++
            counter[0]++
            val cVal = listOf(2, 3, 4, 5).random(random)
            val eval = createEvaluator(activeBound)
            eval.defineVariable(targetVar, targetVal)
            val aVal = try {
                eval.evaluate(com.uncode.app.math.ui.MathFormatter.toParseableString(innerA))
            } catch (_: Throwable) {
                Double.NaN
            }
            if (!aVal.isNaN() && abs(aVal - aVal.roundToLong()) < 1e-7) {
                val aInt = aVal.roundToLong()
                val qCandidates = listOf(2, 3, 4, 5, 6, 7, 8, 9).shuffled(random)
                for (qCand in qCandidates) {
                    val desiredNum = qCand.toLong() * cVal
                    val diff = desiredNum - aInt
                    if (diff != 0L && abs(diff) <= 60L) {
                        val op = if (diff > 0) "+" else "-"
                        val absB = abs(diff)
                        return "($innerA $op $absB)/$cVal"
                    }
                }
            }
        }
        val left = mutateSubtree(numAst, symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter, inExp).ifBlank { "1" }
        var right = mutateSubtree(denAst, symMap, targetVar, targetVal, activeBound, records, promoteLeafId, counter, inExp).trim()
        if (right.isEmpty() || right == "0" || right == "()") {
            right = random.nextInt(2, 6).toString()
        }
        val safeLeft = if (left.contains("+") || left.contains("-")) "($left)" else left
        val safeRight = if (right.contains("/") || right.contains("+") || right.contains("-") || right.contains("*")) "($right)" else right
        return "$safeLeft/$safeRight"
    }

    private fun generateGeneralInstance(
        root: ASTNode,
        variables: List<String>,
        boundVarsInAst: Set<String>,
        records: MutableList<MutationRecord>
    ): MutationResult {
        val hasMatrix = containsMatrixNode(root)
        val hasSymbolicCalc = SymbolicCalculus.containsSymbolicCalculusNode(root)
        val hasComplex = containsComplexNode(root)
        val isNonScalarDomain = hasMatrix || hasSymbolicCalc || hasComplex
        val targetUnknownSymbol = unknownVariablePool.filter { it !in boundVarsInAst }.random(random)
        val isolatedSideVar = if (isNonScalarDomain && root is FunctionNode && root.size >= 3 && root[0].toString() == "Equal") {
            val l = root[1]
            val r = root[2]
            when {
                r is SymbolNode && r.toString() in variables -> r.toString()
                l is SymbolNode && l.toString() in variables -> l.toString()
                else -> null
            }
        } else null
        val primaryOrigVar = isolatedSideVar ?: selectPrimaryVariable(variables, root)
        val isEquation = root is FunctionNode && root.size >= 3 && root[0].toString() == "Equal"
        val leafConstIds = collectMutableLeafIds(root)
        val promoteLeafId = if (isEquation && !isNonScalarDomain && primaryOrigVar == null && leafConstIds.isNotEmpty() && random.nextInt(3) < 2) {
            leafConstIds.random(random)
        } else {
            null
        }

        val usedConsts = mutableSetOf<Int>()
        val symMap = mutableMapOf<String, String>()
        for (v in variables) {
            if (v == primaryOrigVar && promoteLeafId == null && (!isNonScalarDomain || isolatedSideVar != null)) {
                symMap[v] = targetUnknownSymbol
                records.add(MutationRecord(v, targetUnknownSymbol, MutationType.VARIABLE))
            } else {
                val c = pickDistinctConst(usedConsts)
                symMap[v] = c.toString()
                records.add(MutationRecord(v, c.toString(), MutationType.CONSTANT))
            }
        }

        val chosenTargetX = listOf(2, 3, 4, 5, -2, -3).random(random).toDouble()
        var mutatedFormula = mutateSubtree(
            node = root,
            symMap = symMap,
            targetVar = targetUnknownSymbol,
            targetVal = chosenTargetX,
            activeBound = boundVarsInAst,
            records = records,
            promoteLeafId = promoteLeafId,
            counter = intArrayOf(0)
        )
        mutatedFormula = sanitizeFormulaString(mutatedFormula)

        val isStandaloneMatrixRoot = root is FunctionNode && root.isNotEmpty() &&
            root[0].toString().equals("matrix", ignoreCase = true)
        if (isStandaloneMatrixRoot) {
            return MutationResult(
                mutatedFormula = mutatedFormula,
                records = records,
                variableValues = emptyMap()
            )
        }

        val boundVars = collectBoundVariablesInFormula(mutatedFormula)
        val parseableMutated = com.uncode.app.math.ui.MathFormatter.toParseableString(mutatedFormula)
        val varValues = LinkedHashMap<String, Double>()
        val detectedVars = mutableSetOf<String>()
        DoubleEvaluator.getVariables(parseableMutated, detectedVars)
        val freeVars = detectedVars.filter { !isConstantSymbol(it) && !isBuiltInFunction(it) && it !in boundVars }

        if (isNonScalarDomain) {
            if (!mutatedFormula.contains("==") && freeVars.isEmpty()) {
                mutatedFormula = "$mutatedFormula == $targetUnknownSymbol"
            }
            return MutationResult(
                mutatedFormula = mutatedFormula,
                records = records,
                variableValues = varValues
            )
        }

        val (exponentVars, denomVars) = analyzeVariableRoles(parseableMutated)

        if (freeVars.isNotEmpty()) {
            val v = freeVars.first()
            if (!mutatedFormula.contains("==")) {
                val assignment = findValidSingleUnknownAssignment(parseableMutated, v, v in exponentVars, v in denomVars, boundVars)
                if (assignment != null) {
                    val (chosenVal, evalVal) = assignment
                    varValues[v] = chosenVal
                    val rhsStr = formatDouble(evalVal)
                    mutatedFormula = "$mutatedFormula == $rhsStr"
                } else {
                    varValues[v] = 2.0
                    mutatedFormula = "$mutatedFormula == 4"
                }
            } else {
                val parts = parseableMutated.split("==")
                val displayParts = mutatedFormula.split("==")
                if (parts.size == 2 && displayParts.size == 2) {
                    val leftStr = parts[0].trim()
                    val rightStr = parts[1].trim()
                    val dispLeft = displayParts[0].trim()
                    val existingRoots = solveSingleVariableRoots(leftStr, rightStr, v, boundVars)
                    if (existingRoots.isNotEmpty()) {
                        varValues[v] = existingRoots.first()
                    } else {
                        val assignment = findValidSingleUnknownAssignment(leftStr, v, v in exponentVars, v in denomVars, boundVars)
                        if (assignment != null) {
                            val (chosenVal, evalVal) = assignment
                            varValues[v] = chosenVal
                            val rhsStr = formatDouble(evalVal)
                            mutatedFormula = "$dispLeft == $rhsStr"
                        }
                    }
                }
            }
        } else {
            if (!mutatedFormula.contains("==")) {
                val testEval = createEvaluator(boundVars)
                try {
                    val evalVal = testEval.evaluate(parseableMutated)
                    if (!evalVal.isNaN() && !evalVal.isInfinite()) {
                        varValues[targetUnknownSymbol] = evalVal
                        mutatedFormula = "$mutatedFormula == $targetUnknownSymbol"
                    }
                } catch (_: Throwable) {}
            }
        }

        return MutationResult(
            mutatedFormula = mutatedFormula,
            records = records,
            variableValues = varValues
        )
    }

    private fun findValidSingleUnknownAssignment(
        expr: String,
        varName: String,
        inExponent: Boolean,
        inDenom: Boolean,
        boundVars: Set<String> = emptySet()
    ): Pair<Double, Double>? {
        val evaluator = createEvaluator(boundVars)
        val candidates = if (inExponent) {
            listOf(2, 3, 4, 1).shuffled(random)
        } else {
            listOf(2, 3, 4, 5, 6, 7, 8, -2, -3, -4, 9, 10).shuffled(random)
        }

        var fallback: Pair<Double, Double>? = null
        for (c in candidates) {
            if (inDenom && c == 0) continue
            val d = c.toDouble()
            evaluator.defineVariable(varName, d)
            try {
                val res = evaluator.evaluate(expr)
                if (!res.isNaN() && !res.isInfinite() && abs(res) in 1e-6..10000.0) {
                    if (abs(res - res.roundToLong()) < 1e-7) {
                        return d to res
                    }
                    if (fallback == null) {
                        fallback = d to res
                    }
                }
            } catch (_: Throwable) {}
        }
        return fallback
    }

    private fun selectPrimaryVariable(vars: List<String>, root: ASTNode): String? {
        if (vars.isEmpty()) return null
        if (vars.size == 1) return vars.first()

        val counts = mutableMapOf<String, Int>()
        val inPowerBase = mutableSetOf<String>()

        fun walk(node: ASTNode) {
            when (node) {
                is SymbolNode -> {
                    val s = node.toString()
                    if (s in vars) counts[s] = (counts[s] ?: 0) + 1
                }
                is FunctionNode -> {
                    if (node.isNotEmpty()) {
                        val h = node[0].toString()
                        if (h == "Power" && node.size >= 2 && node[1] is SymbolNode) {
                            val b = node[1].toString()
                            if (b in vars) inPowerBase.add(b)
                        }
                        for (i in 1 until node.size) walk(node[i])
                    }
                }
            }
        }
        walk(root)

        if (inPowerBase.isNotEmpty()) {
            return preferredUnknowns.firstOrNull { it in inPowerBase } ?: inPowerBase.first()
        }
        val maxCount = counts.values.maxOrNull() ?: 0
        if (maxCount > 1) {
            val topVars = counts.filterValues { it == maxCount }.keys
            return preferredUnknowns.firstOrNull { it in topVars } ?: topVars.first()
        }
        return preferredUnknowns.firstOrNull { it in vars } ?: vars.first()
    }

    private fun tryGenerateFactoredQuadratic(
        root: ASTNode,
        vars: List<String>,
        records: MutableList<MutationRecord>
    ): MutationResult? {
        if (root !is FunctionNode || root.size < 3 || root[0].toString() != "Equal") return null
        val lhsStr = root[1].toString()
        val rhsStr = root[2].toString().trim()
        if (rhsStr != "0") return null

        val primary = selectPrimaryVariable(vars, root[1]) ?: return null
        val lhsLower = lhsStr.lowercase()
        if (lhsLower.contains("/") || lhsLower.contains("divide")) return null
        val hasSquare = lhsLower.contains("power($primary, 2)") || lhsLower.contains("$primary^2")
        if (!hasSquare) return null

        var primaryOccurrences = 0
        fun countVar(n: ASTNode) {
            when (n) {
                is SymbolNode -> if (n.toString() == primary) primaryOccurrences++
                is FunctionNode -> for (i in 1 until n.size) countVar(n[i])
            }
        }
        countVar(root[1])
        if (primaryOccurrences < 2) return null

        val lhsFn = root[1]
        val hasLinearTerm = lhsFn is FunctionNode && lhsFn.size >= 3 && lhsFn[0].toString() in setOf("Plus", "Subtract")
        if (!hasLinearTerm) return null

        val targetVar = unknownVariablePool.random(random)
        val r1 = listOf(-6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6).random(random)
        val r2 = listOf(-6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6).filter { it != r1 && it != -r1 }.random(random)
        val a = listOf(1, 2, 3, 4).random(random)
        val b = -a * (r1 + r2)
        val c = a * r1 * r2

        val aTerm = if (a == 1) "$targetVar^2" else "$a * $targetVar^2"
        val bTerm = when {
            b == 0 -> ""
            b == 1 -> " + $targetVar"
            b == -1 -> " - $targetVar"
            b > 0 -> " + $b * $targetVar"
            else -> " - ${-b} * $targetVar"
        }
        val cTerm = when {
            c == 0 -> ""
            c > 0 -> " + $c"
            else -> " - ${-c}"
        }

        val formula = "$aTerm$bTerm$cTerm == 0"
        records.add(MutationRecord(original = primary, mutated = targetVar, type = MutationType.VARIABLE))
        records.add(MutationRecord(original = "a,b,c", mutated = "$a,$b,$c", type = MutationType.CONSTANT))

        val roots = listOf(r1.toDouble(), r2.toDouble()).sorted()
        val varValues = mapOf(targetVar to roots.first())

        return MutationResult(
            mutatedFormula = formula,
            records = records,
            variableValues = varValues
        )
    }

    private fun sanitizeFormulaString(raw: String): String {
        var s = raw
        s = s.replace(Regex("""/\s*(?=[+=\-<>*/\)]|$)"""), "/${random.nextInt(2, 6)}")
        s = s.replace(Regex("""/\s*\(\s*\)"""), "/(${random.nextInt(2, 6)})")
        s = s.replace(Regex("""/\s*0(?!\d)"""), "/${random.nextInt(2, 6)}")
        s = s.replace(Regex("""\s*/\s*"""), "/")
        s = s.replace(Regex("""\^\s*\(\s*\)"""), "^2")
        s = s.replace(Regex("""\^\s*(?=[+=\-<>*/\)]|$)"""), "^2")
        s = s.replace(Regex("""\+\s*-\s*"""), "- ")
        s = s.replace(Regex("""-\s*-\s*"""), "+ ")
        return s
    }

    private fun analyzeVariableRoles(formula: String): Pair<Set<String>, Set<String>> {
        val expVars = mutableSetOf<String>()
        val denomVars = mutableSetOf<String>()
        try {
            val root = Parser(true).parse(formula)
            fun walk(node: ASTNode, inExponent: Boolean, inDenom: Boolean) {
                when (node) {
                    is SymbolNode -> {
                        val name = node.toString()
                        if (!isConstantSymbol(name) && !isBuiltInFunction(name)) {
                            if (inExponent) expVars.add(name)
                            if (inDenom) denomVars.add(name)
                        }
                    }
                    is FunctionNode -> {
                        if (node.isEmpty()) return
                        val head = node[0].toString()
                        when (head) {
                            "Power" -> {
                                val expIsNeg = node.size >= 3 && node[2].toString().trim().startsWith("-")
                                if (node.size >= 2) walk(node[1], inExponent, inDenom || expIsNeg)
                                if (node.size >= 3) walk(node[2], true, inDenom)
                            }
                            "Divide" -> {
                                if (node.size >= 2) walk(node[1], inExponent, inDenom)
                                if (node.size >= 3) walk(node[2], inExponent, true)
                            }
                            else -> {
                                for (i in 1 until node.size) {
                                    walk(node[i], inExponent, inDenom)
                                }
                            }
                        }
                    }
                }
            }
            walk(root, false, false)
        } catch (_: Throwable) {}
        return expVars to denomVars
    }

    private fun assignDistinctFallbacks(
        freeVars: List<String>,
        exponentVars: Set<String>,
        denomVars: Set<String>
    ): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        val used = mutableSetOf<Double>()
        val shuffledGeneral = distinctValuePool.shuffled(random)
        val shuffledExp = safeExponentValues.shuffled(random)

        for (v in freeVars) {
            val candidates = if (v in exponentVars) shuffledExp + shuffledGeneral else shuffledGeneral
            val picked = candidates.firstOrNull { cand ->
                val d = cand.toDouble()
                d !in used && (v !in denomVars || d != 0.0)
            }?.toDouble() ?: run {
                var extra = 17.0
                while (extra in used) extra += 1.0
                extra
            }
            used.add(picked)
            result[v] = picked
        }
        return result
    }

    private fun solveDistinctMultiVarEquation(
        leftStr: String,
        rightStr: String,
        freeVars: List<String>,
        exponentVars: Set<String>,
        denomVars: Set<String>,
        boundVars: Set<String> = emptySet()
    ): Map<String, Double>? {
        val evaluator = createEvaluator(boundVars)
        val orderedCandidates = (freeVars.filter { it !in exponentVars } + freeVars.filter { it in exponentVars }).distinct()

        for (solveVar in orderedCandidates) {
            val otherVars = freeVars.filter { it != solveVar }
            for (attempt in 0 until 25) {
                val otherMap = assignDistinctFallbacks(otherVars, exponentVars, denomVars)
                val used = otherMap.values.toHashSet()
                for ((k, v) in otherMap) {
                    evaluator.defineVariable(k, v)
                }

                fun evalDiff(t: Double): Double {
                    evaluator.defineVariable(solveVar, t)
                    return evaluator.evaluate(leftStr) - evaluator.evaluate(rightStr)
                }

                val samplePoints = if (solveVar in exponentVars) safeExponentValues else (-15..15).toList()
                for (pt in samplePoints) {
                    val dPt = pt.toDouble()
                    if (used.any { abs(it - dPt) < 1e-6 }) continue
                    if (solveVar in denomVars && dPt == 0.0) continue
                    try {
                        val diff = evalDiff(dPt)
                        if (!diff.isNaN() && !diff.isInfinite() && abs(diff) < 1e-6) {
                            val fullMap = linkedMapOf<String, Double>()
                            for (v in freeVars) {
                                fullMap[v] = if (v == solveVar) dPt else otherMap[v]!!
                            }
                            return fullMap
                        }
                    } catch (_: Throwable) {}
                }
            }
        }
        return null
    }

    private fun solveSingleVariableRoots(
        leftStr: String,
        rightStr: String,
        varName: String,
        boundVars: Set<String> = emptySet()
    ): List<Double> {
        val evaluator = createEvaluator(boundVars)
        val roots = mutableListOf<Double>()

        fun evalDiff(t: Double): Double? = try {
            evaluator.defineVariable(varName, t)
            val d = evaluator.evaluate(leftStr) - evaluator.evaluate(rightStr)
            if (d.isNaN() || d.isInfinite()) null else d
        } catch (_: Throwable) {
            null
        }

        for (i in -200..200) {
            val d = evalDiff(i.toDouble()) ?: continue
            if (abs(d) < 1e-7) {
                roots.add(i.toDouble())
            }
        }
        if (roots.isNotEmpty()) return roots

        val f0 = evalDiff(0.0)
        val f1 = evalDiff(1.0)
        val f2 = evalDiff(2.0)
        if (f0 != null && f1 != null && f2 != null) {
            val s1 = f1 - f0
            val s2 = f2 - f1
            if (abs(s2 - s1) < 1e-7 && abs(s1) > 1e-9) {
                val sol = -f0 / s1
                val check = evalDiff(sol)
                if (check != null && abs(check) < 1e-6) {
                    return listOf(sol)
                }
            }
        }

        var x = -50.0
        var prev = evalDiff(x)
        val step = 0.25
        while (x < 50.0 && roots.size < 4) {
            val nextX = x + step
            val curr = evalDiff(nextX)
            val prevVal = prev
            if (prevVal != null && curr != null && ((prevVal > 0 && curr < 0) || (prevVal < 0 && curr > 0))) {
                var lo = x
                var hi = nextX
                var fLo: Double = prevVal
                for (iter in 0 until 50) {
                    val mid = (lo + hi) * 0.5
                    val fMid = evalDiff(mid) ?: break
                    if (abs(fMid) < 1e-9) {
                        lo = mid
                        hi = mid
                        break
                    }
                    if ((fLo > 0 && fMid > 0) || (fLo < 0 && fMid < 0)) {
                        lo = mid
                        fLo = fMid
                    } else {
                        hi = mid
                    }
                }
                val root = (lo + hi) * 0.5
                val check = evalDiff(root)
                if (check != null && abs(check) < 1e-5 && roots.none { abs(it - root) < 1e-3 }) {
                    roots.add(root)
                }
            }
            x = nextX
            prev = curr
        }
        return roots
    }

    private sealed class EvalVal {
        data class Scalar(val value: Double) : EvalVal()
        data class ComplexVal(val re: Double, val im: Double) : EvalVal()
        data class Mat(val rows: Int, val cols: Int, val data: DoubleArray) : EvalVal()
    }

    private fun formatMatrixVal(mat: EvalVal.Mat): String {
        val cells = mat.data.joinToString(";") { formatDouble(it) }
        return "matrix(${mat.rows},${mat.cols};$cells)"
    }

    private fun formatComplexVal(c: EvalVal.ComplexVal): String {
        val reZero = abs(c.re) < 1e-9
        val imZero = abs(c.im) < 1e-9
        if (imZero) return formatDouble(c.re)
        val absIm = abs(c.im)
        val imUnit = if (abs(absIm - 1.0) < 1e-9) "i" else "${formatDouble(absIm)} * i"
        if (reZero) {
            return if (c.im < 0) "-$imUnit" else imUnit
        }
        val reStr = formatDouble(c.re)
        val op = if (c.im >= 0) "+" else "-"
        return "$reStr $op $imUnit"
    }

    private fun formatEvalVal(v: EvalVal): String = when (v) {
        is EvalVal.Mat -> formatMatrixVal(v)
        is EvalVal.ComplexVal -> formatComplexVal(v)
        is EvalVal.Scalar -> formatDouble(v.value)
    }

    private fun toComplex(v: EvalVal): EvalVal.ComplexVal? = when (v) {
        is EvalVal.ComplexVal -> v
        is EvalVal.Scalar -> EvalVal.ComplexVal(v.value, 0.0)
        else -> null
    }

    private fun multiplyComplex(a: EvalVal.ComplexVal, b: EvalVal.ComplexVal): EvalVal.ComplexVal =
        EvalVal.ComplexVal(
            re = a.re * b.re - a.im * b.im,
            im = a.re * b.im + a.im * b.re
        )

    private fun divideComplex(a: EvalVal.ComplexVal, b: EvalVal.ComplexVal): EvalVal.ComplexVal? {
        val den = b.re * b.re + b.im * b.im
        if (den < 1e-18) return null
        return EvalVal.ComplexVal(
            re = (a.re * b.re + a.im * b.im) / den,
            im = (a.im * b.re - a.re * b.im) / den
        )
    }

    private fun containsMatrixNode(node: ASTNode?): Boolean {
        if (node == null) return false
        if (node is FunctionNode && node.isNotEmpty()) {
            if (node[0].toString().equals("matrix", ignoreCase = true)) return true
            for (i in 1 until node.size) {
                if (containsMatrixNode(node[i])) return true
            }
        }
        return false
    }

    private fun containsComplexNode(node: ASTNode?): Boolean {
        if (node == null) return false
        if (node is SymbolNode && node.toString() == "i") return true
        if (node is FunctionNode && node.isNotEmpty()) {
            val h = node[0].toString().lowercase()
            if (h == "conj") return true
            for (i in 1 until node.size) {
                if (containsComplexNode(node[i])) return true
            }
        }
        return false
    }

    private fun astMentionsVar(node: ASTNode?, varName: String): Boolean {
        if (node == null) return false
        if (node is SymbolNode && node.toString() == varName) return true
        if (node is FunctionNode) {
            for (i in 1 until node.size) {
                if (astMentionsVar(node[i], varName)) return true
            }
        }
        return false
    }

    private fun multiplyMatrices(a: EvalVal.Mat, b: EvalVal.Mat): EvalVal.Mat? {
        if (a.cols != b.rows) return null
        val out = DoubleArray(a.rows * b.cols)
        for (i in 0 until a.rows) {
            for (j in 0 until b.cols) {
                var sum = 0.0
                for (k in 0 until a.cols) {
                    sum += a.data[i * a.cols + k] * b.data[k * b.cols + j]
                }
                out[i * b.cols + j] = sum
            }
        }
        return EvalVal.Mat(a.rows, b.cols, out)
    }

    private fun identityMatrix(n: Int): EvalVal.Mat {
        val out = DoubleArray(n * n) { idx ->
            val r = idx / n
            val c = idx % n
            if (r == c) 1.0 else 0.0
        }
        return EvalVal.Mat(n, n, out)
    }

    private fun invertMatrix(mat: EvalVal.Mat): EvalVal.Mat? {
        if (mat.rows != mat.cols || mat.rows < 1) return null
        val n = mat.rows
        val aug = Array(n) { r ->
            DoubleArray(2 * n) { c ->
                if (c < n) mat.data[r * n + c]
                else if (c - n == r) 1.0
                else 0.0
            }
        }
        for (i in 0 until n) {
            var pivot = i
            var maxVal = abs(aug[i][i])
            for (r in (i + 1) until n) {
                val v = abs(aug[r][i])
                if (v > maxVal) {
                    maxVal = v
                    pivot = r
                }
            }
            if (maxVal < 1e-11) return null
            if (pivot != i) {
                val tmp = aug[i]
                aug[i] = aug[pivot]
                aug[pivot] = tmp
            }
            val pivVal = aug[i][i]
            for (c in 0 until 2 * n) {
                aug[i][c] /= pivVal
            }
            for (r in 0 until n) {
                if (r != i) {
                    val factor = aug[r][i]
                    for (c in 0 until 2 * n) {
                        aug[r][c] -= factor * aug[i][c]
                    }
                }
            }
        }
        val invData = DoubleArray(n * n)
        for (r in 0 until n) {
            for (c in 0 until n) {
                val v = aug[r][c + n]
                if (v.isNaN() || v.isInfinite()) return null
                invData[r * n + c] = v
            }
        }
        return EvalVal.Mat(n, n, invData)
    }

    private fun solveLinearMatrixEquation(
        exprWithVar: ASTNode?,
        targetMat: EvalVal.Mat,
        varName: String,
        evaluator: DoubleEvaluator
    ): EvalVal.Mat? {
        if (exprWithVar == null) return null
        if (exprWithVar is SymbolNode && exprWithVar.toString() == varName) {
            return targetMat
        }
        if (exprWithVar is FunctionNode && exprWithVar.isNotEmpty()) {
            val head = exprWithVar[0].toString()
            when {
                head == "Plus" && exprWithVar.size == 3 -> {
                    val c1 = exprWithVar[1]
                    val c2 = exprWithVar[2]
                    if (!astMentionsVar(c1, varName)) {
                        val a = evaluateMatrixOrScalarAst(c1, evaluator) as? EvalVal.Mat ?: return null
                        if (a.rows != targetMat.rows || a.cols != targetMat.cols) return null
                        val nextTarget = EvalVal.Mat(targetMat.rows, targetMat.cols, DoubleArray(targetMat.data.size) { targetMat.data[it] - a.data[it] })
                        return solveLinearMatrixEquation(c2, nextTarget, varName, evaluator)
                    }
                    if (!astMentionsVar(c2, varName)) {
                        val a = evaluateMatrixOrScalarAst(c2, evaluator) as? EvalVal.Mat ?: return null
                        if (a.rows != targetMat.rows || a.cols != targetMat.cols) return null
                        val nextTarget = EvalVal.Mat(targetMat.rows, targetMat.cols, DoubleArray(targetMat.data.size) { targetMat.data[it] - a.data[it] })
                        return solveLinearMatrixEquation(c1, nextTarget, varName, evaluator)
                    }
                }
                head == "Subtract" && exprWithVar.size == 3 -> {
                    val c1 = exprWithVar[1]
                    val c2 = exprWithVar[2]
                    if (!astMentionsVar(c2, varName)) {
                        val a = evaluateMatrixOrScalarAst(c2, evaluator) as? EvalVal.Mat ?: return null
                        if (a.rows != targetMat.rows || a.cols != targetMat.cols) return null
                        val nextTarget = EvalVal.Mat(targetMat.rows, targetMat.cols, DoubleArray(targetMat.data.size) { targetMat.data[it] + a.data[it] })
                        return solveLinearMatrixEquation(c1, nextTarget, varName, evaluator)
                    }
                    if (!astMentionsVar(c1, varName)) {
                        val a = evaluateMatrixOrScalarAst(c1, evaluator) as? EvalVal.Mat ?: return null
                        if (a.rows != targetMat.rows || a.cols != targetMat.cols) return null
                        val nextTarget = EvalVal.Mat(targetMat.rows, targetMat.cols, DoubleArray(targetMat.data.size) { a.data[it] - targetMat.data[it] })
                        return solveLinearMatrixEquation(c2, nextTarget, varName, evaluator)
                    }
                }
                head == "Times" && exprWithVar.size == 3 -> {
                    val c1 = exprWithVar[1]
                    val c2 = exprWithVar[2]
                    if (!astMentionsVar(c1, varName)) {
                        val k = evaluateMatrixOrScalarAst(c1, evaluator) ?: return null
                        if (k is EvalVal.Scalar && abs(k.value) > 1e-11) {
                            val nextTarget = EvalVal.Mat(targetMat.rows, targetMat.cols, DoubleArray(targetMat.data.size) { targetMat.data[it] / k.value })
                            return solveLinearMatrixEquation(c2, nextTarget, varName, evaluator)
                        }
                        if (k is EvalVal.Mat) {
                            val kInv = invertMatrix(k) ?: return null
                            val nextTarget = multiplyMatrices(kInv, targetMat) ?: return null
                            return solveLinearMatrixEquation(c2, nextTarget, varName, evaluator)
                        }
                    }
                    if (!astMentionsVar(c2, varName)) {
                        val k = evaluateMatrixOrScalarAst(c2, evaluator) ?: return null
                        if (k is EvalVal.Scalar && abs(k.value) > 1e-11) {
                            val nextTarget = EvalVal.Mat(targetMat.rows, targetMat.cols, DoubleArray(targetMat.data.size) { targetMat.data[it] / k.value })
                            return solveLinearMatrixEquation(c1, nextTarget, varName, evaluator)
                        }
                        if (k is EvalVal.Mat) {
                            val kInv = invertMatrix(k) ?: return null
                            val nextTarget = multiplyMatrices(targetMat, kInv) ?: return null
                            return solveLinearMatrixEquation(c1, nextTarget, varName, evaluator)
                        }
                    }
                }
            }
        }
        return null
    }

    private fun evaluateMatrixOrScalarAst(node: ASTNode?, evaluator: DoubleEvaluator): EvalVal? {
        if (node == null) return null
        if (node is SymbolNode && node.toString() == "i") {
            return EvalVal.ComplexVal(0.0, 1.0)
        }
        if (node is FunctionNode && node.isNotEmpty()) {
            val head = node[0].toString()
            val headLower = head.lowercase()
            when {
                headLower == "matrix" && node.size >= 3 -> {
                    val rVal = try { evaluator.evaluateNode(node[1]) } catch (_: Throwable) { Double.NaN }
                    val cVal = try { evaluator.evaluateNode(node[2]) } catch (_: Throwable) { Double.NaN }
                    if (rVal.isNaN() || cVal.isNaN()) return null
                    val r = rVal.roundToInt()
                    val c = cVal.roundToInt()
                    if (r < 1 || c < 1 || node.size < 3 + r * c) return null
                    val cells = DoubleArray(r * c)
                    for (i in 0 until r * c) {
                        val cellEval = evaluateMatrixOrScalarAst(node[3 + i], evaluator) as? EvalVal.Scalar ?: return null
                        if (cellEval.value.isNaN() || cellEval.value.isInfinite()) return null
                        cells[i] = cellEval.value
                    }
                    return EvalVal.Mat(r, c, cells)
                }
                headLower == "det" -> {
                    if (node.size == 2) {
                        val inner = evaluateMatrixOrScalarAst(node[1], evaluator)
                        if (inner is EvalVal.Mat) {
                            if (inner.rows != inner.cols || inner.rows < 1) return null
                            return EvalVal.Scalar(computeDeterminant(inner.rows, inner.data.toList()))
                        }
                    } else if (node.size >= 3) {
                        val r = try { evaluator.evaluateNode(node[1]).roundToInt() } catch (_: Throwable) { -1 }
                        val c = try { evaluator.evaluateNode(node[2]).roundToInt() } catch (_: Throwable) { -1 }
                        if (r == c && r >= 1 && node.size >= 3 + r * c) {
                            val cells = ArrayList<Double>(r * c)
                            for (i in 0 until r * c) {
                                val s = evaluateMatrixOrScalarAst(node[3 + i], evaluator) as? EvalVal.Scalar ?: return null
                                cells.add(s.value)
                            }
                            return EvalVal.Scalar(computeDeterminant(r, cells))
                        }
                    }
                    return null
                }
                headLower == "conj" && node.size >= 2 -> {
                    val inner = evaluateMatrixOrScalarAst(node[1], evaluator) ?: return null
                    return when (inner) {
                        is EvalVal.ComplexVal -> EvalVal.ComplexVal(inner.re, -inner.im)
                        is EvalVal.Scalar -> EvalVal.Scalar(inner.value)
                        else -> null
                    }
                }
                headLower == "abs" && node.size >= 2 -> {
                    val inner = evaluateMatrixOrScalarAst(node[1], evaluator) ?: return null
                    return when (inner) {
                        is EvalVal.ComplexVal -> EvalVal.Scalar(sqrt(inner.re * inner.re + inner.im * inner.im))
                        is EvalVal.Scalar -> EvalVal.Scalar(abs(inner.value))
                        else -> null
                    }
                }
                headLower == "sqrt" && node.size >= 2 -> {
                    val inner = evaluateMatrixOrScalarAst(node[1], evaluator) ?: return null
                    return when (inner) {
                        is EvalVal.Scalar -> {
                            if (inner.value >= 0.0) EvalVal.Scalar(sqrt(inner.value))
                            else EvalVal.ComplexVal(0.0, sqrt(-inner.value))
                        }
                        is EvalVal.ComplexVal -> {
                            val r = sqrt(inner.re * inner.re + inner.im * inner.im)
                            val re = sqrt(maxOf(0.0, (r + inner.re) * 0.5))
                            val im = (if (inner.im < 0.0) -1.0 else 1.0) * sqrt(maxOf(0.0, (r - inner.re) * 0.5))
                            EvalVal.ComplexVal(re, im)
                        }
                        else -> null
                    }
                }
                head == "Plus" && node.size >= 2 -> {
                    var acc = evaluateMatrixOrScalarAst(node[1], evaluator) ?: return null
                    for (i in 2 until node.size) {
                        val next = evaluateMatrixOrScalarAst(node[i], evaluator) ?: return null
                        acc = when {
                            acc is EvalVal.Scalar && next is EvalVal.Scalar -> EvalVal.Scalar((acc as EvalVal.Scalar).value + next.value)
                            acc is EvalVal.Mat && next is EvalVal.Mat -> {
                                val m1 = acc as EvalVal.Mat
                                if (m1.rows != next.rows || m1.cols != next.cols) return null
                                EvalVal.Mat(m1.rows, m1.cols, DoubleArray(m1.data.size) { m1.data[it] + next.data[it] })
                            }
                            (acc is EvalVal.ComplexVal || next is EvalVal.ComplexVal) && acc !is EvalVal.Mat && next !is EvalVal.Mat -> {
                                val c1 = toComplex(acc) ?: return null
                                val c2 = toComplex(next) ?: return null
                                EvalVal.ComplexVal(c1.re + c2.re, c1.im + c2.im)
                            }
                            else -> return null
                        }
                    }
                    return acc
                }
                head == "Subtract" && node.size == 3 -> {
                    val left = evaluateMatrixOrScalarAst(node[1], evaluator) ?: return null
                    val right = evaluateMatrixOrScalarAst(node[2], evaluator) ?: return null
                    return when {
                        left is EvalVal.Scalar && right is EvalVal.Scalar -> EvalVal.Scalar(left.value - right.value)
                        left is EvalVal.Mat && right is EvalVal.Mat -> {
                            if (left.rows != right.rows || left.cols != right.cols) return null
                            EvalVal.Mat(left.rows, left.cols, DoubleArray(left.data.size) { left.data[it] - right.data[it] })
                        }
                        (left is EvalVal.ComplexVal || right is EvalVal.ComplexVal) && left !is EvalVal.Mat && right !is EvalVal.Mat -> {
                            val c1 = toComplex(left) ?: return null
                            val c2 = toComplex(right) ?: return null
                            EvalVal.ComplexVal(c1.re - c2.re, c1.im - c2.im)
                        }
                        else -> null
                    }
                }
                head == "Times" && node.size >= 2 -> {
                    var acc = evaluateMatrixOrScalarAst(node[1], evaluator) ?: return null
                    for (i in 2 until node.size) {
                        val next = evaluateMatrixOrScalarAst(node[i], evaluator) ?: return null
                        acc = when {
                            acc is EvalVal.Scalar && next is EvalVal.Scalar -> EvalVal.Scalar((acc as EvalVal.Scalar).value * next.value)
                            acc is EvalVal.Scalar && next is EvalVal.Mat -> {
                                val s = (acc as EvalVal.Scalar).value
                                EvalVal.Mat(next.rows, next.cols, DoubleArray(next.data.size) { next.data[it] * s })
                            }
                            acc is EvalVal.Mat && next is EvalVal.Scalar -> {
                                val m = acc as EvalVal.Mat
                                val s = next.value
                                EvalVal.Mat(m.rows, m.cols, DoubleArray(m.data.size) { m.data[it] * s })
                            }
                            acc is EvalVal.Mat && next is EvalVal.Mat -> {
                                multiplyMatrices(acc as EvalVal.Mat, next) ?: return null
                            }
                            (acc is EvalVal.ComplexVal || next is EvalVal.ComplexVal) && acc !is EvalVal.Mat && next !is EvalVal.Mat -> {
                                val c1 = toComplex(acc) ?: return null
                                val c2 = toComplex(next) ?: return null
                                multiplyComplex(c1, c2)
                            }
                            else -> return null
                        }
                    }
                    return acc
                }
                head == "Divide" && node.size == 3 -> {
                    val left = evaluateMatrixOrScalarAst(node[1], evaluator) ?: return null
                    val right = evaluateMatrixOrScalarAst(node[2], evaluator) ?: return null
                    return when {
                        left is EvalVal.Scalar && right is EvalVal.Scalar -> {
                            if (abs(right.value) < 1e-11) null else EvalVal.Scalar(left.value / right.value)
                        }
                        left is EvalVal.Mat && right is EvalVal.Scalar -> {
                            if (abs(right.value) < 1e-11) null
                            else EvalVal.Mat(left.rows, left.cols, DoubleArray(left.data.size) { left.data[it] / right.value })
                        }
                        (left is EvalVal.ComplexVal || right is EvalVal.ComplexVal) && left !is EvalVal.Mat && right !is EvalVal.Mat -> {
                            val c1 = toComplex(left) ?: return null
                            val c2 = toComplex(right) ?: return null
                            divideComplex(c1, c2)
                        }
                        else -> null
                    }
                }
                head == "Power" && node.size == 3 -> {
                    val base = evaluateMatrixOrScalarAst(node[1], evaluator) ?: return null
                    val exp = evaluateMatrixOrScalarAst(node[2], evaluator) ?: return null
                    return when {
                        base is EvalVal.Scalar && exp is EvalVal.Scalar -> {
                            val v = base.value.pow(exp.value)
                            if (v.isNaN() || v.isInfinite()) null else EvalVal.Scalar(v)
                        }
                        base is EvalVal.Mat && exp is EvalVal.Scalar -> {
                            if (base.rows != base.cols) return null
                            val n = exp.value.roundToInt()
                            if (abs(exp.value - n) > 1e-9) return null
                            if (n == 0) return identityMatrix(base.rows)
                            val unitMat = if (n > 0) base else invertMatrix(base) ?: return null
                            val steps = abs(n)
                            var res: EvalVal.Mat = unitMat
                            for (p in 1 until steps) {
                                res = multiplyMatrices(res, unitMat) ?: return null
                            }
                            res
                        }
                        base is EvalVal.ComplexVal && exp is EvalVal.Scalar -> {
                            val n = exp.value.roundToInt()
                            if (abs(exp.value - n) > 1e-9) return null
                            if (n == 0) return EvalVal.ComplexVal(1.0, 0.0)
                            val steps = abs(n)
                            var res: EvalVal.ComplexVal = base
                            for (p in 1 until steps) {
                                res = multiplyComplex(res, base)
                            }
                            if (n > 0) res else divideComplex(EvalVal.ComplexVal(1.0, 0.0), res)
                        }
                        else -> null
                    }
                }
            }
        }
        return try {
            val v = evaluator.evaluateNode(node)
            if (v.isNaN() || v.isInfinite()) null else EvalVal.Scalar(v)
        } catch (_: Throwable) {
            null
        }
    }

    fun evaluateAnswer(formula: String, variableValues: Map<String, Double> = emptyMap()): String {
        val trimmed = formula.trim()
        if (trimmed.isEmpty()) return ""

        return try {
            val boundVars = collectBoundVariablesInFormula(trimmed)
            val normalized = com.uncode.app.math.ui.MathFormatter.toParseableString(trimmed)

            val rootAst = try {
                Parser(true).parse(normalized)
            } catch (_: Throwable) {
                null
            }
            if (rootAst is FunctionNode && rootAst.isNotEmpty()) {
                val h = rootAst[0].toString().lowercase()
                if (h == "sys" && rootAst.size >= 2) {
                    return solveSystemAst(rootAst, boundVars, variableValues)
                }
                if (h == "deg" && rootAst.size >= 2) {
                    val eval = createEvaluator(boundVars)
                    return "${formatDouble(eval.evaluateNode(rootAst[1]))}°"
                }
                if (h == "dms2" && rootAst.size >= 3) {
                    val eval = createEvaluator(boundVars)
                    val degVal = eval.evaluateNode(rootAst[1]) + eval.evaluateNode(rootAst[2]) / 60.0
                    return "${formatDouble(degVal)}°"
                }
                if (h == "dms3" && rootAst.size >= 4) {
                    val eval = createEvaluator(boundVars)
                    val degVal = eval.evaluateNode(rootAst[1]) + eval.evaluateNode(rootAst[2]) / 60.0 + eval.evaluateNode(rootAst[3]) / 3600.0
                    return "${formatDouble(degVal)}°"
                }
            }

            if (rootAst != null && SymbolicCalculus.containsSymbolicCalculusNode(rootAst)) {
                val boundCalcSpecified = boundVars.any { variableValues.containsKey(it) }
                if (!boundCalcSpecified) {
                    if (normalized.contains("==")) {
                        val parts = normalized.split("==")
                        if (parts.size == 2) {
                            val leftStr = parts[0].trim()
                            val rightStr = parts[1].trim()
                            val leftAst = try { Parser(true).parse(leftStr) } catch (_: Throwable) { null }
                            val rightAst = try { Parser(true).parse(rightStr) } catch (_: Throwable) { null }
                            if (rightAst is SymbolNode && leftAst != null && SymbolicCalculus.containsSymbolicCalculusNode(leftAst)) {
                                val symAns = SymbolicCalculus.evaluateSymbolicAst(leftAst)
                                if (symAns != null) return "${rightAst} = $symAns"
                            }
                            if (leftAst is SymbolNode && rightAst != null && SymbolicCalculus.containsSymbolicCalculusNode(rightAst)) {
                                val symAns = SymbolicCalculus.evaluateSymbolicAst(rightAst)
                                if (symAns != null) return "${leftAst} = $symAns"
                            }
                            if (leftAst != null && rightAst != null) {
                                val symLeft = if (SymbolicCalculus.containsSymbolicCalculusNode(leftAst)) SymbolicCalculus.evaluateSymbolicAst(leftAst)?.removeSuffix(" + C") else leftStr
                                val symRight = if (SymbolicCalculus.containsSymbolicCalculusNode(rightAst)) SymbolicCalculus.evaluateSymbolicAst(rightAst)?.removeSuffix(" + C") else rightStr
                                if (symLeft != null && symRight != null && (symLeft != leftStr || symRight != rightStr)) {
                                    return evaluateAnswer("$symLeft == $symRight", variableValues)
                                }
                            }
                        }
                    } else {
                        val symAns = SymbolicCalculus.evaluateSymbolicAst(rootAst)
                        if (symAns != null) return symAns
                    }
                }
            }

            if (rootAst != null && (containsMatrixNode(rootAst) || containsComplexNode(rootAst))) {
                val evaluator = createEvaluator(boundVars)
                for ((k, v) in variableValues) evaluator.defineVariable(k, v)

                if (normalized.contains("==")) {
                    val parts = normalized.split("==")
                    if (parts.size == 2) {
                        val leftStr = parts[0].trim()
                        val rightStr = parts[1].trim()
                        val leftAst = try { Parser(true).parse(leftStr) } catch (_: Throwable) { null }
                        val rightAst = try { Parser(true).parse(rightStr) } catch (_: Throwable) { null }

                        val leftVars = mutableSetOf<String>()
                        val rightVars = mutableSetOf<String>()
                        DoubleEvaluator.getVariables(leftStr, leftVars)
                        DoubleEvaluator.getVariables(rightStr, rightVars)
                        val leftFree = leftVars.filter { !isConstantSymbol(it) && !isBuiltInFunction(it) && it !in boundVars }
                        val rightFree = rightVars.filter { !isConstantSymbol(it) && !isBuiltInFunction(it) && it !in boundVars }
                        val allVars = (leftFree + rightFree).distinct()

                        if (allVars.isEmpty()) {
                            val lEval = evaluateMatrixOrScalarAst(leftAst, evaluator) ?: return "Undefined"
                            val rEval = evaluateMatrixOrScalarAst(rightAst, evaluator) ?: return "Undefined"
                            return when {
                                lEval is EvalVal.Mat && rEval is EvalVal.Mat -> {
                                    val eq = lEval.rows == rEval.rows && lEval.cols == rEval.cols &&
                                        lEval.data.indices.all { abs(lEval.data[it] - rEval.data[it]) < 1e-9 }
                                    if (eq) "True" else "False"
                                }
                                lEval is EvalVal.Scalar && rEval is EvalVal.Scalar -> {
                                    if (abs(lEval.value - rEval.value) < 1e-9) "True" else "False"
                                }
                                (lEval is EvalVal.ComplexVal || rEval is EvalVal.ComplexVal) && lEval !is EvalVal.Mat && rEval !is EvalVal.Mat -> {
                                    val c1 = toComplex(lEval) ?: return "Undefined"
                                    val c2 = toComplex(rEval) ?: return "Undefined"
                                    if (abs(c1.re - c2.re) < 1e-9 && abs(c1.im - c2.im) < 1e-9) "True" else "False"
                                }
                                else -> "Undefined"
                            }
                        } else if (allVars.size == 1) {
                            val varName = allVars.first()
                            if (rightStr == varName && leftFree.isEmpty()) {
                                val lEval = evaluateMatrixOrScalarAst(leftAst, evaluator) ?: return "Undefined"
                                return "$varName = ${formatEvalVal(lEval)}"
                            }
                            if (leftStr == varName && rightFree.isEmpty()) {
                                val rEval = evaluateMatrixOrScalarAst(rightAst, evaluator) ?: return "Undefined"
                                return "$varName = ${formatEvalVal(rEval)}"
                            }
                            if (rightFree.isEmpty() && leftFree.size == 1) {
                                val rEval = evaluateMatrixOrScalarAst(rightAst, evaluator)
                                if (rEval is EvalVal.Mat) {
                                    val solvedMat = solveLinearMatrixEquation(leftAst, rEval, varName, evaluator)
                                    if (solvedMat != null) return "$varName = ${formatMatrixVal(solvedMat)}"
                                }
                            }
                            if (leftFree.isEmpty() && rightFree.size == 1) {
                                val lEval = evaluateMatrixOrScalarAst(leftAst, evaluator)
                                if (lEval is EvalVal.Mat) {
                                    val solvedMat = solveLinearMatrixEquation(rightAst, lEval, varName, evaluator)
                                    if (solvedMat != null) return "$varName = ${formatMatrixVal(solvedMat)}"
                                }
                            }
                            for (cand in -50..50) {
                                evaluator.defineVariable(varName, cand.toDouble())
                                val lEval = evaluateMatrixOrScalarAst(leftAst, evaluator)
                                val rEval = evaluateMatrixOrScalarAst(rightAst, evaluator)
                                if (lEval is EvalVal.Mat && rEval is EvalVal.Mat &&
                                    lEval.rows == rEval.rows && lEval.cols == rEval.cols &&
                                    lEval.data.indices.all { abs(lEval.data[it] - rEval.data[it]) < 1e-7 }
                                ) {
                                    return "$varName = $cand"
                                }
                                if ((lEval is EvalVal.ComplexVal || rEval is EvalVal.ComplexVal) && lEval !is EvalVal.Mat && rEval !is EvalVal.Mat) {
                                    val c1 = toComplex(lEval ?: continue) ?: continue
                                    val c2 = toComplex(rEval ?: continue) ?: continue
                                    if (abs(c1.re - c2.re) < 1e-7 && abs(c1.im - c2.im) < 1e-7) {
                                        return "$varName = $cand"
                                    }
                                }
                            }
                            return "No Solution"
                        }
                    }
                } else {
                    val vars = mutableSetOf<String>()
                    DoubleEvaluator.getVariables(normalized, vars)
                    val freeVars = vars.filter { !isConstantSymbol(it) && !isBuiltInFunction(it) && it !in boundVars && it !in variableValues }
                    if (freeVars.isNotEmpty()) return ""
                    val evaluated = evaluateMatrixOrScalarAst(rootAst, evaluator) ?: return "Undefined"
                    return formatEvalVal(evaluated)
                }
            }

            if (normalized.contains("==")) {
                val parts = normalized.split("==")
                if (parts.size == 2) {
                    val leftStr = parts[0].trim()
                    val rightStr = parts[1].trim()
                    val evaluator = createEvaluator(boundVars)

                    val leftVars = mutableSetOf<String>()
                    val rightVars = mutableSetOf<String>()
                    DoubleEvaluator.getVariables(leftStr, leftVars)
                    DoubleEvaluator.getVariables(rightStr, rightVars)
                    val allVars = (leftVars + rightVars).filter { !isConstantSymbol(it) && !isBuiltInFunction(it) && it !in boundVars }

                    if (allVars.isEmpty()) {
                        val lVal = evaluator.evaluate(leftStr)
                        val rVal = evaluator.evaluate(rightStr)
                        if (lVal.isNaN() || rVal.isNaN() || lVal.isInfinite() || rVal.isInfinite()) {
                            return "Undefined"
                        }
                        val isEq = abs(lVal - rVal) < 1e-9
                        return if (isEq) "True" else "False"
                    } else if (allVars.size == 1) {
                        val varName = allVars.first()
                        val roots = solveSingleVariableRoots(leftStr, rightStr, varName, boundVars)
                        if (roots.isNotEmpty()) {
                            return "$varName = ${roots.joinToString(", ") { formatDouble(it) }}"
                        }
                        if (variableValues.containsKey(varName)) {
                            return "$varName = ${formatDouble(variableValues[varName]!!)}"
                        }
                        return "No Solution"
                    } else {
                        if (variableValues.isNotEmpty()) {
                            return allVars.mapNotNull { v ->
                                variableValues[v]?.let { "$v = ${formatDouble(it)}" }
                            }.joinToString(", ")
                        }
                        val (expVars, denomVars) = analyzeVariableRoles(normalized)
                        val solved = solveDistinctMultiVarEquation(leftStr, rightStr, allVars, expVars, denomVars, boundVars)
                        if (solved != null) {
                            return allVars.mapNotNull { v ->
                                solved[v]?.let { "$v = ${formatDouble(it)}" }
                            }.joinToString(", ")
                        }
                    }
                }
            }

            val evaluator = createEvaluator(boundVars)

            val vars = mutableSetOf<String>()
            DoubleEvaluator.getVariables(normalized, vars)
            val freeVars = vars.filter { !isConstantSymbol(it) && !isBuiltInFunction(it) && it !in boundVars }
            if (freeVars.isNotEmpty()) {
                if (variableValues.isNotEmpty()) {
                    return freeVars.mapNotNull { v ->
                        variableValues[v]?.let { "$v = ${formatDouble(it)}" }
                    }.joinToString(", ")
                }
                return ""
            }

            if (rootAst is FunctionNode && rootAst.size == 3 &&
                rootAst[0].toString() in setOf("Less", "Greater", "LessEqual", "GreaterEqual", "Unequal") &&
                freeVars.isEmpty()
            ) {
                return if (evaluator.evaluateNodeLogical(rootAst)) "True" else "False"
            }

            val isLimitExpr = rootAst is FunctionNode && rootAst.isNotEmpty() &&
                rootAst[0].toString().lowercase() in setOf("lim", "limp", "limm")
            if (!isLimitExpr && rootAst != null && hasZeroDenominator(rootAst, evaluator)) {
                return "Undefined"
            }

            val value = evaluator.evaluate(normalized)
            if (value.isNaN() || (!isLimitExpr && value.isInfinite())) {
                return "Undefined"
            }
            formatDouble(value)
        } catch (e: Throwable) {
            ""
        }
    }

    private fun hasZeroDenominator(node: ASTNode, evaluator: DoubleEvaluator): Boolean {
        if (node is FunctionNode && node.isNotEmpty()) {
            val h = node[0].toString()
            if (h == "Divide" && node.size >= 3) {
                val den = try { evaluator.evaluateNode(node[2]) } catch (_: Throwable) { Double.NaN }
                if (den.isNaN() || abs(den) < 1e-11) return true
            } else if (h == "Power" && node.size == 3) {
                val exp = try { evaluator.evaluateNode(node[2]) } catch (_: Throwable) { 1.0 }
                if (exp < 0.0) {
                    val base = try { evaluator.evaluateNode(node[1]) } catch (_: Throwable) { Double.NaN }
                    if (base.isNaN() || abs(base) < 1e-11) return true
                }
            }
            for (i in 1 until node.size) {
                if (hasZeroDenominator(node[i], evaluator)) return true
            }
        }
        return false
    }

    private fun stripLeadingVarAssignment(s: String): String {
        val parts = s.split(Regex("""==|="""))
        if (parts.size == 2 && parts[0].trim().matches(Regex("""[a-zA-Zα-ωΑ-Ω][a-zA-Z0-9]*"""))) {
            return parts[1].trim()
        }
        return s.trim()
    }

    private fun stripIntegrationConstant(s: String): String {
        return s.replace(Regex("""\s*[+\-]\s*[cC]\s*$"""), "").trim()
    }

    fun areMathematicallyEquivalent(
        userInput: String,
        expectedSolution: String,
        tolerance: Double = 1e-4
    ): Boolean {
        val uTrim = userInput.trim()
        val eTrim = expectedSolution.trim()
        if (uTrim.isEmpty() || eTrim.isEmpty()) return false
        if (uTrim.equals(eTrim, ignoreCase = true)) return true

        val undefTokens = setOf("undefined", "dne", "does not exist", "nan")
        val noSolTokens = setOf("no solution", "no solutions", "none", "false", "∅")
        val uLower = uTrim.lowercase()
        val eLower = eTrim.lowercase()
        if (uLower in undefTokens && eLower in undefTokens) return true
        if (uLower in noSolTokens && eLower in noSolTokens) return true

        val uBody = stripIntegrationConstant(stripLeadingVarAssignment(uTrim))
        val eBody = stripIntegrationConstant(stripLeadingVarAssignment(eTrim))

        try {
            val uExpr = com.uncode.app.math.ui.MathFormatter.toParseableString(uBody)
            val eExpr = com.uncode.app.math.ui.MathFormatter.toParseableString(eBody)
            val uAst = Parser(true).parse(uExpr)
            val eAst = Parser(true).parse(eExpr)
            if (containsMatrixNode(uAst) || containsMatrixNode(eAst) || containsComplexNode(uAst) || containsComplexNode(eAst)) {
                val eval = createEvaluator()
                val uVal = evaluateMatrixOrScalarAst(uAst, eval)
                val eVal = evaluateMatrixOrScalarAst(eAst, eval)
                if (uVal is EvalVal.Mat && eVal is EvalVal.Mat) {
                    return uVal.rows == eVal.rows && uVal.cols == eVal.cols &&
                        uVal.data.indices.all { idx -> abs(uVal.data[idx] - eVal.data[idx]) <= tolerance }
                }
                if ((uVal is EvalVal.ComplexVal || eVal is EvalVal.ComplexVal) && uVal != null && eVal != null && uVal !is EvalVal.Mat && eVal !is EvalVal.Mat) {
                    val c1 = toComplex(uVal) ?: return false
                    val c2 = toComplex(eVal) ?: return false
                    return abs(c1.re - c2.re) <= tolerance && abs(c1.im - c2.im) <= tolerance
                }
                return false
            }
        } catch (_: Throwable) {}

        val uAssigns = parseAssignmentsMap(uTrim)
        val eAssigns = parseAssignmentsMap(eTrim)
        if (uAssigns != null && eAssigns != null && uAssigns.size >= 2 && uAssigns.keys == eAssigns.keys) {
            return uAssigns.all { (k, uVal) ->
                val eVal = eAssigns[k] ?: return@all false
                abs(uVal - eVal) <= tolerance
            }
        }

        val uScalarSet = parseScalarSolutionList(uTrim)
        val eScalarSet = parseScalarSolutionList(eTrim)
        if (uScalarSet != null && eScalarSet != null && uScalarSet.size == eScalarSet.size && uScalarSet.isNotEmpty()) {
            val uSorted = uScalarSet.sorted()
            val eSorted = eScalarSet.sorted()
            return uSorted.indices.all { idx -> abs(uSorted[idx] - eSorted[idx]) <= tolerance }
        }

        return areExpressionsNumericallyEquivalent(uBody, eBody, tolerance)
    }

    private fun parseAssignmentsMap(input: String): Map<String, Double>? {
        val parts = input.split(Regex("""[,;]""")).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        val map = LinkedHashMap<String, Double>()
        val eval = createEvaluator()
        for (p in parts) {
            val eqParts = p.split(Regex("""==|=""")).map { it.trim() }
            if (eqParts.size != 2) return null
            val varName = eqParts[0]
            if (!varName.matches(Regex("""[a-zA-Zα-ωΑ-Ω][a-zA-Z0-9]*"""))) return null
            val normVal = com.uncode.app.math.ui.MathFormatter.toParseableString(eqParts[1])
            val d = try { eval.evaluate(normVal) } catch (_: Throwable) { return null }
            if (d.isNaN() || d.isInfinite()) return null
            map[varName] = d
        }
        return map
    }

    private fun parseScalarSolutionList(input: String): List<Double>? {
        val stripped = if (input.contains("=")) {
            val eqSides = input.split(Regex("""==|="""))
            if (eqSides.size == 2 && eqSides[0].trim().matches(Regex("""[a-zA-Zα-ωΑ-Ω][a-zA-Z0-9]*"""))) {
                eqSides[1].trim()
            } else {
                return null
            }
        } else {
            input
        }
        val tokens = stripped.removeSurrounding("{", "}").split(Regex("""[,;]""")).map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        val eval = createEvaluator()
        val values = ArrayList<Double>(tokens.size)
        for (tok in tokens) {
            val subTok = if (tok.contains("=")) tok.substringAfterLast("=").trim() else tok
            val norm = com.uncode.app.math.ui.MathFormatter.toParseableString(subTok)
            val vars = mutableSetOf<String>()
            try { DoubleEvaluator.getVariables(norm, vars) } catch (_: Throwable) { return null }
            val free = vars.filter { !isConstantSymbol(it) && !isBuiltInFunction(it) }
            if (free.isNotEmpty()) return null
            val v = try { eval.evaluate(norm) } catch (_: Throwable) { return null }
            if (v.isNaN() || v.isInfinite()) return null
            values.add(v)
        }
        return values
    }

    private fun areExpressionsNumericallyEquivalent(exprA: String, exprB: String, tolerance: Double): Boolean {
        return try {
            val normA = com.uncode.app.math.ui.MathFormatter.toParseableString(exprA)
            val normB = com.uncode.app.math.ui.MathFormatter.toParseableString(exprB)
            val boundVars = collectBoundVariablesInFormula(exprA) + collectBoundVariablesInFormula(exprB)
            val varsA = mutableSetOf<String>()
            val varsB = mutableSetOf<String>()
            DoubleEvaluator.getVariables(normA, varsA)
            DoubleEvaluator.getVariables(normB, varsB)
            val freeVars = (varsA + varsB).filter { !isConstantSymbol(it) && !isBuiltInFunction(it) && it !in boundVars }.distinct()
            val eval = createEvaluator(boundVars)
            if (freeVars.isEmpty()) {
                val valA = eval.evaluate(normA)
                val valB = eval.evaluate(normB)
                !valA.isNaN() && !valB.isNaN() && abs(valA - valB) <= tolerance
            } else {
                val sampleVectors = listOf(
                    listOf(2.1, 3.3, 1.7, 2.5),
                    listOf(-1.8, 2.4, -2.9, 1.3),
                    listOf(3.7, -1.4, 2.2, -1.6),
                    listOf(4.3, 1.9, 3.1, 2.8)
                )
                var validComparisons = 0
                for (vec in sampleVectors) {
                    for (idx in freeVars.indices) {
                        eval.defineVariable(freeVars[idx], vec[idx % vec.size])
                    }
                    val valA = try { eval.evaluate(normA) } catch (_: Throwable) { Double.NaN }
                    val valB = try { eval.evaluate(normB) } catch (_: Throwable) { Double.NaN }
                    if (!valA.isNaN() && !valB.isNaN() && !valA.isInfinite() && !valB.isInfinite()) {
                        validComparisons++
                        if (abs(valA - valB) > tolerance) return false
                    }
                }
                validComparisons >= 2
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun mutateSystemOfEquations(
        sysNode: FunctionNode,
        boundVarsInAst: Set<String>,
        records: MutableList<MutationRecord>
    ): MutationResult? {
        val eqCount = sysNode.size - 1
        if (eqCount < 1) return null
        val eqAsts = (1..eqCount).map { sysNode[it] }
        fun collectSysRowVars(n: ASTNode, out: MutableSet<String>) {
            when (n) {
                is FunctionNode -> {
                    if (n.isNotEmpty()) {
                        val h = n[0].toString().lowercase()
                        if (h == "power" && n.size == 3 && n[1].toString().equals("e", ignoreCase = true)) {
                            collectSysRowVars(n[2], out)
                        } else {
                            for (i in 1 until n.size) collectSysRowVars(n[i], out)
                        }
                    }
                }
                is SymbolNode -> {
                    val s = n.toString()
                    if (s.equals("e", ignoreCase = true) || (!isConstantSymbol(s) && !isBuiltInFunction(s) && s !in boundVarsInAst)) {
                        out.add(s)
                    }
                }
            }
        }
        val rowVarSets = eqAsts.map { eqAst ->
            val vSet = LinkedHashSet<String>()
            collectSysRowVars(eqAst, vSet)
            vSet
        }
        val allVars = rowVarSets.flatten().distinct()
        val systemUnknownPool = listOf("x", "y", "z", "w", "v", "u", "t", "s", "r", "q", "p", "n", "m", "k")
        val multiRowVars = allVars.filter { v -> rowVarSets.count { v in it } >= 2 }
            .sortedBy { v ->
                val idx = preferredUnknowns.indexOf(v)
                if (idx >= 0) idx else 100
            }
        val canonicalVars = allVars.filter { it in systemUnknownPool }
            .sortedBy { systemUnknownPool.indexOf(it) }
        val origUnknowns = when {
            multiRowVars.size >= eqCount -> multiRowVars.take(eqCount)
            (multiRowVars + canonicalVars).distinct().isNotEmpty() -> (multiRowVars + canonicalVars).distinct().take(eqCount)
            else -> allVars.take(eqCount)
        }
        val targetUnknowns = if (origUnknowns.isEmpty()) {
            systemUnknownPool.take(eqCount)
        } else {
            origUnknowns.mapIndexed { idx, orig ->
                if (orig in systemUnknownPool) orig else systemUnknownPool.getOrElse(idx) { orig }
            }
        }
        val origSysNorm = com.uncode.app.math.ui.MathFormatter.toParseableString(sysNode.toString()).replace(" ", "")
        val rootPool = listOf(1, 2, 3, 4, 5, -1, -2, -3, -4, 6, -5, 7, -6, 8)
        val coeffPool = listOf(2, 3, 4, 5, 6, -2, -3)

        for (trial in 0 until 60) {
            val chosenRoots = rootPool.shuffled(random).take(targetUnknowns.size).map { it.toDouble() }
            val targetVarMap = LinkedHashMap<String, Double>()
            for (idx in targetUnknowns.indices) {
                targetVarMap[targetUnknowns[idx]] = chosenRoots[idx]
            }
            val trialRecords = mutableListOf<MutationRecord>()
            val mutatedRows = mutableListOf<String>()
            var allRowsValid = true

            for (rowIdx in 0 until eqCount) {
                val eqAst = eqAsts[rowIdx]
                val isEq = eqAst is FunctionNode && eqAst.size >= 3 && eqAst[0].toString() == "Equal"
                val lhsAst = if (isEq) (eqAst as FunctionNode)[1] else eqAst
                val rhsAst = if (isEq) (eqAst as FunctionNode)[2] else null

                val rowSymMap = LinkedHashMap<String, String>()
                for (uIdx in origUnknowns.indices) {
                    val oVar = origUnknowns[uIdx]
                    val tVar = targetUnknowns[uIdx]
                    rowSymMap[oVar] = tVar
                }
                val usedRowConsts = mutableSetOf<Int>()
                for (v in rowVarSets[rowIdx]) {
                    if (v !in rowSymMap) {
                        val c = coeffPool.filter { it !in usedRowConsts }.randomOrNull(random) ?: random.nextInt(2, 7)
                        usedRowConsts.add(c)
                        rowSymMap[v] = if (c < 0) "($c)" else c.toString()
                        trialRecords.add(MutationRecord(v, c.toString(), MutationType.CONSTANT))
                    }
                }

                var mutLhs = sanitizeFormulaString(
                    mutateSubtree(
                        node = lhsAst,
                        symMap = rowSymMap,
                        targetVar = targetUnknowns.firstOrNull() ?: "x",
                        targetVal = chosenRoots.firstOrNull() ?: 2.0,
                        activeBound = boundVarsInAst,
                        records = trialRecords,
                        promoteLeafId = null,
                        counter = intArrayOf(0)
                    )
                )
                if (trial >= 15 && targetUnknowns.isNotEmpty()) {
                    val focusVar = targetUnknowns[rowIdx % targetUnknowns.size]
                    val scaleC = random.nextInt(2, 6)
                    if (Regex("""\b$focusVar\b""").containsMatchIn(mutLhs) && !Regex("""[0-9]\s*\*\s*$focusVar\b""").containsMatchIn(mutLhs)) {
                        mutLhs = mutLhs.replaceFirst(Regex("""\b$focusVar\b"""), "$scaleC * $focusVar")
                    }
                }

                val rhsVars = LinkedHashSet<String>()
                if (rhsAst != null) {
                    collectTokens(rhsAst, rhsVars, LinkedHashSet(), LinkedHashSet(), boundVarsInAst)
                }
                val rhsHasUnknown = rhsVars.any { it in origUnknowns }

                val eval = createEvaluator(boundVarsInAst)
                for ((k, v) in targetVarMap) eval.defineVariable(k, v)
                val lhsVal = try {
                    eval.evaluate(com.uncode.app.math.ui.MathFormatter.toParseableString(mutLhs))
                } catch (_: Throwable) {
                    Double.NaN
                }
                if (lhsVal.isNaN() || lhsVal.isInfinite() || abs(lhsVal - lhsVal.roundToLong()) > 1e-6 || abs(lhsVal) > 250.0) {
                    allRowsValid = false
                    break
                }
                val lhsInt = lhsVal.roundToLong()

                if (rhsAst != null && rhsHasUnknown) {
                    val mutRhsBase = sanitizeFormulaString(
                        mutateSubtree(
                            node = rhsAst,
                            symMap = rowSymMap,
                            targetVar = targetUnknowns.firstOrNull() ?: "x",
                            targetVal = chosenRoots.firstOrNull() ?: 2.0,
                            activeBound = boundVarsInAst,
                            records = trialRecords,
                            promoteLeafId = null,
                            counter = intArrayOf(0)
                        )
                    )
                    val rhsBaseVal = try {
                        eval.evaluate(com.uncode.app.math.ui.MathFormatter.toParseableString(mutRhsBase))
                    } catch (_: Throwable) {
                        Double.NaN
                    }
                    if (rhsBaseVal.isNaN() || rhsBaseVal.isInfinite() || abs(rhsBaseVal - rhsBaseVal.roundToLong()) > 1e-6) {
                        allRowsValid = false
                        break
                    }
                    val offset = lhsInt - rhsBaseVal.roundToLong()
                    val fullRhs = when {
                        offset == 0L -> mutRhsBase
                        offset > 0L -> "$mutRhsBase + $offset"
                        else -> "$mutRhsBase - ${abs(offset)}"
                    }
                    mutatedRows.add("$mutLhs == $fullRhs")
                } else {
                    mutatedRows.add("$mutLhs == $lhsInt")
                }
            }

            if (!allRowsValid || mutatedRows.size != eqCount) continue
            val candidateFormula = "sys(${mutatedRows.joinToString(";")})"
            val candNorm = com.uncode.app.math.ui.MathFormatter.toParseableString(candidateFormula).replace(" ", "")
            if (candNorm == origSysNorm && trial < 45) continue

            val candAst = try {
                Parser(true).parse(com.uncode.app.math.ui.MathFormatter.toParseableString(candidateFormula))
            } catch (_: Throwable) {
                null
            }
            if (candAst is FunctionNode) {
                val solvedStr = solveSystemAst(candAst, boundVarsInAst, emptyMap())
                val expectedStr = targetUnknowns.indices.joinToString(", ") { idx ->
                    "${targetUnknowns[idx]} = ${formatDouble(chosenRoots[idx])}"
                }
                if (solvedStr == expectedStr) {
                    records.addAll(trialRecords)
                    return MutationResult(
                        mutatedFormula = candidateFormula,
                        records = records,
                        variableValues = targetVarMap
                    )
                }
            }
        }
        return null
    }

    private fun astToInfixString(node: ASTNode): String {
        return when (node) {
            is FunctionNode -> {
                if (node.isEmpty()) ""
                else {
                    val h = node[0].toString()
                    val hLower = h.lowercase()
                    when {
                        h == "Equal" && node.size == 3 -> "${astToInfixString(node[1])} == ${astToInfixString(node[2])}"
                        h == "Plus" && node.size >= 2 -> (1 until node.size).joinToString(" + ") { astToInfixString(node[it]) }
                        h == "Subtract" && node.size == 3 -> "${astToInfixString(node[1])} - (${astToInfixString(node[2])})"
                        h == "Times" && node.size >= 2 -> (1 until node.size).joinToString(" * ") { " (${astToInfixString(node[it])}) " }
                        h == "Divide" && node.size == 3 -> "(${astToInfixString(node[1])})/(${astToInfixString(node[2])})"
                        h == "Power" && node.size == 3 -> "(${astToInfixString(node[1])})^(${astToInfixString(node[2])})"
                        (hLower == "matrix" || hLower == "det") && node.size >= 3 -> {
                            val r = astToInfixString(node[1])
                            val c = astToInfixString(node[2])
                            val cells = (3 until node.size).joinToString(";") { astToInfixString(node[it]) }
                            "$hLower($r,$c;$cells)"
                        }
                        hLower in setOf("root", "logb", "sub", "mixed", "perm", "comb", "binom", "fn1", "fn2", "dms2", "dms3", "lim", "limp", "limm", "derivv", "derivn", "intv", "defint", "sum") -> {
                            val args = (1 until node.size).joinToString(";") { astToInfixString(node[it]) }
                            "$hLower($args)"
                        }
                        else -> {
                            val args = (1 until node.size).joinToString(", ") { astToInfixString(node[it]) }
                            "$hLower($args)"
                        }
                    }
                }
            }
            else -> node.toString()
        }
    }

    private fun solveSystemAst(
        sysNode: FunctionNode,
        boundVars: Set<String>,
        variableValues: Map<String, Double>
    ): String {
        val eqNodes = (1 until sysNode.size).map { sysNode[it] }
        if (eqNodes.isEmpty()) return ""

        val hasAnyEqual = eqNodes.any { it is FunctionNode && it.size >= 3 && it[0].toString() == "Equal" }
        if (!hasAnyEqual) {
            val evaluatedRows = eqNodes.mapNotNull { rowNode ->
                val exprStr = astToInfixString(rowNode)
                val ans = evaluateAnswer(exprStr, variableValues)
                ans.takeIf { it.isNotEmpty() }
            }
            return when {
                evaluatedRows.isEmpty() -> ""
                evaluatedRows.size == 1 -> evaluatedRows[0]
                else -> "sys(${evaluatedRows.joinToString(";")})"
            }
        }

        val lhsNodes = ArrayList<ASTNode>(eqNodes.size)
        val rhsNodes = ArrayList<ASTNode>(eqNodes.size)
        for (eq in eqNodes) {
            if (eq is FunctionNode && eq.size >= 3 && eq[0].toString() == "Equal") {
                lhsNodes.add(eq[1])
                rhsNodes.add(eq[2])
            } else {
                lhsNodes.add(eq)
                rhsNodes.add(IntegerNode(0))
            }
        }

        val rawVars = LinkedHashSet<String>()
        collectTokens(sysNode, rawVars, LinkedHashSet(), LinkedHashSet(), boundVars)
        val systemOrder = listOf("x", "y", "z", "w", "v", "u", "t")
        val vars = rawVars.sortedWith(
            compareBy<String> {
                val idx = systemOrder.indexOf(it)
                if (idx >= 0) idx else 100
            }.thenBy {
                val idx2 = preferredUnknowns.indexOf(it)
                if (idx2 >= 0) idx2 else 200
            }.thenBy { it }
        )

        val m = eqNodes.size
        val n = vars.size
        val eval = createEvaluator(boundVars)

        if (n == 0) {
            val allTrue = (0 until m).all { i ->
                val diff = eval.evaluateNode(lhsNodes[i]) - eval.evaluateNode(rhsNodes[i])
                !diff.isNaN() && abs(diff) < 1e-7
            }
            return if (allTrue) "True" else "False"
        }

        fun evalEqDiff(eqIdx: Int, pt: DoubleArray): Double {
            for (j in 0 until n) {
                eval.defineVariable(vars[j], pt[j])
            }
            return eval.evaluateNode(lhsNodes[eqIdx]) - eval.evaluateNode(rhsNodes[eqIdx])
        }

        if (m >= n) {
            val zeroPt = DoubleArray(n) { 0.0 }
            val cVec = DoubleArray(m)
            var linearPossible = true
            for (i in 0 until m) {
                val v0 = try {
                    evalEqDiff(i, zeroPt)
                } catch (_: Throwable) {
                    Double.NaN
                }
                if (v0.isNaN() || v0.isInfinite()) {
                    linearPossible = false
                    break
                }
                cVec[i] = v0
            }

            if (linearPossible) {
                val aug = Array(m) { DoubleArray(n + 1) }
                for (j in 0 until n) {
                    val unitPt = DoubleArray(n) { if (it == j) 1.0 else 0.0 }
                    for (i in 0 until m) {
                        val vj = try {
                            evalEqDiff(i, unitPt)
                        } catch (_: Throwable) {
                            Double.NaN
                        }
                        if (vj.isNaN() || vj.isInfinite()) {
                            linearPossible = false
                            break
                        }
                        aug[i][j] = vj - cVec[i]
                    }
                    if (!linearPossible) break
                }
                for (i in 0 until m) {
                    aug[i][n] = -cVec[i]
                }

                if (linearPossible) {
                    var singular = false
                    for (col in 0 until n) {
                        var maxRow = col
                        for (r in (col + 1) until m) {
                            if (abs(aug[r][col]) > abs(aug[maxRow][col])) {
                                maxRow = r
                            }
                        }
                        if (maxRow != col) {
                            val tmp = aug[col]
                            aug[col] = aug[maxRow]
                            aug[maxRow] = tmp
                        }
                        if (abs(aug[col][col]) < 1e-10) {
                            singular = true
                            break
                        }
                        for (r in (col + 1) until m) {
                            val factor = aug[r][col] / aug[col][col]
                            for (c in col..n) {
                                aug[r][c] -= factor * aug[col][c]
                            }
                        }
                    }

                    if (!singular) {
                        val u = DoubleArray(n)
                        for (i in (n - 1) downTo 0) {
                            var sum = 0.0
                            for (j in (i + 1) until n) {
                                sum += aug[i][j] * u[j]
                            }
                            u[i] = (aug[i][n] - sum) / aug[i][i]
                        }
                        val allSatisfied = (0 until m).all { i ->
                            val check = try {
                                evalEqDiff(i, u)
                            } catch (_: Throwable) {
                                Double.NaN
                            }
                            !check.isNaN() && !check.isInfinite() && abs(check) < 1e-6
                        }
                        if (allSatisfied) {
                            return vars.indices.joinToString(", ") { idx ->
                                "${vars[idx]} = ${formatDouble(u[idx])}"
                            }
                        }
                    }
                }
            }
        }

        if (vars.all { variableValues.containsKey(it) }) {
            val u = DoubleArray(n) { variableValues[vars[it]]!! }
            val allSatisfied = (0 until m).all { i ->
                val check = try {
                    evalEqDiff(i, u)
                } catch (_: Throwable) {
                    Double.NaN
                }
                !check.isNaN() && !check.isInfinite() && abs(check) < 1e-6
            }
            if (allSatisfied) {
                return vars.indices.joinToString(", ") { idx ->
                    "${vars[idx]} = ${formatDouble(u[idx])}"
                }
            }
        }

        if (n == 1) {
            val roots = solveSingleVariableRoots(lhsNodes[0].toString(), rhsNodes[0].toString(), vars[0], boundVars)
                .filter { r ->
                    val pt = doubleArrayOf(r)
                    (0 until m).all { i ->
                        val c = try { evalEqDiff(i, pt) } catch (_: Throwable) { Double.NaN }
                        !c.isNaN() && abs(c) < 1e-5
                    }
                }
            if (roots.isNotEmpty()) {
                return "${vars[0]} = ${roots.joinToString(", ") { formatDouble(it) }}"
            }
        } else if (n == 2) {
            for (v0 in -15..15) {
                for (v1 in -15..15) {
                    val pt = doubleArrayOf(v0.toDouble(), v1.toDouble())
                    val ok = (0 until m).all { i ->
                        val c = try { evalEqDiff(i, pt) } catch (_: Throwable) { Double.NaN }
                        !c.isNaN() && abs(c) < 1e-6
                    }
                    if (ok) {
                        return "${vars[0]} = $v0, ${vars[1]} = $v1"
                    }
                }
            }
        }

        if (variableValues.isNotEmpty()) {
            return vars.mapNotNull { v ->
                variableValues[v]?.let { "$v = ${formatDouble(it)}" }
            }.joinToString(", ")
        }
        return "No Solution"
    }

    private fun formatDouble(d: Double): String {
        if (d.isNaN()) return "NaN"
        if (d.isInfinite()) return if (d > 0) "∞" else "-∞"
        if (abs(d - d.roundToLong()) < 1e-9) {
            return d.roundToLong().toString()
        }
        val rounded = BigDecimal(d).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros()
        return rounded.toPlainString()
    }

    private fun needsParentheses(expr: String): Boolean {
        val trimmed = expr.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            var depth = 0
            for (idx in trimmed.indices) {
                if (trimmed[idx] == '(') depth++
                else if (trimmed[idx] == ')') depth--
                if (depth == 0 && idx < trimmed.lastIndex) return true
            }
            return false
        }
        return trimmed.contains("+") || trimmed.contains("-") || trimmed.contains("*") || trimmed.contains("/") || trimmed.contains("^") || trimmed.contains(" ")
    }

    private fun formatMutatedFunction(head: String, args: List<String>, records: MutableList<MutationRecord>): String {
        val fnLower = head.lowercase()
        return when (fnLower) {
            "equal" -> {
                val left = args.getOrElse(0) { "" }
                val right = args.getOrElse(1) { "" }
                "$left == $right"
            }
            "plus" -> {
                if (args.isEmpty()) "0"
                else {
                    val cleanedArgs = args.mapIndexed { idx, a ->
                        if (idx > 0 && a.trim().startsWith("-")) a.trim().removePrefix("-").trim() else a.trim()
                    }
                    if (cleanedArgs.size == 2) {
                        val op = if (random.nextBoolean()) "+" else "-"
                        if (op != "+") records.add(MutationRecord("+", op, MutationType.OPERATOR))
                        "${cleanedArgs[0]} $op ${cleanedArgs[1]}"
                    } else {
                        val ops = (1 until cleanedArgs.size).map { if (random.nextBoolean()) "+" else "-" }
                        buildString {
                            append(cleanedArgs[0])
                            for (idx in ops.indices) {
                                append(" ${ops[idx]} ${cleanedArgs[idx + 1]}")
                            }
                        }
                    }
                }
            }
            "subtract" -> {
                val left = args.getOrElse(0) { "" }
                val right = args.getOrElse(1) { "" }
                val op = if (random.nextBoolean()) "+" else "-"
                if (op != "-") records.add(MutationRecord("-", op, MutationType.OPERATOR))
                "$left $op $right"
            }
            "times" -> {
                if (args.isEmpty()) "1"
                else args.joinToString(" * ")
            }
            "divide" -> {
                val left = args.getOrElse(0) { "1" }.ifBlank { "1" }
                var right = args.getOrElse(1) { "" }.trim()
                if (right.isEmpty() || right == "0" || right == "()") {
                    right = random.nextInt(2, 6).toString()
                }
                val safeLeft = if (left.contains("+") || left.contains("-")) "($left)" else left
                val safeRight = if (right.contains("/") || right.contains("+") || right.contains("-") || right.contains("*")) "($right)" else right
                "$safeLeft/$safeRight"
            }
            "power" -> {
                val base = args.getOrElse(0) { "2" }.ifBlank { "2" }
                val safeBase = if (needsParentheses(base)) "($base)" else base
                val exp = args.getOrElse(1) { "2" }.ifBlank { "2" }
                "$safeBase^$exp"
            }
            "sys", "root", "logb", "sub", "mixed", "perm", "comb", "binom", "fn1", "fn2", "dms2", "dms3" -> {
                "$fnLower(${args.joinToString(";")})"
            }
            "deriv" -> "d/dx(${args.joinToString(", ")})"
            else -> "$fnLower(${args.joinToString(", ")})"
        }
    }

    private fun mapHeadToOperator(head: String): String? = when (head) {
        "Equal" -> "=="
        "Unequal" -> "!="
        "Plus" -> "+"
        "Subtract" -> "-"
        "Times" -> "*"
        "Divide" -> "/"
        "Power" -> "^"
        "Less" -> "<"
        "Greater" -> ">"
        "LessEqual" -> "<="
        "GreaterEqual" -> ">="
        else -> null
    }

    private fun isConstantSymbol(name: String): Boolean =
        name.equals("pi", ignoreCase = true) ||
        name.equals("e", ignoreCase = true) ||
        name.equals("infinity", ignoreCase = true) ||
        name.equals("oo", ignoreCase = true) ||
        name.equals("i", ignoreCase = false)

    private fun isBuiltInFunction(name: String): Boolean =
        name.lowercase() in setOf(
            "sin", "cos", "tan", "sec", "csc", "cot", "asin", "acos", "atan", "acot",
            "arcsin", "arccos", "arctan", "arccot", "arcsec", "arccsc",
            "sinh", "cosh", "tanh", "coth", "sech", "csch",
            "arsinh", "arcosh", "artanh", "arcoth", "arsech", "arcsch",
            "sqrt", "cbrt", "root", "abs", "ln", "log", "log2", "logb", "exp", "mod",
            "sgn", "perm", "comb", "binom", "conj", "det", "matrix", "rad", "deg",
            "dms2", "dms3", "mixed", "sub", "fn1", "fn2", "sys",
            "lim", "limp", "limm", "deriv", "derivv", "derivn",
            "intx", "intv", "defint", "integrate", "sum"
        )
}
