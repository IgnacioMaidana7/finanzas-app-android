package com.maidana.finanzasapp.data.repository

import com.maidana.finanzasapp.data.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccounts(): Flow<List<Account>>
    suspend fun getAccount(id: String): Result<Account?>
    suspend fun updateInitialBalance(id: String, initialBalance: Long): Result<Unit>
    suspend fun updateInitialBalances(balances: Map<String, Long>): Result<Unit>
    suspend fun ensureDefaultAccounts(uid: String): Result<Unit>
    suspend fun addAccount(account: Account): Result<Account>
    suspend fun updateAccount(account: Account): Result<Unit>
}
