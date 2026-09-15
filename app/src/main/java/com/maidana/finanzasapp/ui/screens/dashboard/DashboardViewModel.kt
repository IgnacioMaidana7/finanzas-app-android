package com.maidana.finanzasapp.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidana.finanzasapp.data.mock.MockDataProvider
import com.maidana.finanzasapp.data.model.Budget
import com.maidana.finanzasapp.data.model.SavingsGoal
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.data.repository.AuthRepository
import com.maidana.finanzasapp.data.repository.BudgetRepository
import com.maidana.finanzasapp.data.repository.FirebaseAuthRepository
import com.maidana.finanzasapp.data.repository.FirestoreBudgetRepository
import com.maidana.finanzasapp.data.repository.FirestoreTransactionRepository
import com.maidana.finanzasapp.data.repository.TransactionRepository
import com.maidana.finanzasapp.ui.screens.budgets.BudgetsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    // Presupuestos reales calculados para el mes actual:
    val budgets: List<Budget> = emptyList(),
    // Mocks que continúan usándose:
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
    private val budgetRepository: BudgetRepository = FirestoreBudgetRepository(),
    private val currentTimeProvider: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            userName = authRepository.getCurrentUser()?.displayName?.ifBlank { null }
                ?: MockDataProvider.user.displayName.ifBlank { "Usuario" }
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var loadDataJob: Job? = null

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
        loadDataJob?.cancel()
        _uiState.update { it.copy(status = DashboardStatus.Loading, errorMessage = null) }

        loadDataJob = viewModelScope.launch {
            combine(
                transactionRepository.getTransactions(),
                budgetRepository.getBudgets()
            ) { transactions, rawBudgets ->
                val now = currentTimeProvider()
                val nowCal = Calendar.getInstance().apply { timeInMillis = now }
                val currentMonth = nowCal.get(Calendar.MONTH) + 1
                val currentYear = nowCal.get(Calendar.YEAR)

                val newStatus = if (transactions.isEmpty()) {
                    DashboardStatus.Empty
                } else {
                    DashboardStatus.Success
                }

                // 1. Saldo Total histórico
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

                // 3. Presupuestos reales del mes actual con cálculo dinámico de consumo
                val currentMonthBudgets = rawBudgets
                    .filter { it.month == currentMonth && it.year == currentYear }
                    .map { budget ->
                        val spent = currentMonthTransactions
                            .filter { tx ->
                                tx.type == TransactionType.GASTO &&
                                BudgetsViewModel.categoriesMatch(budget.category, tx.category)
                            }
                            .sumOf { it.amount }
                        budget.copy(spentAmount = spent)
                    }

                // 4. Últimos 4 movimientos reales ordenados por fecha descendente
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
                        budgets = currentMonthBudgets,
                        errorMessage = null
                    )
                }
            }
            .catch { exception ->
                val msg = exception.message?.takeIf { it.isNotBlank() }
                    ?: "No se pudieron cargar los datos del Dashboard."
                _uiState.update {
                    it.copy(
                        status = DashboardStatus.Error(msg),
                        errorMessage = msg
                    )
                }
            }
            .collect {}
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
