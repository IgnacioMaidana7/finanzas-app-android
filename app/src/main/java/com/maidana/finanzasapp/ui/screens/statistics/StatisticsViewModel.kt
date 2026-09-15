package com.maidana.finanzasapp.ui.screens.statistics

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidana.finanzasapp.data.model.CategoryExpenseStat
import com.maidana.finanzasapp.data.model.MonthlyEvolutionPoint
import com.maidana.finanzasapp.data.model.PeriodFinancialSummary
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.data.repository.FirestoreTransactionRepository
import com.maidana.finanzasapp.data.repository.TransactionRepository
import com.maidana.finanzasapp.ui.components.getCategoryIcon
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

sealed interface StatisticsStatus {
    data object Loading : StatisticsStatus
    data object Success : StatisticsStatus
    data object Empty : StatisticsStatus
    data class Error(val message: String) : StatisticsStatus
}

data class StatisticsUiState(
    val selectedPeriod: String = "1M",
    val periods: List<String> = listOf("1M", "3M", "6M", "Año"),
    val status: StatisticsStatus = StatisticsStatus.Loading,
    val summary: PeriodFinancialSummary = PeriodFinancialSummary(),
    val categoryExpenses: List<CategoryExpenseStat> = emptyList(),
    val balanceEvolution: List<MonthlyEvolutionPoint> = emptyList(),
    val errorMessage: String? = null
) {
    val isLoading: Boolean get() = status is StatisticsStatus.Loading
    val isSuccess: Boolean get() = status is StatisticsStatus.Success
    val isEmpty: Boolean get() = status is StatisticsStatus.Empty
    val isError: Boolean get() = status is StatisticsStatus.Error
}

data class MonthYear(val year: Int, val month: Int) // month: 0..11

class StatisticsViewModel(
    private val transactionRepository: TransactionRepository = FirestoreTransactionRepository(),
    private val currentTimeProvider: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow("1M")
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var loadDataJob: Job? = null

    init {
        loadData()
    }

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
    }

    fun loadData() {
        loadDataJob?.cancel()
        _uiState.update { it.copy(status = StatisticsStatus.Loading, errorMessage = null) }

        loadDataJob = viewModelScope.launch {
            combine(
                transactionRepository.getTransactions(),
                _selectedPeriod
            ) { transactions, period ->
                calculateStatistics(transactions, period)
            }.catch { exception ->
                val msg = exception.message?.takeIf { it.isNotBlank() }
                    ?: "No se pudieron cargar las estadísticas."
                _uiState.update {
                    it.copy(
                        status = StatisticsStatus.Error(msg),
                        errorMessage = msg
                    )
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun calculateStatistics(
        allTransactions: List<Transaction>,
        period: String
    ): StatisticsUiState {
        val nowMs = currentTimeProvider()
        val monthCount = getMonthCount(period)
        val months = computeMonthWindow(monthCount, nowMs)

        val oldestMonth = months.first()
        val newestMonth = months.last()
        val startTimestamp = getPeriodStartTimestamp(oldestMonth)
        val endTimestamp = getPeriodEndTimestamp(newestMonth)

        // Transacciones del período seleccionado
        val periodTransactions = allTransactions.filter { it.date in startTimestamp..endTimestamp }

        if (periodTransactions.isEmpty()) {
            return StatisticsUiState(
                selectedPeriod = period,
                status = StatisticsStatus.Empty,
                summary = PeriodFinancialSummary(),
                categoryExpenses = emptyList(),
                balanceEvolution = emptyList(),
                errorMessage = null
            )
        }

        // 1. Resumen Financiero
        val totalIncome = periodTransactions
            .filter { it.type == TransactionType.INGRESO }
            .sumOf { it.amount }
        val totalExpense = periodTransactions
            .filter { it.type == TransactionType.GASTO }
            .sumOf { it.amount }
        val balance = totalIncome - totalExpense

        val savingsRate = if (totalIncome > 0L) {
            (balance.coerceAtLeast(0L).toDouble() / totalIncome.toDouble()).toFloat()
        } else {
            0f
        }
        val savingsRatePercent = (savingsRate * 100).roundToInt()
        val savingsRateText = if (totalIncome > 0L) "$savingsRatePercent%" else "0%"

        val summary = PeriodFinancialSummary(
            income = totalIncome,
            expense = totalExpense,
            balance = balance,
            savingsRate = savingsRate,
            savingsRateText = savingsRateText
        )

        // 2. Evolución del saldo cronológica (antiguo a reciente)
        // Saldo previo al inicio del período
        val initialBalance = allTransactions
            .filter { it.date < startTimestamp }
            .sumOf { if (it.type == TransactionType.INGRESO) it.amount else -it.amount }

        var runningBalance = initialBalance
        val balanceEvolution = months.map { my ->
            val monthTxs = periodTransactions.filter { matchesMonth(it.date, my.year, my.month) }
            val inc = monthTxs.filter { it.type == TransactionType.INGRESO }.sumOf { it.amount }
            val exp = monthTxs.filter { it.type == TransactionType.GASTO }.sumOf { it.amount }
            val monthlyBal = inc - exp
            runningBalance += monthlyBal

            MonthlyEvolutionPoint(
                month = getMonthAbbreviation(my.month),
                year = my.year,
                monthIndex = my.month,
                income = inc,
                expense = exp,
                monthlyBalance = monthlyBal,
                balance = runningBalance
            )
        }

        // 3. Gastos por categoría (solo GASTO, orden descendente por importe)
        val expenseTxs = periodTransactions.filter { it.type == TransactionType.GASTO }
        val categoryExpenses = if (totalExpense > 0L) {
            val grouped = expenseTxs.groupBy { it.category.trim().ifBlank { "Varios" } }
            var paletteIndex = 0
            grouped.map { (cat, txs) ->
                val amount = txs.sumOf { it.amount }
                val percentage = (amount.toDouble() / totalExpense.toDouble()).toFloat()
                val iconColor = getCategoryIcon(cat).color
                val finalColor = if (iconColor != Color(0xFF64748B)) {
                    iconColor
                } else {
                    palette[paletteIndex % palette.size].also { paletteIndex++ }
                }
                CategoryExpenseStat(
                    category = cat,
                    amount = amount,
                    percentage = percentage,
                    color = finalColor
                )
            }.sortedByDescending { it.amount }
        } else {
            emptyList()
        }

        return StatisticsUiState(
            selectedPeriod = period,
            status = StatisticsStatus.Success,
            summary = summary,
            categoryExpenses = categoryExpenses,
            balanceEvolution = balanceEvolution,
            errorMessage = null
        )
    }

    companion object {
        private val monthNames = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

        private val palette = listOf(
            Color(0xFF3B82F6), // Blue
            Color(0xFF10B981), // Green
            Color(0xFFF59E0B), // Amber
            Color(0xFFEC4899), // Pink
            Color(0xFF8B5CF6), // Purple
            Color(0xFF14B8A6), // Teal
            Color(0xFFEF4444), // Red
            Color(0xFF6366F1), // Indigo
            Color(0xFF0EA5E9), // Sky
            Color(0xFFF97316)  // Orange
        )

        fun getMonthCount(period: String): Int {
            return when (period.trim().lowercase()) {
                "1m", "1 mes" -> 1
                "3m", "3 meses" -> 3
                "6m", "6 meses" -> 6
                "año", "ano", "12m", "1y", "1a" -> 12
                else -> 1
            }
        }

        fun computeMonthWindow(monthCount: Int, nowMs: Long): List<MonthYear> {
            val list = mutableListOf<MonthYear>()
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMs
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, -(monthCount - 1))
            }
            repeat(monthCount) {
                list.add(MonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)))
                cal.add(Calendar.MONTH, 1)
            }
            return list
        }

        fun getPeriodStartTimestamp(oldest: MonthYear): Long {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, oldest.year)
                set(Calendar.MONTH, oldest.month)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun getPeriodEndTimestamp(newest: MonthYear): Long {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, newest.year)
                set(Calendar.MONTH, newest.month)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return cal.timeInMillis
        }

        fun matchesMonth(txDate: Long, year: Int, month: Int): Boolean {
            val cal = Calendar.getInstance().apply { timeInMillis = txDate }
            return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        }

        fun getMonthAbbreviation(monthIndex: Int): String {
            return monthNames.getOrElse(monthIndex) { "" }
        }
    }
}
