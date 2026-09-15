package com.maidana.finanzasapp.data.model

data class GoalContribution(
    val id: String = "",
    val goalId: String = "",
    val amount: Long = 0L,
    val date: Long = 0L,
    val dateString: String = "",
    val note: String = "",
    val createdAt: Long = 0L
)

data class SavingsGoal(
    val id: String = "",
    val name: String = "",
    val targetAmount: Long = 0L,
    val savedAmount: Long = 0L,
    val targetDate: Long = 0L,
    val targetDateString: String = "",
    val createdAt: Long = 0L,
    val contributions: List<GoalContribution> = emptyList()
) {
    // Propiedades de conveniencia y compatibilidad
    val title: String get() = name.ifBlank { "Meta de Ahorro" }
    val currentAmount: Long get() = savedAmount

    val progress: Float
        get() = if (targetAmount > 0L) (savedAmount.toFloat() / targetAmount.toFloat()) else 0f

    val remainingAmount: Long
        get() = (targetAmount - savedAmount).coerceAtLeast(0L)

    val isCompleted: Boolean
        get() = targetAmount > 0L && savedAmount >= targetAmount

    val isExceeded: Boolean
        get() = targetAmount > 0L && savedAmount > targetAmount
}

