package com.maidana.finanzasapp.ui.components

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {
    private val argentineSymbols = DecimalFormatSymbols(Locale.forLanguageTag("es-AR")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    // Patrón uniforme para moneda argentina en pesos enteros: "$ 1.000"
    private val currencyFormat = DecimalFormat("$ #,##0", argentineSymbols)
    private val numberFormat = DecimalFormat("#,##0", argentineSymbols)

    fun format(amount: Long): String {
        return try {
            currencyFormat.format(amount)
        } catch (_: Exception) {
            "$ ${ThousandsSeparatorVisualTransformation.formatAmount(amount)}"
        }
    }

    fun format(amount: Double): String {
        return try {
            currencyFormat.format(kotlin.math.round(amount).toLong())
        } catch (_: Exception) {
            "$ ${ThousandsSeparatorVisualTransformation.formatAmount(kotlin.math.round(amount).toLong())}"
        }
    }

    fun formatWithoutSymbol(amount: Long): String {
        return try {
            numberFormat.format(amount)
        } catch (_: Exception) {
            ThousandsSeparatorVisualTransformation.formatAmount(amount)
        }
    }

    fun formatWithSign(amount: Long, isIncome: Boolean): String {
        val sign = if (isIncome) "+ " else "- "
        return sign + format(kotlin.math.abs(amount))
    }

    fun formatWithSign(amount: Double, isIncome: Boolean): String {
        val sign = if (isIncome) "+ " else "- "
        return sign + format(kotlin.math.abs(amount))
    }
}
