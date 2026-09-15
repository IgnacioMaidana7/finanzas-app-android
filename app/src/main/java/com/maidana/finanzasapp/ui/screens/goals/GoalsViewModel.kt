package com.maidana.finanzasapp.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidana.finanzasapp.data.model.GoalContribution
import com.maidana.finanzasapp.data.model.SavingsGoal
import com.maidana.finanzasapp.data.repository.FirestoreGoalRepository
import com.maidana.finanzasapp.data.repository.GoalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface GoalsStatus {
    data object Loading : GoalsStatus
    data object Success : GoalsStatus
    data object Empty : GoalsStatus
    data class Error(val message: String) : GoalsStatus
}

sealed interface SaveGoalState {
    data object Idle : SaveGoalState
    data object Loading : SaveGoalState
    data object Success : SaveGoalState
    data class Error(val message: String) : SaveGoalState
}

sealed interface ContributionState {
    data object Idle : ContributionState
    data object Loading : ContributionState
    data object Success : ContributionState
    data class Error(val message: String) : ContributionState
}

sealed interface DeleteGoalState {
    data object Idle : DeleteGoalState
    data object Loading : DeleteGoalState
    data object Success : DeleteGoalState
    data class Error(val message: String) : DeleteGoalState
}

data class GoalsUiState(
    val status: GoalsStatus = GoalsStatus.Loading,
    val goals: List<SavingsGoal> = emptyList(),
    val errorMessage: String? = null
) {
    val isLoading: Boolean get() = status is GoalsStatus.Loading
    val isSuccess: Boolean get() = status is GoalsStatus.Success
    val isEmpty: Boolean get() = status is GoalsStatus.Empty
    val isError: Boolean get() = status is GoalsStatus.Error

    val totalTarget: Long get() = goals.sumOf { it.targetAmount }
    val totalSaved: Long get() = goals.sumOf { it.savedAmount }
    val overallPercentage: Float
        get() = if (totalTarget > 0L) (totalSaved.toFloat() / totalTarget.toFloat()) else 0f
}

class GoalsViewModel(
    private val goalRepository: GoalRepository = FirestoreGoalRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveGoalState>(SaveGoalState.Idle)
    val saveState: StateFlow<SaveGoalState> = _saveState.asStateFlow()

    private val _contributionState = MutableStateFlow<ContributionState>(ContributionState.Idle)
    val contributionState: StateFlow<ContributionState> = _contributionState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteGoalState>(DeleteGoalState.Idle)
    val deleteState: StateFlow<DeleteGoalState> = _deleteState.asStateFlow()

    private var loadGoalsJob: Job? = null

    init {
        loadData()
    }

    fun loadData() {
        loadGoalsJob?.cancel()
        _uiState.update { it.copy(status = GoalsStatus.Loading, errorMessage = null) }

        loadGoalsJob = viewModelScope.launch {
            goalRepository.getGoals()
                .catch { e ->
                    val msg = e.message?.takeIf { it.isNotBlank() }
                        ?: "No se pudieron cargar los objetivos de ahorro."
                    _uiState.update {
                        it.copy(
                            status = GoalsStatus.Error(msg),
                            errorMessage = msg
                        )
                    }
                }
                .collect { goals ->
                    val status = if (goals.isEmpty()) GoalsStatus.Empty else GoalsStatus.Success
                    _uiState.update {
                        it.copy(
                            status = status,
                            goals = goals,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun getGoalById(id: String): Flow<SavingsGoal?> {
        return goalRepository.getGoalById(id)
    }

    fun getContributions(goalId: String): Flow<List<GoalContribution>> {
        return goalRepository.getContributions(goalId)
    }

    fun addGoal(
        name: String,
        targetAmount: Long,
        initialAmount: Long = 0L,
        targetDateString: String = ""
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _saveState.value = SaveGoalState.Error("El nombre del objetivo es obligatorio.")
            return
        }
        if (targetAmount <= 0L) {
            _saveState.value = SaveGoalState.Error("El monto objetivo debe ser mayor a $ 0.")
            return
        }
        if (initialAmount < 0L) {
            _saveState.value = SaveGoalState.Error("El monto inicial no puede ser negativo.")
            return
        }
        if (initialAmount > targetAmount) {
            _saveState.value = SaveGoalState.Error("El monto inicial no puede ser mayor al monto objetivo.")
            return
        }

        _saveState.value = SaveGoalState.Loading
        viewModelScope.launch {
            val newGoal = SavingsGoal(
                name = trimmedName,
                targetAmount = targetAmount,
                savedAmount = initialAmount,
                targetDateString = targetDateString.trim().ifBlank { "Sin fecha" },
                createdAt = System.currentTimeMillis()
            )
            val result = goalRepository.addGoal(newGoal, initialAmount)
            result.fold(
                onSuccess = {
                    _saveState.value = SaveGoalState.Success
                },
                onFailure = { e ->
                    _saveState.value = SaveGoalState.Error(
                        e.message?.takeIf { it.isNotBlank() } ?: "Error al guardar el objetivo."
                    )
                }
            )
        }
    }

    fun addContribution(goalId: String, amount: Long, note: String) {
        if (amount <= 0L) {
            _contributionState.value = ContributionState.Error("El monto del aporte debe ser mayor a $ 0.")
            return
        }

        _contributionState.value = ContributionState.Loading
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val contribution = GoalContribution(
                goalId = goalId,
                amount = amount,
                date = System.currentTimeMillis(),
                dateString = dateStr,
                note = note.trim().ifBlank { "Aporte a la meta" },
                createdAt = System.currentTimeMillis()
            )
            val result = goalRepository.addContribution(goalId, contribution)
            result.fold(
                onSuccess = {
                    _contributionState.value = ContributionState.Success
                },
                onFailure = { e ->
                    _contributionState.value = ContributionState.Error(
                        e.message?.takeIf { it.isNotBlank() } ?: "Error al registrar el aporte."
                    )
                }
            )
        }
    }

    fun deleteGoal(goalId: String) {
        _deleteState.value = DeleteGoalState.Loading
        viewModelScope.launch {
            val result = goalRepository.deleteGoal(goalId)
            result.fold(
                onSuccess = {
                    _deleteState.value = DeleteGoalState.Success
                },
                onFailure = { e ->
                    _deleteState.value = DeleteGoalState.Error(
                        e.message?.takeIf { it.isNotBlank() } ?: "Error al eliminar el objetivo."
                    )
                }
            )
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveGoalState.Idle
    }

    fun resetContributionState() {
        _contributionState.value = ContributionState.Idle
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteGoalState.Idle
    }
}
