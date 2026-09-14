package com.maidana.finanzasapp.ui.screens.goals

import androidx.lifecycle.ViewModel
import com.maidana.finanzasapp.data.mock.MockDataProvider
import com.maidana.finanzasapp.data.model.GoalContribution
import com.maidana.finanzasapp.data.model.SavingsGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GoalsUiState(
    val goals: List<SavingsGoal> = MockDataProvider.goals,
    val isLoading: Boolean = false
) {
    val totalTarget: Double get() = goals.sumOf { it.targetAmount }
    val totalSaved: Double get() = goals.sumOf { it.currentAmount }
    val overallPercentage: Float get() = if (totalTarget > 0) (totalSaved / totalTarget).toFloat() else 0f
}

class GoalsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    fun getGoalById(id: String): SavingsGoal? {
        return _uiState.value.goals.find { it.id == id }
            ?: _uiState.value.goals.firstOrNull() // Fallback para demostración
    }

    fun addGoal(
        title: String,
        targetAmount: Double,
        initialAmount: Double = 0.0,
        targetDateString: String = "Diciembre 2026"
    ) {
        val newId = "meta_${System.currentTimeMillis()}"
        val initialContributions = if (initialAmount > 0) {
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            listOf(
                GoalContribution(
                    id = "c_init",
                    goalId = newId,
                    amount = initialAmount,
                    date = dateStr,
                    note = "Aporte inicial"
                )
            )
        } else emptyList()

        val newGoal = SavingsGoal(
            id = newId,
            title = title,
            targetAmount = targetAmount,
            currentAmount = initialAmount,
            targetDateString = targetDateString,
            contributions = initialContributions
        )

        _uiState.update { current ->
            current.copy(goals = current.goals + newGoal)
        }
    }

    fun addContribution(goalId: String, amount: Double, note: String) {
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val newContribution = GoalContribution(
            id = "c_${System.currentTimeMillis()}",
            goalId = goalId,
            amount = amount,
            date = dateStr,
            note = note.ifBlank { "Aporte manual" }
        )

        _uiState.update { current ->
            val updatedGoals = current.goals.map { goal ->
                if (goal.id == goalId) {
                    goal.copy(
                        currentAmount = goal.currentAmount + amount,
                        contributions = listOf(newContribution) + goal.contributions
                    )
                } else goal
            }
            current.copy(goals = updatedGoals)
        }
    }
}
