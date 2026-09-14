package com.maidana.finanzasapp.ui.screens.statistics

import androidx.lifecycle.ViewModel
import com.maidana.finanzasapp.data.mock.CategoryExpenseStat
import com.maidana.finanzasapp.data.mock.MockDataProvider
import com.maidana.finanzasapp.data.mock.MonthlyEvolutionPoint
import com.maidana.finanzasapp.data.mock.PeriodFinancialSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class StatisticsUiState(
    val selectedPeriod: String = "1M",
    val periods: List<String> = listOf("1M", "3M", "6M", "Año"),
    val summary: PeriodFinancialSummary = MockDataProvider.getSummaryForPeriod("1M"),
    val categoryExpenses: List<CategoryExpenseStat> = MockDataProvider.categoryExpenses,
    val balanceEvolution: List<MonthlyEvolutionPoint> = MockDataProvider.balanceEvolution,
    val isLoading: Boolean = false
)

class StatisticsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    fun setPeriod(period: String) {
        _uiState.update { current ->
            current.copy(
                selectedPeriod = period,
                summary = MockDataProvider.getSummaryForPeriod(period)
            )
        }
    }
}
