package com.maidana.finanzasapp.data.model

data class GoalContribution(
    val id: String = "",
    val goalId: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val note: String = ""
)

data class SavingsGoal(
    val id: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val deadline: Long? = null,
    val targetDateString: String = "",
    val contributions: List<GoalContribution> = emptyList()
)

