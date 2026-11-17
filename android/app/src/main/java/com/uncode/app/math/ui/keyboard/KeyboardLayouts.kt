package com.uncode.app.math.ui.keyboard

object KeyboardLayouts {
    val basicRows: List<List<SciCalcKey>> = listOf(
        listOf(
            SciCalcKey(
                label = "(□)",
                insertText = "()",
                cursorOffset = -1,
                category = KeyCategory.OPERATOR,
                hasAlternate = true,
                alternates = listOf(
                    SciCalcKey("(□)", "()", -1, KeyCategory.OPERATOR),
                    SciCalcKey("(", "(", 0, KeyCategory.OPERATOR),
                    SciCalcKey(")", ")", 0, KeyCategory.OPERATOR)
                )
            ),
            SciCalcKey(
                label = ">",
                insertText = ">",
                cursorOffset = 0,
                category = KeyCategory.OPERATOR,
                hasAlternate = true,
                alternates = listOf(
                    SciCalcKey(">", ">", 0, KeyCategory.OPERATOR),
                    SciCalcKey("≥", ">=", 0, KeyCategory.OPERATOR),
                    SciCalcKey("<", "<", 0, KeyCategory.OPERATOR),
                    SciCalcKey("≤", "<=", 0, KeyCategory.OPERATOR)
                )
            ),
            SciCalcKey("7", "7", 0, KeyCategory.NUMBER),
            SciCalcKey("8", "8", 0, KeyCategory.NUMBER),
            SciCalcKey("9", "9", 0, KeyCategory.NUMBER),
            SciCalcKey("÷", "/", 0, KeyCategory.OPERATOR)
        ),
        listOf(
            SciCalcKey(
                label = "a/b",
                insertText = "/()",
                cursorOffset = -1,
                category = KeyCategory.OPERATOR,
                hasAlternate = true,
                alternates = listOf(
                    SciCalcKey("a/b", "/()", -1, KeyCategory.OPERATOR),
                    SciCalcKey("mixed_frac", "mixed(;;)", -3, KeyCategory.OPERATOR)
                )
            ),
            SciCalcKey(
                label = "√x",
                insertText = "√()",
                cursorOffset = -1,
                category = KeyCategory.FUNCTION,
                hasAlternate = true,
                alternates = listOf(
                    SciCalcKey("√x", "√()", -1, KeyCategory.FUNCTION),
                    SciCalcKey("∛", "∛()", -1, KeyCategory.FUNCTION),
                    SciCalcKey("ⁿ√", "root(;)", -2, KeyCategory.FUNCTION)
                )
            ),
            SciCalcKey("4", "4", 0, KeyCategory.NUMBER),
            SciCalcKey("5", "5", 0, KeyCategory.NUMBER),
            SciCalcKey("6", "6", 0, KeyCategory.NUMBER),
            SciCalcKey("×", "*", 0, KeyCategory.OPERATOR)
        ),
        listOf(
            SciCalcKey(
                label = "x²",
                insertText = "^(2)",
                cursorOffset = 0,
                category = KeyCategory.FUNCTION,
                hasAlternate = true,
                alternates = listOf(
                    SciCalcKey("x²", "^(2)", 0, KeyCategory.FUNCTION),
                    SciCalcKey("xⁿ", "^()", -1, KeyCategory.FUNCTION),
                    SciCalcKey("x³", "^(3)", 0, KeyCategory.FUNCTION)
                )
            ),
            SciCalcKey(
                label = "x",
                insertText = "x",
                cursorOffset = 0,
                category = KeyCategory.VARIABLE,
                hasAlternate = true,
                alternates = listOf(
                    SciCalcKey("x", "x", 0, KeyCategory.VARIABLE),
                    SciCalcKey("y", "y", 0, KeyCategory.VARIABLE),
                    SciCalcKey("z", "z", 0, KeyCategory.VARIABLE),
                    SciCalcKey("a", "a", 0, KeyCategory.VARIABLE),
                    SciCalcKey("b", "b", 0, KeyCategory.VARIABLE),
                    SciCalcKey("c", "c", 0, KeyCategory.VARIABLE)
                )
            ),
            SciCalcKey("1", "1", 0, KeyCategory.NUMBER),
            SciCalcKey("2", "2", 0, KeyCategory.NUMBER),
            SciCalcKey("3", "3", 0, KeyCategory.NUMBER),
            SciCalcKey("−", "-", 0, KeyCategory.OPERATOR)
        ),
        listOf(
            SciCalcKey(
                label = "π",
                insertText = "π",
                cursorOffset = 0,
                category = KeyCategory.VARIABLE,
                hasAlternate = true,
                alternates = listOf(
                    SciCalcKey("π", "π", 0, KeyCategory.VARIABLE),
                    SciCalcKey("π/2", "(π)/(2)", 0, KeyCategory.VARIABLE),
                    SciCalcKey("π/3", "(π)/(3)", 0, KeyCategory.VARIABLE)
                )
            ),
            SciCalcKey("%", "%", 0, KeyCategory.OPERATOR),
            SciCalcKey("0", "0", 0, KeyCategory.NUMBER),
            SciCalcKey(".", ".", 0, KeyCategory.NUMBER),
            SciCalcKey(
                label = "=",
                insertText = "==",
                cursorOffset = 0,
                category = KeyCategory.ACTION,
                hasAlternate = true,
                alternates = listOf(
                    SciCalcKey("=", "==", 0, KeyCategory.ACTION),
                    SciCalcKey("≠", "!=", 0, KeyCategory.OPERATOR)
                )
            ),
            SciCalcKey("+", "+", 0, KeyCategory.OPERATOR)
        )
    )

    val functionRows: List<List<SciCalcKey>> = listOf(
        listOf(
            SciCalcKey("|x|", "||", -1, KeyCategory.FUNCTION),
            SciCalcKey("f(x)", "f(x)", 0, KeyCategory.FUNCTION),
            SciCalcKey("log₁₀", "log()", -1, KeyCategory.FUNCTION),
            SciCalcKey("ⁿ√", "root(;)", -2, KeyCategory.FUNCTION),
            SciCalcKey("i", "i", 0, KeyCategory.VARIABLE),
            SciCalcKey("□,□,□", ", ", 0, KeyCategory.OPERATOR)
        ),
        listOf(
            SciCalcKey("□_□", "sub(;)", -2, KeyCategory.FUNCTION),
            SciCalcKey("□(□)", "fn1(;)", -2, KeyCategory.FUNCTION),
            SciCalcKey("log₂", "log2()", -1, KeyCategory.FUNCTION),
            SciCalcKey("□P□", "perm(;)", -2, KeyCategory.FUNCTION),
            SciCalcKey("z", "z", 0, KeyCategory.VARIABLE),
            SciCalcKey("!", "!", 0, KeyCategory.OPERATOR)
        ),
        listOf(
            SciCalcKey("e", "e", 0, KeyCategory.VARIABLE),
            SciCalcKey("f(x,y)", "f(x, y)", 0, KeyCategory.FUNCTION),
            SciCalcKey("log_□", "logb(;)", -2, KeyCategory.FUNCTION),
            SciCalcKey("□C□", "comb(;)", -2, KeyCategory.FUNCTION),
            SciCalcKey("z̄", "conj()", -1, KeyCategory.FUNCTION),
            SciCalcKey("[□]", "matrix(2,2;;;;)", -4, KeyCategory.FUNCTION)
        ),
        listOf(
            SciCalcKey("exp", "exp()", -1, KeyCategory.FUNCTION),
            SciCalcKey("□(□,□)", "fn2(;;)", -3, KeyCategory.FUNCTION),
            SciCalcKey("ln", "ln()", -1, KeyCategory.FUNCTION),
            SciCalcKey("(□/□)", "binom(;)", -2, KeyCategory.FUNCTION),
            SciCalcKey("sign", "sgn()", -1, KeyCategory.FUNCTION),
            SciCalcKey("", "", 0, KeyCategory.OPERATOR)
        )
    )

    val trigRows: List<List<SciCalcKey>> = listOf(
        listOf(
            SciCalcKey("rad", "rad()", -1, KeyCategory.FUNCTION),
            SciCalcKey("sin", "sin()", -1, KeyCategory.FUNCTION),
            SciCalcKey("cos", "cos()", -1, KeyCategory.FUNCTION),
            SciCalcKey("tan", "tan()", -1, KeyCategory.FUNCTION),
            SciCalcKey("cot", "cot()", -1, KeyCategory.FUNCTION),
            SciCalcKey("sec", "sec()", -1, KeyCategory.FUNCTION),
            SciCalcKey("csc", "csc()", -1, KeyCategory.FUNCTION)
        ),
        listOf(
            SciCalcKey("□°", "deg()", -1, KeyCategory.OPERATOR),
            SciCalcKey("arcsin", "arcsin()", -1, KeyCategory.FUNCTION),
            SciCalcKey("arccos", "arccos()", -1, KeyCategory.FUNCTION),
            SciCalcKey("arctan", "arctan()", -1, KeyCategory.FUNCTION),
            SciCalcKey("arccot", "arccot()", -1, KeyCategory.FUNCTION),
            SciCalcKey("arcsec", "arcsec()", -1, KeyCategory.FUNCTION),
            SciCalcKey("arccsc", "arccsc()", -1, KeyCategory.FUNCTION)
        ),
        listOf(
            SciCalcKey("□°□'", "dms2(;)", -2, KeyCategory.OPERATOR),
            SciCalcKey("sinh", "sinh()", -1, KeyCategory.FUNCTION),
            SciCalcKey("cosh", "cosh()", -1, KeyCategory.FUNCTION),
            SciCalcKey("tanh", "tanh()", -1, KeyCategory.FUNCTION),
            SciCalcKey("coth", "coth()", -1, KeyCategory.FUNCTION),
            SciCalcKey("sech", "sech()", -1, KeyCategory.FUNCTION),
            SciCalcKey("csch", "csch()", -1, KeyCategory.FUNCTION)
        ),
        listOf(
            SciCalcKey("□°□'□''", "dms3(;;)", -3, KeyCategory.OPERATOR),
            SciCalcKey("arsinh", "arsinh()", -1, KeyCategory.FUNCTION),
            SciCalcKey("arcosh", "arcosh()", -1, KeyCategory.FUNCTION),
            SciCalcKey("artanh", "artanh()", -1, KeyCategory.FUNCTION),
            SciCalcKey("arcoth", "arcoth()", -1, KeyCategory.FUNCTION),
            SciCalcKey("arsech", "arsech()", -1, KeyCategory.FUNCTION),
            SciCalcKey("arcsch", "arcsch()", -1, KeyCategory.FUNCTION)
        )
    )

    val calculusRows: List<List<SciCalcKey>> = listOf(
        listOf(
            SciCalcKey("lim", "lim(;;)", -3, KeyCategory.FUNCTION),
            SciCalcKey("d/dx", "d/dx()", -1, KeyCategory.FUNCTION),
            SciCalcKey("∫", "intx()", -1, KeyCategory.FUNCTION),
            SciCalcKey("dy/dx", "(dy)/(dx)", 0, KeyCategory.FUNCTION),
            SciCalcKey("aₙ", "sub(a;)", -1, KeyCategory.VARIABLE)
        ),
        listOf(
            SciCalcKey("lim⁺", "lim+(;;)", -3, KeyCategory.FUNCTION),
            SciCalcKey("d/d□", "derivv(;)", -2, KeyCategory.FUNCTION),
            SciCalcKey("∫d□", "intv(;)", -2, KeyCategory.FUNCTION),
            SciCalcKey("dx", "dx", 0, KeyCategory.VARIABLE),
            SciCalcKey("□,□,□", ", ", 0, KeyCategory.OPERATOR)
        ),
        listOf(
            SciCalcKey("lim⁻", "lim-(;;)", -3, KeyCategory.FUNCTION),
            SciCalcKey("d²/dx²", "derivn(;;)", -3, KeyCategory.FUNCTION),
            SciCalcKey("∫_a^b", "defint(;;;)", -4, KeyCategory.FUNCTION),
            SciCalcKey("dy", "dy", 0, KeyCategory.VARIABLE),
            SciCalcKey("", "", 0, KeyCategory.OPERATOR)
        ),
        listOf(
            SciCalcKey("∞", "oo", 0, KeyCategory.VARIABLE),
            SciCalcKey("", "", 0, KeyCategory.OPERATOR),
            SciCalcKey("∑", "sum(;;;)", -4, KeyCategory.FUNCTION),
            SciCalcKey("y'", "y'", 0, KeyCategory.VARIABLE),
            SciCalcKey("", "", 0, KeyCategory.OPERATOR)
        )
    )

    val abcRows: List<List<SciCalcKey>> = listOf(
        listOf(
            SciCalcKey("a", "a", 0, KeyCategory.VARIABLE),
            SciCalcKey("b", "b", 0, KeyCategory.VARIABLE),
            SciCalcKey("c", "c", 0, KeyCategory.VARIABLE),
            SciCalcKey("d", "d", 0, KeyCategory.VARIABLE),
            SciCalcKey("e", "e", 0, KeyCategory.VARIABLE),
            SciCalcKey("f", "f", 0, KeyCategory.VARIABLE),
            SciCalcKey("g", "g", 0, KeyCategory.VARIABLE),
            SciCalcKey("h", "h", 0, KeyCategory.VARIABLE)
        ),
        listOf(
            SciCalcKey("i", "i", 0, KeyCategory.VARIABLE),
            SciCalcKey("j", "j", 0, KeyCategory.VARIABLE),
            SciCalcKey("k", "k", 0, KeyCategory.VARIABLE),
            SciCalcKey("l", "l", 0, KeyCategory.VARIABLE),
            SciCalcKey("m", "m", 0, KeyCategory.VARIABLE),
            SciCalcKey("n", "n", 0, KeyCategory.VARIABLE),
            SciCalcKey("o", "o", 0, KeyCategory.VARIABLE),
            SciCalcKey("p", "p", 0, KeyCategory.VARIABLE)
        ),
        listOf(
            SciCalcKey("q", "q", 0, KeyCategory.VARIABLE),
            SciCalcKey("r", "r", 0, KeyCategory.VARIABLE),
            SciCalcKey("s", "s", 0, KeyCategory.VARIABLE),
            SciCalcKey("t", "t", 0, KeyCategory.VARIABLE),
            SciCalcKey("u", "u", 0, KeyCategory.VARIABLE),
            SciCalcKey("v", "v", 0, KeyCategory.VARIABLE),
            SciCalcKey("w", "w", 0, KeyCategory.VARIABLE),
            SciCalcKey("x", "x", 0, KeyCategory.VARIABLE)
        ),
        listOf(
            SciCalcKey("y", "y", 0, KeyCategory.VARIABLE),
            SciCalcKey("z", "z", 0, KeyCategory.VARIABLE),
            SciCalcKey(
                label = "α",
                insertText = "α",
                cursorOffset = 0,
                category = KeyCategory.VARIABLE,
                hasAlternate = true,
                alternates = listOf(
                    SciCalcKey("α", "α", 0, KeyCategory.VARIABLE),
                    SciCalcKey("γ", "γ", 0, KeyCategory.VARIABLE),
                    SciCalcKey("δ", "δ", 0, KeyCategory.VARIABLE),
                    SciCalcKey("ε", "ε", 0, KeyCategory.VARIABLE)
                )
            ),
            SciCalcKey(
                label = "β",
                insertText = "β",
                cursorOffset = 0,
                category = KeyCategory.VARIABLE,
                hasAlternate = true,
                alternates = listOf(
                    SciCalcKey("β", "β", 0, KeyCategory.VARIABLE),
                    SciCalcKey("λ", "λ", 0, KeyCategory.VARIABLE),
                    SciCalcKey("μ", "μ", 0, KeyCategory.VARIABLE)
                )
            ),
            SciCalcKey(
                label = "θ",
                insertText = "θ",
                cursorOffset = 0,
                category = KeyCategory.VARIABLE,
                hasAlternate = true,
                alternates = listOf(
                    SciCalcKey("θ", "θ", 0, KeyCategory.VARIABLE),
                    SciCalcKey("π", "π", 0, KeyCategory.VARIABLE),
                    SciCalcKey("σ", "σ", 0, KeyCategory.VARIABLE)
                )
            ),
            SciCalcKey(
                label = "ρ",
                insertText = "ρ",
                cursorOffset = 0,
                category = KeyCategory.VARIABLE,
                hasAlternate = true,
                alternates = listOf(
                    SciCalcKey("ρ", "ρ", 0, KeyCategory.VARIABLE),
                    SciCalcKey("τ", "τ", 0, KeyCategory.VARIABLE),
                    SciCalcKey("ω", "ω", 0, KeyCategory.VARIABLE)
                )
            ),
            SciCalcKey(
                label = "Φ",
                insertText = "φ",
                cursorOffset = 0,
                category = KeyCategory.VARIABLE,
                hasAlternate = true,
                alternates = listOf(
                    SciCalcKey("Φ", "φ", 0, KeyCategory.VARIABLE),
                    SciCalcKey("Δ", "Δ", 0, KeyCategory.VARIABLE)
                )
            ),
            SciCalcKey(",", ", ", 0, KeyCategory.OPERATOR)
        )
    )
}
