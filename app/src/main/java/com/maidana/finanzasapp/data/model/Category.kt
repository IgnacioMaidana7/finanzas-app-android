package com.maidana.finanzasapp.data.model

/**
 * Representa una categoría para movimientos (ingresos y gastos) y presupuestos.
 *
 * Puede ser predefinida del sistema (isPredefined = true) o personalizada
 * por el usuario (isPredefined = false).
 */
data class Category(
    val id: String = "",
    val name: String = "",
    val type: TransactionType = TransactionType.GASTO,
    val isPredefined: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
