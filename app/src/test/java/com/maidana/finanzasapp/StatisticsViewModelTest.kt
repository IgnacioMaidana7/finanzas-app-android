package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.model.Account
import com.maidana.finanzasapp.data.model.AccountType
import com.maidana.finanzasapp.data.model.Budget
import com.maidana.finanzasapp.data.model.BudgetHealthStatus
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.ui.screens.statistics.StatisticsStatus
import com.maidana.finanzasapp.ui.screens.statistics.StatisticsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeTransactionRepository
    private lateinit var fakeBudgetRepository: FakeBudgetRepository
    private lateinit var fakeAccountRepository: FakeAccountRepository
    private lateinit var viewModel: StatisticsViewModel

    // Fecha fija de prueba: 15 de Septiembre de 2026
    private val fixedNow: Long = Calendar.getInstance().apply {
        set(2026, Calendar.SEPTEMBER, 15, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val sept10: Long = Calendar.getInstance().apply {
        set(2026, Calendar.SEPTEMBER, 10, 10, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val sept12: Long = Calendar.getInstance().apply {
        set(2026, Calendar.SEPTEMBER, 12, 10, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val ago20: Long = Calendar.getInstance().apply {
        set(2026, Calendar.AUGUST, 20, 10, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val jul15: Long = Calendar.getInstance().apply {
        set(2026, Calendar.JULY, 15, 10, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val may10: Long = Calendar.getInstance().apply {
        set(2026, Calendar.MAY, 10, 10, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val nov2025: Long = Calendar.getInstance().apply {
        set(2025, Calendar.NOVEMBER, 15, 10, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTransactionRepository()
        fakeBudgetRepository = FakeBudgetRepository()
        fakeAccountRepository = FakeAccountRepository("user_1")
        viewModel = StatisticsViewModel(
            transactionRepository = fakeRepository,
            budgetRepository = fakeBudgetRepository,
            accountRepository = fakeAccountRepository,
            currentTimeProvider = { fixedNow }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun status_whenNoTransactions_isEmpty() {
        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertEquals(0L, state.summary.income)
        assertEquals(0L, state.summary.expense)
        assertEquals(0L, state.summary.balance)
        assertTrue(state.categoryExpenses.isEmpty())
        assertTrue(state.balanceEvolution.isEmpty())
    }

    @Test
    fun filter_1Month_onlyIncludesCurrentMonthTransactions() {
        val txs = listOf(
            Transaction(id = "1", title = "Sept Ingreso", amount = 100000L, type = TransactionType.INGRESO, date = sept10),
            Transaction(id = "2", title = "Sept Gasto", amount = 40000L, type = TransactionType.GASTO, date = sept12),
            Transaction(id = "3", title = "Ago Gasto", amount = 50000L, type = TransactionType.GASTO, date = ago20)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("1M")

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertEquals(100000L, state.summary.income)
        assertEquals(40000L, state.summary.expense)
        assertEquals(60000L, state.summary.balance)
        // Serie temporal de 1M contiene 1 mes
        assertEquals(1, state.balanceEvolution.size)
        assertEquals("Sep", state.balanceEvolution[0].month)
    }

    @Test
    fun filter_3Months_includesLastThreeMonths() {
        val txs = listOf(
            Transaction(id = "1", title = "Sept", amount = 30000L, type = TransactionType.INGRESO, date = sept10),
            Transaction(id = "2", title = "Ago", amount = 20000L, type = TransactionType.INGRESO, date = ago20),
            Transaction(id = "3", title = "Jul", amount = 10000L, type = TransactionType.INGRESO, date = jul15),
            Transaction(id = "4", title = "May (Fuera)", amount = 50000L, type = TransactionType.INGRESO, date = may10)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("3M")

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertEquals(60000L, state.summary.income)
        // Serie temporal de 3M: Jul, Ago, Sep
        assertEquals(3, state.balanceEvolution.size)
        assertEquals("Jul", state.balanceEvolution[0].month)
        assertEquals("Ago", state.balanceEvolution[1].month)
        assertEquals("Sep", state.balanceEvolution[2].month)
    }

    @Test
    fun filter_6Months_includesLastSixMonths() {
        val txs = listOf(
            Transaction(id = "1", title = "Sept", amount = 10000L, type = TransactionType.INGRESO, date = sept10),
            Transaction(id = "2", title = "May", amount = 20000L, type = TransactionType.INGRESO, date = may10),
            Transaction(id = "3", title = "Nov 2025 (Fuera)", amount = 40000L, type = TransactionType.INGRESO, date = nov2025)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("6M")

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertEquals(30000L, state.summary.income)
        // Serie temporal de 6M contiene 6 meses (Abr a Sep 2026)
        assertEquals(6, state.balanceEvolution.size)
        assertEquals("Abr", state.balanceEvolution[0].month)
        assertEquals("Sep", state.balanceEvolution[5].month)
    }

    @Test
    fun filter_annual_includesLastTwelveMonths() {
        val txs = listOf(
            Transaction(id = "1", title = "Sept 26", amount = 10000L, type = TransactionType.INGRESO, date = sept10),
            Transaction(id = "2", title = "Nov 25", amount = 20000L, type = TransactionType.INGRESO, date = nov2025),
            // Hace 14 meses (Julio 2025 - fuera)
            Transaction(
                id = "3", title = "Jul 25", amount = 50000L, type = TransactionType.INGRESO,
                date = Calendar.getInstance().apply { set(2025, Calendar.JULY, 1) }.timeInMillis
            )
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("Año")

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertEquals(30000L, state.summary.income)
        assertEquals(12, state.balanceEvolution.size)
        assertEquals("Oct", state.balanceEvolution[0].month) // Octubre 2025
        assertEquals("Sep", state.balanceEvolution[11].month) // Septiembre 2026
    }

    @Test
    fun filter_withYearRollover_correctlySpansAcrossYearBoundary() {
        // Simular fecha actual: 15 de Febrero de 2027
        val feb2027Now = Calendar.getInstance().apply {
            set(2027, Calendar.FEBRUARY, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val feb2027Tx = Calendar.getInstance().apply { set(2027, Calendar.FEBRUARY, 5); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val ene2027Tx = Calendar.getInstance().apply { set(2027, Calendar.JANUARY, 10); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val dic2026Tx = Calendar.getInstance().apply { set(2026, Calendar.DECEMBER, 25); set(Calendar.MILLISECOND, 0) }.timeInMillis

        val localViewModel = StatisticsViewModel(
            transactionRepository = fakeRepository,
            currentTimeProvider = { feb2027Now }
        )

        val txs = listOf(
            Transaction(id = "1", title = "Dic 26", amount = 50000L, type = TransactionType.INGRESO, date = dic2026Tx),
            Transaction(id = "2", title = "Ene 27", amount = 40000L, type = TransactionType.INGRESO, date = ene2027Tx),
            Transaction(id = "3", title = "Feb 27", amount = 30000L, type = TransactionType.INGRESO, date = feb2027Tx)
        )
        fakeRepository.emitTransactions(txs)
        localViewModel.setPeriod("3M")

        val state = localViewModel.uiState.value
        assertTrue(state.isSuccess)
        assertEquals(120000L, state.summary.income)
        assertEquals(3, state.balanceEvolution.size)
        assertEquals("Dic", state.balanceEvolution[0].month)
        assertEquals(2026, state.balanceEvolution[0].year)
        assertEquals("Ene", state.balanceEvolution[1].month)
        assertEquals(2027, state.balanceEvolution[1].year)
        assertEquals("Feb", state.balanceEvolution[2].month)
        assertEquals(2027, state.balanceEvolution[2].year)
    }

    @Test
    fun summary_totalIncome_and_totalExpenses_and_netBalance() {
        val txs = listOf(
            Transaction(id = "1", title = "Salario", amount = 300000L, type = TransactionType.INGRESO, date = sept10),
            Transaction(id = "2", title = "Freelance", amount = 100000L, type = TransactionType.INGRESO, date = sept10),
            Transaction(id = "3", title = "Alquiler", amount = 150000L, type = TransactionType.GASTO, date = sept12),
            Transaction(id = "4", title = "Servicios", amount = 50000L, type = TransactionType.GASTO, date = sept12)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("1M")

        val state = viewModel.uiState.value
        assertEquals(400000L, state.summary.income)
        assertEquals(200000L, state.summary.expense)
        assertEquals(200000L, state.summary.balance)
    }

    @Test
    fun summary_savingsRate_calculatedCorrectly() {
        val txs = listOf(
            Transaction(id = "1", title = "Ingreso", amount = 100000L, type = TransactionType.INGRESO, date = sept10),
            Transaction(id = "2", title = "Gasto", amount = 25000L, type = TransactionType.GASTO, date = sept12)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("1M")

        val state = viewModel.uiState.value
        // Balance = 75000 / 100000 = 75%
        assertEquals(0.75f, state.summary.savingsRate, 0.001f)
        assertEquals("75%", state.summary.savingsRateText)
    }

    @Test
    fun summary_savingsRate_whenIncomeIsZero_doesNotDivideByZero() {
        val txs = listOf(
            Transaction(id = "1", title = "Solo Gasto", amount = 50000L, type = TransactionType.GASTO, date = sept10)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("1M")

        val state = viewModel.uiState.value
        assertEquals(0L, state.summary.income)
        assertEquals(50000L, state.summary.expense)
        assertEquals(-50000L, state.summary.balance)
        assertEquals(0f, state.summary.savingsRate, 0.001f)
        assertEquals("0%", state.summary.savingsRateText)
    }

    @Test
    fun monthlyGrouping_orderedChronologically_oldestToNewest() {
        val txs = listOf(
            Transaction(id = "1", title = "Sept", amount = 1000L, type = TransactionType.INGRESO, date = sept10),
            Transaction(id = "2", title = "Jul", amount = 2000L, type = TransactionType.INGRESO, date = jul15),
            Transaction(id = "3", title = "Ago", amount = 3000L, type = TransactionType.INGRESO, date = ago20)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("3M")

        val points = viewModel.uiState.value.balanceEvolution
        assertEquals(3, points.size)
        assertEquals("Jul", points[0].month)
        assertEquals("Ago", points[1].month)
        assertEquals("Sep", points[2].month)
    }

    @Test
    fun monthlyGrouping_monthsWithoutTransactions_haveZeroValues() {
        // En 3M (Jul, Ago, Sep), solo hay movimientos en Jul y Sep. Agosto queda vacío.
        val txs = listOf(
            Transaction(id = "1", title = "Jul", amount = 10000L, type = TransactionType.INGRESO, date = jul15),
            Transaction(id = "2", title = "Sep", amount = 20000L, type = TransactionType.INGRESO, date = sept10)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("3M")

        val points = viewModel.uiState.value.balanceEvolution
        assertEquals(3, points.size)

        // Agosto
        val agoPoint = points[1]
        assertEquals("Ago", agoPoint.month)
        assertEquals(0L, agoPoint.income)
        assertEquals(0L, agoPoint.expense)
        assertEquals(0L, agoPoint.monthlyBalance)
    }

    @Test
    fun monthlyGrouping_accumulatesBalanceChronologically() {
        // Histórico previo (Mayo 2026): +100.000 saldo acumulado previo al período de 3M
        // Jul: +50.000 -> saldo = 150.000
        // Ago: -20.000 -> saldo = 130.000
        // Sep: +40.000 -> saldo = 170.000
        val txs = listOf(
            Transaction(id = "prev", title = "Previo", amount = 100000L, type = TransactionType.INGRESO, date = may10),
            Transaction(id = "1", title = "Jul", amount = 50000L, type = TransactionType.INGRESO, date = jul15),
            Transaction(id = "2", title = "Ago", amount = 20000L, type = TransactionType.GASTO, date = ago20),
            Transaction(id = "3", title = "Sep", amount = 40000L, type = TransactionType.INGRESO, date = sept10)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("3M")

        val points = viewModel.uiState.value.balanceEvolution
        assertEquals(3, points.size)

        assertEquals("Jul", points[0].month)
        assertEquals(150000L, points[0].balance)

        assertEquals("Ago", points[1].month)
        assertEquals(130000L, points[1].balance)

        assertEquals("Sep", points[2].month)
        assertEquals(170000L, points[2].balance)
    }

    @Test
    fun categoryExpenses_groupsOnlyGastoTransactions_andCalculatesPercentages() {
        val txs = listOf(
            Transaction(id = "1", title = "Sueldo", amount = 500000L, type = TransactionType.INGRESO, category = "Salario", date = sept10),
            Transaction(id = "2", title = "Super", amount = 60000L, type = TransactionType.GASTO, category = "Alimentación", date = sept10),
            Transaction(id = "3", title = "Almacén", amount = 40000L, type = TransactionType.GASTO, category = "Alimentación", date = sept12),
            Transaction(id = "4", title = "Nafta", amount = 100000L, type = TransactionType.GASTO, category = "Transporte", date = sept12)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("1M")

        val categories = viewModel.uiState.value.categoryExpenses
        // Total gastos = 200.000 (Alimentación: 100.000 (50%), Transporte: 100.000 (50%))
        assertEquals(2, categories.size)

        val totalPercentage = categories.sumOf { it.percentage.toDouble() }
        assertEquals(1.0, totalPercentage, 0.01)
    }

    @Test
    fun categoryExpenses_sortedDescendingByAmount() {
        val txs = listOf(
            Transaction(id = "1", title = "Café", amount = 5000L, type = TransactionType.GASTO, category = "Ocio", date = sept10),
            Transaction(id = "2", title = "Alquiler", amount = 200000L, type = TransactionType.GASTO, category = "Vivienda", date = sept10),
            Transaction(id = "3", title = "Luz", amount = 30000L, type = TransactionType.GASTO, category = "Servicios", date = sept12)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("1M")

        val categories = viewModel.uiState.value.categoryExpenses
        assertEquals(3, categories.size)
        assertEquals("Vivienda", categories[0].category)
        assertEquals(200000L, categories[0].amount)
        assertEquals("Servicios", categories[1].category)
        assertEquals(30000L, categories[1].amount)
        assertEquals("Ocio", categories[2].category)
        assertEquals(5000L, categories[2].amount)
    }

    @Test
    fun categoryExpenses_excludesIngresoTransactions() {
        val txs = listOf(
            Transaction(id = "1", title = "Sueldo", amount = 500000L, type = TransactionType.INGRESO, category = "Salario", date = sept10),
            Transaction(id = "2", title = "Inversión", amount = 50000L, type = TransactionType.INGRESO, category = "Inversión", date = sept10),
            Transaction(id = "3", title = "Comida", amount = 20000L, type = TransactionType.GASTO, category = "Alimentación", date = sept12)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("1M")

        val categories = viewModel.uiState.value.categoryExpenses
        assertEquals(1, categories.size)
        assertEquals("Alimentación", categories[0].category)
        assertEquals(20000L, categories[0].amount)
        assertEquals(1.0f, categories[0].percentage, 0.001f)
    }

    @Test
    fun categoryExpenses_whenNoExpenses_returnsEmptyList() {
        val txs = listOf(
            Transaction(id = "1", title = "Solo Ingreso", amount = 100000L, type = TransactionType.INGRESO, category = "Salario", date = sept10)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("1M")

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertTrue(state.categoryExpenses.isEmpty())
    }

    @Test
    fun status_whenRepositoryFails_isError() {
        fakeRepository.shouldThrowOnGet = true
        viewModel.loadData()

        val state = viewModel.uiState.value
        assertTrue(state.isError)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun setPeriod_dynamicallyUpdatesStatisticsAndRecalculates() {
        val txs = listOf(
            Transaction(id = "1", title = "Ago Ingreso", amount = 150000L, type = TransactionType.INGRESO, date = ago20),
            Transaction(id = "2", title = "Sept Ingreso", amount = 250000L, type = TransactionType.INGRESO, date = sept10)
        )
        fakeRepository.emitTransactions(txs)

        // En 1M solo debe verse Septiembre
        viewModel.setPeriod("1M")
        assertEquals(250000L, viewModel.uiState.value.summary.income)

        // Al cambiar a 3M debe verse Agosto + Septiembre
    }

    // =========================================================================
    // NUEVOS TESTS DE ESTADÍSTICAS Y ANALÍTICA (ETAPA 4)
    // =========================================================================

    @Test
    fun filter_previousMonth_onlyIncludesPreviousMonthTransactions() {
        val txs = listOf(
            Transaction(id = "1", title = "Sept Ingreso", amount = 100000L, type = TransactionType.INGRESO, date = sept10),
            Transaction(id = "2", title = "Ago Ingreso", amount = 80000L, type = TransactionType.INGRESO, date = ago20),
            Transaction(id = "3", title = "Jul Ingreso", amount = 50000L, type = TransactionType.INGRESO, date = jul15)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("Mes anterior")

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        // En agosto (mes anterior a septiembre) hubo 80.000 de ingreso
        assertEquals(80000L, state.summary.income)
        assertEquals(1, state.monthlyComparisons.size)
        assertEquals("Ago", state.monthlyComparisons[0].month)
    }

    @Test
    fun savingsRate_whenIncomeZero_returnsZeroPercent_andHandlesGracefully() {
        val txs = listOf(
            Transaction(id = "1", title = "Gasto Sin Ingresos", amount = 30000L, type = TransactionType.GASTO, date = sept10)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("1M")

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertEquals(0L, state.summary.income)
        assertEquals(30000L, state.summary.expense)
        assertEquals(-30000L, state.summary.balance)
        assertEquals(0f, state.summary.savingsRate, 0.001f)
        assertEquals("0%", state.summary.savingsRateText)
    }

    @Test
    fun savingsRate_whenExpenseGreaterThanIncome_returnsNegativeBalance_andZeroSavingsRate() {
        val txs = listOf(
            Transaction(id = "1", title = "Ingreso Menor", amount = 50000L, type = TransactionType.INGRESO, date = sept10),
            Transaction(id = "2", title = "Gasto Mayor", amount = 80000L, type = TransactionType.GASTO, date = sept12)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("1M")

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertEquals(50000L, state.summary.income)
        assertEquals(80000L, state.summary.expense)
        assertEquals(-30000L, state.summary.balance)
        // La tasa de ahorro ante balance negativo se acota a 0%
        assertEquals(0f, state.summary.savingsRate, 0.001f)
        assertEquals("0%", state.summary.savingsRateText)
    }

    @Test
    fun savingsRate_whenPositiveSavings_calculatesCorrectPercentage() {
        val txs = listOf(
            Transaction(id = "1", title = "Sueldo", amount = 200000L, type = TransactionType.INGRESO, date = sept10),
            Transaction(id = "2", title = "Gastos", amount = 50000L, type = TransactionType.GASTO, date = sept12)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("1M")

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertEquals(150000L, state.summary.balance)
        // Tasa de ahorro: 150.000 / 200.000 = 75%
        assertEquals(0.75f, state.summary.savingsRate, 0.001f)
        assertEquals("75%", state.summary.savingsRateText)
    }

    @Test
    fun categoryExpenses_whenMoreThanFiveCategories_aggregatesTopFourPlusOthers() {
        val txs = listOf(
            Transaction(id = "1", title = "Vivienda", amount = 70000L, type = TransactionType.GASTO, category = "Vivienda", date = sept10),
            Transaction(id = "2", title = "Alimentación", amount = 60000L, type = TransactionType.GASTO, category = "Alimentación", date = sept10),
            Transaction(id = "3", title = "Transporte", amount = 50000L, type = TransactionType.GASTO, category = "Transporte", date = sept10),
            Transaction(id = "4", title = "Salud", amount = 40000L, type = TransactionType.GASTO, category = "Salud", date = sept10),
            Transaction(id = "5", title = "Servicios", amount = 30000L, type = TransactionType.GASTO, category = "Servicios", date = sept10),
            Transaction(id = "6", title = "Ocio", amount = 20000L, type = TransactionType.GASTO, category = "Ocio", date = sept10),
            Transaction(id = "7", title = "Educación", amount = 10000L, type = TransactionType.GASTO, category = "Educación", date = sept10)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("1M")

        val state = viewModel.uiState.value
        // La lista visual principal se compacta a 5 elementos (4 principales + 1 agrupador "Otros")
        assertEquals(5, state.categoryExpenses.size)
        assertEquals("Vivienda", state.categoryExpenses[0].category)
        assertEquals("Alimentación", state.categoryExpenses[1].category)
        assertEquals("Transporte", state.categoryExpenses[2].category)
        assertEquals("Salud", state.categoryExpenses[3].category)
        assertEquals("Otras categorías", state.categoryExpenses[4].category)
        assertTrue(state.categoryExpenses[4].isOthers)
        // Monto de "Otras categorías": 30.000 + 20.000 + 10.000 = 60.000
        assertEquals(60000L, state.categoryExpenses[4].amount)
        assertEquals(3, state.categoryExpenses[4].subcategoriesCount)

        // La lista completa preserva las 7 categorías individuales
        assertEquals(7, state.allCategoryExpenses.size)
    }

    @Test
    fun categoryExpenses_customAndArchivedCategories_aggregatedByTitle() {
        val txs = listOf(
            Transaction(id = "1", title = "Veterinaria", amount = 25000L, type = TransactionType.GASTO, category = "Mascotas", date = sept10),
            Transaction(id = "2", title = "Cuota gym", amount = 15000L, type = TransactionType.GASTO, category = "Gimnasio Archivada", date = sept10)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("1M")

        val categories = viewModel.uiState.value.categoryExpenses
        assertEquals(2, categories.size)
        assertEquals("Mascotas", categories[0].category)
        assertEquals(25000L, categories[0].amount)
        assertEquals("Gimnasio Archivada", categories[1].category)
        assertEquals(15000L, categories[1].amount)
    }

    @Test
    fun monthlyComparisons_detectsDeficit_whenExpenseExceedsIncome() {
        val txs = listOf(
            // Agosto: superávit (Ingreso 100k, Gasto 40k)
            Transaction(id = "1", title = "Ingreso Ago", amount = 100000L, type = TransactionType.INGRESO, date = ago20),
            Transaction(id = "2", title = "Gasto Ago", amount = 40000L, type = TransactionType.GASTO, date = ago20),
            // Septiembre: déficit (Ingreso 30k, Gasto 70k)
            Transaction(id = "3", title = "Ingreso Sep", amount = 30000L, type = TransactionType.INGRESO, date = sept10),
            Transaction(id = "4", title = "Gasto Sep", amount = 70000L, type = TransactionType.GASTO, date = sept12)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("3M")

        val points = viewModel.uiState.value.monthlyComparisons
        assertEquals(3, points.size) // Jul, Ago, Sep

        val agoPoint = points.first { it.month == "Ago" }
        assertFalse(agoPoint.isDeficit)
        assertEquals(60000L, agoPoint.monthlyBalance)

        val sepPoint = points.first { it.month == "Sep" }
        assertTrue(sepPoint.isDeficit)
        assertEquals(-40000L, sepPoint.monthlyBalance)
    }

    @Test
    fun cumulativeBalanceEvolution_includesInitialAccountBalances_andLegacyTransactions() = kotlinx.coroutines.test.runTest {
        // 1. Saldos iniciales en cuentas: Efectivo 100k, Digital 200k = Total 300k
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_CASH_ID, 100000L)
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_DIGITAL_ID, 200000L)

        // 2. Movimiento legacy previo al período (Julio 2026): +50k
        val priorLegacyTx = Transaction(
            id = "legacy_prev",
            title = "Legacy Prev",
            amount = 50000L,
            type = TransactionType.INGRESO,
            accountId = "",
            date = jul15
        )

        // 3. Movimiento del período (Septiembre 2026): Gasto de 20k
        val sepTx = Transaction(
            id = "sep_tx",
            title = "Gasto Sep",
            amount = 20000L,
            type = TransactionType.GASTO,
            accountId = Account.DEFAULT_CASH_ID,
            date = sept10
        )

        fakeRepository.emitTransactions(listOf(priorLegacyTx, sepTx))
        viewModel.setPeriod("1M") // Solo Septiembre

        val points = viewModel.uiState.value.balanceEvolution
        assertEquals(1, points.size)
        // Saldo previo: 300.000 (cuentas) + 50.000 (legacy previo) = 350.000
        // En septiembre: -20.000
        // Saldo acumulado final en septiembre: 350.000 - 20.000 = 330.000
        assertEquals(330000L, points[0].balance)
    }

    @Test
    fun accountDistribution_calculatesDerivedBalanceForCashAndDigital_toleratingLegacy() = kotlinx.coroutines.test.runTest {
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_CASH_ID, 100000L)
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_DIGITAL_ID, 500000L)

        val txs = listOf(
            // Efectivo: -30.000 -> saldo esperado = 70.000
            Transaction(id = "1", title = "Gasto Ef", amount = 30000L, type = TransactionType.GASTO, accountId = Account.DEFAULT_CASH_ID, date = sept10),
            // Digital: +100.000 -> saldo esperado = 600.000
            Transaction(id = "2", title = "Ingreso Dig", amount = 100000L, type = TransactionType.INGRESO, accountId = Account.DEFAULT_DIGITAL_ID, date = sept10),
            // Legacy: sin accountId ni cuenta -> no altera saldos específicos de cuentas
            Transaction(id = "3", title = "Gasto Legacy", amount = 15000L, type = TransactionType.GASTO, accountId = "", date = sept10)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("1M")

        val accounts = viewModel.uiState.value.accountDistributions
        assertEquals(2, accounts.size)

        val cash = accounts.first { it.type == AccountType.CASH }
        assertEquals(70000L, cash.balance)

        val digital = accounts.first { it.type == AccountType.DIGITAL }
        assertEquals(600000L, digital.balance)

        // Total positivo: 70k + 600k = 670k
        assertEquals(70000f / 670000f, cash.percentage, 0.001f)
        assertEquals(600000f / 670000f, digital.percentage, 0.001f)
    }

    @Test
    fun budgetPerformance_categorizesIntoNormalWarningAndExceeded() = kotlinx.coroutines.test.runTest {
        val budgets = listOf(
            // Septiembre 2026 (mes 9)
            Budget(id = "b1", category = "Alimentación", limitAmount = 100000L, month = 9, year = 2026),
            Budget(id = "b2", category = "Servicios", limitAmount = 100000L, month = 9, year = 2026),
            Budget(id = "b3", category = "Ocio", limitAmount = 100000L, month = 9, year = 2026)
        )
        fakeBudgetRepository.emitBudgets(budgets)

        val txs = listOf(
            // Alimentación: gastó 50.000 (50%) -> NORMAL
            Transaction(id = "t1", title = "Super", amount = 50000L, type = TransactionType.GASTO, category = "Alimentación", date = sept10),
            // Servicios: gastó 90.000 (90%) -> WARNING
            Transaction(id = "t2", title = "Luz y Gas", amount = 90000L, type = TransactionType.GASTO, category = "Servicios", date = sept10),
            // Ocio: gastó 120.000 (120%) -> EXCEEDED
            Transaction(id = "t3", title = "Cine y Salidas", amount = 120000L, type = TransactionType.GASTO, category = "Ocio", date = sept10)
        )
        fakeRepository.emitTransactions(txs)
        viewModel.setPeriod("Mes actual")

        val performances = viewModel.uiState.value.budgetPerformances
        assertEquals(3, performances.size)

        val ocio = performances.first { it.category == "Ocio" }
        assertEquals(120000L, ocio.spentAmount)
        assertEquals(1.2f, ocio.percentage, 0.001f)
        assertEquals(BudgetHealthStatus.EXCEEDED, ocio.status)

        val servicios = performances.first { it.category == "Servicios" }
        assertEquals(90000L, servicios.spentAmount)
        assertEquals(0.9f, servicios.percentage, 0.001f)
        assertEquals(BudgetHealthStatus.WARNING, servicios.status)

        val comida = performances.first { it.category == "Alimentación" }
        assertEquals(50000L, comida.spentAmount)
        assertEquals(0.5f, comida.percentage, 0.001f)
        assertEquals(BudgetHealthStatus.NORMAL, comida.status)
    }
}
