package com.maidana.finanzasapp.data.model

enum class TransactionType {
    INGRESO,
    GASTO
}

data class Transaction(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: TransactionType = TransactionType.GASTO,
    val category: String = "",
    val date: Long = System.currentTimeMillis()
)
