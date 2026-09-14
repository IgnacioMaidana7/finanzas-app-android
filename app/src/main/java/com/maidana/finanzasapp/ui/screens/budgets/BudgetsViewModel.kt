package com.maidana.finanzasapp.ui.screens.budgets

import androidx.lifecycle.ViewModel
import com.maidana.finanzasapp.data.mock.MockDataProvider
import com.maidana.finanzasapp.data.model.Budget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BudgetsUiState(
    val budgets: List<Budget> = MockDataProvider.budgets,
    val isLoading: Boolean = false
) {
    val totalLimit: Double get() = budgets.sumOf { it.limitAmount }
    val totalSpent: Double get() = budgets.sumOf { it.spentAmount }
    val totalRemaining: Double get() = (totalLimit - totalSpent).coerceAtLeast(0.0)
    val globalPercentage: Float get() = if (totalLimit > 0) (totalSpent / totalLimit).toFloat() else 0f
}

class BudgetsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    fun addBudget(category: String, limitAmount: Double, month: String = "Septiembre 2026") {
        val newBudget = Budget(
            id = "bg_${System.currentTimeMillis()}",
            category = category,
            limitAmount = limitAmount,
            spentAmount = 0.0,
            month = month
        )
        _uiState.update { current ->
            current.copy(budgets = current.budgets + newBudget)
        }
    }
}
