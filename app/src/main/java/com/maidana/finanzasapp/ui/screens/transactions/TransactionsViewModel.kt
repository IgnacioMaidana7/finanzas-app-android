package com.maidana.finanzasapp.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import com.maidana.finanzasapp.data.repository.FirestoreTransactionRepository
import com.maidana.finanzasapp.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToLong

enum class TransactionFilter {
    ALL,
    INCOME,
    EXPENSE
}

sealed interface TransactionsStatus {
    data object Loading : TransactionsStatus
    data object Success : TransactionsStatus
    data object Empty : TransactionsStatus
    data class Error(val message: String) : TransactionsStatus
}

sealed interface SaveTransactionState {
    data object Idle : SaveTransactionState
    data object Loading : SaveTransactionState
    data object Success : SaveTransactionState
    data class Error(val message: String) : SaveTransactionState
}

data class TransactionsUiState(
    val status: TransactionsStatus = TransactionsStatus.Loading,
    val transactions: List<Transaction> = emptyList(),
    val selectedFilter: TransactionFilter = TransactionFilter.ALL,
    val errorMessage: String? = null,
    val isDeleting: Boolean = false,
    val deleteError: String? = null
) {
    val isLoading: Boolean get() = status is TransactionsStatus.Loading
    val isEmpty: Boolean get() = status is TransactionsStatus.Empty
    val isError: Boolean get() = status is TransactionsStatus.Error

    val filteredTransactions: List<Transaction>
        get() = when (selectedFilter) {
            TransactionFilter.ALL -> transactions
            TransactionFilter.INCOME -> transactions.filter { it.type == TransactionType.INGRESO }
            TransactionFilter.EXPENSE -> transactions.filter { it.type == TransactionType.GASTO }
        }

    val totalIncome: Long
        get() = transactions.filter { it.type == TransactionType.INGRESO }.sumOf { it.amount }

    val totalExpense: Long
        get() = transactions.filter { it.type == TransactionType.GASTO }.sumOf { it.amount }
}

class TransactionsViewModel(
    private val repository: TransactionRepository = FirestoreTransactionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveTransactionState>(SaveTransactionState.Idle)
    val saveState: StateFlow<SaveTransactionState> = _saveState.asStateFlow()

    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        loadTransactions()
    }

    fun loadTransactions() {
        loadJob?.cancel()
        _uiState.update { it.copy(status = TransactionsStatus.Loading, errorMessage = null) }
        loadJob = viewModelScope.launch {
            repository.getTransactions()
                .catch { exception ->
                    val friendlyMsg = exception.message?.takeIf { it.isNotBlank() }
                        ?: "No se pudieron cargar los movimientos. Verifica tu conexión."
                    _uiState.update {
                        it.copy(
                            status = TransactionsStatus.Error(friendlyMsg),
                            errorMessage = friendlyMsg
                        )
                    }
                }
                .collect { list ->
                    val newStatus = if (list.isEmpty()) {
                        TransactionsStatus.Empty
                    } else {
                        TransactionsStatus.Success
                    }
                    _uiState.update {
                        it.copy(
                            status = newStatus,
                            transactions = list,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun setFilter(filter: TransactionFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun saveTransaction(
        title: String,
        amountText: String,
        type: TransactionType,
        category: String,
        dateString: String
    ) {
        if (_saveState.value is SaveTransactionState.Loading) return

        // 1. Validar Monto obligatorio y mayor que cero (en pesos enteros)
        val cleanAmount = amountText.trim()
        if (cleanAmount.isBlank()) {
            _saveState.value = SaveTransactionState.Error("El monto es obligatorio.")
            return
        }

        val rawAmount = cleanAmount.toLongOrNull()
            ?: cleanAmount.replace(",", ".").toDoubleOrNull()?.toLong()

        if (rawAmount == null || rawAmount <= 0L) {
            _saveState.value = SaveTransactionState.Error("El monto debe ser un número entero mayor a cero.")
            return
        }

        // 2. Validar Categoría
        if (category.trim().isBlank()) {
            _saveState.value = SaveTransactionState.Error("Debes seleccionar una categoría.")
            return
        }

        // 3. Validar Descripción
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) {
            _saveState.value = SaveTransactionState.Error("Ingresa una descripción o nota para el movimiento.")
            return
        }

        // 4. Validar Fecha
        val parsedDate = try {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                isLenient = false
            }
            format.parse(dateString.trim())?.time
        } catch (_: Exception) {
            null
        }

        if (parsedDate == null) {
            _saveState.value = SaveTransactionState.Error("La fecha no es válida. Usa el formato dd/mm/aaaa.")
            return
        }

        _saveState.value = SaveTransactionState.Loading

        viewModelScope.launch {
            val newTransaction = Transaction(
                id = "",
                title = cleanTitle,
                description = cleanTitle,
                amount = rawAmount,
                type = type,
                category = category.trim(),
                date = parsedDate,
                createdAt = System.currentTimeMillis()
            )

            val result = repository.addTransaction(newTransaction)
            result.fold(
                onSuccess = {
                    _saveState.value = SaveTransactionState.Success
                },
                onFailure = { error ->
                    val msg = error.message?.takeIf { it.isNotBlank() }
                        ?: "No se pudo guardar el movimiento. Inténtalo de nuevo."
                    _saveState.value = SaveTransactionState.Error(msg)
                }
            )
        }
    }

    fun deleteTransaction(transactionId: String) {
        if (transactionId.isBlank()) return

        _uiState.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            val result = repository.deleteTransaction(transactionId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isDeleting = false, deleteError = null) }
                },
                onFailure = { error ->
                    val msg = error.message?.takeIf { it.isNotBlank() }
                        ?: "No se pudo eliminar el movimiento. Inténtalo de nuevo."
                    _uiState.update { it.copy(isDeleting = false, deleteError = msg) }
                }
            )
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveTransactionState.Idle
    }

    fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = null) }
    }
}
