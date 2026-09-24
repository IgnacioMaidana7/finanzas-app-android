package com.maidana.finanzasapp.data.model

import androidx.compose.ui.graphics.Color

/**
 * Filtro temporal para estadísticas financieras.
 */
enum class StatisticsPeriod(val displayName: String, val aliases: List<String>) {
    CURRENT_MONTH("Mes actual", listOf("1m", "1 mes", "este mes")),
    PREVIOUS_MONTH("Mes anterior", listOf("mes ant", "mes ant.", "anterior")),
    LAST_3_MONTHS("3 meses", listOf("3m", "3 meses", "ultimos 3 meses", "últimos 3 meses")),
    LAST_6_MONTHS("6 meses", listOf("6m", "6 meses", "ultimos 6 meses", "últimos 6 meses")),
    CURRENT_YEAR("Año actual", listOf("año", "ano", "12m", "1y", "1a", "este año", "este ano"));

    companion object {
        fun fromString(value: String): StatisticsPeriod {
            val clean = value.trim().lowercase()
            return entries.firstOrNull { period ->
                period.name.equals(clean, ignoreCase = true) ||
                period.displayName.equals(clean, ignoreCase = true) ||
                period.aliases.any { it.equals(clean, ignoreCase = true) }
            } ?: CURRENT_MONTH
        }
    }
}

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
    val balance: Long = 0L,
    val isDeficit: Boolean = expense > income
)

/**
 * Distribución de gastos agregados por categoría.
 */
data class CategoryExpenseStat(
    val category: String,
    val amount: Long,
    val percentage: Float,
    val color: Color,
    val isOthers: Boolean = false,
    val subcategoriesCount: Int = 1
)

/**
 * Distribución de fondos por cuenta / origen del dinero.
 */
data class AccountDistributionStat(
    val accountId: String,
    val name: String,
    val type: AccountType,
    val balance: Long,
    val percentage: Float,
    val color: Color
)

/**
 * Estado de consumo de presupuesto.
 */
enum class BudgetHealthStatus {
    NORMAL,    // < 80%
    WARNING,   // 80% .. 100%
    EXCEEDED   // > 100%
}

/**
 * Comparación de presupuesto vs gasto real para el período actual.
 */
data class BudgetPerformanceStat(
    val category: String,
    val categoryId: String = "",
    val limitAmount: Long = 0L,
    val spentAmount: Long = 0L,
    val percentage: Float = 0f,
    val status: BudgetHealthStatus = BudgetHealthStatus.NORMAL
)
