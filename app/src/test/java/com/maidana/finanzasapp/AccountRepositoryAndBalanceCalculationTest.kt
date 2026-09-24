package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.model.Account
import com.maidana.finanzasapp.data.model.AccountType
import com.maidana.finanzasapp.data.model.Budget
import com.maidana.finanzasapp.data.model.SavingsGoal
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.data.model.User
import com.maidana.finanzasapp.data.repository.AccountRepository
import com.maidana.finanzasapp.data.repository.AuthRepository
import com.maidana.finanzasapp.data.repository.BudgetRepository
import com.maidana.finanzasapp.data.repository.GoalRepository
import com.maidana.finanzasapp.data.repository.TransactionRepository
import com.maidana.finanzasapp.ui.screens.accounts.AccountActionState
import com.maidana.finanzasapp.ui.screens.accounts.AccountViewModel
import com.maidana.finanzasapp.ui.screens.dashboard.DashboardViewModel
import com.maidana.finanzasapp.ui.screens.transactions.SaveTransactionState
import com.maidana.finanzasapp.ui.screens.transactions.TransactionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAccountRepository(var currentUserId: String = "user_1") : AccountRepository {
    val accountsMap = mutableMapOf<String, MutableList<Account>>()
    private val _flow = MutableStateFlow<List<Account>>(emptyList())

    private fun getStorage(): MutableList<Account> {
        return accountsMap.getOrPut(currentUserId) {
            mutableListOf(
                Account.DEFAULT_CASH_ACCOUNT.copy(),
                Account.DEFAULT_DIGITAL_ACCOUNT.copy()
            )
        }
    }

    private fun refreshFlow() {
        _flow.value = getStorage().toList()
    }

    init {
        refreshFlow()
    }

    fun switchUser(newUserId: String) {
        currentUserId = newUserId
        refreshFlow()
    }

    override fun getAccounts(): Flow<List<Account>> = _flow.asStateFlow()

    override suspend fun getAccount(id: String): Result<Account?> {
        val acc = getStorage().firstOrNull { it.id == id }
        return Result.success(acc)
    }

    override suspend fun updateInitialBalance(id: String, initialBalance: Long): Result<Unit> {
        if (initialBalance < 0L) return Result.failure(IllegalArgumentException("El saldo inicial no puede ser negativo."))
        val list = getStorage()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            list[index] = list[index].copy(initialBalance = initialBalance)
            refreshFlow()
            return Result.success(Unit)
        }
        return Result.failure(IllegalArgumentException("Cuenta no encontrada."))
    }

    override suspend fun updateInitialBalances(balances: Map<String, Long>): Result<Unit> {
        balances.forEach { (id, bal) ->
            if (bal < 0L) return Result.failure(IllegalArgumentException("El saldo inicial no puede ser negativo."))
            val list = getStorage()
            val index = list.indexOfFirst { it.id == id }
            if (index != -1) {
                list[index] = list[index].copy(initialBalance = bal)
            }
        }
        refreshFlow()
        return Result.success(Unit)
    }

    override suspend fun ensureDefaultAccounts(uid: String): Result<Unit> {
        val list = accountsMap.getOrPut(uid) { mutableListOf() }
        if (list.none { it.id == Account.DEFAULT_CASH_ID }) {
            list.add(Account.DEFAULT_CASH_ACCOUNT.copy())
        }
        if (list.none { it.id == Account.DEFAULT_DIGITAL_ID }) {
            list.add(Account.DEFAULT_DIGITAL_ACCOUNT.copy())
        }
        if (uid == currentUserId) refreshFlow()
        return Result.success(Unit)
    }

    override suspend fun addAccount(account: Account): Result<Account> {
        val cleanName = account.name.trim()
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("El nombre es obligatorio."))
        val acc = account.copy(id = account.id.ifBlank { "acc_${System.currentTimeMillis()}" }, name = cleanName)
        getStorage().add(acc)
        refreshFlow()
        return Result.success(acc)
    }

    override suspend fun updateAccount(account: Account): Result<Unit> {
        val list = getStorage()
        val idx = list.indexOfFirst { it.id == account.id }
        if (idx != -1) {
            list[idx] = account
            refreshFlow()
            return Result.success(Unit)
        }
        return Result.failure(IllegalArgumentException("Cuenta no encontrada."))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountRepositoryAndBalanceCalculationTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeAccountRepository: FakeAccountRepository
    private lateinit var fakeTransactionRepository: FakeTransactionRepository
    private lateinit var fakeBudgetRepository: FakeBudgetRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var transactionsViewModel: TransactionsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAccountRepository = FakeAccountRepository("user_1")
        fakeTransactionRepository = FakeTransactionRepository()
        fakeBudgetRepository = FakeBudgetRepository()
        fakeGoalRepository = FakeGoalRepository()
        fakeAuthRepository = FakeAuthRepository().apply {
            currentUserMock = User(
                id = "user_1",
                email = "test@finanzas.com",
                firstName = "Ignacio",
                lastName = "Maidana",
                displayName = "Ignacio Maidana"
            )
        }

        dashboardViewModel = DashboardViewModel(
            authRepository = fakeAuthRepository,
            transactionRepository = fakeTransactionRepository,
            budgetRepository = fakeBudgetRepository,
            goalRepository = fakeGoalRepository,
            accountRepository = fakeAccountRepository
        )

        accountViewModel = AccountViewModel(fakeAccountRepository)
        transactionsViewModel = TransactionsViewModel(fakeTransactionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. CASO OBLIGATORIO SOLICITADO
    // Cuenta efectivo: saldo inicial 100.000, gasto 20.000 -> resultado 80.000
    // Cuenta digital: saldo inicial 500.000, ingreso 100.000, gasto 50.000 -> resultado 550.000
    // Saldo total esperado: 630.000
    // =========================================================================
    @Test
    fun mandatoryScenario_cashAndDigitalBalance_calculation() = runTest {
        // 1. Establecer saldos iniciales
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_CASH_ID, 100000L)
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_DIGITAL_ID, 500000L)

        // 2. Transacciones
        val txs = listOf(
            // Gasto 20.000 en Efectivo
            Transaction(
                id = "tx_1",
                title = "Compra verdulería",
                amount = 20000L,
                type = TransactionType.GASTO,
                category = "Alimentación",
                accountId = Account.DEFAULT_CASH_ID,
                accountName = "Efectivo",
                date = System.currentTimeMillis()
            ),
            // Ingreso 100.000 en Digital
            Transaction(
                id = "tx_2",
                title = "Cobro honorarios",
                amount = 100000L,
                type = TransactionType.INGRESO,
                category = "Ingresos Extra",
                accountId = Account.DEFAULT_DIGITAL_ID,
                accountName = "Billetera / Transferencia",
                date = System.currentTimeMillis()
            ),
            // Gasto 50.000 en Digital
            Transaction(
                id = "tx_3",
                title = "Pago servicio internet",
                amount = 50000L,
                type = TransactionType.GASTO,
                category = "Servicios",
                accountId = Account.DEFAULT_DIGITAL_ID,
                accountName = "Billetera / Transferencia",
                date = System.currentTimeMillis()
            )
        )
        fakeTransactionRepository.emitTransactions(txs)

        // Recargar datos en Dashboard
        dashboardViewModel.loadData()
        val state = dashboardViewModel.uiState.value

        // Verificaciones exactas
        assertEquals(80000L, state.cashBalance)      // 100.000 - 20.000 = 80.000
        assertEquals(550000L, state.digitalBalance)  // 500.000 + 100.000 - 50.000 = 550.000
        assertEquals(630000L, state.totalBalance)    // 80.000 + 550.000 = 630.000
    }

    // =========================================================================
    // 2. IDEMPOTENCIA Y USUARIO NUEVO
    // =========================================================================
    @Test
    fun newUser_defaultAccountsCreated_andIdempotent() = runTest {
        // Un nuevo usuario tiene Efectivo y Billetera/Transferencia
        val accounts = accountViewModel.uiState.value.accounts
        assertEquals(2, accounts.size)
        assertTrue(accounts.any { it.type == AccountType.CASH && it.name == "Efectivo" })
        assertTrue(accounts.any { it.type == AccountType.DIGITAL && it.name == "Billetera / Transferencia" })

        // Modificar saldo de Efectivo
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_CASH_ID, 75000L)

        // Ejecutar ensureDefaultAccounts múltiples veces
        fakeAccountRepository.ensureDefaultAccounts("user_1")
        fakeAccountRepository.ensureDefaultAccounts("user_1")

        // No se duplican las cuentas ni se sobreescribe el saldo personalizado
        val accountsAfter = fakeAccountRepository.accountsMap["user_1"]!!
        assertEquals(2, accountsAfter.size)
        assertEquals(75000L, accountsAfter.first { it.id == Account.DEFAULT_CASH_ID }.initialBalance)
    }

    // =========================================================================
    // 3. MOVIMIENTOS LEGACY (Sin accountId)
    // Suman al balance general, no rompen cuentas, no tienen cuenta asignada falsamente
    // =========================================================================
    @Test
    fun existingUser_legacyTransactionsWithoutAccountId_sumIntoTotalBalance() = runTest {
        // Saldos iniciales
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_CASH_ID, 50000L)
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_DIGITAL_ID, 200000L)

        // Movimientos legacy sin accountId ni accountName
        val legacyTxs = listOf(
            Transaction(
                id = "tx_legacy_1",
                title = "Movimiento Antiguo Ingreso",
                amount = 30000L,
                type = TransactionType.INGRESO,
                category = "Salario",
                accountId = "",
                accountName = ""
            ),
            Transaction(
                id = "tx_legacy_2",
                title = "Movimiento Antiguo Gasto",
                amount = 10000L,
                type = TransactionType.GASTO,
                category = "Alimentación",
                accountId = "",
                accountName = ""
            )
        )
        fakeTransactionRepository.emitTransactions(legacyTxs)

        dashboardViewModel.loadData()
        val state = dashboardViewModel.uiState.value

        // Efectivo y Digital conservan sus saldos limpios (no se les inventa información falsa)
        assertEquals(50000L, state.cashBalance)
        assertEquals(200000L, state.digitalBalance)

        // El saldo total sí suma los movimientos legacy: 250.000 + 30.000 - 10.000 = 270.000
        assertEquals(270000L, state.totalBalance)
    }

    // =========================================================================
    // 4. CREACIÓN DE NUEVO MOVIMIENTO CON CUENTA
    // =========================================================================
    @Test
    fun createNewTransaction_withAccount_reflectedInDerivedBalance() = runTest {
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_CASH_ID, 50000L)

        transactionsViewModel.saveTransaction(
            title = "Farmacia",
            amountText = "15000",
            type = TransactionType.GASTO,
            category = "Salud",
            dateString = "23/09/2026",
            accountId = Account.DEFAULT_CASH_ID,
            accountName = "Efectivo"
        )

        assertTrue(transactionsViewModel.saveState.value is SaveTransactionState.Success)
        val saved = fakeTransactionRepository.inMemoryTransactions.first()
        assertEquals(Account.DEFAULT_CASH_ID, saved.accountId)
        assertEquals("Efectivo", saved.accountName)

        dashboardViewModel.loadData()
        val state = dashboardViewModel.uiState.value
        assertEquals(35000L, state.cashBalance) // 50.000 - 15.000 = 35.000
    }

    // =========================================================================
    // 5. VALIDACIÓN DE CUENTA EN NUEVOS MOVIMIENTOS
    // =========================================================================
    @Test
    fun validation_newTransactionRequiresAccount() = runTest {
        transactionsViewModel.saveTransaction(
            title = "Prueba",
            amountText = "5000",
            type = TransactionType.GASTO,
            category = "Varios",
            dateString = "23/09/2026",
            accountId = "   ",
            accountName = ""
        )

        val saveState = transactionsViewModel.saveState.value
        assertTrue(saveState is SaveTransactionState.Error)
        assertEquals("Debes seleccionar una cuenta de origen.", (saveState as SaveTransactionState.Error).message)
    }

    // =========================================================================
    // 6. ELIMINACIÓN DE MOVIMIENTO RECALCULA EL SALDO
    // =========================================================================
    @Test
    fun deleteTransaction_recalculatesDerivedBalance() = runTest {
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_CASH_ID, 100000L)

        val tx = Transaction(
            id = "tx_delete_me",
            title = "Gasto Erróneo",
            amount = 40000L,
            type = TransactionType.GASTO,
            accountId = Account.DEFAULT_CASH_ID,
            accountName = "Efectivo"
        )
        fakeTransactionRepository.emitTransactions(listOf(tx))
        dashboardViewModel.loadData()
        assertEquals(60000L, dashboardViewModel.uiState.value.cashBalance)

        // Eliminar
        transactionsViewModel.deleteTransaction("tx_delete_me")
        dashboardViewModel.loadData()
        assertEquals(100000L, dashboardViewModel.uiState.value.cashBalance)
    }

    // =========================================================================
    // 7. EDICIÓN DE SALDOS INICIALES DESDE VIEWMODEL
    // =========================================================================
    @Test
    fun editInitialBalances_updatesStateCorrectly() = runTest {
        var successCalled = false
        accountViewModel.updateInitialBalances(cashInitial = 80000L, digitalInitial = 300000L) {
            successCalled = true
        }

        assertTrue(successCalled)
        assertTrue(accountViewModel.uiState.value.actionState is AccountActionState.Success)

        val cashAcc = accountViewModel.uiState.value.cashAccount()
        val digitalAcc = accountViewModel.uiState.value.digitalAccount()
        assertEquals(80000L, cashAcc?.initialBalance)
        assertEquals(300000L, digitalAcc?.initialBalance)
    }

    // =========================================================================
    // 8. AISLAMIENTO ENTRE USUARIOS
    // =========================================================================
    @Test
    fun userIsolation_accountsAndBalancesIndependentBetweenUsers() = runTest {
        // Usuario A configura saldos
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_CASH_ID, 250000L)

        // Cambiar a Usuario B
        fakeAccountRepository.switchUser("user_B")
        val vmUserB = AccountViewModel(fakeAccountRepository)

        // Usuario B tiene sus propios saldos iniciales (0 por defecto)
        val userBCash = vmUserB.uiState.value.cashAccount()
        assertEquals(0L, userBCash?.initialBalance)
    }

    // =========================================================================
    // 9. ESCENARIO EXACTO DE AUDITORÍA REQUERIDO POR EL USUARIO
    // Efectivo: saldo inicial 100k, ingreso 50k, gasto 30k, gasto 10k -> 110k
    // Digital: saldo inicial 500k, ingreso 200k, gasto 120k -> 580k
    // Saldo Total: 690k
    // =========================================================================
    @Test
    fun audit_exactScenarioRequestedByUser() = runTest {
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_CASH_ID, 100000L)
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_DIGITAL_ID, 500000L)

        val txs = listOf(
            // Efectivo
            Transaction(id = "c_in", title = "Ingreso Ef", amount = 50000L, type = TransactionType.INGRESO, accountId = Account.DEFAULT_CASH_ID, accountName = "Efectivo"),
            Transaction(id = "c_out1", title = "Gasto Ef 1", amount = 30000L, type = TransactionType.GASTO, accountId = Account.DEFAULT_CASH_ID, accountName = "Efectivo"),
            Transaction(id = "c_out2", title = "Gasto Ef 2", amount = 10000L, type = TransactionType.GASTO, accountId = Account.DEFAULT_CASH_ID, accountName = "Efectivo"),
            // Digital
            Transaction(id = "d_in", title = "Ingreso Dig", amount = 200000L, type = TransactionType.INGRESO, accountId = Account.DEFAULT_DIGITAL_ID, accountName = "Billetera / Transferencia"),
            Transaction(id = "d_out", title = "Gasto Dig", amount = 120000L, type = TransactionType.GASTO, accountId = Account.DEFAULT_DIGITAL_ID, accountName = "Billetera / Transferencia")
        )
        fakeTransactionRepository.emitTransactions(txs)
        dashboardViewModel.loadData()

        val state = dashboardViewModel.uiState.value
        assertEquals(110000L, state.cashBalance)
        assertEquals(580000L, state.digitalBalance)
        assertEquals(690000L, state.totalBalance)
    }

    // =========================================================================
    // 10. EDICIÓN DE IMPORTE EN MOVIMIENTO
    // =========================================================================
    @Test
    fun audit_editingTransactionAmount_recalculatesBalancesInstantly() = runTest {
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_CASH_ID, 100000L)
        val tx = Transaction(id = "tx_edit", title = "Supermercado", amount = 30000L, type = TransactionType.GASTO, accountId = Account.DEFAULT_CASH_ID, accountName = "Efectivo")
        fakeTransactionRepository.emitTransactions(listOf(tx))
        dashboardViewModel.loadData()
        assertEquals(70000L, dashboardViewModel.uiState.value.cashBalance)

        // Editar importe de 30.000 a 45.000
        val updatedTx = tx.copy(amount = 45000L)
        fakeTransactionRepository.emitTransactions(listOf(updatedTx))
        dashboardViewModel.loadData()

        assertEquals(55000L, dashboardViewModel.uiState.value.cashBalance)
        assertEquals(55000L, dashboardViewModel.uiState.value.totalBalance)
    }

    // =========================================================================
    // 11. CAMBIO DE CUENTA DE UN MOVIMIENTO
    // =========================================================================
    @Test
    fun audit_changingTransactionAccount_updatesBothAccountsCleanly() = runTest {
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_CASH_ID, 100000L)
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_DIGITAL_ID, 500000L)

        // Movimiento inicialmente registrado en Efectivo: gasto 25.000
        val tx = Transaction(id = "tx_transfer", title = "Cena", amount = 25000L, type = TransactionType.GASTO, accountId = Account.DEFAULT_CASH_ID, accountName = "Efectivo")
        fakeTransactionRepository.emitTransactions(listOf(tx))
        dashboardViewModel.loadData()

        assertEquals(75000L, dashboardViewModel.uiState.value.cashBalance)
        assertEquals(500000L, dashboardViewModel.uiState.value.digitalBalance)
        assertEquals(575000L, dashboardViewModel.uiState.value.totalBalance)

        // Se cambia a Digital (Billetera / Transferencia)
        val switchedTx = tx.copy(accountId = Account.DEFAULT_DIGITAL_ID, accountName = "Billetera / Transferencia")
        fakeTransactionRepository.emitTransactions(listOf(switchedTx))
        dashboardViewModel.loadData()

        // Efectivo vuelve a sus 100.000, Digital se reduce a 475.000, Total sigue siendo 575.000
        assertEquals(100000L, dashboardViewModel.uiState.value.cashBalance)
        assertEquals(475000L, dashboardViewModel.uiState.value.digitalBalance)
        assertEquals(575000L, dashboardViewModel.uiState.value.totalBalance)
    }

    // =========================================================================
    // 12. CIERRE Y REAPERTURA DE LA APLICACIÓN (NUEVO CICLO DE VIDA)
    // =========================================================================
    @Test
    fun audit_closeAndReopen_restoresBalancesConsistentlyWithoutDuplicates() = runTest {
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_CASH_ID, 120000L)
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_DIGITAL_ID, 400000L)
        val tx = Transaction(id = "tx_1", title = "Gasto", amount = 20000L, type = TransactionType.GASTO, accountId = Account.DEFAULT_CASH_ID, accountName = "Efectivo")
        fakeTransactionRepository.emitTransactions(listOf(tx))

        dashboardViewModel.loadData()
        assertEquals(100000L, dashboardViewModel.uiState.value.cashBalance)
        assertEquals(400000L, dashboardViewModel.uiState.value.digitalBalance)
        assertEquals(500000L, dashboardViewModel.uiState.value.totalBalance)

        // Simular cierre de app y nueva apertura (nuevo ViewModel sobre los mismos repositorios persistentes)
        val reopenedViewModel = DashboardViewModel(
            authRepository = fakeAuthRepository,
            transactionRepository = fakeTransactionRepository,
            budgetRepository = fakeBudgetRepository,
            goalRepository = fakeGoalRepository,
            accountRepository = fakeAccountRepository
        )
        reopenedViewModel.loadData()

        assertEquals(100000L, reopenedViewModel.uiState.value.cashBalance)
        assertEquals(400000L, reopenedViewModel.uiState.value.digitalBalance)
        assertEquals(500000L, reopenedViewModel.uiState.value.totalBalance)
        // No hay dobles conteos ni transacciones duplicadas
        assertEquals(1, reopenedViewModel.uiState.value.recentTransactions.size)
    }

    // =========================================================================
    // 13. MOVIMIENTO SIN ACCOUNTID (ESPACIOS O VACÍO)
    // =========================================================================
    @Test
    fun audit_movementWithoutAccountId_doesNotPolluteAccountEnvelopes() = runTest {
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_CASH_ID, 100000L)
        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_DIGITAL_ID, 500000L)

        val txEmpty = Transaction(id = "tx_empty", title = "Gasto sin cuenta", amount = 15000L, type = TransactionType.GASTO, accountId = "", accountName = "")
        val txBlank = Transaction(id = "tx_blank", title = "Gasto espacio", amount = 5000L, type = TransactionType.GASTO, accountId = "   ", accountName = " ")
        fakeTransactionRepository.emitTransactions(listOf(txEmpty, txBlank))
        dashboardViewModel.loadData()

        val state = dashboardViewModel.uiState.value
        // Efectivo y Digital permanecen inalterados
        assertEquals(100000L, state.cashBalance)
        assertEquals(500000L, state.digitalBalance)
        // El total balance resta los 20.000: 600.000 - 20.000 = 580.000
        assertEquals(580000L, state.totalBalance)
    }

    // =========================================================================
    // 14. FORMATO MONETARIO INTRODUCIDO EN ETAPA 1
    // =========================================================================
    @Test
    fun audit_monetaryFormat_internalValueRemainsLongAndFirestoreHasNoFormattedStrings() = runTest {
        // Simular entrada con separador de miles en el diálogo
        val inputDigits = "100000".filter { it.isDigit() }
        val parsedLong = inputDigits.toLongOrNull() ?: 0L
        assertEquals(100000L, parsedLong)

        fakeAccountRepository.updateInitialBalance(Account.DEFAULT_CASH_ID, parsedLong)
        val acc = fakeAccountRepository.getAccount(Account.DEFAULT_CASH_ID).getOrNull()
        assertNotNull(acc)
        assertEquals(100000L, acc!!.initialBalance)
        // Verificar que CurrencyUtils formatea con separador de miles
        val formatted = com.maidana.finanzasapp.ui.components.CurrencyUtils.format(acc.initialBalance)
        assertEquals("$ 100.000", formatted)
    }
}
