package com.maidana.finanzasapp.ui.components

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val locale = Locale.forLanguageTag("es-AR")
    private val format: NumberFormat = NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 0
    }

    fun format(amount: Double): String {
        return try {
            format.format(amount)
        } catch (_: Exception) {
            "$ ${"%,.0f".format(amount)}"
        }
    }

    fun formatWithSign(amount: Double, isIncome: Boolean): String {
        val sign = if (isIncome) "+ " else "- "
        return sign + format(kotlin.math.abs(amount))
    }
}
