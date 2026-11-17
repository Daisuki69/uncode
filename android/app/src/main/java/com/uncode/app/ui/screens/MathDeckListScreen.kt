package com.uncode.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.uncode.app.viewmodel.MainViewModel

@Composable
fun MathDeckListScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSelectDeck: (Long) -> Unit,
    onNavigateToOverallStats: () -> Unit,
    onNavigateToManualEditor: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    DeckListScreen(
        viewModel = viewModel,
        onBack = onBack,
        onSelectDeck = onSelectDeck,
        onNavigateToOverallStats = onNavigateToOverallStats,
        onNavigateToTemplateLibrary = {},
        onNavigateToManualEditor = onNavigateToManualEditor,
        onNavigateToCustomImport = {},
        isMathOnly = true,
        modifier = modifier
    )
}
