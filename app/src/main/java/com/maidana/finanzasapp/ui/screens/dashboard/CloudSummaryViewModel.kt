package com.maidana.finanzasapp.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.maidana.finanzasapp.data.repository.CloudSummary
import com.maidana.finanzasapp.data.repository.CloudSummaryRepository
import com.maidana.finanzasapp.data.repository.SummaryRepository
import com.maidana.finanzasapp.data.repository.SummaryUnavailableException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CloudSummaryState {
    data object Idle : CloudSummaryState
    data object Loading : CloudSummaryState
    data class Ready(val summary: CloudSummary) : CloudSummaryState
    data class Failed(val message: String) : CloudSummaryState
}

interface SummarySession {
    val uid: String?
    fun observe(listener: (String?) -> Unit): () -> Unit
}

class FirebaseSummarySession(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) : SummarySession {
    override val uid: String? get() = auth.currentUser?.uid
    override fun observe(listener: (String?) -> Unit): () -> Unit {
        val registration = FirebaseAuth.AuthStateListener { listener(it.currentUser?.uid) }
        auth.addAuthStateListener(registration)
        return { auth.removeAuthStateListener(registration) }
    }
}

class CloudSummaryViewModel(
    private val repository: SummaryRepository = CloudSummaryRepository(),
    private val session: SummarySession = FirebaseSummarySession()
) : ViewModel() {
    private val state = MutableStateFlow<CloudSummaryState>(CloudSummaryState.Idle)
    val uiState = state.asStateFlow()
    private var request: Job? = null
    private var uid = session.uid
    private val unsubscribe = session.observe { nextUid ->
        if (uid != nextUid) {
            uid = nextUid
            request?.cancel()
            state.value = CloudSummaryState.Idle
        }
    }

    fun load(month: String) {
        request?.cancel()
        request = viewModelScope.launch {
            state.value = CloudSummaryState.Loading
            try {
                state.value = CloudSummaryState.Ready(repository.fetch(month))
            } catch (error: CancellationException) {
                throw error
            } catch (error: SummaryUnavailableException) {
                state.value = CloudSummaryState.Failed(error.message ?: "No se pudo consultar el resumen.")
            } catch (_: Exception) {
                state.value = CloudSummaryState.Failed("No se pudo consultar el resumen. Revisá tu conexión o sesión e intentá nuevamente.")
            }
        }
    }

    fun reset() {
        request?.cancel()
        state.value = CloudSummaryState.Idle
    }

    override fun onCleared() {
        unsubscribe()
        super.onCleared()
    }
}
