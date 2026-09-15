package com.maidana.finanzasapp.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidana.finanzasapp.data.mock.MockDataProvider
import com.maidana.finanzasapp.data.model.Budget
import com.maidana.finanzasapp.data.model.SavingsGoal
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.data.repository.AuthRepository
import com.maidana.finanzasapp.data.repository.FirebaseAuthRepository
import com.maidana.finanzasapp.data.repository.FirestoreTransactionRepository
import com.maidana.finanzasapp.data.repository.TransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

sealed interface DashboardStatus {
    data object Loading : DashboardStatus
    data object Success : DashboardStatus
    data object Empty : DashboardStatus
    data class Error(val message: String) : DashboardStatus
}

data class DashboardUiState(
    val userName: String = "Usuario",
    val status: DashboardStatus = DashboardStatus.Loading,
    val totalBalance: Long = 0L,
    val monthlyIncome: Long = 0L,
    val monthlyExpense: Long = 0L,
    val monthlySavings: Long = 0L,
    val savingsRateText: String = "0%",
    val expenseRateText: String = "0% del ingreso",
    val recentTransactions: List<Transaction> = emptyList(),
    val errorMessage: String? = null,
    // Mocks que continúan usándose:
    val budgets: List<Budget> = MockDataProvider.budgets.take(3),
    val featuredGoal: SavingsGoal? = MockDataProvider.goals.firstOrNull()
) {
    val isLoading: Boolean get() = status is DashboardStatus.Loading
    val isSuccess: Boolean get() = status is DashboardStatus.Success
    val isEmpty: Boolean get() = status is DashboardStatus.Empty
    val isError: Boolean get() = status is DashboardStatus.Error
}

class DashboardViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val transactionRepository: TransactionRepository = FirestoreTransactionRepository(),
    private val currentTimeProvider: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            userName = authRepository.getCurrentUser()?.displayName?.ifBlank { null }
                ?: MockDataProvider.user.displayName.ifBlank { "Usuario" }
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var loadTransactionsJob: Job? = null

    init {
        observeUser()
        loadData()
    }

    private fun observeUser() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                val name = user?.displayName?.ifBlank { user.email.substringBefore("@") }
                    ?: MockDataProvider.user.displayName.ifBlank { "Usuario" }
                _uiState.update { it.copy(userName = name) }
            }
        }
    }

    fun loadData() {
        loadTransactionsJob?.cancel()
        _uiState.update { it.copy(status = DashboardStatus.Loading, errorMessage = null) }

        loadTransactionsJob = viewModelScope.launch {
            transactionRepository.getTransactions()
                .catch { exception ->
                    val msg = exception.message?.takeIf { it.isNotBlank() }
                        ?: "No se pudieron cargar los movimientos del Dashboard."
                    _uiState.update {
                        it.copy(
                            status = DashboardStatus.Error(msg),
                            errorMessage = msg
                        )
                    }
                }
                .collect { transactions ->
                    val now = currentTimeProvider()
                    val newStatus = if (transactions.isEmpty()) {
                        DashboardStatus.Empty
                    } else {
                        DashboardStatus.Success
                    }

                    // 1. Saldo Total histórico: suma total de ingresos históricos - suma total de gastos históricos
                    val totalIncomeHistorical = transactions
                        .filter { it.type == TransactionType.INGRESO }
                        .sumOf { it.amount }
                    val totalExpenseHistorical = transactions
                        .filter { it.type == TransactionType.GASTO }
                        .sumOf { it.amount }
                    val totalBalance = totalIncomeHistorical - totalExpenseHistorical

                    // 2. Período: mes y año actual
                    val currentMonthTransactions = transactions.filter { isSameMonthAndYear(it.date, now) }
                    val monthlyIncome = currentMonthTransactions
                        .filter { it.type == TransactionType.INGRESO }
                        .sumOf { it.amount }
                    val monthlyExpense = currentMonthTransactions
                        .filter { it.type == TransactionType.GASTO }
                        .sumOf { it.amount }
                    val monthlySavings = monthlyIncome - monthlyExpense

                    // Textos auxiliares de porcentaje
                    val savingsRateText = if (monthlyIncome > 0L) {
                        val rate = ((monthlySavings.coerceAtLeast(0L).toDouble() / monthlyIncome) * 100.0).roundToInt()
                        "Tasa de ahorro $rate%"
                    } else {
                        "Sin ingresos este mes"
                    }

                    val expenseRateText = if (monthlyIncome > 0L) {
                        val rate = ((monthlyExpense.toDouble() / monthlyIncome) * 100.0).roundToInt()
                        "$rate% del ingreso"
                    } else {
                        "Total gastos del mes"
                    }

                    // 3. Últimos 4 movimientos reales ordenados por fecha descendente
                    val recent = transactions.sortedByDescending { it.date }.take(4)

                    _uiState.update {
                        it.copy(
                            status = newStatus,
                            totalBalance = totalBalance,
                            monthlyIncome = monthlyIncome,
                            monthlyExpense = monthlyExpense,
                            monthlySavings = monthlySavings,
                            savingsRateText = savingsRateText,
                            expenseRateText = expenseRateText,
                            recentTransactions = recent,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    companion object {
        fun isSameMonthAndYear(timestamp: Long, nowTimestamp: Long): Boolean {
            val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp }
            val cal2 = Calendar.getInstance().apply { timeInMillis = nowTimestamp }
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
        }
    }
}
