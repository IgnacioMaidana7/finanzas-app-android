package com.maidana.finanzasapp.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidana.finanzasapp.data.mock.MockDataProvider
import com.maidana.finanzasapp.data.model.Budget
import com.maidana.finanzasapp.data.model.SavingsGoal
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.repository.AuthRepository
import com.maidana.finanzasapp.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val userName: String = "Usuario",
    val totalBalance: Double = 1540000.0,
    val monthlyIncome: Double = 1155000.0,
    val monthlyExpense: Double = 471100.0,
    val monthlySavings: Double = 683900.0,
    val recentTransactions: List<Transaction> = MockDataProvider.transactions.take(4),
    val budgets: List<Budget> = MockDataProvider.budgets.take(3),
    val featuredGoal: SavingsGoal? = MockDataProvider.goals.firstOrNull()
)

class DashboardViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        DashboardUiState(
            userName = authRepository.getCurrentUser()?.displayName?.ifBlank { null }
                ?: MockDataProvider.user.displayName.ifBlank { "Usuario" }
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                val name = user?.displayName?.ifBlank { user.email.substringBefore("@") }
                    ?: MockDataProvider.user.displayName.ifBlank { "Usuario" }
                _uiState.update { it.copy(userName = name) }
            }
        }
    }
}
