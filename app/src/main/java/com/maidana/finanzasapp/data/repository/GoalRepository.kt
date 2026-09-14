package com.maidana.finanzasapp.data.repository

import com.maidana.finanzasapp.data.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getGoals(): Flow<List<SavingsGoal>>
    fun getGoalById(id: String): Flow<SavingsGoal?>
    suspend fun addGoal(goal: SavingsGoal): Result<Unit>
    suspend fun contributeToGoal(id: String, amount: Double): Result<Unit>
}
