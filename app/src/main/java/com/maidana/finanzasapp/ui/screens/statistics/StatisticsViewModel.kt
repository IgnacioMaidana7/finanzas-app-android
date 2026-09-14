package com.maidana.finanzasapp.ui.screens.statistics

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StatisticsUiState(
    val selectedTimeRange: String = "Mes Actual",
    val isLoading: Boolean = false
)

class StatisticsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()
}
