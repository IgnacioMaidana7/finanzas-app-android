package com.maidana.finanzasapp.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class DollarQuote(
    val market: String,
    val name: String,
    val buy: BigDecimal?,
    val sell: BigDecimal?,
    val updatedAt: Instant
)

data class MonthlyInflation(val period: YearMonth, val percent: BigDecimal)

data class IndicatorSnapshot<T>(
    val values: List<T>,
    val fetchedAt: Long,
    val stale: Boolean = false,
    val notice: String? = null
)

data class InflationComparison(val adjusted: BigDecimal, val accumulatedPercent: BigDecimal)

object FinancialCalculations {
    fun dollarsForPesos(pesos: BigDecimal, quote: DollarQuote): BigDecimal {
        require(pesos.signum() > 0) { "Ingresá un importe mayor que cero." }
        val rate = quote.sell
        require(rate != null && rate.signum() > 0) { "La cotización de venta no está disponible." }
        return pesos.divide(rate, 2, RoundingMode.HALF_UP)
    }

    fun adjustForInflation(
        amount: BigDecimal,
        base: YearMonth,
        destination: YearMonth,
        series: List<MonthlyInflation>
    ): InflationComparison {
        require(amount.signum() > 0) { "Ingresá un importe mayor que cero." }
        val months = ChronoUnit.MONTHS.between(base, destination)
        require(months in 0..1200) { "El mes destino debe ser igual o posterior al mes base." }
        require(series.map { it.period }.distinct().size == series.size) { "La serie contiene meses duplicados." }
        val byPeriod = series.associateBy { it.period }
        var factor = BigDecimal.ONE
        for (offset in 1..months) {
            val period = base.plusMonths(offset)
            val rate = byPeriod[period]?.percent
                ?: throw IllegalArgumentException("No hay inflación publicada para $period. No se realizó el cálculo.")
            require(rate > BigDecimal("-100")) { "La serie contiene una variación no válida." }
            factor = factor.multiply(BigDecimal.ONE.add(rate.movePointLeft(2)))
        }
        return InflationComparison(
            amount.multiply(factor).setScale(2, RoundingMode.HALF_UP),
            factor.subtract(BigDecimal.ONE).movePointRight(2).setScale(2, RoundingMode.HALF_UP)
        )
    }
}

/** Accepts local decimal notation without passing money through floating point. */
object DecimalAmounts {
    fun parse(input: String): BigDecimal? {
        val raw = input.trim().replace("$", "").replace(" ", "")
        if (!raw.matches(Regex("[0-9]+(?:[.,][0-9]+)*")) || raw.length > 24) return null
        val normalized = when {
            ',' in raw && '.' in raw -> {
                val decimal = if (raw.lastIndexOf(',') > raw.lastIndexOf('.')) ',' else '.'
                val grouping = if (decimal == ',') '.' else ','
                val pieces = raw.split(decimal)
                if (pieces.size != 2 || pieces[1].length !in 1..2) return null
                if (!validGrouping(pieces[0], grouping)) return null
                pieces[0].replace(grouping.toString(), "") + "." + pieces[1]
            }
            ',' in raw || '.' in raw -> {
                val separator = if (',' in raw) ',' else '.'
                val parts = raw.split(separator)
                when {
                    parts.size == 2 && parts[1].length in 1..2 -> parts.joinToString(".")
                    validGrouping(raw, separator) -> parts.joinToString("")
                    else -> return null
                }
            }
            else -> raw
        }
        return normalized.toBigDecimalOrNull()?.takeIf { it <= BigDecimal("999999999999999") }
    }

    private fun validGrouping(value: String, separator: Char): Boolean {
        val parts = value.split(separator)
        return parts.size == 1 || (parts[0].length in 1..3 && parts.drop(1).all { it.length == 3 })
    }
}
