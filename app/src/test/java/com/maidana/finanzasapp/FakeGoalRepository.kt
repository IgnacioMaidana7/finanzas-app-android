package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.model.GoalContribution
import com.maidana.finanzasapp.data.model.SavingsGoal
import com.maidana.finanzasapp.data.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FakeGoalRepository(
    initialGoals: List<SavingsGoal> = emptyList()
) : GoalRepository {

    var shouldFailAdd = false
    var shouldFailAddContribution = false
    var shouldFailDelete = false
    var shouldThrowOnGet = false

    val inMemoryGoals = initialGoals.toMutableList()
    val inMemoryContributions = mutableMapOf<String, MutableList<GoalContribution>>()

    private val _goalsFlow = MutableStateFlow<List<SavingsGoal>>(initialGoals)
    private val _contributionsFlows = mutableMapOf<String, MutableStateFlow<List<GoalContribution>>>()

    override fun getGoals(): Flow<List<SavingsGoal>> {
        if (shouldThrowOnGet) {
            return flow {
                throw Exception("Error simulado de conexión en objetivos")
            }
        }
        return _goalsFlow
    }

    override fun getGoalById(id: String): Flow<SavingsGoal?> {
        return _goalsFlow.map { goals -> goals.find { it.id == id } }
    }

    override fun getContributions(goalId: String): Flow<List<GoalContribution>> {
        val flow = _contributionsFlows.getOrPut(goalId) {
            MutableStateFlow(inMemoryContributions[goalId] ?: emptyList())
        }
        return flow
    }

    override suspend fun addGoal(goal: SavingsGoal, initialAmount: Long): Result<String> {
        return if (shouldFailAdd) {
            Result.failure(Exception("Error al guardar objetivo en Firestore"))
        } else {
            val goalId = if (goal.id.isBlank()) "goal_${System.currentTimeMillis()}" else goal.id
            val goalWithId = goal.copy(id = goalId, savedAmount = initialAmount)
            inMemoryGoals.add(0, goalWithId)

            if (initialAmount > 0L) {
                val initialContribution = GoalContribution(
                    id = "contrib_init_${System.currentTimeMillis()}",
                    goalId = goalId,
                    amount = initialAmount,
                    date = goal.createdAt,
                    dateString = "Inicial",
                    note = "Monto inicial acumulado",
                    createdAt = goal.createdAt
                )
                val list = inMemoryContributions.getOrPut(goalId) { mutableListOf() }
                list.add(0, initialContribution)
                _contributionsFlows[goalId]?.value = list.toList()
            }

            _goalsFlow.value = inMemoryGoals.toList()
            Result.success(goalId)
        }
    }

    override suspend fun addContribution(goalId: String, contribution: GoalContribution): Result<Unit> {
        return if (shouldFailAddContribution) {
            Result.failure(Exception("Error al registrar aporte en Firestore"))
        } else {
            val index = inMemoryGoals.indexOfFirst { it.id == goalId }
            if (index != -1) {
                val current = inMemoryGoals[index]
                inMemoryGoals[index] = current.copy(savedAmount = current.savedAmount + contribution.amount)
            }

            val contributionWithId = if (contribution.id.isBlank()) {
                contribution.copy(id = "contrib_${System.currentTimeMillis()}", goalId = goalId)
            } else {
                contribution.copy(goalId = goalId)
            }

            val list = inMemoryContributions.getOrPut(goalId) { mutableListOf() }
            list.add(0, contributionWithId)

            _goalsFlow.value = inMemoryGoals.toList()
            _contributionsFlows.getOrPut(goalId) { MutableStateFlow(emptyList()) }.value = list.toList()
            Result.success(Unit)
        }
    }

    override suspend fun deleteGoal(id: String): Result<Unit> {
        return if (shouldFailDelete) {
            Result.failure(Exception("Error al eliminar objetivo en Firestore"))
        } else {
            inMemoryGoals.removeAll { it.id == id }
            inMemoryContributions.remove(id)
            _goalsFlow.value = inMemoryGoals.toList()
            _contributionsFlows.remove(id)
            Result.success(Unit)
        }
    }

    fun emitGoals(goals: List<SavingsGoal>) {
        inMemoryGoals.clear()
        inMemoryGoals.addAll(goals)
        _goalsFlow.value = goals.toList()
    }
}
