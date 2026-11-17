package com.uncode.app.math.ui.keyboard

enum class KeyCategory {
    NUMBER,
    OPERATOR,
    VARIABLE,
    FUNCTION,
    ACTION
}

enum class KeyboardTab(val topLine: String, val bottomLine: String) {
    BASIC("+ −", "× ÷"),
    FUNCTIONS("f(x) e", "log ln"),
    TRIG("sin cos", "tan cot"),
    CALCULUS("lim dx", "∫ ∑ ∞"),
    ABC("abc", "")
}

data class SciCalcKey(
    val label: String,
    val insertText: String,
    val cursorOffset: Int = 0,
    val category: KeyCategory = KeyCategory.VARIABLE,
    val isWide: Boolean = false,
    val isSubscriptOrSuperscript: Boolean = false,
    val hasAlternate: Boolean = false,
    val altInsertText: String? = null,
    val altCursorOffset: Int = 0,
    val alternates: List<SciCalcKey> = emptyList()
)
