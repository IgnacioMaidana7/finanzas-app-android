package com.maidana.finanzasapp.data.model

enum class TransactionType {
    INGRESO,
    GASTO
}

data class Transaction(
    val id: String = "",
    val title: String = "",
    val description: String = title,
    val amount: Long = 0L,
    val type: TransactionType = TransactionType.GASTO,
    val category: String = "",
    val categoryId: String = "",
    val accountId: String = "",
    val accountName: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
