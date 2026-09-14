package com.maidana.finanzasapp.data.model

data class SavingsGoal(
    val id: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val deadline: Long? = null
)
