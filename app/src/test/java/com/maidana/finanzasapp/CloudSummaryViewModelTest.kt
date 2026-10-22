package com.maidana.finanzasapp

import androidx.lifecycle.ViewModelStore
import com.maidana.finanzasapp.data.repository.CloudSummary
import com.maidana.finanzasapp.data.repository.SummaryRepository
import com.maidana.finanzasapp.data.repository.SummaryUnavailableException
import com.maidana.finanzasapp.ui.screens.dashboard.CloudSummaryState
import com.maidana.finanzasapp.ui.screens.dashboard.CloudSummaryViewModel
import com.maidana.finanzasapp.ui.screens.dashboard.SummarySession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CloudSummaryViewModelTest {
    private class Session : SummarySession {
        override var uid: String? = "synthetic-a"
        var listener: ((String?) -> Unit)? = null
        override fun observe(listener: (String?) -> Unit): () -> Unit {
            this.listener = listener
            return { this.listener = null }
        }
        fun switchTo(uid: String?) { this.uid = uid; listener?.invoke(uid) }
    }

    private val summary = CloudSummary("2026-10", 100, 30, 70, 2)

    @Test
    fun accountSwitchCancelsAndClearsInFlightPrivateResult() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val session = Session()
        val pending = CompletableDeferred<CloudSummary>()
        val vm = CloudSummaryViewModel(object : SummaryRepository {
            override suspend fun fetch(month: String) = pending.await()
        }, session)
        val store = ViewModelStore().apply { put("summary", vm) }
        try {
            vm.load("2026-10")
            advanceUntilIdle()
            assertEquals(CloudSummaryState.Loading, vm.uiState.value)
            session.switchTo("synthetic-b")
            pending.complete(summary)
            advanceUntilIdle()
            assertEquals(CloudSummaryState.Idle, vm.uiState.value)
        } finally { store.clear(); Dispatchers.resetMain() }
        assertNull(session.listener)
    }

    @Test
    fun resultIsClearedOnLogoutAndMonthChange() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val session = Session()
        val vm = CloudSummaryViewModel(object : SummaryRepository {
            override suspend fun fetch(month: String) = summary
        }, session)
        val store = ViewModelStore().apply { put("summary", vm) }
        try {
            vm.load("2026-10"); advanceUntilIdle()
            assertEquals(CloudSummaryState.Ready(summary), vm.uiState.value)
            vm.reset()
            assertEquals(CloudSummaryState.Idle, vm.uiState.value)
            vm.load("2026-10"); advanceUntilIdle()
            session.switchTo(null)
            assertEquals(CloudSummaryState.Idle, vm.uiState.value)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test
    fun safeErrorIsVisibleAndRetryCanSucceed() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var fail = true
        val vm = CloudSummaryViewModel(object : SummaryRepository {
            override suspend fun fetch(month: String): CloudSummary {
                if (fail) throw SummaryUnavailableException("Este mes tiene demasiados movimientos.")
                return summary
            }
        }, Session())
        val store = ViewModelStore().apply { put("summary", vm) }
        try {
            vm.load("2026-10"); advanceUntilIdle()
            assertEquals(CloudSummaryState.Failed("Este mes tiene demasiados movimientos."), vm.uiState.value)
            fail = false
            vm.load("2026-10"); advanceUntilIdle()
            assertEquals(CloudSummaryState.Ready(summary), vm.uiState.value)
        } finally { store.clear(); Dispatchers.resetMain() }
    }
}
