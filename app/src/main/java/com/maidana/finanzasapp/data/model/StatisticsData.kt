package com.maidana.finanzasapp.data.model

import androidx.compose.ui.graphics.Color

/**
 * Resumen financiero consolidado para un período específico.
 */
data class PeriodFinancialSummary(
    val income: Long = 0L,
    val expense: Long = 0L,
    val balance: Long = 0L,
    val savingsRate: Float = 0f,
    val savingsRateText: String = "0%"
)

/**
 * Punto de datos para la serie temporal de evolución mensual.
 * Se ordena cronológicamente (de más antiguo a más reciente).
 */
data class MonthlyEvolutionPoint(
    val month: String,
    val year: Int = 0,
    val monthIndex: Int = 0,
    val income: Long = 0L,
    val expense: Long = 0L,
    val monthlyBalance: Long = 0L,
    val balance: Long = 0L
)

/**
 * Distribución de gastos agregados por categoría.
 */
data class CategoryExpenseStat(
    val category: String,
    val amount: Long,
    val percentage: Float,
    val color: Color
)
