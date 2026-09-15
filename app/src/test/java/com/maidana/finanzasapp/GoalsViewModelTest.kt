package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.model.SavingsGoal
import com.maidana.finanzasapp.ui.screens.goals.ContributionState
import com.maidana.finanzasapp.ui.screens.goals.DeleteGoalState
import com.maidana.finanzasapp.ui.screens.goals.GoalsStatus
import com.maidana.finanzasapp.ui.screens.goals.GoalsViewModel
import com.maidana.finanzasapp.ui.screens.goals.SaveGoalState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeGoalRepository: FakeGoalRepository
    private lateinit var viewModel: GoalsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeGoalRepository = FakeGoalRepository()
        viewModel = GoalsViewModel(goalRepository = fakeGoalRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_whenRepositoryEmpty_isGoalsStatusEmpty() {
        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertEquals(0, state.goals.size)
        assertEquals(0L, state.totalTarget)
        assertEquals(0L, state.totalSaved)
        assertEquals(0f, state.overallPercentage, 0.001f)
    }

    @Test
    fun initialState_whenRepositoryHasGoals_isGoalsStatusSuccess_andCalculatesMetrics() {
        val goals = listOf(
            SavingsGoal(id = "1", name = "Vacaciones", targetAmount = 200000L, savedAmount = 100000L),
            SavingsGoal(id = "2", name = "Fondo Emergencia", targetAmount = 300000L, savedAmount = 150000L)
        )
        fakeGoalRepository.emitGoals(goals)

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertEquals(2, state.goals.size)
        assertEquals(500000L, state.totalTarget)
        assertEquals(250000L, state.totalSaved)
        assertEquals(0.5f, state.overallPercentage, 0.001f)
    }

    @Test
    fun addGoal_validData_succeedsAndUpdatesState() {
        viewModel.addGoal(
            name = "Nuevo Auto",
            targetAmount = 1000000L,
            initialAmount = 0L,
            targetDateString = "Diciembre 2026"
        )

        assertTrue(viewModel.saveState.value is SaveGoalState.Success)
        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertEquals(1, state.goals.size)
        assertEquals("Nuevo Auto", state.goals[0].name)
        assertEquals(1000000L, state.goals[0].targetAmount)
        assertEquals(0L, state.goals[0].savedAmount)
    }

    @Test
    fun addGoal_withInitialAmount_createsGoalAndInitialContribution() = runTest {
        viewModel.addGoal(
            name = "Viaje a Japón",
            targetAmount = 500000L,
            initialAmount = 100000L,
            targetDateString = "Junio 2027"
        )

        assertTrue(viewModel.saveState.value is SaveGoalState.Success)
        val state = viewModel.uiState.value
        assertEquals(1, state.goals.size)
        val createdGoal = state.goals[0]
        assertEquals(100000L, createdGoal.savedAmount)

        // Verificar que el aporte inicial existe en el repositorio
        val contributions = fakeGoalRepository.getContributions(createdGoal.id).first()
        assertEquals(1, contributions.size)
        assertEquals(100000L, contributions[0].amount)
    }

    @Test
    fun addGoal_blankName_setsValidationError() {
        viewModel.addGoal(
            name = "   ",
            targetAmount = 50000L
        )

        val saveState = viewModel.saveState.value
        assertTrue(saveState is SaveGoalState.Error)
        assertEquals("El nombre del objetivo es obligatorio.", (saveState as SaveGoalState.Error).message)
    }

    @Test
    fun addGoal_invalidTargetAmount_setsValidationError() {
        viewModel.addGoal(
            name = "Ahorro",
            targetAmount = 0L
        )

        val saveState = viewModel.saveState.value
        assertTrue(saveState is SaveGoalState.Error)
        assertEquals("El monto objetivo debe ser mayor a $ 0.", (saveState as SaveGoalState.Error).message)
    }

    @Test
    fun addGoal_initialAmountExceedsTarget_setsValidationError() {
        viewModel.addGoal(
            name = "Ahorro",
            targetAmount = 100000L,
            initialAmount = 150000L
        )

        val saveState = viewModel.saveState.value
        assertTrue(saveState is SaveGoalState.Error)
        assertEquals("El monto inicial no puede ser mayor al monto objetivo.", (saveState as SaveGoalState.Error).message)
    }

    @Test
    fun addGoal_repositoryFailure_setsErrorState() {
        fakeGoalRepository.shouldFailAdd = true
        viewModel.addGoal(
            name = "Meta Fallida",
            targetAmount = 100000L
        )

        val saveState = viewModel.saveState.value
        assertTrue(saveState is SaveGoalState.Error)
        assertNotNull((saveState as SaveGoalState.Error).message)
    }

    @Test
    fun addContribution_validData_incrementsGoalSavedAmount_andAddsContribution() = runTest {
        viewModel.addGoal(
            name = "Fondo",
            targetAmount = 200000L,
            initialAmount = 50000L
        )
        val goalId = viewModel.uiState.value.goals[0].id

        viewModel.addContribution(
            goalId = goalId,
            amount = 25000L,
            note = "Ahorro de la semana"
        )

        assertTrue(viewModel.contributionState.value is ContributionState.Success)
        val updatedGoal = viewModel.uiState.value.goals.find { it.id == goalId }
        assertNotNull(updatedGoal)
        assertEquals(75000L, updatedGoal!!.savedAmount)

        val contributions = fakeGoalRepository.getContributions(goalId).first()
        // Aporte inicial (50.000) + nuevo aporte (25.000) = 2 aportes
        assertEquals(2, contributions.size)
        assertEquals(25000L, contributions[0].amount)
        assertEquals("Ahorro de la semana", contributions[0].note)
    }

    @Test
    fun addContribution_invalidAmount_setsValidationError() {
        viewModel.addContribution(
            goalId = "some_goal",
            amount = 0L,
            note = "Invalido"
        )

        val state = viewModel.contributionState.value
        assertTrue(state is ContributionState.Error)
        assertEquals("El monto del aporte debe ser mayor a $ 0.", (state as ContributionState.Error).message)
    }

    @Test
    fun addContribution_repositoryFailure_setsErrorState() {
        fakeGoalRepository.shouldFailAddContribution = true
        viewModel.addContribution(
            goalId = "some_goal",
            amount = 10000L,
            note = "Error test"
        )

        val state = viewModel.contributionState.value
        assertTrue(state is ContributionState.Error)
        assertNotNull((state as ContributionState.Error).message)
    }

    @Test
    fun deleteGoal_removesGoalAndAssociatedContributions() = runTest {
        viewModel.addGoal(
            name = "Para Borrar",
            targetAmount = 100000L,
            initialAmount = 20000L
        )
        val goalId = viewModel.uiState.value.goals[0].id

        viewModel.deleteGoal(goalId)

        assertTrue(viewModel.deleteState.value is DeleteGoalState.Success)
        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals(0, viewModel.uiState.value.goals.size)

        val contributions = fakeGoalRepository.getContributions(goalId).first()
        assertTrue(contributions.isEmpty())
    }

    @Test
    fun deleteGoal_repositoryFailure_setsErrorState() {
        fakeGoalRepository.shouldFailDelete = true
        viewModel.deleteGoal("some_id")

        val state = viewModel.deleteState.value
        assertTrue(state is DeleteGoalState.Error)
        assertNotNull((state as DeleteGoalState.Error).message)
    }

    @Test
    fun goalProgress_andExceededCalculation() {
        val normalGoal = SavingsGoal(id = "1", name = "G1", targetAmount = 100000L, savedAmount = 50000L)
        assertEquals(0.5f, normalGoal.progress, 0.001f)
        assertFalse(normalGoal.isCompleted)
        assertFalse(normalGoal.isExceeded)
        assertEquals(50000L, normalGoal.remainingAmount)

        val completedGoal = SavingsGoal(id = "2", name = "G2", targetAmount = 100000L, savedAmount = 100000L)
        assertEquals(1.0f, completedGoal.progress, 0.001f)
        assertTrue(completedGoal.isCompleted)
        assertFalse(completedGoal.isExceeded)
        assertEquals(0L, completedGoal.remainingAmount)

        val exceededGoal = SavingsGoal(id = "3", name = "G3", targetAmount = 100000L, savedAmount = 150000L)
        assertEquals(1.5f, exceededGoal.progress, 0.001f)
        assertTrue(exceededGoal.isCompleted)
        assertTrue(exceededGoal.isExceeded)
        assertEquals(0L, exceededGoal.remainingAmount)
    }

    @Test
    fun loadData_whenRepositoryFails_setsGoalsStatusError() {
        fakeGoalRepository.shouldThrowOnGet = true
        viewModel.loadData()

        val state = viewModel.uiState.value
        assertTrue(state.isError)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Error simulado"))
    }
}
