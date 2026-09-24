package com.maidana.finanzasapp.data.model

enum class AccountType {
    CASH,
    DIGITAL
}

data class Account(
    val id: String = "",
    val name: String = "",
    val type: AccountType = AccountType.CASH,
    val initialBalance: Long = 0L,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_CASH_ID = "account_default_cash"
        const val DEFAULT_DIGITAL_ID = "account_default_digital"

        val DEFAULT_CASH_ACCOUNT = Account(
            id = DEFAULT_CASH_ID,
            name = "Efectivo",
            type = AccountType.CASH,
            initialBalance = 0L,
            isActive = true
        )

        val DEFAULT_DIGITAL_ACCOUNT = Account(
            id = DEFAULT_DIGITAL_ID,
            name = "Billetera / Transferencia",
            type = AccountType.DIGITAL,
            initialBalance = 0L,
            isActive = true
        )

        val DEFAULT_ACCOUNTS = listOf(DEFAULT_CASH_ACCOUNT, DEFAULT_DIGITAL_ACCOUNT)
    }
}
