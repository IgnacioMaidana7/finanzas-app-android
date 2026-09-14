package com.maidana.finanzasapp.ui.screens.goals

import androidx.lifecycle.ViewModel
import com.maidana.finanzasapp.data.model.SavingsGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GoalsUiState(
    val goals: List<SavingsGoal> = emptyList(),
    val selectedGoal: SavingsGoal? = null,
    val isLoading: Boolean = false
)

class GoalsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()
}
