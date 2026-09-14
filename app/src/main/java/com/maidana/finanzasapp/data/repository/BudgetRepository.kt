package com.maidana.finanzasapp.data.repository

import com.maidana.finanzasapp.data.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgets(month: String): Flow<List<Budget>>
    suspend fun addBudget(budget: Budget): Result<Unit>
    suspend fun updateBudget(budget: Budget): Result<Unit>
}
