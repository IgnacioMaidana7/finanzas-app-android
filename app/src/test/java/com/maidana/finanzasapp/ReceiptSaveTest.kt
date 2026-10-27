package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.data.repository.TransactionRepository
import com.maidana.finanzasapp.ui.screens.transactions.SaveTransactionState
import com.maidana.finanzasapp.ui.screens.transactions.TransactionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptSaveTest {
    private class RecordingRepository : TransactionRepository {
        val attempts = mutableListOf<Transaction>()
        var fail = false
        override fun getTransactions() = flowOf(emptyList<Transaction>())
        override suspend fun addTransaction(transaction: Transaction): Result<Unit> {
            attempts += transaction
            return if (fail) Result.failure(IllegalStateException("Sin conexión")) else Result.success(Unit)
        }
        override suspend fun deleteTransaction(id: String) = Result.success(Unit)
    }
    @Before fun before() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun after() { Dispatchers.resetMain() }
    private fun save(vm: TransactionsViewModel, amount: String = "1000", owner: String? = "A", date: String = "27/10/2026") {
        vm.saveTransaction("Comercio", amount, TransactionType.GASTO, "Alimentación", date, expectedOwnerUid = owner)
    }
    @Test fun sessionChangeRejectsReceiptBeforeAnyWrite() {
        val repository = RecordingRepository()
        val vm = TransactionsViewModel(repository) { "B" }
        save(vm)
        assertTrue(vm.saveState.value is SaveTransactionState.Error)
        assertTrue(repository.attempts.isEmpty())
    }
    @Test fun fractionalPesosAreNeverSilentlyTruncated() {
        val repository = RecordingRepository()
        val vm = TransactionsViewModel(repository) { "A" }
        save(vm, "1234.56")
        assertTrue(vm.saveState.value is SaveTransactionState.Error)
        assertTrue(repository.attempts.isEmpty())
    }
    @Test fun samePayloadRetryKeepsDocumentIdAfterAnUnknownWriteFailure() {
        val repository = RecordingRepository().apply { fail = true }
        val vm = TransactionsViewModel(repository) { "A" }
        save(vm); repository.fail = false; save(vm)
        assertEquals(2, repository.attempts.size)
        assertEquals(repository.attempts[0].id, repository.attempts[1].id)
        assertTrue(repository.attempts[0].id.isNotBlank())
        assertEquals("A", repository.attempts.last().expectedOwnerUid)
    }
    @Test fun invalidDateSuffixDoesNotPassValidation() {
        val repository = RecordingRepository()
        val vm = TransactionsViewModel(repository) { "A" }
        save(vm, date = "27/10/2026 basura")
        assertTrue(repository.attempts.isEmpty())
    }
    @Test fun repeatedConfirmAfterSuccessDoesNotCreateAnotherMovement() {
        val repository = RecordingRepository()
        val vm = TransactionsViewModel(repository) { "A" }
        save(vm); save(vm)
        assertEquals(1, repository.attempts.size)
    }
    @Test fun distinctMovementsCanStillBeSavedSequentially() {
        val repository = RecordingRepository()
        val vm = TransactionsViewModel(repository) { "A" }
        save(vm, "1000"); save(vm, "2000")
        assertEquals(2, repository.attempts.size)
        assertNotEquals(repository.attempts[0].id, repository.attempts[1].id)
    }
}
