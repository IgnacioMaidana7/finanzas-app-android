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

    val PREDEFINED_EXPENSE_CATEGORIES: List<Category> = EXPENSE_CATEGORIES.map { name ->
        Category(
            id = "predefined_gasto_${name.lowercase().replace(" ", "_")}",
            name = name,
            type = TransactionType.GASTO,
            isPredefined = true,
            isActive = true
        )
    }

    val PREDEFINED_INCOME_CATEGORIES: List<Category> = INCOME_CATEGORIES.map { name ->
        Category(
            id = "predefined_ingreso_${name.lowercase().replace(" ", "_")}",
            name = name,
            type = TransactionType.INGRESO,
            isPredefined = true,
            isActive = true
        )
    }

    val ALL_PREDEFINED_CATEGORIES: List<Category> =
        PREDEFINED_EXPENSE_CATEGORIES + PREDEFINED_INCOME_CATEGORIES
}
