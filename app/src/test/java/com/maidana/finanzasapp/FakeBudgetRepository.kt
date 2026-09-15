package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.model.Budget
import com.maidana.finanzasapp.data.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class FakeBudgetRepository(
    initialBudgets: List<Budget> = emptyList()
) : BudgetRepository {
    var shouldFailAdd = false
    var shouldFailDelete = false
    var shouldThrowOnGet = false

    val inMemoryBudgets = initialBudgets.toMutableList()
    private val _flow = MutableStateFlow<List<Budget>>(initialBudgets)

    override fun getBudgets(): Flow<List<Budget>> {
        if (shouldThrowOnGet) {
            return flow {
                throw Exception("Error simulado de conexión en presupuestos")
            }
        }
        return _flow
    }

    override suspend fun addBudget(budget: Budget): Result<Unit> {
        return if (shouldFailAdd) {
            Result.failure(Exception("Error al guardar presupuesto en Firestore"))
        } else {
            val budgetWithId = if (budget.id.isBlank()) {
                budget.copy(id = "bg_${System.currentTimeMillis()}")
            } else {
                budget
            }
            inMemoryBudgets.add(0, budgetWithId)
            _flow.value = inMemoryBudgets.toList()
            Result.success(Unit)
        }
    }

    override suspend fun deleteBudget(id: String): Result<Unit> {
        return if (shouldFailDelete) {
            Result.failure(Exception("Error al eliminar presupuesto en Firestore"))
        } else {
            inMemoryBudgets.removeAll { it.id == id }
            _flow.value = inMemoryBudgets.toList()
            Result.success(Unit)
        }
    }

    fun emitBudgets(budgets: List<Budget>) {
        inMemoryBudgets.clear()
        inMemoryBudgets.addAll(budgets)
        _flow.value = budgets
    }
}
