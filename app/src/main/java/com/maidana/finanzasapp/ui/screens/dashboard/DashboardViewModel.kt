package com.maidana.finanzasapp.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import com.maidana.finanzasapp.data.mock.MockDataProvider
import com.maidana.finanzasapp.data.model.Budget
import com.maidana.finanzasapp.data.model.SavingsGoal
import com.maidana.finanzasapp.data.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DashboardUiState(
    val userName: String = MockDataProvider.user.displayName.ifBlank { "Usuario" },
    val totalBalance: Double = 1540000.0,
    val monthlyIncome: Double = 1155000.0,
    val monthlyExpense: Double = 471100.0,
    val monthlySavings: Double = 683900.0,
    val recentTransactions: List<Transaction> = MockDataProvider.transactions.take(4),
    val budgets: List<Budget> = MockDataProvider.budgets.take(3),
    val featuredGoal: SavingsGoal? = MockDataProvider.goals.firstOrNull()
)

class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
}
