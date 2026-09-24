package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.model.Account
import com.maidana.finanzasapp.data.model.AccountType
import com.maidana.finanzasapp.data.model.Budget
import com.maidana.finanzasapp.data.model.BudgetHealthStatus
import com.maidana.finanzasapp.data.model.Category
import com.maidana.finanzasapp.data.model.SavingsGoal
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.data.model.User
import com.maidana.finanzasapp.ui.screens.accounts.AccountViewModel
import com.maidana.finanzasapp.ui.screens.auth.AuthState
import com.maidana.finanzasapp.ui.screens.auth.AuthViewModel
import com.maidana.finanzasapp.ui.screens.budgets.BudgetsViewModel
import com.maidana.finanzasapp.ui.screens.categories.CategoryViewModel
import com.maidana.finanzasapp.ui.screens.dashboard.DashboardStatus
import com.maidana.finanzasapp.ui.screens.dashboard.DashboardViewModel
import com.maidana.finanzasapp.ui.screens.goals.GoalsViewModel
import com.maidana.finanzasapp.ui.screens.statistics.StatisticsStatus
import com.maidana.finanzasapp.ui.screens.statistics.StatisticsViewModel
import com.maidana.finanzasapp.ui.screens.transactions.TransactionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Suite de Auditoría Final Integral (Etapas 1, 2, 3 y 4).
 *
 * Simula el flujo completo de usuario y verifica rigurosamente:
 * 1. Registro de usuario: "Ignacio Maidana".
 * 2. Saludo en Dashboard: "Hola Ignacio".
 * 3. Navegación inmediata a Movimientos, Presupuestos, Objetivos y Estadísticas sin errores ni permisos denegados.
 * 4. Configuración de saldos iniciales: Efectivo 100.000, Digital 500.000.
 * 5. Creación de categoría personalizada: "Universidad".
 * 6. Gasto 50.000 en Universidad (Billetera / Transferencia).
 * 7. Gasto 20.000 en Alimentación (Efectivo).
 * 8. Ingreso 200.000 en Billetera / Transferencia.
 *    -> Verificación: Efectivo: 80.000 | Digital: 650.000 | Total: 730.000.
 * 9. Presupuesto para "Universidad" (100.000) con 50% consumido.
 * 10. Estadísticas completas: Ingreso 200.000, Gasto 70.000, Superávit 130.000, Tasa 65%, Cuentas y Categorías.
 * 11. Cierre y reapertura de la app (persistencia y ausencia de duplicados).
 * 12. Compatibilidad con movimientos legacy sin accountId.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EndToEndCompleteFlowAuditTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var fakeAccountRepo: FakeAccountRepository
    private lateinit var fakeTxRepo: FakeTransactionRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var fakeBudgetRepo: FakeBudgetRepository
    private lateinit var fakeGoalRepo: FakeGoalRepository

    // Timestamp fijo: 23 Marzo 2024
    private val fixedTimestamp = 1711200000000L
    private val timeProvider: () -> Long = { fixedTimestamp }
    private val fixedDateString = "23/03/2024"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepo = FakeAuthRepository()
        fakeAccountRepo = FakeAccountRepository("user_ignacio")
        fakeTxRepo = FakeTransactionRepository()
        fakeCategoryRepo = FakeCategoryRepository("user_ignacio")
        fakeBudgetRepo = FakeBudgetRepository()
        fakeGoalRepo = FakeGoalRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun completeEndToEndUserAuditScenario() = runTest {
        // =====================================================================
        // PASO 1: Registrar un usuario nuevo: Ignacio Maidana
        // =====================================================================
        val authViewModel = AuthViewModel(fakeAuthRepo)
        authViewModel.register(
            firstName = "Ignacio",
            lastName = "Maidana",
            email = "ignacio.maidana@test.com",
            password = "Password123",
            confirmPassword = "Password123"
        )

        val authState = authViewModel.uiState.value.authState
        assertTrue("El registro debe autenticar exitosamente al usuario", authState is AuthState.Authenticated)
        val registeredUser = (authState as AuthState.Authenticated).user
        assertEquals("Ignacio", registeredUser.firstName)
        assertEquals("Maidana", registeredUser.lastName)
        assertEquals("Ignacio Maidana", registeredUser.fullName)
        assertEquals("Ignacio", registeredUser.greetingName)

        // Configurar el usuario autenticado en el repo
        fakeAuthRepo.currentUserMock = registeredUser

        // =====================================================================
        // PASO 2: Entrar al Dashboard -> Debe mostrar "Hola Ignacio"
        // =====================================================================
        val dashboardViewModel = DashboardViewModel(
            authRepository = fakeAuthRepo,
            transactionRepository = fakeTxRepo,
            budgetRepository = fakeBudgetRepo,
            goalRepository = fakeGoalRepo,
            accountRepository = fakeAccountRepo,
            currentTimeProvider = timeProvider
        )

        assertEquals("Ignacio", dashboardViewModel.uiState.value.userName)
        val greetingHeading = "¡Hola, ${dashboardViewModel.uiState.value.userName}! 👋"
        assertTrue(greetingHeading.contains("Hola, Ignacio"))

        // =====================================================================
        // PASO 3: Entrar inmediatamente a Movimientos, Presupuestos, Objetivos y Estadísticas
        // No debe existir ningún PERMISSION_DENIED ni necesidad de reiniciar la app
        // =====================================================================
        val txViewModel = TransactionsViewModel(fakeTxRepo)
        val budgetsViewModel = BudgetsViewModel(fakeBudgetRepo, fakeTxRepo, timeProvider)
        val goalsViewModel = GoalsViewModel(fakeGoalRepo)
        val categoryViewModel = CategoryViewModel(fakeCategoryRepo)
        val accountViewModel = AccountViewModel(fakeAccountRepo)
        val statisticsViewModel = StatisticsViewModel(
            transactionRepository = fakeTxRepo,
            budgetRepository = fakeBudgetRepo,
            accountRepository = fakeAccountRepo,
            currentTimeProvider = timeProvider
        )

        // Verificamos que ninguno tenga error
        assertNull(txViewModel.uiState.value.errorMessage)
        assertNull(budgetsViewModel.uiState.value.errorMessage)
        assertNull(goalsViewModel.uiState.value.errorMessage)
        assertNull(categoryViewModel.uiState.value.errorMessage)
        assertNull(accountViewModel.uiState.value.errorMessage)
        assertNull(statisticsViewModel.uiState.value.errorMessage)

        // Cuentas por defecto existen inmediatamente
        val initialAccounts = accountViewModel.uiState.value.accounts
        assertEquals(2, initialAccounts.size)
        assertTrue(initialAccounts.any { it.id == Account.DEFAULT_CASH_ID })
        assertTrue(initialAccounts.any { it.id == Account.DEFAULT_DIGITAL_ID })

        // =====================================================================
        // PASO 4: Configurar saldo inicial:
        // Efectivo: 100.000
        // Billetera / Transferencia: 500.000
        // =====================================================================
        var balancesSaved = false
        accountViewModel.updateInitialBalances(cashInitial = 100000L, digitalInitial = 500000L) {
            balancesSaved = true
        }
        assertTrue(balancesSaved)

        dashboardViewModel.loadData()
        val postBalanceState = dashboardViewModel.uiState.value
        assertEquals(100000L, postBalanceState.cashBalance)
        assertEquals(500000L, postBalanceState.digitalBalance)
        assertEquals(600000L, postBalanceState.totalBalance)

        // =====================================================================
        // PASO 5: Crear categoría personalizada: "Universidad" (Tipo GASTO)
        // =====================================================================
        var createdCategory: Category? = null
        categoryViewModel.addCategory(
            name = "Universidad",
            type = TransactionType.GASTO
        ) { cat ->
            createdCategory = cat
        }
        assertNotNull(createdCategory)
        val categories = categoryViewModel.uiState.value.categories
        assertTrue(categories.any { it.name.equals("Universidad", ignoreCase = true) })
        val universidadCat = categories.first { it.name.equals("Universidad", ignoreCase = true) }

        // =====================================================================
        // PASO 6: Crear gasto: 50.000, "Universidad", Billetera / Transferencia
        // =====================================================================
        txViewModel.saveTransaction(
            title = "Matrícula Universidad",
            amountText = "50000",
            type = TransactionType.GASTO,
            category = universidadCat.name,
            categoryId = universidadCat.id,
            accountId = Account.DEFAULT_DIGITAL_ID,
            accountName = "Billetera / Transferencia",
            dateString = fixedDateString
        )

        // =====================================================================
        // PASO 7: Crear gasto: 20.000, "Alimentación", Efectivo
        // =====================================================================
        txViewModel.saveTransaction(
            title = "Compra Supermercado",
            amountText = "20000",
            type = TransactionType.GASTO,
            category = "Alimentación",
            categoryId = "cat_alimentacion",
            accountId = Account.DEFAULT_CASH_ID,
            accountName = "Efectivo",
            dateString = fixedDateString
        )

        // =====================================================================
        // PASO 8: Crear ingreso: 200.000, Billetera / Transferencia
        // =====================================================================
        txViewModel.saveTransaction(
            title = "Cobro Sueldo",
            amountText = "200000",
            type = TransactionType.INGRESO,
            category = "Sueldo",
            categoryId = "cat_sueldo",
            accountId = Account.DEFAULT_DIGITAL_ID,
            accountName = "Billetera / Transferencia",
            dateString = fixedDateString
        )

        // VERIFICACIÓN ESTRICTA DE SALDOS (Paso 8)
        dashboardViewModel.loadData()
        val step8State = dashboardViewModel.uiState.value
        assertEquals("Efectivo debe ser 80.000 (100.000 - 20.000)", 80000L, step8State.cashBalance)
        assertEquals("Digital debe ser 650.000 (500.000 - 50.000 + 200.000)", 650000L, step8State.digitalBalance)
        assertEquals("Saldo total debe ser 730.000 (80.000 + 650.000)", 730000L, step8State.totalBalance)
        assertEquals(200000L, step8State.monthlyIncome)
        assertEquals(70000L, step8State.monthlyExpense)
        assertEquals(130000L, step8State.monthlySavings)
        assertEquals("Tasa de ahorro 65%", step8State.savingsRateText)

        // =====================================================================
        // PASO 9: Crear presupuesto usando "Universidad"
        // Límite mensual: 100.000
        // =====================================================================
        val cal = Calendar.getInstance().apply { timeInMillis = fixedTimestamp }
        val curMonth = cal.get(Calendar.MONTH) + 1
        val curYear = cal.get(Calendar.YEAR)

        budgetsViewModel.saveBudget(
            category = universidadCat.name,
            categoryId = universidadCat.id,
            limitAmount = 100000L,
            month = curMonth,
            year = curYear
        )

        val budgetList = budgetsViewModel.uiState.value.budgets
        assertEquals(1, budgetList.size)
        val uniBudget = budgetList.first()
        assertEquals(100000L, uniBudget.limitAmount)
        assertEquals("El presupuesto debe registrar 50.000 gastados en Universidad", 50000L, uniBudget.spentAmount)
        assertEquals("El porcentaje debe ser exactamente 50%", 0.5f, uniBudget.percentage, 0.001f)
        assertEquals(50000L, uniBudget.remainingAmount)

        // =====================================================================
        // PASO 10: Revisar Estadísticas / Analíticas
        // =====================================================================
        val stats = statisticsViewModel.uiState.value
        assertEquals(StatisticsStatus.Success, stats.status)

        // 10.1 Resumen Financiero
        assertEquals(200000L, stats.summary.income)
        assertEquals(70000L, stats.summary.expense)
        assertEquals(130000L, stats.summary.balance)
        assertEquals(0.65f, stats.summary.savingsRate, 0.01f)
        assertEquals("65%", stats.summary.savingsRateText)

        // 10.2 Desglose por Categorías (Universidad $50k, Alimentación $20k)
        assertEquals(2, stats.categoryExpenses.size)
        val catUni = stats.categoryExpenses.firstOrNull { it.category.equals("Universidad", ignoreCase = true) }
        val catAli = stats.categoryExpenses.firstOrNull { it.category.equals("Alimentación", ignoreCase = true) }
        assertNotNull(catUni)
        assertNotNull(catAli)
        assertEquals(50000L, catUni!!.amount)
        assertEquals(20000L, catAli!!.amount)
        assertEquals(0.714f, catUni.percentage, 0.01f)
        assertEquals(0.286f, catAli.percentage, 0.01f)

        // 10.3 Distribución de Cuentas (Efectivo $80k, Digital $650k)
        assertEquals(2, stats.accountDistributions.size)
        val accCash = stats.accountDistributions.firstOrNull { it.type == AccountType.CASH }
        val accDigital = stats.accountDistributions.firstOrNull { it.type == AccountType.DIGITAL }
        assertNotNull(accCash)
        assertNotNull(accDigital)
        assertEquals(80000L, accCash!!.balance)
        assertEquals(650000L, accDigital!!.balance)

        // 10.4 Rendimiento de Presupuestos
        assertEquals(1, stats.budgetPerformances.size)
        val perf = stats.budgetPerformances.first()
        assertEquals("Universidad", perf.category)
        assertEquals(100000L, perf.limitAmount)
        assertEquals(50000L, perf.spentAmount)
        assertEquals(0.5f, perf.percentage, 0.001f)
        assertEquals(BudgetHealthStatus.NORMAL, perf.status)

        // =====================================================================
        // PASO 11: Cierre y reapertura de la app (Persistencia y sin duplicados)
        // =====================================================================
        val reopenedDashboardViewModel = DashboardViewModel(
            authRepository = fakeAuthRepo,
            transactionRepository = fakeTxRepo,
            budgetRepository = fakeBudgetRepo,
            goalRepository = fakeGoalRepo,
            accountRepository = fakeAccountRepo,
            currentTimeProvider = timeProvider
        )
        val reopenedTxViewModel = TransactionsViewModel(fakeTxRepo)
        val reopenedBudgetsViewModel = BudgetsViewModel(fakeBudgetRepo, fakeTxRepo, timeProvider)
        val reopenedStatsViewModel = StatisticsViewModel(
            transactionRepository = fakeTxRepo,
            budgetRepository = fakeBudgetRepo,
            accountRepository = fakeAccountRepo,
            currentTimeProvider = timeProvider
        )

        reopenedDashboardViewModel.loadData()
        val reopenedDashboardState = reopenedDashboardViewModel.uiState.value

        assertEquals(80000L, reopenedDashboardState.cashBalance)
        assertEquals(650000L, reopenedDashboardState.digitalBalance)
        assertEquals(730000L, reopenedDashboardState.totalBalance)
        assertEquals(3, reopenedDashboardState.recentTransactions.size)
        assertEquals(3, reopenedTxViewModel.uiState.value.transactions.size)
        assertEquals(1, reopenedBudgetsViewModel.uiState.value.budgets.size)
        assertEquals(130000L, reopenedStatsViewModel.uiState.value.summary.balance)

        // =====================================================================
        // PASO 12: Usuario existente con movimientos legacy sin accountId
        // =====================================================================
        // Agregamos un movimiento legacy de ingreso ($40.000) y de gasto ($15.000)
        val legacyIncome = Transaction(
            id = "tx_legacy_inc",
            title = "Ingreso Antiguo",
            amount = 40000L,
            type = TransactionType.INGRESO,
            category = "Otros",
            accountId = "", // Legacy: sin accountId
            accountName = "",
            date = fixedTimestamp
        )
        val legacyExpense = Transaction(
            id = "tx_legacy_exp",
            title = "Gasto Antiguo",
            amount = 15000L,
            type = TransactionType.GASTO,
            category = "Otros",
            accountId = "", // Legacy: sin accountId
            accountName = "",
            date = fixedTimestamp
        )

        fakeTxRepo.addTransaction(legacyIncome)
        fakeTxRepo.addTransaction(legacyExpense)

        reopenedDashboardViewModel.loadData()
        val legacyState = reopenedDashboardViewModel.uiState.value

        // Las cuentas Efectivo y Digital NO deben verse contaminadas ni alteradas
        assertEquals(80000L, legacyState.cashBalance)
        assertEquals(650000L, legacyState.digitalBalance)
        // El total balance debe sumar y restar los movimientos legacy:
        // 730.000 + 40.000 - 15.000 = 755.000
        assertEquals(755000L, legacyState.totalBalance)
    }
}
