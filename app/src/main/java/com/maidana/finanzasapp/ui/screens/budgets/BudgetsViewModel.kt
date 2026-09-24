package com.maidana.finanzasapp.ui.screens.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidana.finanzasapp.data.model.Budget
import com.maidana.finanzasapp.data.model.CategoryConstants
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.data.repository.BudgetRepository
import com.maidana.finanzasapp.data.repository.FirestoreBudgetRepository
import com.maidana.finanzasapp.data.repository.FirestoreTransactionRepository
import com.maidana.finanzasapp.data.repository.TransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface BudgetsStatus {
    data object Loading : BudgetsStatus
    data object Success : BudgetsStatus
    data object Empty : BudgetsStatus
    data class Error(val message: String) : BudgetsStatus
}

sealed interface SaveBudgetState {
    data object Idle : SaveBudgetState
    data object Loading : SaveBudgetState
    data object Success : SaveBudgetState
    data class Error(val message: String) : SaveBudgetState
}

sealed interface DeleteBudgetState {
    data object Idle : DeleteBudgetState
    data object Loading : DeleteBudgetState
    data object Success : DeleteBudgetState
    data class Error(val message: String) : DeleteBudgetState
}

data class BudgetsUiState(
    val status: BudgetsStatus = BudgetsStatus.Loading,
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val budgets: List<Budget> = emptyList(),
    val errorMessage: String? = null
) {
    val isLoading: Boolean get() = status is BudgetsStatus.Loading
    val isEmpty: Boolean get() = status is BudgetsStatus.Empty
    val isError: Boolean get() = status is BudgetsStatus.Error

    val totalLimit: Long get() = budgets.sumOf { it.limitAmount }
    val totalSpent: Long get() = budgets.sumOf { it.spentAmount }
    val totalRemaining: Long get() = (totalLimit - totalSpent).coerceAtLeast(0L)
    val globalPercentage: Float get() = if (totalLimit > 0L) totalSpent.toFloat() / totalLimit.toFloat() else 0f
    val periodLabel: String get() = "${BudgetsViewModel.getMonthName(selectedMonth)} $selectedYear"
}

class BudgetsViewModel(
    private val budgetRepository: BudgetRepository = FirestoreBudgetRepository(),
    private val transactionRepository: TransactionRepository = FirestoreTransactionRepository(),
    private val currentTimeProvider: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val initialCalendar = Calendar.getInstance().apply { timeInMillis = currentTimeProvider() }
    private val _selectedMonth = MutableStateFlow(initialCalendar.get(Calendar.MONTH) + 1)
    private val _selectedYear = MutableStateFlow(initialCalendar.get(Calendar.YEAR))

    private val _uiState = MutableStateFlow(
        BudgetsUiState(
            selectedMonth = _selectedMonth.value,
            selectedYear = _selectedYear.value
        )
    )
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveBudgetState>(SaveBudgetState.Idle)
    val saveState: StateFlow<SaveBudgetState> = _saveState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteBudgetState>(DeleteBudgetState.Idle)
    val deleteState: StateFlow<DeleteBudgetState> = _deleteState.asStateFlow()

    private var observeJob: Job? = null
    private var allRawBudgets: List<Budget> = emptyList()

    init {
        loadData()
    }

    fun loadData() {
        observeJob?.cancel()
        _uiState.update { it.copy(status = BudgetsStatus.Loading, errorMessage = null) }

        observeJob = viewModelScope.launch {
            combine(
                budgetRepository.getBudgets(),
                transactionRepository.getTransactions(),
                _selectedMonth,
                _selectedYear
            ) { rawBudgets, transactions, month, year ->
                allRawBudgets = rawBudgets
                val filteredBudgets = rawBudgets.filter { it.month == month && it.year == year }
                val computedBudgets = filteredBudgets.map { budget ->
                    val spent = calculateSpentAmount(budget, transactions)
                    budget.copy(spentAmount = spent)
                }

                val status = if (computedBudgets.isEmpty()) {
                    BudgetsStatus.Empty
                } else {
                    BudgetsStatus.Success
                }

                BudgetsUiState(
                    status = status,
                    selectedMonth = month,
                    selectedYear = year,
                    budgets = computedBudgets,
                    errorMessage = null
                )
            }
            .catch { exception ->
                val msg = exception.message?.takeIf { it.isNotBlank() }
                    ?: "No se pudieron cargar los presupuestos."
                _uiState.update {
                    it.copy(
                        status = BudgetsStatus.Error(msg),
                        errorMessage = msg
                    )
                }
            }
            .collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun selectPeriod(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    fun previousMonth() {
        if (_selectedMonth.value == 1) {
            _selectedMonth.value = 12
            _selectedYear.value -= 1
        } else {
            _selectedMonth.value -= 1
        }
    }

    fun nextMonth() {
        if (_selectedMonth.value == 12) {
            _selectedMonth.value = 1
            _selectedYear.value += 1
        } else {
            _selectedMonth.value += 1
        }
    }

    fun saveBudget(category: String, limitAmount: Long, month: Int, year: Int, categoryId: String = "") {
        val catTrimmed = category.trim()
        if (catTrimmed.isBlank()) {
            _saveState.value = SaveBudgetState.Error("Debes seleccionar una categoría.")
            return
        }
        if (limitAmount <= 0L) {
            _saveState.value = SaveBudgetState.Error("El límite mensual debe ser mayor a 0.")
            return
        }
        if (month !in 1..12 || year < 2000 || year > 2100) {
            _saveState.value = SaveBudgetState.Error("El período ingresado no es válido.")
            return
        }

        val resolvedCategoryId = categoryId.trim().ifBlank {
            CategoryConstants.PREDEFINED_EXPENSE_CATEGORIES.firstOrNull {
                it.name.equals(catTrimmed, ignoreCase = true)
            }?.id.orEmpty()
        }

        val isDuplicate = allRawBudgets.any {
            it.month == month && it.year == year && (
                (it.categoryId.isNotBlank() && resolvedCategoryId.isNotBlank() && it.categoryId == resolvedCategoryId) ||
                categoriesMatch(it.category, catTrimmed)
            )
        }
        if (isDuplicate) {
            _saveState.value = SaveBudgetState.Error(
                "Ya existe un presupuesto para la categoría '$catTrimmed' en ${getMonthName(month)} $year."
            )
            return
        }

        _saveState.value = SaveBudgetState.Loading
        viewModelScope.launch {
            val newBudget = Budget(
                category = catTrimmed,
                categoryId = resolvedCategoryId,
                limitAmount = limitAmount,
                month = month,
                year = year,
                createdAt = currentTimeProvider()
            )
            val result = budgetRepository.addBudget(newBudget)
            result.fold(
                onSuccess = {
                    _selectedMonth.value = month
                    _selectedYear.value = year
                    _saveState.value = SaveBudgetState.Success
                },
                onFailure = { e ->
                    _saveState.value = SaveBudgetState.Error(
                        e.message?.takeIf { it.isNotBlank() } ?: "Error al guardar el presupuesto."
                    )
                }
            )
        }
    }

    fun deleteBudget(id: String) {
        _deleteState.value = DeleteBudgetState.Loading
        viewModelScope.launch {
            val result = budgetRepository.deleteBudget(id)
            result.fold(
                onSuccess = {
                    _deleteState.value = DeleteBudgetState.Success
                },
                onFailure = { e ->
                    _deleteState.value = DeleteBudgetState.Error(
                        e.message?.takeIf { it.isNotBlank() } ?: "No se pudo eliminar el presupuesto."
                    )
                }
            )
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveBudgetState.Idle
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteBudgetState.Idle
    }

    companion object {
        fun calculateSpentAmount(budget: Budget, transactions: List<Transaction>): Long {
            return transactions
                .filter { tx ->
                    tx.type == TransactionType.GASTO &&
                    matchesPeriod(tx.date, budget.month, budget.year) &&
                    categoriesMatch(budget, tx)
                }
                .sumOf { it.amount }
        }

        fun matchesPeriod(timestamp: Long, month: Int, year: Int): Boolean {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            return (cal.get(Calendar.MONTH) + 1) == month && cal.get(Calendar.YEAR) == year
        }

        fun categoriesMatch(budget: Budget, transaction: Transaction): Boolean {
            if (budget.categoryId.isNotBlank() && transaction.categoryId.isNotBlank()) {
                if (budget.categoryId == transaction.categoryId) return true
            }
            return categoriesMatch(budget.category, transaction.category)
        }

        fun categoriesMatch(budgetCategory: String, transactionCategory: String): Boolean {
            val b = budgetCategory.trim().lowercase()
            val t = transactionCategory.trim().lowercase()
            if (b == t) return true
            if (b == "vivienda y servicios" && (t == "vivienda" || t == "servicios")) return true
            if (b == "salidas y ocio" && t == "ocio") return true
            if (b == "salud y cuidado" && t == "salud") return true
            return false
        }

        fun getMonthName(month: Int): String {
            return when (month) {
                1 -> "Enero"
                2 -> "Febrero"
                3 -> "Marzo"
                4 -> "Abril"
                5 -> "Mayo"
                6 -> "Junio"
                7 -> "Julio"
                8 -> "Agosto"
                9 -> "Septiembre"
                10 -> "Octubre"
                11 -> "Noviembre"
                12 -> "Diciembre"
                else -> "Mes $month"
            }
        }
    }
}
