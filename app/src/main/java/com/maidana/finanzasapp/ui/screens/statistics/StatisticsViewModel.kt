package com.maidana.finanzasapp.ui.screens.statistics

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidana.finanzasapp.data.model.Account
import com.maidana.finanzasapp.data.model.AccountDistributionStat
import com.maidana.finanzasapp.data.model.AccountType
import com.maidana.finanzasapp.data.model.Budget
import com.maidana.finanzasapp.data.model.BudgetHealthStatus
import com.maidana.finanzasapp.data.model.BudgetPerformanceStat
import com.maidana.finanzasapp.data.model.CategoryExpenseStat
import com.maidana.finanzasapp.data.model.MonthlyEvolutionPoint
import com.maidana.finanzasapp.data.model.PeriodFinancialSummary
import com.maidana.finanzasapp.data.model.StatisticsPeriod
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.data.repository.AccountRepository
import com.maidana.finanzasapp.data.repository.BudgetRepository
import com.maidana.finanzasapp.data.repository.FirestoreAccountRepository
import com.maidana.finanzasapp.data.repository.FirestoreBudgetRepository
import com.maidana.finanzasapp.data.repository.FirestoreTransactionRepository
import com.maidana.finanzasapp.data.repository.TransactionRepository
import com.maidana.finanzasapp.ui.components.getCategoryIcon
import com.maidana.finanzasapp.ui.screens.budgets.BudgetsViewModel
import com.maidana.finanzasapp.ui.theme.ExpenseRed
import com.maidana.finanzasapp.ui.theme.IncomeGreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    val selectedPeriod: String = "Mes actual",
    val periods: List<String> = listOf("Mes actual", "Mes anterior", "3 meses", "6 meses", "Año actual"),
    val status: StatisticsStatus = StatisticsStatus.Loading,
    val summary: PeriodFinancialSummary = PeriodFinancialSummary(),
    val monthlyComparisons: List<MonthlyEvolutionPoint> = emptyList(),
    val categoryExpenses: List<CategoryExpenseStat> = emptyList(),
    val allCategoryExpenses: List<CategoryExpenseStat> = emptyList(),
    val balanceEvolution: List<MonthlyEvolutionPoint> = emptyList(),
    val accountDistributions: List<AccountDistributionStat> = emptyList(),
    val budgetPerformances: List<BudgetPerformanceStat> = emptyList(),
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
    private val budgetRepository: BudgetRepository? = null,
    private val accountRepository: AccountRepository? = null,
    private val currentTimeProvider: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val safeBudgetRepository: BudgetRepository? by lazy {
        budgetRepository ?: runCatching { FirestoreBudgetRepository() }.getOrNull()
    }

    private val safeAccountRepository: AccountRepository? by lazy {
        accountRepository ?: runCatching { FirestoreAccountRepository() }.getOrNull()
    }

    private val _selectedPeriod = MutableStateFlow("Mes actual")
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
            val transactionsFlow = transactionRepository.getTransactions()
            val budgetsFlow = safeBudgetRepository?.getBudgets() ?: flowOf(emptyList())
            val accountsFlow = safeAccountRepository?.getAccounts() ?: flowOf(emptyList())

            combine(
                transactionsFlow,
                budgetsFlow,
                accountsFlow,
                _selectedPeriod
            ) { transactions, budgets, accounts, period ->
                calculateStatistics(transactions, budgets, accounts, period, currentTimeProvider())
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

    fun calculateStatistics(
        allTransactions: List<Transaction>,
        budgets: List<Budget>,
        accounts: List<Account>,
        period: String,
        nowMs: Long
    ): StatisticsUiState {
        val parsedPeriod = StatisticsPeriod.fromString(period)
        val months = computeMonthWindowForPeriod(parsedPeriod, nowMs)

        val oldestMonth = months.first()
        val newestMonth = months.last()
        val startTimestamp = getPeriodStartTimestamp(oldestMonth)
        val endTimestamp = getPeriodEndTimestamp(newestMonth)

        // Transacciones del período seleccionado
        val periodTransactions = allTransactions.filter { it.date in startTimestamp..endTimestamp }

        // 4. Distribución por Cuenta (Derivado global)
        val accountDistributions = calculateAccountDistributions(accounts, allTransactions)

        if (periodTransactions.isEmpty()) {
            return StatisticsUiState(
                selectedPeriod = period,
                status = StatisticsStatus.Empty,
                summary = PeriodFinancialSummary(),
                monthlyComparisons = emptyList(),
                categoryExpenses = emptyList(),
                allCategoryExpenses = emptyList(),
                balanceEvolution = emptyList(),
                accountDistributions = accountDistributions,
                budgetPerformances = emptyList(),
                errorMessage = null
            )
        }

        // 1. Resumen Superior (KPIs)
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

        // 2. Gráfico 1 & 3: Comparación Mensual Ingresos vs Gastos y Evolución del Saldo
        // Saldo patrimonial inicial previo al inicio del período
        val totalInitialBalance = accounts.sumOf { it.initialBalance }
        val priorTransactionsNet = allTransactions
            .filter { it.date < startTimestamp }
            .sumOf { if (it.type == TransactionType.INGRESO) it.amount else -it.amount }

        var runningBalance = totalInitialBalance + priorTransactionsNet
        val monthlyPoints = months.map { my ->
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
                balance = runningBalance,
                isDeficit = exp > inc
            )
        }

        // 3. Gráfico 2: Gastos por Categoría (Top 5 + Otros)
        val expenseTxs = periodTransactions.filter { it.type == TransactionType.GASTO }
        val (topCategories, allCategories) = calculateCategoryExpenses(expenseTxs, totalExpense)

        // 5. Gráfico 5: Presupuesto vs Gasto Real (del mes representativo)
        val targetMonthYear = if (parsedPeriod == StatisticsPeriod.PREVIOUS_MONTH) oldestMonth else newestMonth
        val budgetPerformances = calculateBudgetPerformance(budgets, allTransactions, targetMonthYear)

        return StatisticsUiState(
            selectedPeriod = period,
            status = StatisticsStatus.Success,
            summary = summary,
            monthlyComparisons = monthlyPoints,
            categoryExpenses = topCategories,
            allCategoryExpenses = allCategories,
            balanceEvolution = monthlyPoints,
            accountDistributions = accountDistributions,
            budgetPerformances = budgetPerformances,
            errorMessage = null
        )
    }

    private fun calculateCategoryExpenses(
        expenseTxs: List<Transaction>,
        totalExpense: Long
    ): Pair<List<CategoryExpenseStat>, List<CategoryExpenseStat>> {
        if (totalExpense <= 0L || expenseTxs.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }

        val grouped = expenseTxs.groupBy { it.category.trim().ifBlank { "Varios" } }
        var paletteIndex = 0
        val allList = grouped.map { (cat, txs) ->
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
                color = finalColor,
                isOthers = false,
                subcategoriesCount = 1
            )
        }.sortedByDescending { it.amount }

        // Si hay más de 5 categorías, agrupar de la 5ta en adelante en "Otras categorías"
        val topList = if (allList.size > 5) {
            val primary = allList.take(4)
            val others = allList.drop(4)
            val othersAmount = others.sumOf { it.amount }
            val othersPercentage = others.map { it.percentage }.sum()
            val othersItem = CategoryExpenseStat(
                category = "Otras categorías",
                amount = othersAmount,
                percentage = othersPercentage,
                color = Color(0xFF94A3B8), // Slate gray
                isOthers = true,
                subcategoriesCount = others.size
            )
            primary + othersItem
        } else {
            allList
        }

        return Pair(topList, allList)
    }

    private fun calculateAccountDistributions(
        accounts: List<Account>,
        allTransactions: List<Transaction>
    ): List<AccountDistributionStat> {
        if (accounts.isEmpty()) return emptyList()

        val accountStats = accounts.map { account ->
            val inc = allTransactions
                .filter { it.type == TransactionType.INGRESO && it.accountId == account.id }
                .sumOf { it.amount }
            val exp = allTransactions
                .filter { it.type == TransactionType.GASTO && it.accountId == account.id }
                .sumOf { it.amount }
            val balance = account.initialBalance + inc - exp

            val color = when (account.type) {
                AccountType.CASH -> IncomeGreen
                AccountType.DIGITAL -> Color(0xFF3B82F6) // Material Blue
            }

            AccountDistributionStat(
                accountId = account.id,
                name = account.name,
                type = account.type,
                balance = balance,
                percentage = 0f,
                color = color
            )
        }

        val totalPositive = accountStats.sumOf { it.balance.coerceAtLeast(0L) }
        return accountStats.map { stat ->
            val pct = if (totalPositive > 0L) {
                (stat.balance.coerceAtLeast(0L).toFloat() / totalPositive.toFloat())
            } else {
                0f
            }
            stat.copy(percentage = pct)
        }
    }

    private fun calculateBudgetPerformance(
        budgets: List<Budget>,
        allTransactions: List<Transaction>,
        targetMonthYear: MonthYear
    ): List<BudgetPerformanceStat> {
        val targetMonth = targetMonthYear.month + 1 // 1..12
        val targetYear = targetMonthYear.year

        val monthBudgets = budgets.filter { it.month == targetMonth && it.year == targetYear }
        if (monthBudgets.isEmpty()) return emptyList()

        val monthExpenseTxs = allTransactions.filter { tx ->
            tx.type == TransactionType.GASTO && matchesMonth(tx.date, targetYear, targetMonthYear.month)
        }

        return monthBudgets.map { budget ->
            val spent = monthExpenseTxs
                .filter { tx -> BudgetsViewModel.categoriesMatch(budget, tx) }
                .sumOf { it.amount }

            val percentage = if (budget.limitAmount > 0L) {
                spent.toFloat() / budget.limitAmount.toFloat()
            } else {
                0f
            }

            val status = when {
                percentage > 1.0f -> BudgetHealthStatus.EXCEEDED
                percentage >= 0.8f -> BudgetHealthStatus.WARNING
                else -> BudgetHealthStatus.NORMAL
            }

            BudgetPerformanceStat(
                category = budget.category,
                categoryId = budget.categoryId,
                limitAmount = budget.limitAmount,
                spentAmount = spent,
                percentage = percentage,
                status = status
            )
        }.sortedByDescending { it.percentage }
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
                "1m", "1 mes", "mes actual", "este mes" -> 1
                "mes ant", "mes anterior", "anterior" -> 1
                "3m", "3 meses", "ultimos 3 meses", "últimos 3 meses" -> 3
                "6m", "6 meses", "ultimos 6 meses", "últimos 6 meses" -> 6
                "año", "ano", "12m", "1y", "1a", "año actual", "este año" -> 12
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

        fun computeMonthWindowForPeriod(period: StatisticsPeriod, nowMs: Long): List<MonthYear> {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMs
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            return when (period) {
                StatisticsPeriod.CURRENT_MONTH -> {
                    listOf(MonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)))
                }
                StatisticsPeriod.PREVIOUS_MONTH -> {
                    cal.add(Calendar.MONTH, -1)
                    listOf(MonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)))
                }
                StatisticsPeriod.LAST_3_MONTHS -> {
                    val list = mutableListOf<MonthYear>()
                    cal.add(Calendar.MONTH, -2)
                    repeat(3) {
                        list.add(MonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)))
                        cal.add(Calendar.MONTH, 1)
                    }
                    list
                }
                StatisticsPeriod.LAST_6_MONTHS -> {
                    val list = mutableListOf<MonthYear>()
                    cal.add(Calendar.MONTH, -5)
                    repeat(6) {
                        list.add(MonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)))
                        cal.add(Calendar.MONTH, 1)
                    }
                    list
                }
                StatisticsPeriod.CURRENT_YEAR -> {
                    val list = mutableListOf<MonthYear>()
                    cal.add(Calendar.MONTH, -11)
                    repeat(12) {
                        list.add(MonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)))
                        cal.add(Calendar.MONTH, 1)
                    }
                    list
                }
            }
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
