package com.maidana.finanzasapp.ui.screens.transactions

import androidx.lifecycle.ViewModel
import com.maidana.finanzasapp.data.mock.MockDataProvider
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TransactionFilter {
    ALL,
    INCOME,
    EXPENSE
}

data class TransactionsUiState(
    val transactions: List<Transaction> = MockDataProvider.transactions,
    val selectedFilter: TransactionFilter = TransactionFilter.ALL,
    val isLoading: Boolean = false
) {
    val filteredTransactions: List<Transaction>
        get() = when (selectedFilter) {
            TransactionFilter.ALL -> transactions
            TransactionFilter.INCOME -> transactions.filter { it.type == TransactionType.INGRESO }
            TransactionFilter.EXPENSE -> transactions.filter { it.type == TransactionType.GASTO }
        }

    val totalIncome: Double
        get() = transactions.filter { it.type == TransactionType.INGRESO }.sumOf { it.amount }

    val totalExpense: Double
        get() = transactions.filter { it.type == TransactionType.GASTO }.sumOf { it.amount }
}

class TransactionsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    fun setFilter(filter: TransactionFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        date: Long = System.currentTimeMillis()
    ) {
        val newTx = Transaction(
            id = "tx_${System.currentTimeMillis()}",
            title = title,
            amount = amount,
            type = type,
            category = category,
            date = date
        )
        _uiState.update { current ->
            current.copy(transactions = listOf(newTx) + current.transactions)
        }
    }
}
