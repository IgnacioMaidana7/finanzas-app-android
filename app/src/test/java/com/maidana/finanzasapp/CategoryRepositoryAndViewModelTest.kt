package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.model.Budget
import com.maidana.finanzasapp.data.model.Category
import com.maidana.finanzasapp.data.model.CategoryConstants
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.data.repository.CategoryRepository
import com.maidana.finanzasapp.ui.screens.budgets.BudgetsViewModel
import com.maidana.finanzasapp.ui.screens.categories.CategoryActionState
import com.maidana.finanzasapp.ui.screens.categories.CategoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
import java.util.UUID

/**
 * Repositorio Fake en memoria que simula la lógica de FirestoreCategoryRepository con aislamiento por usuario.
 */
class FakeCategoryRepository(var currentUserId: String = "user_1") : CategoryRepository {
    // Almacena categorías por userId: Map<userId, MutableList<Category>>
    val userCategoriesMap = mutableMapOf<String, MutableList<Category>>()
    val transactionsMap = mutableMapOf<String, MutableList<Transaction>>()
    val budgetsMap = mutableMapOf<String, MutableList<Budget>>()

    private val _flow = MutableStateFlow<List<Category>>(emptyList())

    private fun getStorage(): MutableList<Category> {
        return userCategoriesMap.getOrPut(currentUserId) { mutableListOf() }
    }

    private fun refreshFlow() {
        val custom = getStorage()
        _flow.value = CategoryConstants.ALL_PREDEFINED_CATEGORIES + custom
    }

    init {
        refreshFlow()
    }

    fun switchUser(newUserId: String) {
        currentUserId = newUserId
        refreshFlow()
    }

    override fun getCategories(): Flow<List<Category>> = _flow.asStateFlow()

    override suspend fun addCategory(name: String, type: TransactionType): Result<Category> {
        val clean = name.trim()
        if (clean.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre de la categoría es obligatorio."))
        }

        // Duplicado con predefinidas
        if (CategoryConstants.ALL_PREDEFINED_CATEGORIES.any { it.type == type && it.name.equals(clean, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Ya existe una categoría predefinida con el nombre '$clean'."))
        }

        // Duplicado con activas del usuario
        if (getStorage().any { it.type == type && it.isActive && it.name.equals(clean, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Ya tienes una categoría activa llamada '$clean'."))
        }

        val category = Category(
            id = "cat_${UUID.randomUUID()}",
            name = clean,
            type = type,
            isPredefined = false,
            isActive = true
        )
        getStorage().add(category)
        refreshFlow()
        return Result.success(category)
    }

    override suspend fun updateCategory(id: String, newName: String, oldName: String): Result<Unit> {
        if (id.startsWith("predefined_")) {
            return Result.failure(IllegalArgumentException("No se pueden modificar las categorías predefinidas del sistema."))
        }

        val clean = newName.trim()
        if (clean.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre de la categoría es obligatorio."))
        }

        val list = getStorage()
        val index = list.indexOfFirst { it.id == id }
        if (index == -1) {
            return Result.failure(IllegalArgumentException("La categoría no existe."))
        }

        val current = list[index]
        if (CategoryConstants.ALL_PREDEFINED_CATEGORIES.any { it.type == current.type && it.name.equals(clean, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Ya existe una categoría predefinida con el nombre '$clean'."))
        }

        if (list.any { it.id != id && it.type == current.type && it.isActive && it.name.equals(clean, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Ya tienes otra categoría activa con el nombre '$clean'."))
        }

        list[index] = current.copy(name = clean)

        // Sincronizar movimientos y presupuestos asociados por ID o por nombre previo
        val txs = transactionsMap.getOrPut(currentUserId) { mutableListOf() }
        for (i in txs.indices) {
            if (txs[i].categoryId == id || (oldName.isNotBlank() && txs[i].category.equals(oldName, ignoreCase = true))) {
                txs[i] = txs[i].copy(category = clean, categoryId = id)
            }
        }
        val budgets = budgetsMap.getOrPut(currentUserId) { mutableListOf() }
        for (i in budgets.indices) {
            if (budgets[i].categoryId == id || (oldName.isNotBlank() && budgets[i].category.equals(oldName, ignoreCase = true))) {
                budgets[i] = budgets[i].copy(category = clean, categoryId = id)
            }
        }

        refreshFlow()
        return Result.success(Unit)
    }

    override suspend fun setCategoryActive(id: String, isActive: Boolean): Result<Unit> {
        if (id.startsWith("predefined_")) {
            return Result.failure(IllegalArgumentException("No se pueden archivar las categorías predefinidas del sistema."))
        }

        val list = getStorage()
        val index = list.indexOfFirst { it.id == id }
        if (index == -1) {
            return Result.failure(IllegalArgumentException("La categoría no existe."))
        }

        list[index] = list[index].copy(isActive = isActive)
        refreshFlow()
        return Result.success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryRepositoryAndViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeCategoryRepository
    private lateinit var viewModel: CategoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeCategoryRepository("user_A")
        viewModel = CategoryViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. Usuario sin categorías personalizadas: solo obtiene predefinidas
    @Test
    fun scenario1_userWithoutCustomCategories_onlyHasPredefined() = runTest {
        val state = viewModel.uiState.value
        assertEquals(CategoryConstants.ALL_PREDEFINED_CATEGORIES.size, state.categories.size)
        assertTrue(state.customCategories().isEmpty())
        assertEquals(CategoryConstants.EXPENSE_CATEGORIES.size, state.activeExpenseCategories().size)
        assertEquals(CategoryConstants.INCOME_CATEGORIES.size, state.activeIncomeCategories().size)
    }

    // 2. Crear primera categoría personalizada
    @Test
    fun scenario2_createFirstCategory_successAndAppearsInList() = runTest {
        var created: Category? = null
        viewModel.addCategory("Universidad", TransactionType.GASTO) {
            created = it
        }

        assertNotNull(created)
        assertEquals("Universidad", created?.name)
        assertEquals(TransactionType.GASTO, created?.type)
        assertFalse(created?.isPredefined ?: true)
        assertTrue(created?.isActive ?: false)

        val state = viewModel.uiState.value
        assertEquals(1, state.customCategories().size)
        assertEquals("Universidad", state.customCategories().first().name)
        assertTrue(state.actionState is CategoryActionState.Success)
    }

    // Validaciones: nombre vacío y duplicados
    @Test
    fun validation_emptyNameAndDuplicates_rejectedWithFriendlyMessage() = runTest {
        // Vacío
        viewModel.addCategory("   ", TransactionType.GASTO)
        assertTrue(viewModel.uiState.value.actionState is CategoryActionState.Error)

        // Duplicado con categoría predefinida (case-insensitive)
        viewModel.addCategory("alimentación", TransactionType.GASTO)
        val errState = viewModel.uiState.value.actionState
        assertTrue(errState is CategoryActionState.Error)
        assertTrue((errState as CategoryActionState.Error).message.contains("predefinida"))

        // Crear válida
        viewModel.addCategory("Mascotas", TransactionType.GASTO)
        assertTrue(viewModel.uiState.value.actionState is CategoryActionState.Success)

        // Duplicado con categoría propia activa
        viewModel.addCategory("mascotas", TransactionType.GASTO)
        assertTrue(viewModel.uiState.value.actionState is CategoryActionState.Error)
    }

    // 3. Usar categoría personalizada en un movimiento
    @Test
    fun scenario3_useCustomCategoryInTransaction() = runTest {
        var createdCat: Category? = null
        viewModel.addCategory("Mascotas", TransactionType.GASTO) { createdCat = it }

        val tx = Transaction(
            id = "tx_1",
            title = "Alimento para perro",
            amount = 15000L,
            type = TransactionType.GASTO,
            category = createdCat!!.name,
            categoryId = createdCat!!.id
        )

        assertEquals("Mascotas", tx.category)
        assertEquals(createdCat!!.id, tx.categoryId)
    }

    // 4. Usar categoría personalizada en un presupuesto y verificar cálculo de gastos
    @Test
    fun scenario4_useCustomCategoryInBudget_andMatchTransactions() = runTest {
        var createdCat: Category? = null
        viewModel.addCategory("Mascotas", TransactionType.GASTO) { createdCat = it }

        val budget = Budget(
            id = "b_1",
            category = createdCat!!.name,
            categoryId = createdCat!!.id,
            limitAmount = 50000L,
            month = 10,
            year = 2026
        )

        val txMatch = Transaction(
            id = "tx_1",
            title = "Veterinaria",
            amount = 20000L,
            type = TransactionType.GASTO,
            category = "Mascotas",
            categoryId = createdCat!!.id
        )

        val txOther = Transaction(
            id = "tx_2",
            title = "Supermercado",
            amount = 10000L,
            type = TransactionType.GASTO,
            category = "Alimentación"
        )

        // Comprobar que categoriesMatch reconoce la categoría personalizada
        assertTrue(BudgetsViewModel.categoriesMatch(budget.category, txMatch.category))
        assertFalse(BudgetsViewModel.categoriesMatch(budget.category, txOther.category))
    }

    // 5. Cerrar/reabrir app y comprobar persistencia
    @Test
    fun scenario5_persistenceAcrossViewModelReinstantiation() = runTest {
        viewModel.addCategory("Mascotas", TransactionType.GASTO)
        viewModel.addCategory("Freelance", TransactionType.INGRESO)

        // Simular nueva apertura creando nuevo ViewModel conectado al mismo repo
        val reloadedViewModel = CategoryViewModel(fakeRepository)
        val state = reloadedViewModel.uiState.value
        assertEquals(2, state.customCategories().size)
        assertTrue(state.customCategories().any { it.name == "Mascotas" })
        assertTrue(state.customCategories().any { it.name == "Freelance" })
    }

    // 6. Editar nombre de categoría personalizada
    @Test
    fun scenario6_editCategoryName_successAndPredefinedProtected() = runTest {
        var customCat: Category? = null
        viewModel.addCategory("Uni", TransactionType.GASTO) { customCat = it }

        // Editar nombre
        viewModel.updateCategory(customCat!!.id, "Universidad", oldName = "Uni")
        val state = viewModel.uiState.value
        assertEquals("Universidad", state.customCategories().first { it.id == customCat!!.id }.name)

        // Intentar editar predefinida debe ser rechazado
        viewModel.updateCategory("predefined_gasto_alimentacion", "Comida")
        val err = viewModel.uiState.value.actionState
        assertTrue(err is CategoryActionState.Error)
        assertTrue((err as CategoryActionState.Error).message.contains("predefinidas"))
    }

    // 7. Archivar categoría personalizada
    @Test
    fun scenario7_archiveCategory_excludedFromActiveLists() = runTest {
        var customCat: Category? = null
        viewModel.addCategory("Cursos", TransactionType.GASTO) { customCat = it }

        assertTrue(viewModel.uiState.value.activeExpenseCategories().any { it.name == "Cursos" })

        // Archivar
        viewModel.archiveCategory(customCat!!.id)
        val state = viewModel.uiState.value

        // Ya NO debe aparecer en categorías activas para nuevos movimientos
        assertFalse(state.activeExpenseCategories().any { it.name == "Cursos" })
        // Pero sigue existiendo en customCategories con isActive = false
        val archived = state.customCategories().first { it.id == customCat!!.id }
        assertFalse(archived.isActive)

        // Reactivar
        viewModel.reactivateCategory(customCat!!.id)
        assertTrue(viewModel.uiState.value.activeExpenseCategories().any { it.name == "Cursos" })
    }

    // 8. Movimiento histórico continúa mostrando la categoría aunque esté archivada
    @Test
    fun scenario8_historicalTransactionRetainsCategoryNameAfterArchiving() = runTest {
        var customCat: Category? = null
        viewModel.addCategory("Cursos", TransactionType.GASTO) { customCat = it }

        val historicalTx = Transaction(
            id = "tx_hist",
            title = "Curso Kotlin",
            amount = 10000L,
            type = TransactionType.GASTO,
            category = customCat!!.name,
            categoryId = customCat!!.id
        )

        // Archivar categoría
        viewModel.archiveCategory(customCat!!.id)

        // El movimiento histórico conserva su nombre de categoría intacto
        assertEquals("Cursos", historicalTx.category)
        assertEquals(customCat!!.id, historicalTx.categoryId)
    }

    // 9. Aislamiento entre usuarios (categorías de Usuario A no aparecen en Usuario B)
    @Test
    fun scenario9_userIsolation_userCategoriesNotVisibleToOtherUser() = runTest {
        // Usuario A crea una categoría
        viewModel.addCategory("Gimnasio", TransactionType.GASTO)
        assertEquals(1, viewModel.uiState.value.customCategories().size)

        // Cambiar a Usuario B
        fakeRepository.switchUser("user_B")
        val vmUserB = CategoryViewModel(fakeRepository)

        // Usuario B NO ve la categoría de Usuario A
        assertEquals(0, vmUserB.uiState.value.customCategories().size)
        assertFalse(vmUserB.uiState.value.categories.any { it.name == "Gimnasio" })
    }

    // 10. Categorías predefinidas continúan funcionando
    @Test
    fun scenario10_predefinedCategoriesAlwaysPresentAndProtected() = runTest {
        val state = viewModel.uiState.value
        for (predefined in CategoryConstants.EXPENSE_CATEGORIES) {
            assertTrue(state.activeExpenseCategories().any { it.name == predefined })
        }
        for (predefined in CategoryConstants.INCOME_CATEGORIES) {
            assertTrue(state.activeIncomeCategories().any { it.name == predefined })
        }

        // Intentar archivar predefinida falla
        viewModel.archiveCategory("predefined_gasto_transporte")
        val err = viewModel.uiState.value.actionState
        assertTrue(err is CategoryActionState.Error)
        assertTrue((err as CategoryActionState.Error).message.contains("predefinidas"))
    }

    // AUDIT 1: Usuario A crea "Mascotas", Usuario B NO debe verla
    @Test
    fun auditScenario1_userACreatesMascotas_userBCannotSeeIt() = runTest {
        viewModel.addCategory("Mascotas", TransactionType.GASTO)
        val userACategories = viewModel.uiState.value.customCategories()
        assertEquals(1, userACategories.size)
        assertEquals("Mascotas", userACategories.first().name)

        // Cambiar sesión a Usuario B
        fakeRepository.switchUser("user_B")
        val vmUserB = CategoryViewModel(fakeRepository)

        // Usuario B NO ve "Mascotas" en absoluto
        val userBCustom = vmUserB.uiState.value.customCategories()
        assertTrue(userBCustom.isEmpty())
        assertFalse(vmUserB.uiState.value.categories.any { it.name.equals("Mascotas", ignoreCase = true) })
    }

    // AUDIT 2: Usuario A crea movimiento con "Mascotas" y luego renombra a "Veterinaria"
    @Test
    fun auditScenario2_renameMascotasToVeterinaria_historicalRepresentationAndBudgetIntegrity() = runTest {
        var createdCat: Category? = null
        viewModel.addCategory("Mascotas", TransactionType.GASTO) { createdCat = it }
        val catId = createdCat!!.id

        // Movimiento histórico inicial
        val initialTx = Transaction(
            id = "tx_vet_1",
            title = "Vacunación perro",
            amount = 12000L,
            type = TransactionType.GASTO,
            category = "Mascotas",
            categoryId = catId,
            date = java.util.Calendar.getInstance().apply {
                set(2026, java.util.Calendar.SEPTEMBER, 15, 12, 0, 0)
            }.timeInMillis
        )
        fakeRepository.transactionsMap.getOrPut("user_A") { mutableListOf() }.add(initialTx)

        // Presupuesto inicial
        val initialBudget = Budget(
            id = "b_mascotas",
            category = "Mascotas",
            categoryId = catId,
            limitAmount = 30000L,
            month = 9,
            year = 2026
        )
        fakeRepository.budgetsMap.getOrPut("user_A") { mutableListOf() }.add(initialBudget)

        // Comprobar cálculo de gasto antes del renombre
        assertEquals(
            12000L,
            BudgetsViewModel.calculateSpentAmount(initialBudget, listOf(initialTx))
        )

        // Usuario A renombra la categoría a "Veterinaria"
        viewModel.updateCategory(id = catId, newName = "Veterinaria", oldName = "Mascotas")
        assertTrue(viewModel.uiState.value.actionState is CategoryActionState.Success)

        // 1. La categoría en sí ahora se llama "Veterinaria"
        val updatedCat = viewModel.uiState.value.customCategories().first { it.id == catId }
        assertEquals("Veterinaria", updatedCat.name)

        // 2. Las transacciones históricas sincronizadas tienen nombre "Veterinaria" y conservan categoryId
        val syncedTxs = fakeRepository.transactionsMap["user_A"]!!
        assertEquals("Veterinaria", syncedTxs.first().category)
        assertEquals(catId, syncedTxs.first().categoryId)

        // 3. Los presupuestos sincronizados tienen nombre "Veterinaria" y conservan categoryId
        val syncedBudgets = fakeRepository.budgetsMap["user_A"]!!
        assertEquals("Veterinaria", syncedBudgets.first().category)
        assertEquals(catId, syncedBudgets.first().categoryId)

        // 4. El cálculo de consumo del presupuesto se mantiene íntegro tras el renombre
        assertEquals(
            12000L,
            BudgetsViewModel.calculateSpentAmount(syncedBudgets.first(), syncedTxs)
        )

        // 5. Coincidencia por ID: si el presupuesto o la transacción tienen el ID coincidente,
        // categoriesMatch da true incluso si los nombres difirieran durante una transición
        val mismatchedNameTx = initialTx.copy(category = "Mascotas")
        val renamedBudget = syncedBudgets.first()
        assertTrue(BudgetsViewModel.categoriesMatch(renamedBudget, mismatchedNameTx))
    }

    // AUDIT 3: Usuario A archiva la categoría: desaparece de nuevas opciones pero movimientos históricos intactos
    @Test
    fun auditScenario3_archiveCategory_disappearsFromNewOptions_historicalTxDisplayCorrectly() = runTest {
        var createdCat: Category? = null
        viewModel.addCategory("Mascotas", TransactionType.GASTO) { createdCat = it }
        val catId = createdCat!!.id

        val tx = Transaction(
            id = "tx_hist_1",
            title = "Juguete gato",
            amount = 5000L,
            type = TransactionType.GASTO,
            category = "Mascotas",
            categoryId = catId,
            date = java.util.Calendar.getInstance().apply {
                set(2026, java.util.Calendar.SEPTEMBER, 15, 12, 0, 0)
            }.timeInMillis
        )

        val budget = Budget(
            id = "b_mascotas",
            category = "Mascotas",
            categoryId = catId,
            limitAmount = 25000L,
            month = 9,
            year = 2026
        )

        // Archivar la categoría
        viewModel.archiveCategory(catId)
        val state = viewModel.uiState.value

        // 1. Desaparece de las opciones para nuevos gastos
        assertFalse(state.activeExpenseCategories().any { it.id == catId || it.name == "Mascotas" })

        // 2. El movimiento histórico existente sigue existiendo y muestra "Mascotas"
        assertEquals("Mascotas", tx.category)
        assertEquals(catId, tx.categoryId)

        // 3. El presupuesto histórico sigue computando el gasto del movimiento correctamente
        assertEquals(5000L, BudgetsViewModel.calculateSpentAmount(budget, listOf(tx)))
    }

    // AUDIT 4: Robustez de categoriesMatch con IDs y fallback a nombres legacy
    @Test
    fun auditScenario4_categoriesMatch_robustnessWithIdsAndNames() {
        val budgetWithId = Budget(category = "Veterinaria", categoryId = "cat_123")
        val txWithSameId = Transaction(category = "Veterinaria", categoryId = "cat_123")
        val txWithSameIdDifferentName = Transaction(category = "Mascotas", categoryId = "cat_123")
        val txWithDifferentId = Transaction(category = "Veterinaria", categoryId = "cat_999")

        // Mismo categoryId coincide sin importar diferencias temporales de nombre
        assertTrue(BudgetsViewModel.categoriesMatch(budgetWithId, txWithSameId))
        assertTrue(BudgetsViewModel.categoriesMatch(budgetWithId, txWithSameIdDifferentName))

        // Documentos Legacy: sin categoryId (vacío)
        val legacyBudget = Budget(category = "Alimentación", categoryId = "")
        val legacyTx = Transaction(category = "Alimentación", categoryId = "")
        assertTrue(BudgetsViewModel.categoriesMatch(legacyBudget, legacyTx))

        // Documento nuevo con ID vs documento legacy sin ID
        val modernTxPredefined = Transaction(category = "Alimentación", categoryId = "predefined_gasto_alimentación")
        assertTrue(BudgetsViewModel.categoriesMatch(legacyBudget, modernTxPredefined))

        // Aliases legacy conocidos ("Vivienda y Servicios" coincide con "Vivienda")
        val compoundBudget = Budget(category = "Vivienda y Servicios", categoryId = "")
        val subTx = Transaction(category = "Vivienda", categoryId = "")
        assertTrue(BudgetsViewModel.categoriesMatch(compoundBudget, subTx))

        // Categorías que no coinciden ni por ID ni por nombre
        val unrelatedTx = Transaction(category = "Transporte", categoryId = "predefined_gasto_transporte")
        assertFalse(BudgetsViewModel.categoriesMatch(budgetWithId, unrelatedTx))
    }

    // AUDIT 5: Detección estricta de nombres vacíos y con espacios en blanco
    @Test
    fun auditScenario5_blankAndWhitespaceCategoryNamesRejected() = runTest {
        viewModel.addCategory("", TransactionType.GASTO)
        assertTrue(viewModel.uiState.value.actionState is CategoryActionState.Error)

        viewModel.addCategory("   ", TransactionType.GASTO)
        assertTrue(viewModel.uiState.value.actionState is CategoryActionState.Error)

        viewModel.addCategory("\t\n  ", TransactionType.INGRESO)
        assertTrue(viewModel.uiState.value.actionState is CategoryActionState.Error)
    }

    // AUDIT 6: Detección estricta de duplicados con mayúsculas/minúsculas y espacios
    @Test
    fun auditScenario6_caseInsensitiveAndTrimmedDuplicatesRejected() = runTest {
        viewModel.addCategory("Mascotas", TransactionType.GASTO)
        assertTrue(viewModel.uiState.value.actionState is CategoryActionState.Success)

        // Intento en minúsculas
        viewModel.addCategory("mascotas", TransactionType.GASTO)
        assertTrue(viewModel.uiState.value.actionState is CategoryActionState.Error)

        // Intento con espacios alrededor
        viewModel.addCategory("  Mascotas  ", TransactionType.GASTO)
        assertTrue(viewModel.uiState.value.actionState is CategoryActionState.Error)

        // Intento con mayúsculas completas
        viewModel.addCategory("MASCOTAS", TransactionType.GASTO)
        assertTrue(viewModel.uiState.value.actionState is CategoryActionState.Error)
    }
}
