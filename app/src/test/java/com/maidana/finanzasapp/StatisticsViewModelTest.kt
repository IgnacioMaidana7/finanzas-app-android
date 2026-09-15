package com.maidana.finanzasapp

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeTransactionRepository
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
        viewModel = StatisticsViewModel(
            transactionRepository = fakeRepository,
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
        viewModel.setPeriod("3M")
        assertEquals(400000L, viewModel.uiState.value.summary.income)
    }
}
