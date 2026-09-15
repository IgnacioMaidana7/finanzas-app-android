package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.mock.MockDataProvider
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.data.model.User
import com.maidana.finanzasapp.ui.screens.dashboard.DashboardStatus
import com.maidana.finanzasapp.ui.screens.dashboard.DashboardViewModel
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
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeTransactionRepository: FakeTransactionRepository
    private lateinit var fakeBudgetRepository: FakeBudgetRepository
    private lateinit var viewModel: DashboardViewModel

    // Fecha fija de prueba: 15 de Septiembre de 2026
    private val fixedNow: Long = Calendar.getInstance().apply {
        set(2026, Calendar.SEPTEMBER, 15, 12, 0, 0)
    }.timeInMillis

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository().apply {
            currentUserMock = User(id = "uid_123", email = "test@finanzasapp.com", displayName = "Ignacio")
        }
        fakeTransactionRepository = FakeTransactionRepository()
        fakeBudgetRepository = FakeBudgetRepository(MockDataProvider.budgets)
        viewModel = DashboardViewModel(
            authRepository = fakeAuthRepository,
            transactionRepository = fakeTransactionRepository,
            budgetRepository = fakeBudgetRepository,
            currentTimeProvider = { fixedNow }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_whenRepositoryEmpty_isDashboardStatusEmpty() {
        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertEquals(0L, state.totalBalance)
        assertEquals(0L, state.monthlyIncome)
        assertEquals(0L, state.monthlyExpense)
        assertEquals(0L, state.monthlySavings)
        assertTrue(state.recentTransactions.isEmpty())
        // Verifica que presupuestos y objetivos mock continúen disponibles
        assertTrue(state.budgets.isNotEmpty())
        assertNotNull(state.featuredGoal)
    }

    @Test
    fun calculation_monthlyIncome_and_monthlyExpense_and_monthlySavings() {
        val sept10 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 10, 10, 0, 0)
        }.timeInMillis

        val sept12 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 12, 10, 0, 0)
        }.timeInMillis

        val testTransactions = listOf(
            Transaction(id = "1", title = "Sueldo", amount = 350000L, type = TransactionType.INGRESO, date = sept10),
            Transaction(id = "2", title = "Freelance", amount = 150000L, type = TransactionType.INGRESO, date = sept12),
            Transaction(id = "3", title = "Supermercado", amount = 80000L, type = TransactionType.GASTO, date = sept12),
            Transaction(id = "4", title = "Servicios", amount = 20000L, type = TransactionType.GASTO, date = sept10)
        )
        fakeTransactionRepository.emitTransactions(testTransactions)

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertEquals(500000L, state.monthlyIncome)
        assertEquals(100000L, state.monthlyExpense)
        assertEquals(400000L, state.monthlySavings)
        assertEquals(400000L, state.totalBalance)
    }

    @Test
    fun calculation_totalBalance_usesHistoricalTransactions() {
        // Movimientos del mes anterior (Agosto 2026)
        val ago20 = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 20, 10, 0, 0)
        }.timeInMillis

        // Movimientos del mes actual (Septiembre 2026)
        val sept5 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 5, 10, 0, 0)
        }.timeInMillis

        val testTransactions = listOf(
            // Histórico previo: +600.000, -200.000 -> saldo histórico acumulado = 400.000
            Transaction(id = "h1", title = "Sueldo Agosto", amount = 600000L, type = TransactionType.INGRESO, date = ago20),
            Transaction(id = "h2", title = "Gasto Agosto", amount = 200000L, type = TransactionType.GASTO, date = ago20),
            // Mes actual: +300.000, -50.000 -> ahorro del mes = 250.000
            Transaction(id = "c1", title = "Sueldo Septiembre", amount = 300000L, type = TransactionType.INGRESO, date = sept5),
            Transaction(id = "c2", title = "Gasto Septiembre", amount = 50000L, type = TransactionType.GASTO, date = sept5)
        )
        fakeTransactionRepository.emitTransactions(testTransactions)

        val state = viewModel.uiState.value
        // Mes actual solo cuenta Septiembre:
        assertEquals(300000L, state.monthlyIncome)
        assertEquals(50000L, state.monthlyExpense)
        assertEquals(250000L, state.monthlySavings)

        // Saldo total cuenta TODO el histórico (900.000 ingresos - 250.000 gastos = 650.000):
        assertEquals(650000L, state.totalBalance)
    }

    @Test
    fun filtering_byCurrentMonth_ignoresOtherMonthsForMonthlySummary() {
        val ago = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 15, 10, 0, 0)
        }.timeInMillis

        val oct = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 10, 0, 0)
        }.timeInMillis

        val testTransactions = listOf(
            Transaction(id = "1", title = "Ingreso Agosto", amount = 100000L, type = TransactionType.INGRESO, date = ago),
            Transaction(id = "2", title = "Gasto Octubre", amount = 50000L, type = TransactionType.GASTO, date = oct)
        )
        fakeTransactionRepository.emitTransactions(testTransactions)

        val state = viewModel.uiState.value
        // En septiembre no hubo movimientos:
        assertEquals(0L, state.monthlyIncome)
        assertEquals(0L, state.monthlyExpense)
        assertEquals(0L, state.monthlySavings)

        // Pero el saldo histórico refleja los movimientos:
        assertEquals(50000L, state.totalBalance)
    }

    @Test
    fun recentTransactions_selectsUpTo4_orderedByDateDescending() {
        val d1 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 1, 10, 0) }.timeInMillis
        val d2 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 2, 10, 0) }.timeInMillis
        val d3 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 3, 10, 0) }.timeInMillis
        val d4 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 4, 10, 0) }.timeInMillis
        val d5 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 5, 10, 0) }.timeInMillis
        val d6 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 6, 10, 0) }.timeInMillis

        val testTransactions = listOf(
            Transaction(id = "1", title = "T1", amount = 1000L, date = d1),
            Transaction(id = "2", title = "T2", amount = 2000L, date = d2),
            Transaction(id = "3", title = "T3", amount = 3000L, date = d3),
            Transaction(id = "4", title = "T4", amount = 4000L, date = d4),
            Transaction(id = "5", title = "T5", amount = 5000L, date = d5),
            Transaction(id = "6", title = "T6", amount = 6000L, date = d6)
        )
        fakeTransactionRepository.emitTransactions(testTransactions)

        val state = viewModel.uiState.value
        assertEquals(4, state.recentTransactions.size)
        // Deben ser los más recientes: d6, d5, d4, d3
        assertEquals("6", state.recentTransactions[0].id)
        assertEquals("5", state.recentTransactions[1].id)
        assertEquals("4", state.recentTransactions[2].id)
        assertEquals("3", state.recentTransactions[3].id)
    }

    @Test
    fun loadData_whenRepositoryFails_isDashboardStatusError_andMocksRemainAvailable() {
        fakeTransactionRepository.shouldThrowOnGet = true
        viewModel.loadData()

        val state = viewModel.uiState.value
        assertTrue(state.isError)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Error simulado"))

        // No bloquea presupuestos ni objetivos mock
        assertTrue(state.budgets.isNotEmpty())
        assertNotNull(state.featuredGoal)
    }

    @Test
    fun isSameMonthAndYear_correctlyIdentifiesDates() {
        val sept1 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 1) }.timeInMillis
        val sept30 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 30) }.timeInMillis
        val ago31 = Calendar.getInstance().apply { set(2026, Calendar.AUGUST, 31) }.timeInMillis
        val sept2025 = Calendar.getInstance().apply { set(2025, Calendar.SEPTEMBER, 15) }.timeInMillis

        assertTrue(DashboardViewModel.isSameMonthAndYear(sept1, fixedNow))
        assertTrue(DashboardViewModel.isSameMonthAndYear(sept30, fixedNow))
        org.junit.Assert.assertFalse(DashboardViewModel.isSameMonthAndYear(ago31, fixedNow))
        org.junit.Assert.assertFalse(DashboardViewModel.isSameMonthAndYear(sept2025, fixedNow))
    }
}
