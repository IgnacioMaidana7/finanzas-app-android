package com.maidana.finanzasapp.data.model

enum class BudgetStatus {
    NORMAL,
    NEAR_LIMIT,
    EXCEEDED
}

data class Budget(
    val id: String = "",
    val category: String = "",
    val categoryId: String = "",
    val limitAmount: Long = 0L,
    val month: Int = 0,
    val year: Int = 0,
    val createdAt: Long = 0L,
    // Atributo en memoria calculado exclusivamente en tiempo de ejecución por el ViewModel:
    val spentAmount: Long = 0L
) {
    val remainingAmount: Long
        get() = limitAmount - spentAmount

    val percentage: Float
        get() = if (limitAmount > 0L) spentAmount.toFloat() / limitAmount.toFloat() else 0f

    val status: BudgetStatus
        get() = when {
            percentage > 1.0f -> BudgetStatus.EXCEEDED
            percentage >= 0.75f -> BudgetStatus.NEAR_LIMIT
            else -> BudgetStatus.NORMAL
        }
}
