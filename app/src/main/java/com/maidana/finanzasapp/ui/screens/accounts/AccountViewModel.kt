package com.maidana.finanzasapp.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidana.finanzasapp.data.model.Account
import com.maidana.finanzasapp.data.model.AccountType
import com.maidana.finanzasapp.data.repository.AccountRepository
import com.maidana.finanzasapp.data.repository.FirestoreAccountRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AccountActionState {
    data object Idle : AccountActionState
    data object Loading : AccountActionState
    data class Success(val message: String) : AccountActionState
    data class Error(val message: String) : AccountActionState
}

data class AccountUiState(
    val accounts: List<Account> = Account.DEFAULT_ACCOUNTS,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionState: AccountActionState = AccountActionState.Idle
) {
    fun cashAccount(): Account? = accounts.firstOrNull { it.type == AccountType.CASH && it.isActive }
    fun digitalAccount(): Account? = accounts.firstOrNull { it.type == AccountType.DIGITAL && it.isActive }
    fun activeAccounts(): List<Account> = accounts.filter { it.isActive }
}

class AccountViewModel(
    private val repository: AccountRepository = FirestoreAccountRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        loadJob = viewModelScope.launch {
            repository.getAccounts()
                .catch { error ->
                    val msg = error.message?.takeIf { it.isNotBlank() }
                        ?: "No se pudieron cargar las cuentas."
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = msg,
                            accounts = Account.DEFAULT_ACCOUNTS
                        )
                    }
                }
                .collect { list ->
                    _uiState.update {
                        it.copy(
                            accounts = list.ifEmpty { Account.DEFAULT_ACCOUNTS },
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun updateInitialBalances(
        cashInitial: Long,
        digitalInitial: Long,
        onSuccess: () -> Unit = {}
    ) {
        if (cashInitial < 0L || digitalInitial < 0L) {
            _uiState.update {
                it.copy(actionState = AccountActionState.Error("El saldo inicial no puede ser negativo."))
            }
            return
        }

        _uiState.update { it.copy(actionState = AccountActionState.Loading) }

        viewModelScope.launch {
            val cashAcc = uiState.value.cashAccount() ?: Account.DEFAULT_CASH_ACCOUNT
            val digitalAcc = uiState.value.digitalAccount() ?: Account.DEFAULT_DIGITAL_ACCOUNT

            val balancesMap = mapOf(
                cashAcc.id to cashInitial,
                digitalAcc.id to digitalInitial
            )

            val result = repository.updateInitialBalances(balancesMap)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(actionState = AccountActionState.Success("Saldos iniciales actualizados correctamente."))
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    val msg = error.message?.takeIf { it.isNotBlank() }
                        ?: "No se pudieron actualizar los saldos iniciales."
                    _uiState.update { it.copy(actionState = AccountActionState.Error(msg)) }
                }
            )
        }
    }

    fun resetActionState() {
        _uiState.update { it.copy(actionState = AccountActionState.Idle) }
    }
}
