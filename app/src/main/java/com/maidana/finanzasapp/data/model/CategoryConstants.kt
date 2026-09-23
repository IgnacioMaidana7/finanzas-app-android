package com.maidana.finanzasapp.data.model

/**
 * Categorías estándar centralizadas para movimientos y presupuestos.
 * Garantiza consistencia total entre formularios, filtros, presupuestos y estadísticas.
 */
object CategoryConstants {

    val EXPENSE_CATEGORIES: List<String> = listOf(
        "Alimentación",
        "Vivienda",
        "Servicios",
        "Transporte",
        "Salud",
        "Educación",
        "Ocio",
        "Tecnología",
        "Otros"
    )

    val INCOME_CATEGORIES: List<String> = listOf(
        "Salario",
        "Inversión",
        "Ingresos Extra",
        "Otros"
    )
}
