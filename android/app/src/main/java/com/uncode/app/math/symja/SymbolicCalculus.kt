package com.uncode.app.math.symja

import org.matheclipse.parser.client.ast.ASTNode
import org.matheclipse.parser.client.ast.FloatNode
import org.matheclipse.parser.client.ast.FractionNode
import org.matheclipse.parser.client.ast.FunctionNode
import org.matheclipse.parser.client.ast.IntegerNode
import org.matheclipse.parser.client.ast.SymbolNode
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

sealed class SymExpr {
    data class Num(val value: Double) : SymExpr()
    data class Sym(val name: String) : SymExpr()
    data class Add(val terms: List<SymExpr>) : SymExpr()
    data class Mul(val factors: List<SymExpr>) : SymExpr()
    data class Div(val num: SymExpr, val den: SymExpr) : SymExpr()
    data class Pow(val base: SymExpr, val exp: SymExpr) : SymExpr()
    data class Fn(val name: String, val args: List<SymExpr>) : SymExpr()
}

object SymbolicCalculus {

    fun containsSymbolicCalculusNode(node: ASTNode?): Boolean {
        if (node == null) return false
        if (node is FunctionNode && node.isNotEmpty()) {
            val h = node[0].toString().lowercase()
            if (h in setOf("deriv", "derivv", "derivn", "intx", "intv", "integrate")) return true
            for (i in 1 until node.size) {
                if (containsSymbolicCalculusNode(node[i])) return true
            }
        }
        return false
    }

    fun evaluateSymbolicAst(node: ASTNode): String? {
        val (expr, hasIndefiniteIntegral) = evalNodeWithIntegralFlag(node) ?: return null
        val simplified = simplify(expr)
        val baseStr = formatExpr(simplified)
        return if (hasIndefiniteIntegral) {
            if (baseStr == "0") "C" else "$baseStr + C"
        } else {
            baseStr
        }
    }

    private fun evalNodeWithIntegralFlag(node: ASTNode): Pair<SymExpr, Boolean>? {
        if (node is FunctionNode && node.isNotEmpty()) {
            val h = node[0].toString().lowercase()
            when {
                h == "deriv" && node.size >= 2 -> {
                    val body = fromAst(node[1]) ?: return null
                    return simplify(diff(body, "x", 1)) to false
                }
                h == "derivv" && node.size >= 3 -> {
                    val vName = node[1].toString()
                    val body = fromAst(node[2]) ?: return null
                    return simplify(diff(body, vName, 1)) to false
                }
                h == "derivn" && node.size >= 4 -> {
                    val vName = node[1].toString()
                    val ord = node[2].toString().toDoubleOrNull()?.roundToInt()?.coerceIn(1, 6) ?: 1
                    val body = fromAst(node[3]) ?: return null
                    return simplify(diff(body, vName, ord)) to false
                }
                (h == "intx" || h == "integrate") && node.size >= 2 -> {
                    val body = fromAst(node[1]) ?: return null
                    val antideriv = integrate(simplify(body), "x") ?: return null
                    return simplify(antideriv) to true
                }
                h == "intv" && node.size >= 3 -> {
                    val vName = node[2].toString()
                    val body = fromAst(node[1]) ?: return null
                    val antideriv = integrate(simplify(body), vName) ?: return null
                    return simplify(antideriv) to true
                }
            }
        }
        val sym = fromAst(node) ?: return null
        val hasInt = containsIndefiniteIntegralAst(node)
        return simplify(sym) to hasInt
    }

    private fun containsIndefiniteIntegralAst(node: ASTNode): Boolean {
        if (node is FunctionNode && node.isNotEmpty()) {
            val h = node[0].toString().lowercase()
            if (h in setOf("intx", "intv", "integrate")) return true
            for (i in 1 until node.size) {
                if (containsIndefiniteIntegralAst(node[i])) return true
            }
        }
        return false
    }

    fun fromAst(node: ASTNode): SymExpr? {
        return when (node) {
            is IntegerNode -> SymExpr.Num(node.toString().toDoubleOrNull() ?: return null)
            is FloatNode -> SymExpr.Num(node.toString().toDoubleOrNull() ?: return null)
            is FractionNode -> {
                val parts = node.toString().split("/")
                if (parts.size == 2) {
                    val n = parts[0].trim().toDoubleOrNull() ?: return null
                    val d = parts[1].trim().toDoubleOrNull() ?: return null
                    SymExpr.Div(SymExpr.Num(n), SymExpr.Num(d))
                } else null
            }
            is SymbolNode -> SymExpr.Sym(node.toString())
            is FunctionNode -> {
                if (node.isEmpty()) return null
                val head = node[0].toString()
                val headLower = head.lowercase()
                when {
                    head == "Plus" && node.size >= 2 -> {
                        val list = ArrayList<SymExpr>(node.size - 1)
                        for (i in 1 until node.size) {
                            list.add(fromAst(node[i]) ?: return null)
                        }
                        SymExpr.Add(list)
                    }
                    head == "Subtract" && node.size == 3 -> {
                        val a = fromAst(node[1]) ?: return null
                        val b = fromAst(node[2]) ?: return null
                        SymExpr.Add(listOf(a, SymExpr.Mul(listOf(SymExpr.Num(-1.0), b))))
                    }
                    head == "Times" && node.size >= 2 -> {
                        val list = ArrayList<SymExpr>(node.size - 1)
                        for (i in 1 until node.size) {
                            list.add(fromAst(node[i]) ?: return null)
                        }
                        SymExpr.Mul(list)
                    }
                    head == "Divide" && node.size == 3 -> {
                        val n = fromAst(node[1]) ?: return null
                        val d = fromAst(node[2]) ?: return null
                        SymExpr.Div(n, d)
                    }
                    head == "Power" && node.size == 3 -> {
                        val b = fromAst(node[1]) ?: return null
                        val e = fromAst(node[2]) ?: return null
                        SymExpr.Pow(b, e)
                    }
                    headLower == "deriv" && node.size >= 2 -> {
                        val inner = fromAst(node[1]) ?: return null
                        simplify(diff(inner, "x", 1))
                    }
                    headLower == "derivv" && node.size >= 3 -> {
                        val vName = node[1].toString()
                        val inner = fromAst(node[2]) ?: return null
                        simplify(diff(inner, vName, 1))
                    }
                    headLower == "derivn" && node.size >= 4 -> {
                        val vName = node[1].toString()
                        val ord = node[2].toString().toDoubleOrNull()?.roundToInt()?.coerceIn(1, 6) ?: 1
                        val inner = fromAst(node[3]) ?: return null
                        simplify(diff(inner, vName, ord))
                    }
                    (headLower == "intx" || headLower == "integrate") && node.size >= 2 -> {
                        val inner = fromAst(node[1]) ?: return null
                        integrate(simplify(inner), "x")
                    }
                    headLower == "intv" && node.size >= 3 -> {
                        val vName = node[2].toString()
                        val inner = fromAst(node[1]) ?: return null
                        integrate(simplify(inner), vName)
                    }
                    else -> {
                        val args = ArrayList<SymExpr>(node.size - 1)
                        for (i in 1 until node.size) {
                            args.add(fromAst(node[i]) ?: return null)
                        }
                        SymExpr.Fn(headLower, args)
                    }
                }
            }
            else -> null
        }
    }

    fun dependsOn(expr: SymExpr, v: String): Boolean = when (expr) {
        is SymExpr.Num -> false
        is SymExpr.Sym -> expr.name == v
        is SymExpr.Add -> expr.terms.any { dependsOn(it, v) }
        is SymExpr.Mul -> expr.factors.any { dependsOn(it, v) }
        is SymExpr.Div -> dependsOn(expr.num, v) || dependsOn(expr.den, v)
        is SymExpr.Pow -> dependsOn(expr.base, v) || dependsOn(expr.exp, v)
        is SymExpr.Fn -> expr.args.any { dependsOn(it, v) }
    }

    fun diff(expr: SymExpr, v: String, order: Int = 1): SymExpr {
        var current = simplify(expr)
        for (step in 0 until order) {
            current = simplify(diffOnce(current, v))
        }
        return current
    }

    private fun diffOnce(expr: SymExpr, v: String): SymExpr {
        if (!dependsOn(expr, v)) return SymExpr.Num(0.0)
        return when (expr) {
            is SymExpr.Num -> SymExpr.Num(0.0)
            is SymExpr.Sym -> if (expr.name == v) SymExpr.Num(1.0) else SymExpr.Num(0.0)
            is SymExpr.Add -> SymExpr.Add(expr.terms.map { diffOnce(it, v) })
            is SymExpr.Mul -> {
                val k = expr.factors.size
                if (k == 0) SymExpr.Num(0.0)
                else if (k == 1) diffOnce(expr.factors[0], v)
                else {
                    val sumTerms = ArrayList<SymExpr>(k)
                    for (i in 0 until k) {
                        val df = diffOnce(expr.factors[i], v)
                        val prod = ArrayList<SymExpr>(k)
                        for (j in 0 until k) {
                            prod.add(if (i == j) df else expr.factors[j])
                        }
                        sumTerms.add(SymExpr.Mul(prod))
                    }
                    SymExpr.Add(sumTerms)
                }
            }
            is SymExpr.Div -> {
                if (!dependsOn(expr.den, v)) {
                    SymExpr.Div(diffOnce(expr.num, v), expr.den)
                } else {
                    val dNum = diffOnce(expr.num, v)
                    val dDen = diffOnce(expr.den, v)
                    val top = SymExpr.Add(
                        listOf(
                            SymExpr.Mul(listOf(dNum, expr.den)),
                            SymExpr.Mul(listOf(SymExpr.Num(-1.0), expr.num, dDen))
                        )
                    )
                    SymExpr.Div(top, SymExpr.Pow(expr.den, SymExpr.Num(2.0)))
                }
            }
            is SymExpr.Pow -> {
                val baseDep = dependsOn(expr.base, v)
                val expDep = dependsOn(expr.exp, v)
                when {
                    baseDep && !expDep -> {
                        val newExp = simplify(SymExpr.Add(listOf(expr.exp, SymExpr.Num(-1.0))))
                        SymExpr.Mul(listOf(expr.exp, SymExpr.Pow(expr.base, newExp), diffOnce(expr.base, v)))
                    }
                    !baseDep && expDep -> {
                        val du = diffOnce(expr.exp, v)
                        if (expr.base is SymExpr.Sym && expr.base.name.equals("e", ignoreCase = true)) {
                            SymExpr.Mul(listOf(expr, du))
                        } else {
                            SymExpr.Mul(listOf(expr, SymExpr.Fn("ln", listOf(expr.base)), du))
                        }
                    }
                    else -> {
                        val du = diffOnce(expr.base, v)
                        val dw = diffOnce(expr.exp, v)
                        SymExpr.Mul(
                            listOf(
                                expr,
                                SymExpr.Add(
                                    listOf(
                                        SymExpr.Mul(listOf(dw, SymExpr.Fn("ln", listOf(expr.base)))),
                                        SymExpr.Div(SymExpr.Mul(listOf(expr.exp, du)), expr.base)
                                    )
                                )
                            )
                        )
                    }
                }
            }
            is SymExpr.Fn -> {
                if (expr.args.isEmpty()) return SymExpr.Num(0.0)
                val u = expr.args[0]
                val du = diffOnce(u, v)
                when (expr.name) {
                    "sin" -> SymExpr.Mul(listOf(SymExpr.Fn("cos", listOf(u)), du))
                    "cos" -> SymExpr.Mul(listOf(SymExpr.Num(-1.0), SymExpr.Fn("sin", listOf(u)), du))
                    "tan" -> SymExpr.Mul(listOf(SymExpr.Pow(SymExpr.Fn("sec", listOf(u)), SymExpr.Num(2.0)), du))
                    "cot" -> SymExpr.Mul(listOf(SymExpr.Num(-1.0), SymExpr.Pow(SymExpr.Fn("csc", listOf(u)), SymExpr.Num(2.0)), du))
                    "sec" -> SymExpr.Mul(listOf(SymExpr.Fn("sec", listOf(u)), SymExpr.Fn("tan", listOf(u)), du))
                    "csc" -> SymExpr.Mul(listOf(SymExpr.Num(-1.0), SymExpr.Fn("csc", listOf(u)), SymExpr.Fn("cot", listOf(u)), du))
                    "ln" -> SymExpr.Div(du, u)
                    "log" -> SymExpr.Div(du, SymExpr.Mul(listOf(u, SymExpr.Fn("ln", listOf(SymExpr.Num(10.0))))))
                    "log2" -> SymExpr.Div(du, SymExpr.Mul(listOf(u, SymExpr.Fn("ln", listOf(SymExpr.Num(2.0))))))
                    "exp" -> SymExpr.Mul(listOf(SymExpr.Fn("exp", listOf(u)), du))
                    "sqrt" -> SymExpr.Div(du, SymExpr.Mul(listOf(SymExpr.Num(2.0), SymExpr.Fn("sqrt", listOf(u)))))
                    "cbrt" -> SymExpr.Div(du, SymExpr.Mul(listOf(SymExpr.Num(3.0), SymExpr.Pow(u, SymExpr.Div(SymExpr.Num(2.0), SymExpr.Num(3.0))))))
                    "sinh" -> SymExpr.Mul(listOf(SymExpr.Fn("cosh", listOf(u)), du))
                    "cosh" -> SymExpr.Mul(listOf(SymExpr.Fn("sinh", listOf(u)), du))
                    "tanh" -> SymExpr.Mul(listOf(SymExpr.Pow(SymExpr.Fn("sech", listOf(u)), SymExpr.Num(2.0)), du))
                    "arcsin", "asin" -> SymExpr.Div(du, SymExpr.Fn("sqrt", listOf(SymExpr.Add(listOf(SymExpr.Num(1.0), SymExpr.Mul(listOf(SymExpr.Num(-1.0), SymExpr.Pow(u, SymExpr.Num(2.0)))))))))
                    "arccos", "acos" -> SymExpr.Mul(listOf(SymExpr.Num(-1.0), SymExpr.Div(du, SymExpr.Fn("sqrt", listOf(SymExpr.Add(listOf(SymExpr.Num(1.0), SymExpr.Mul(listOf(SymExpr.Num(-1.0), SymExpr.Pow(u, SymExpr.Num(2.0)))))))))))
                    "arctan", "atan" -> SymExpr.Div(du, SymExpr.Add(listOf(SymExpr.Num(1.0), SymExpr.Pow(u, SymExpr.Num(2.0)))))
                    else -> SymExpr.Num(0.0)
                }
            }
        }
    }

    fun integrate(expr: SymExpr, v: String): SymExpr? {
        val clean = simplify(expr)
        if (!dependsOn(clean, v)) {
            if (clean is SymExpr.Num && abs(clean.value) < 1e-12) return SymExpr.Num(0.0)
            return simplify(SymExpr.Mul(listOf(clean, SymExpr.Sym(v))))
        }
        return when (clean) {
            is SymExpr.Sym -> {
                if (clean.name == v) {
                    SymExpr.Div(SymExpr.Pow(SymExpr.Sym(v), SymExpr.Num(2.0)), SymExpr.Num(2.0))
                } else {
                    SymExpr.Mul(listOf(clean, SymExpr.Sym(v)))
                }
            }
            is SymExpr.Add -> {
                val integratedTerms = ArrayList<SymExpr>(clean.terms.size)
                for (t in clean.terms) {
                    integratedTerms.add(integrate(t, v) ?: return null)
                }
                simplify(SymExpr.Add(integratedTerms))
            }
            is SymExpr.Mul -> {
                val consts = ArrayList<SymExpr>()
                val varParts = ArrayList<SymExpr>()
                for (f in clean.factors) {
                    if (dependsOn(f, v)) varParts.add(f) else consts.add(f)
                }
                if (varParts.isEmpty()) {
                    simplify(SymExpr.Mul(consts + SymExpr.Sym(v)))
                } else if (consts.isNotEmpty()) {
                    val rest = if (varParts.size == 1) varParts[0] else SymExpr.Mul(varParts)
                    val intRest = integrate(rest, v) ?: return null
                    simplify(SymExpr.Mul(consts + intRest))
                } else {
                    null
                }
            }
            is SymExpr.Div -> {
                if (!dependsOn(clean.den, v)) {
                    val intNum = integrate(clean.num, v) ?: return null
                    simplify(SymExpr.Div(intNum, clean.den))
                } else if (!dependsOn(clean.num, v)) {
                    when (val d = clean.den) {
                        is SymExpr.Sym -> if (d.name == v) {
                            simplify(SymExpr.Mul(listOf(clean.num, SymExpr.Fn("ln", listOf(SymExpr.Fn("abs", listOf(SymExpr.Sym(v))))))))
                        } else null
                        is SymExpr.Pow -> if (d.base is SymExpr.Sym && d.base.name == v && d.exp is SymExpr.Num) {
                            integrate(SymExpr.Mul(listOf(clean.num, SymExpr.Pow(SymExpr.Sym(v), SymExpr.Num(-d.exp.value)))), v)
                        } else null
                        else -> null
                    }
                } else null
            }
            is SymExpr.Pow -> {
                if (clean.base is SymExpr.Sym && clean.base.name == v && clean.exp is SymExpr.Num) {
                    val n = clean.exp.value
                    if (abs(n + 1.0) < 1e-9) {
                        SymExpr.Fn("ln", listOf(SymExpr.Fn("abs", listOf(SymExpr.Sym(v)))))
                    } else {
                        val nPlus1 = n + 1.0
                        simplify(SymExpr.Div(SymExpr.Pow(SymExpr.Sym(v), SymExpr.Num(nPlus1)), SymExpr.Num(nPlus1)))
                    }
                } else if (clean.base is SymExpr.Sym && clean.base.name.equals("e", ignoreCase = true)) {
                    val lin = extractLinearCoeff(clean.exp, v) ?: return null
                    simplify(SymExpr.Div(clean, SymExpr.Num(lin.first)))
                } else if (clean.base is SymExpr.Fn && clean.base.name == "sec" && clean.exp is SymExpr.Num && abs(clean.exp.value - 2.0) < 1e-9 && clean.base.args.size == 1) {
                    val u = clean.base.args[0]
                    val lin = extractLinearCoeff(u, v) ?: return null
                    simplify(SymExpr.Div(SymExpr.Fn("tan", listOf(u)), SymExpr.Num(lin.first)))
                } else null
            }
            is SymExpr.Fn -> {
                if (clean.args.size == 1) {
                    val u = clean.args[0]
                    val lin = extractLinearCoeff(u, v) ?: return null
                    val a = lin.first
                    when (clean.name) {
                        "sin" -> simplify(SymExpr.Div(SymExpr.Mul(listOf(SymExpr.Num(-1.0), SymExpr.Fn("cos", listOf(u)))), SymExpr.Num(a)))
                        "cos" -> simplify(SymExpr.Div(SymExpr.Fn("sin", listOf(u)), SymExpr.Num(a)))
                        "exp" -> simplify(SymExpr.Div(SymExpr.Fn("exp", listOf(u)), SymExpr.Num(a)))
                        "sqrt" -> if (abs(lin.second) < 1e-9 && abs(a - 1.0) < 1e-9) {
                            SymExpr.Div(SymExpr.Mul(listOf(SymExpr.Num(2.0), SymExpr.Pow(SymExpr.Sym(v), SymExpr.Div(SymExpr.Num(3.0), SymExpr.Num(2.0))))), SymExpr.Num(3.0))
                        } else null
                        else -> null
                    }
                } else null
            }
            else -> null
        }
    }

    private fun extractLinearCoeff(expr: SymExpr, v: String): Pair<Double, Double>? {
        val s = simplify(expr)
        return when (s) {
            is SymExpr.Sym -> if (s.name == v) 1.0 to 0.0 else null
            is SymExpr.Mul -> {
                if (s.factors.size == 2 && s.factors[0] is SymExpr.Num && s.factors[1] == SymExpr.Sym(v)) {
                    (s.factors[0] as SymExpr.Num).value to 0.0
                } else null
            }
            is SymExpr.Add -> {
                if (s.terms.size == 2) {
                    val l0 = extractLinearCoeff(s.terms[0], v)
                    val t1 = s.terms[1]
                    if (l0 != null && t1 is SymExpr.Num) return l0.first to (l0.second + t1.value)
                    val l1 = extractLinearCoeff(s.terms[1], v)
                    val t0 = s.terms[0]
                    if (l1 != null && t0 is SymExpr.Num) return l1.first to (l1.second + t0.value)
                }
                null
            }
            else -> null
        }
    }

    fun simplify(expr: SymExpr): SymExpr {
        return when (expr) {
            is SymExpr.Num -> if (abs(expr.value) < 1e-12) SymExpr.Num(0.0) else expr
            is SymExpr.Sym -> expr
            is SymExpr.Fn -> {
                val simpArgs = expr.args.map { simplify(it) }
                if (expr.name == "sqrt" && simpArgs.size == 1 && simpArgs[0] is SymExpr.Num) {
                    val v = (simpArgs[0] as SymExpr.Num).value
                    if (v >= 0.0) {
                        val r = kotlin.math.sqrt(v)
                        if (abs(r - r.roundToLong()) < 1e-9) return SymExpr.Num(r.roundToLong().toDouble())
                    }
                }
                if (expr.name == "ln" && simpArgs.size == 1) {
                    val a0 = simpArgs[0]
                    if (a0 is SymExpr.Num && abs(a0.value - 1.0) < 1e-9) return SymExpr.Num(0.0)
                    if (a0 is SymExpr.Sym && a0.name.equals("e", ignoreCase = true)) return SymExpr.Num(1.0)
                }
                SymExpr.Fn(expr.name, simpArgs)
            }
            is SymExpr.Pow -> {
                val b = simplify(expr.base)
                val e = simplify(expr.exp)
                when {
                    e is SymExpr.Num && abs(e.value) < 1e-12 -> SymExpr.Num(1.0)
                    e is SymExpr.Num && abs(e.value - 1.0) < 1e-12 -> b
                    b is SymExpr.Num && abs(b.value) < 1e-12 -> SymExpr.Num(0.0)
                    b is SymExpr.Num && abs(b.value - 1.0) < 1e-12 -> SymExpr.Num(1.0)
                    b is SymExpr.Num && e is SymExpr.Num -> {
                        val p = b.value.pow(e.value)
                        if (!p.isNaN() && !p.isInfinite() && abs(p - p.roundToLong()) < 1e-9) {
                            SymExpr.Num(p.roundToLong().toDouble())
                        } else {
                            SymExpr.Pow(b, e)
                        }
                    }
                    b is SymExpr.Pow -> simplify(SymExpr.Pow(b.base, SymExpr.Mul(listOf(b.exp, e))))
                    else -> SymExpr.Pow(b, e)
                }
            }
            is SymExpr.Mul -> simplifyMul(expr.factors)
            is SymExpr.Div -> simplifyDiv(expr.num, expr.den)
            is SymExpr.Add -> simplifyAdd(expr.terms)
        }
    }

    private fun simplifyMul(rawFactors: List<SymExpr>): SymExpr {
        val flat = ArrayList<SymExpr>()
        fun collect(f: SymExpr) {
            val s = simplify(f)
            if (s is SymExpr.Mul) {
                for (sub in s.factors) collect(sub)
            } else {
                flat.add(s)
            }
        }
        for (f in rawFactors) collect(f)

        val numFactors = ArrayList<SymExpr>()
        val denFactors = ArrayList<SymExpr>()
        var hasDiv = false
        for (f in flat) {
            if (f is SymExpr.Div) {
                hasDiv = true
                numFactors.add(f.num)
                denFactors.add(f.den)
            } else {
                numFactors.add(f)
            }
        }
        if (hasDiv) {
            return simplifyDiv(SymExpr.Mul(numFactors), SymExpr.Mul(denFactors))
        }

        var coeff = 1.0
        val symbolic = ArrayList<SymExpr>()
        for (f in flat) {
            if (f is SymExpr.Num) {
                if (abs(f.value) < 1e-12) return SymExpr.Num(0.0)
                coeff *= f.value
            } else {
                symbolic.add(f)
            }
        }

        val combinedPowers = ArrayList<SymExpr>()
        for (item in symbolic) {
            val (itemBase, itemExp) = if (item is SymExpr.Pow) item.base to item.exp else item to SymExpr.Num(1.0)
            var merged = false
            for (idx in combinedPowers.indices) {
                val existing = combinedPowers[idx]
                val (exBase, exExp) = if (existing is SymExpr.Pow) existing.base to existing.exp else existing to SymExpr.Num(1.0)
                if (exBase == itemBase) {
                    val sumExp = simplify(SymExpr.Add(listOf(exExp, itemExp)))
                    combinedPowers[idx] = simplify(SymExpr.Pow(exBase, sumExp))
                    merged = true
                    break
                }
            }
            if (!merged) combinedPowers.add(item)
        }

        val filtered = combinedPowers.filter { !(it is SymExpr.Num && abs(it.value - 1.0) < 1e-12) }
        if (filtered.isEmpty()) return SymExpr.Num(coeff)
        if (filtered.size == 1 && filtered[0] is SymExpr.Add && abs(coeff + 1.0) >= 1e-12) {
            val addNode = filtered[0] as SymExpr.Add
            return simplify(SymExpr.Add(addNode.terms.map { SymExpr.Mul(listOf(SymExpr.Num(coeff), it)) }))
        }
        if (abs(coeff - 1.0) < 1e-12) {
            return if (filtered.size == 1) filtered[0] else SymExpr.Mul(filtered)
        }
        return SymExpr.Mul(listOf(SymExpr.Num(coeff)) + filtered)
    }

    private fun simplifyDiv(rawNum: SymExpr, rawDen: SymExpr): SymExpr {
        val n = simplify(rawNum)
        val d = simplify(rawDen)
        if (n is SymExpr.Num && abs(n.value) < 1e-12) return SymExpr.Num(0.0)
        if (d is SymExpr.Num && abs(d.value - 1.0) < 1e-12) return n
        if (d is SymExpr.Num && abs(d.value + 1.0) < 1e-12) return simplify(SymExpr.Mul(listOf(SymExpr.Num(-1.0), n)))
        if (n == d) return SymExpr.Num(1.0)

        if (n is SymExpr.Num && d is SymExpr.Num && abs(d.value) > 1e-12) {
            val q = n.value / d.value
            if (abs(q - q.roundToLong()) < 1e-9) {
                return SymExpr.Num(q.roundToLong().toDouble())
            }
            val nLong = n.value.roundToLong()
            val dLong = d.value.roundToLong()
            if (abs(n.value - nLong) < 1e-9 && abs(d.value - dLong) < 1e-9 && dLong != 0L) {
                val g = gcd(abs(nLong), abs(dLong))
                val sign = if (dLong < 0) -1L else 1L
                val rNum = (nLong / g) * sign
                val rDen = abs(dLong) / g
                return if (rDen == 1L) SymExpr.Num(rNum.toDouble()) else SymExpr.Div(SymExpr.Num(rNum.toDouble()), SymExpr.Num(rDen.toDouble()))
            }
        }

        val (numCoeff, numRest) = extractLeadingCoeff(n)
        val (denCoeff, denRest) = extractLeadingCoeff(d)
        if (abs(denCoeff) > 1e-12) {
            val q = numCoeff / denCoeff
            if (abs(q - q.roundToLong()) < 1e-9 && denRest == SymExpr.Num(1.0)) {
                return simplify(SymExpr.Mul(listOf(SymExpr.Num(q.roundToLong().toDouble()), numRest)))
            }
            val nLong = numCoeff.roundToLong()
            val dLong = denCoeff.roundToLong()
            if (abs(numCoeff - nLong) < 1e-9 && abs(denCoeff - dLong) < 1e-9 && dLong != 0L) {
                val g = gcd(abs(nLong), abs(dLong))
                if (g > 1L || dLong < 0L) {
                    val sign = if (dLong < 0) -1L else 1L
                    val rNum = (nLong / g) * sign
                    val rDen = abs(dLong) / g
                    val newTop = simplify(SymExpr.Mul(listOf(SymExpr.Num(rNum.toDouble()), numRest)))
                    val newBot = simplify(SymExpr.Mul(listOf(SymExpr.Num(rDen.toDouble()), denRest)))
                    return if (newBot is SymExpr.Num && abs(newBot.value - 1.0) < 1e-12) newTop else SymExpr.Div(newTop, newBot)
                }
            }
        }

        return SymExpr.Div(n, d)
    }

    private fun simplifyAdd(rawTerms: List<SymExpr>): SymExpr {
        val flat = ArrayList<SymExpr>()
        fun collect(t: SymExpr) {
            val s = simplify(t)
            if (s is SymExpr.Add) {
                for (sub in s.terms) collect(sub)
            } else {
                flat.add(s)
            }
        }
        for (t in rawTerms) collect(t)

        var constSum = 0.0
        val groups = LinkedHashMap<SymExpr, Double>()
        for (t in flat) {
            if (t is SymExpr.Num) {
                constSum += t.value
            } else {
                val (c, base) = extractLeadingCoeff(t)
                groups[base] = (groups[base] ?: 0.0) + c
            }
        }

        val resultTerms = ArrayList<SymExpr>()
        for ((base, coeff) in groups) {
            if (abs(coeff) > 1e-12) {
                resultTerms.add(simplify(SymExpr.Mul(listOf(SymExpr.Num(coeff), base))))
            }
        }
        if (abs(constSum) > 1e-12) {
            resultTerms.add(SymExpr.Num(constSum))
        }
        return when {
            resultTerms.isEmpty() -> SymExpr.Num(0.0)
            resultTerms.size == 1 -> resultTerms[0]
            else -> SymExpr.Add(resultTerms)
        }
    }

    private fun extractLeadingCoeff(expr: SymExpr): Pair<Double, SymExpr> {
        return when (expr) {
            is SymExpr.Num -> expr.value to SymExpr.Num(1.0)
            is SymExpr.Mul -> {
                if (expr.factors.isNotEmpty() && expr.factors[0] is SymExpr.Num) {
                    val c = (expr.factors[0] as SymExpr.Num).value
                    val rem = expr.factors.drop(1)
                    val restExpr = when {
                        rem.isEmpty() -> SymExpr.Num(1.0)
                        rem.size == 1 -> rem[0]
                        else -> SymExpr.Mul(rem)
                    }
                    c to restExpr
                } else {
                    1.0 to expr
                }
            }
            else -> 1.0 to expr
        }
    }

    private fun gcd(a: Long, b: Long): Long {
        var x = a
        var y = b
        while (y != 0L) {
            val t = y
            y = x % y
            x = t
        }
        return if (x == 0L) 1L else x
    }

    fun formatExpr(expr: SymExpr): String {
        return when (expr) {
            is SymExpr.Num -> formatNumber(expr.value)
            is SymExpr.Sym -> expr.name
            is SymExpr.Add -> {
                if (expr.terms.isEmpty()) return "0"
                val sb = StringBuilder()
                for (idx in expr.terms.indices) {
                    val termStr = formatExpr(expr.terms[idx])
                    if (idx == 0) {
                        sb.append(termStr)
                    } else if (termStr.startsWith("-")) {
                        sb.append(" - ").append(termStr.removePrefix("-").trim())
                    } else {
                        sb.append(" + ").append(termStr)
                    }
                }
                sb.toString()
            }
            is SymExpr.Mul -> {
                if (expr.factors.isEmpty()) return "1"
                if (expr.factors.size == 2 && expr.factors[0] is SymExpr.Num && abs((expr.factors[0] as SymExpr.Num).value + 1.0) < 1e-12) {
                    val right = expr.factors[1]
                    val rStr = formatExpr(right)
                    return if (right is SymExpr.Add) "-($rStr)" else "-$rStr"
                }
                expr.factors.joinToString(" * ") { f ->
                    val s = formatExpr(f)
                    if (f is SymExpr.Add) "($s)" else s
                }
            }
            is SymExpr.Div -> {
                val nStr = formatExpr(expr.num)
                val dStr = formatExpr(expr.den)
                val safeN = if (expr.num is SymExpr.Add) "($nStr)" else nStr
                val safeD = if (expr.den is SymExpr.Add || expr.den is SymExpr.Mul || expr.den is SymExpr.Div) "($dStr)" else dStr
                "$safeN/$safeD"
            }
            is SymExpr.Pow -> {
                val bStr = formatExpr(expr.base)
                val eStr = formatExpr(expr.exp)
                val safeB = if (expr.base is SymExpr.Add || expr.base is SymExpr.Mul || expr.base is SymExpr.Div || expr.base is SymExpr.Pow || bStr.startsWith("-")) "($bStr)" else bStr
                val safeE = if (expr.exp is SymExpr.Add || expr.exp is SymExpr.Mul || expr.exp is SymExpr.Div || eStr.startsWith("-")) "($eStr)" else eStr
                "$safeB^$safeE"
            }
            is SymExpr.Fn -> {
                "${expr.name}(${expr.args.joinToString(", ") { formatExpr(it) }})"
            }
        }
    }

    private fun formatNumber(d: Double): String {
        if (abs(d - d.roundToLong()) < 1e-9) {
            return d.roundToLong().toString()
        }
        return BigDecimal(d).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }
}
