package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.data.repository.TransactionRepository
import com.maidana.finanzasapp.ui.screens.transactions.SaveTransactionState
import com.maidana.finanzasapp.ui.screens.transactions.TransactionFilter
import com.maidana.finanzasapp.ui.screens.transactions.TransactionsStatus
import com.maidana.finanzasapp.ui.screens.transactions.TransactionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTransactionRepository : TransactionRepository {
    var shouldFailAdd = false
    var shouldFailDelete = false
    var shouldThrowOnGet = false

    val inMemoryTransactions = mutableListOf<Transaction>()
    private val _flow = MutableStateFlow<List<Transaction>>(emptyList())

    override fun getTransactions(): Flow<List<Transaction>> {
        if (shouldThrowOnGet) {
            return flow {
                throw Exception("Error simulado de conexión")
            }
        }
        return _flow
    }

    override suspend fun addTransaction(transaction: Transaction): Result<Unit> {
        return if (shouldFailAdd) {
            Result.failure(Exception("Error al guardar en Firestore"))
        } else {
            val txWithId = if (transaction.id.isBlank()) {
                transaction.copy(id = "tx_${System.currentTimeMillis()}")
            } else {
                transaction
            }
            inMemoryTransactions.add(0, txWithId)
            _flow.value = inMemoryTransactions.toList()
            Result.success(Unit)
        }
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> {
        return if (shouldFailDelete) {
            Result.failure(Exception("Error al eliminar de Firestore"))
        } else {
            inMemoryTransactions.removeAll { it.id == id }
            _flow.value = inMemoryTransactions.toList()
            Result.success(Unit)
        }
    }

    fun emitTransactions(list: List<Transaction>) {
        inMemoryTransactions.clear()
        inMemoryTransactions.addAll(list)
        _flow.value = list
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeTransactionRepository
    private lateinit var viewModel: TransactionsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTransactionRepository()
        viewModel = TransactionsViewModel(repository = fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_whenRepositoryEmpty_isTransactionsStatusEmpty() {
        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertTrue(state.transactions.isEmpty())
        assertNull(state.errorMessage)
    }

    @Test
    fun loadTransactions_whenHasData_isTransactionsStatusSuccess() {
        val testData = listOf(
            Transaction(id = "1", title = "Sueldo", amount = 500000L, type = TransactionType.INGRESO, category = "Salario"),
            Transaction(id = "2", title = "Supermercado", amount = 85000L, type = TransactionType.GASTO, category = "Alimentación")
        )
        fakeRepository.emitTransactions(testData)

        val state = viewModel.uiState.value
        assertTrue(state.status is TransactionsStatus.Success)
        assertEquals(2, state.transactions.size)
        assertEquals(500000L, state.totalIncome)
        assertEquals(85000L, state.totalExpense)
    }

    @Test
    fun loadTransactions_whenRepositoryFails_isTransactionsStatusError() {
        fakeRepository.shouldThrowOnGet = true
        viewModel.loadTransactions()

        val state = viewModel.uiState.value
        assertTrue(state.isError)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Error simulado"))
    }

    @Test
    fun filterTransactions_all_income_expense() {
        val testData = listOf(
            Transaction(id = "1", title = "Sueldo", amount = 200000L, type = TransactionType.INGRESO, category = "Salario"),
            Transaction(id = "2", title = "Freelance", amount = 50000L, type = TransactionType.INGRESO, category = "Ingresos Extra"),
            Transaction(id = "3", title = "Alquiler", amount = 120000L, type = TransactionType.GASTO, category = "Vivienda")
        )
        fakeRepository.emitTransactions(testData)

        // ALL
        viewModel.setFilter(TransactionFilter.ALL)
        assertEquals(3, viewModel.uiState.value.filteredTransactions.size)

        // INCOME
        viewModel.setFilter(TransactionFilter.INCOME)
        assertEquals(2, viewModel.uiState.value.filteredTransactions.size)
        assertTrue(viewModel.uiState.value.filteredTransactions.all { it.type == TransactionType.INGRESO })

        // EXPENSE
        viewModel.setFilter(TransactionFilter.EXPENSE)
        assertEquals(1, viewModel.uiState.value.filteredTransactions.size)
        assertTrue(viewModel.uiState.value.filteredTransactions.all { it.type == TransactionType.GASTO })
    }

    @Test
    fun totalsCalculation_sumsCorrectly() {
        val testData = listOf(
            Transaction(id = "1", amount = 100L, type = TransactionType.INGRESO),
            Transaction(id = "2", amount = 50L, type = TransactionType.INGRESO),
            Transaction(id = "3", amount = 30L, type = TransactionType.GASTO),
            Transaction(id = "4", amount = 20L, type = TransactionType.GASTO)
        )
        fakeRepository.emitTransactions(testData)

        val state = viewModel.uiState.value
        assertEquals(150L, state.totalIncome)
        assertEquals(50L, state.totalExpense)
    }

    @Test
    fun saveTransaction_validations() {
        // Monto vacío
        viewModel.saveTransaction(
            title = "Supermercado",
            amountText = "",
            type = TransactionType.GASTO,
            category = "Alimentación",
            dateString = "15/09/2026"
        )
        assertTrue(viewModel.saveState.value is SaveTransactionState.Error)
        assertEquals("El monto es obligatorio.", (viewModel.saveState.value as SaveTransactionState.Error).message)

        // Monto cero o negativo
        viewModel.saveTransaction(
            title = "Supermercado",
            amountText = "0",
            type = TransactionType.GASTO,
            category = "Alimentación",
            dateString = "15/09/2026"
        )
        assertTrue(viewModel.saveState.value is SaveTransactionState.Error)
        assertEquals("El monto debe ser un número entero mayor a cero.", (viewModel.saveState.value as SaveTransactionState.Error).message)

        viewModel.saveTransaction(
            title = "Supermercado",
            amountText = "-50",
            type = TransactionType.GASTO,
            category = "Alimentación",
            dateString = "15/09/2026"
        )
        assertTrue(viewModel.saveState.value is SaveTransactionState.Error)

        // Categoría vacía
        viewModel.saveTransaction(
            title = "Supermercado",
            amountText = "1500",
            type = TransactionType.GASTO,
            category = "   ",
            dateString = "15/09/2026"
        )
        assertTrue(viewModel.saveState.value is SaveTransactionState.Error)
        assertEquals("Debes seleccionar una categoría.", (viewModel.saveState.value as SaveTransactionState.Error).message)

        // Descripción vacía
        viewModel.saveTransaction(
            title = "   ",
            amountText = "1500",
            type = TransactionType.GASTO,
            category = "Alimentación",
            dateString = "15/09/2026"
        )
        assertTrue(viewModel.saveState.value is SaveTransactionState.Error)
        assertEquals("Ingresa una descripción o nota para el movimiento.", (viewModel.saveState.value as SaveTransactionState.Error).message)

        // Fecha inválida
        viewModel.saveTransaction(
            title = "Supermercado",
            amountText = "1500",
            type = TransactionType.GASTO,
            category = "Alimentación",
            dateString = "fecha_invalida"
        )
        assertTrue(viewModel.saveState.value is SaveTransactionState.Error)
        assertEquals("La fecha no es válida. Usa el formato dd/mm/aaaa.", (viewModel.saveState.value as SaveTransactionState.Error).message)
    }

    @Test
    fun saveTransaction_success_updatesStateAndRepository() {
        viewModel.saveTransaction(
            title = "Cena Restaurante",
            amountText = "18500",
            type = TransactionType.GASTO,
            category = "Ocio",
            dateString = "15/09/2026"
        )

        assertTrue(viewModel.saveState.value is SaveTransactionState.Success)
        assertEquals(1, fakeRepository.inMemoryTransactions.size)
        val saved = fakeRepository.inMemoryTransactions.first()
        assertEquals("Cena Restaurante", saved.description)
        assertEquals(18500L, saved.amount)
        assertEquals(TransactionType.GASTO, saved.type)
        assertEquals("Ocio", saved.category)

        // Reset state
        viewModel.resetSaveState()
        assertTrue(viewModel.saveState.value is SaveTransactionState.Idle)
    }

    @Test
    fun saveTransaction_whenRepositoryFails_setsErrorState() {
        fakeRepository.shouldFailAdd = true
        viewModel.saveTransaction(
            title = "Cena Restaurante",
            amountText = "1000",
            type = TransactionType.GASTO,
            category = "Ocio",
            dateString = "15/09/2026"
        )

        assertTrue(viewModel.saveState.value is SaveTransactionState.Error)
        assertEquals("Error al guardar en Firestore", (viewModel.saveState.value as SaveTransactionState.Error).message)
    }

    @Test
    fun deleteTransaction_success_removesFromRepository() {
        val testData = listOf(
            Transaction(id = "tx_1", title = "Sueldo", amount = 1000L),
            Transaction(id = "tx_2", title = "Gasto", amount = 200L)
        )
        fakeRepository.emitTransactions(testData)
        assertEquals(2, viewModel.uiState.value.transactions.size)

        viewModel.deleteTransaction("tx_1")
        assertFalse(viewModel.uiState.value.isDeleting)
        assertNull(viewModel.uiState.value.deleteError)
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("tx_2", viewModel.uiState.value.transactions.first().id)
    }

    @Test
    fun deleteTransaction_whenRepositoryFails_setsDeleteError() {
        val testData = listOf(Transaction(id = "tx_1", title = "Sueldo", amount = 1000L))
        fakeRepository.emitTransactions(testData)

        fakeRepository.shouldFailDelete = true
        viewModel.deleteTransaction("tx_1")

        assertEquals("Error al eliminar de Firestore", viewModel.uiState.value.deleteError)
        viewModel.clearDeleteError()
        assertNull(viewModel.uiState.value.deleteError)
    }
}
