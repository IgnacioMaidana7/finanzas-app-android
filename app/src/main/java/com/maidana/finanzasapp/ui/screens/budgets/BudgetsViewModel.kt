package com.maidana.finanzasapp.ui.screens.budgets

import androidx.lifecycle.ViewModel
import com.maidana.finanzasapp.data.model.Budget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BudgetsUiState(
    val budgets: List<Budget> = emptyList(),
    val isLoading: Boolean = false
)

class BudgetsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()
}
