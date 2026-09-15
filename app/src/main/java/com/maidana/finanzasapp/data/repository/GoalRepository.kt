package com.maidana.finanzasapp.data.repository

import com.maidana.finanzasapp.data.model.GoalContribution
import com.maidana.finanzasapp.data.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getGoals(): Flow<List<SavingsGoal>>
    fun getGoalById(id: String): Flow<SavingsGoal?>
    fun getContributions(goalId: String): Flow<List<GoalContribution>>
    suspend fun addGoal(goal: SavingsGoal, initialAmount: Long = 0L): Result<String>
    suspend fun addContribution(goalId: String, contribution: GoalContribution): Result<Unit>
    suspend fun deleteGoal(id: String): Result<Unit>
}
