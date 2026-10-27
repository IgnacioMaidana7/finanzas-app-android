package com.maidana.finanzasapp.ui.screens.indicators

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maidana.finanzasapp.data.local.PreferencesIndicatorCache
import com.maidana.finanzasapp.data.remote.HttpIndicatorSource
import com.maidana.finanzasapp.data.repository.CachedIndicatorRepository
import com.maidana.finanzasapp.data.repository.IndicatorRepository
import com.maidana.finanzasapp.domain.DollarQuote
import com.maidana.finanzasapp.domain.IndicatorSnapshot
import com.maidana.finanzasapp.domain.MonthlyInflation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface IndicatorLoad<out T> {
    data object Loading : IndicatorLoad<Nothing>
    data class Ready<T>(val snapshot: IndicatorSnapshot<T>) : IndicatorLoad<T>
    data class Failed(val message: String) : IndicatorLoad<Nothing>
}

data class IndicatorsUiState(
    val quotes: IndicatorLoad<DollarQuote> = IndicatorLoad.Loading,
    val inflation: IndicatorLoad<MonthlyInflation> = IndicatorLoad.Loading
)

class IndicatorsViewModel(private val repository: IndicatorRepository) : ViewModel() {
    private val _state = MutableStateFlow(IndicatorsUiState())
    val state = _state.asStateFlow()
    private var quotesJob: Job? = null
    private var inflationJob: Job? = null

    init { loadQuotes(); loadInflation() }

    fun loadQuotes() {
        if (quotesJob?.isActive == true) return
        quotesJob = viewModelScope.launch {
            _state.update { it.copy(quotes = IndicatorLoad.Loading) }
            try {
                val snapshot = repository.quotes()
                _state.update { it.copy(quotes = IndicatorLoad.Ready(snapshot)) }
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(quotes = IndicatorLoad.Failed(e.message ?: "No se pudieron consultar las cotizaciones.")) } }
        }
    }

    fun loadInflation() {
        if (inflationJob?.isActive == true) return
        inflationJob = viewModelScope.launch {
            _state.update { it.copy(inflation = IndicatorLoad.Loading) }
            try {
                val snapshot = repository.inflation()
                _state.update { it.copy(inflation = IndicatorLoad.Ready(snapshot)) }
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(inflation = IndicatorLoad.Failed(e.message ?: "No se pudo consultar la inflación.")) } }
        }
    }

    companion object {
        @Volatile private var sharedRepository: IndicatorRepository? = null
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = synchronized(IndicatorsViewModel::class.java) {
                    sharedRepository ?: CachedIndicatorRepository(HttpIndicatorSource(), PreferencesIndicatorCache(context.applicationContext))
                        .also { sharedRepository = it }
                }
                IndicatorsViewModel(repository)
            }
        }
    }
}
