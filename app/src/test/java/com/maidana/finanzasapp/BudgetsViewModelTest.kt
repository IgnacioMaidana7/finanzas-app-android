package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.model.Budget
import com.maidana.finanzasapp.data.model.BudgetStatus
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.ui.screens.budgets.BudgetsStatus
import com.maidana.finanzasapp.ui.screens.budgets.BudgetsViewModel
import com.maidana.finanzasapp.ui.screens.budgets.DeleteBudgetState
import com.maidana.finanzasapp.ui.screens.budgets.SaveBudgetState
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
class BudgetsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeBudgetRepository: FakeBudgetRepository
    private lateinit var fakeTransactionRepository: FakeTransactionRepository
    private lateinit var viewModel: BudgetsViewModel

    // Fecha fija: 15 de Septiembre de 2026 (mes 9, año 2026)
    private val fixedNow: Long = Calendar.getInstance().apply {
        set(2026, Calendar.SEPTEMBER, 15, 12, 0, 0)
    }.timeInMillis

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeBudgetRepository = FakeBudgetRepository()
        fakeTransactionRepository = FakeTransactionRepository()
        viewModel = BudgetsViewModel(
            budgetRepository = fakeBudgetRepository,
            transactionRepository = fakeTransactionRepository,
            currentTimeProvider = { fixedNow }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_whenRepositoriesEmpty_isBudgetsStatusEmpty() {
        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertEquals(9, state.selectedMonth)
        assertEquals(2026, state.selectedYear)
        assertEquals("Septiembre 2026", state.periodLabel)
        assertEquals(0L, state.totalLimit)
        assertEquals(0L, state.totalSpent)
        assertEquals(0L, state.totalRemaining)
        assertEquals(0f, state.globalPercentage, 0.001f)
        assertTrue(state.budgets.isEmpty())
    }

    @Test
    fun calculation_spentAmount_sumsMatchingExpenseTransactionsForCategoryAndPeriod() {
        val sept10 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 10, 10, 0) }.timeInMillis
        val sept12 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 12, 10, 0) }.timeInMillis

        val testBudget = Budget(
            id = "b1",
            category = "Alimentación",
            limitAmount = 100000L,
            month = 9,
            year = 2026
        )
        fakeBudgetRepository.emitBudgets(listOf(testBudget))

        val transactions = listOf(
            Transaction(id = "t1", title = "Super Coto", amount = 30000L, type = TransactionType.GASTO, category = "Alimentación", date = sept10),
            Transaction(id = "t2", title = "Verdulería", amount = 20000L, type = TransactionType.GASTO, category = "Alimentación", date = sept12)
        )
        fakeTransactionRepository.emitTransactions(transactions)

        val state = viewModel.uiState.value
        assertFalse(state.isEmpty)
        assertEquals(1, state.budgets.size)

        val budget = state.budgets.first()
        assertEquals(50000L, budget.spentAmount)
        assertEquals(50000L, budget.remainingAmount)
        assertEquals(0.5f, budget.percentage, 0.001f)
        assertEquals(BudgetStatus.NORMAL, budget.status)

        // Resumen global
        assertEquals(100000L, state.totalLimit)
        assertEquals(50000L, state.totalSpent)
        assertEquals(50000L, state.totalRemaining)
        assertEquals(0.5f, state.globalPercentage, 0.001f)
    }

    @Test
    fun calculation_spentAmount_ignoresIncomeTransactions() {
        val sept10 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 10, 10, 0) }.timeInMillis

        val testBudget = Budget(
            id = "b1",
            category = "Alimentación",
            limitAmount = 100000L,
            month = 9,
            year = 2026
        )
        fakeBudgetRepository.emitBudgets(listOf(testBudget))

        val transactions = listOf(
            Transaction(id = "t1", title = "Devolución Super", amount = 15000L, type = TransactionType.INGRESO, category = "Alimentación", date = sept10)
        )
        fakeTransactionRepository.emitTransactions(transactions)

        val state = viewModel.uiState.value
        val budget = state.budgets.first()
        assertEquals(0L, budget.spentAmount)
        assertEquals(100000L, budget.remainingAmount)
    }

    @Test
    fun calculation_spentAmount_ignoresDifferentCategories() {
        val sept10 = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 10, 10, 0) }.timeInMillis

        val testBudget = Budget(
            id = "b1",
            category = "Alimentación",
            limitAmount = 100000L,
            month = 9,
            year = 2026
        )
        fakeBudgetRepository.emitBudgets(listOf(testBudget))

        val transactions = listOf(
            Transaction(id = "t1", title = "YPF Combustible", amount = 40000L, type = TransactionType.GASTO, category = "Transporte", date = sept10)
        )
        fakeTransactionRepository.emitTransactions(transactions)

        val state = viewModel.uiState.value
        val budget = state.budgets.first()
        assertEquals(0L, budget.spentAmount)
        assertEquals(100000L, budget.remainingAmount)
    }

    @Test
    fun calculation_spentAmount_ignoresTransactionsFromDifferentMonths() {
        val ago15 = Calendar.getInstance().apply { set(2026, Calendar.AUGUST, 15, 10, 0) }.timeInMillis
        val oct15 = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 15, 10, 0) }.timeInMillis

        val testBudget = Budget(
            id = "b1",
            category = "Alimentación",
            limitAmount = 100000L,
            month = 9,
            year = 2026
        )
        fakeBudgetRepository.emitBudgets(listOf(testBudget))

        val transactions = listOf(
            Transaction(id = "t1", title = "Super Agosto", amount = 50000L, type = TransactionType.GASTO, category = "Alimentación", date = ago15),
            Transaction(id = "t2", title = "Super Octubre", amount = 30000L, type = TransactionType.GASTO, category = "Alimentación", date = oct15)
        )
        fakeTransactionRepository.emitTransactions(transactions)

        val state = viewModel.uiState.value
        val budget = state.budgets.first()
        assertEquals(0L, budget.spentAmount)
        assertEquals(100000L, budget.remainingAmount)
    }

    @Test
    fun status_calculation_normal_nearLimit_and_exceeded() {
        val date = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 5, 10, 0) }.timeInMillis

        val bNormal = Budget(id = "b1", category = "Transporte", limitAmount = 100000L, month = 9, year = 2026)
        val bNear = Budget(id = "b2", category = "Ocio", limitAmount = 100000L, month = 9, year = 2026)
        val bExceeded = Budget(id = "b3", category = "Servicios", limitAmount = 100000L, month = 9, year = 2026)
        fakeBudgetRepository.emitBudgets(listOf(bNormal, bNear, bExceeded))

        val transactions = listOf(
            // Transporte: 70.000 / 100.000 = 70% -> NORMAL (< 75%)
            Transaction(id = "t1", title = "Nafta", amount = 70000L, type = TransactionType.GASTO, category = "Transporte", date = date),
            // Ocio: 85.000 / 100.000 = 85% -> NEAR_LIMIT (>= 75% y <= 100%)
            Transaction(id = "t2", title = "Cena", amount = 85000L, type = TransactionType.GASTO, category = "Ocio", date = date),
            // Servicios: 110.000 / 100.000 = 110% -> EXCEEDED (> 100%)
            Transaction(id = "t3", title = "Luz y Gas", amount = 110000L, type = TransactionType.GASTO, category = "Servicios", date = date)
        )
        fakeTransactionRepository.emitTransactions(transactions)

        val state = viewModel.uiState.value
        val budgets = state.budgets.associateBy { it.category }

        assertEquals(BudgetStatus.NORMAL, budgets["Transporte"]?.status)
        assertEquals(BudgetStatus.NEAR_LIMIT, budgets["Ocio"]?.status)
        assertEquals(BudgetStatus.EXCEEDED, budgets["Servicios"]?.status)
    }

    @Test
    fun saveBudget_validatesCategoryRequired() {
        viewModel.saveBudget(category = "   ", limitAmount = 50000L, month = 9, year = 2026)

        val saveState = viewModel.saveState.value
        assertTrue(saveState is SaveBudgetState.Error)
        assertEquals("Debes seleccionar una categoría.", (saveState as SaveBudgetState.Error).message)
        assertTrue(fakeBudgetRepository.inMemoryBudgets.isEmpty())
    }

    @Test
    fun saveBudget_validatesLimitGreaterThanZero() {
        viewModel.saveBudget(category = "Alimentación", limitAmount = 0L, month = 9, year = 2026)

        val saveState = viewModel.saveState.value
        assertTrue(saveState is SaveBudgetState.Error)
        assertEquals("El límite mensual debe ser mayor a 0.", (saveState as SaveBudgetState.Error).message)
        assertTrue(fakeBudgetRepository.inMemoryBudgets.isEmpty())
    }

    @Test
    fun saveBudget_preventsDuplicateCategoryInSamePeriod() {
        val existing = Budget(id = "b1", category = "Alimentación", limitAmount = 50000L, month = 9, year = 2026)
        fakeBudgetRepository.emitBudgets(listOf(existing))

        // Intentar crear otro presupuesto para Alimentación en Septiembre 2026
        viewModel.saveBudget(category = "Alimentación", limitAmount = 80000L, month = 9, year = 2026)

        val saveState = viewModel.saveState.value
        assertTrue(saveState is SaveBudgetState.Error)
        assertTrue((saveState as SaveBudgetState.Error).message.contains("Ya existe un presupuesto"))
        // La lista no fue modificada:
        assertEquals(1, fakeBudgetRepository.inMemoryBudgets.size)
    }

    @Test
    fun saveBudget_allowsSameCategoryInDifferentPeriod() {
        val existing = Budget(id = "b1", category = "Alimentación", limitAmount = 50000L, month = 9, year = 2026)
        fakeBudgetRepository.emitBudgets(listOf(existing))

        // Crear presupuesto para Alimentación pero en Octubre 2026 (mes 10)
        viewModel.saveBudget(category = "Alimentación", limitAmount = 90000L, month = 10, year = 2026)

        val saveState = viewModel.saveState.value
        assertTrue(saveState is SaveBudgetState.Success)
        assertEquals(2, fakeBudgetRepository.inMemoryBudgets.size)
    }

    @Test
    fun deleteBudget_removesBudgetSuccessfully() {
        val b1 = Budget(id = "b1", category = "Alimentación", limitAmount = 50000L, month = 9, year = 2026)
        val b2 = Budget(id = "b2", category = "Transporte", limitAmount = 30000L, month = 9, year = 2026)
        fakeBudgetRepository.emitBudgets(listOf(b1, b2))

        assertEquals(2, viewModel.uiState.value.budgets.size)

        viewModel.deleteBudget("b1")

        assertTrue(viewModel.deleteState.value is DeleteBudgetState.Success)
        assertEquals(1, fakeBudgetRepository.inMemoryBudgets.size)
        assertEquals("Transporte", fakeBudgetRepository.inMemoryBudgets.first().category)
    }

    @Test
    fun errorState_whenBudgetRepositoryFails() {
        fakeBudgetRepository.shouldThrowOnGet = true
        viewModel.loadData()

        val state = viewModel.uiState.value
        assertTrue(state.isError)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Error simulado"))
    }
}
