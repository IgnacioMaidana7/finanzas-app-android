package com.maidana.finanzasapp.domain

import java.math.BigDecimal
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

data class ReceiptDraft(
    val merchant: String?,
    val date: String?,
    val amount: BigDecimal?,
    val warnings: List<String>
)

/** Heuristics only: text recognition never authorizes a financial write. */
object ReceiptParser {
    private val number = Regex("(?<![\\p{L}\\d])\\d+(?:[.,]\\d+)*(?![\\p{L}\\d])")
    private val datePattern = Regex("\\b\\d{2}[/\\-]\\d{2}[/\\-]\\d{4}\\b")
    private val ignoredTotal = Regex("SUB\\s*TOTAL|TOTAL.*(?:IVA|IMPUEST|ARTICUL|PRODUCT|ITEM)|VUELTO|CAMBIO")
    private val total = Regex("\\bTOTAL\\b|IMPORTE\\s+A\\s+PAGAR")
    private val foreign = Regex("\\b(?:USD|EUR)\\b|U\\${'$'}S|US\\${'$'}")

    fun parse(text: String): ReceiptDraft {
        require(text.isNotBlank()) { "No se encontró texto. Probá otra foto o completá el gasto manualmente." }
        val lines = text.lines().map(String::trim).filter(String::isNotEmpty)
        val warnings = mutableListOf<String>()
        val candidates = mutableListOf<BigDecimal>()
        lines.forEachIndexed { i, line ->
            val normalized = normalized(line)
            val match = total.find(normalized)
            if (match != null && !ignoredTotal.containsMatchIn(normalized)) {
                val tail = normalized.substring(match.range.last + 1)
                val matches = number.findAll(tail).toList()
                val amounts = if (matches.isNotEmpty()) matches.mapNotNull { DecimalAmounts.parse(it.value) }
                else lines.getOrNull(i + 1)?.takeIf { it.matches(Regex("[\\d$.,\\s]+")) }?.let { listOfNotNull(DecimalAmounts.parse(it)) }.orEmpty()
                candidates += amounts.filter { it.signum() > 0 }
            }
        }
        val distinct = candidates.distinctBy { it.stripTrailingZeros() }
        val foreignCurrency = foreign.containsMatchIn(normalized(text))
        val amount = distinct.singleOrNull().takeUnless { foreignCurrency }
        when {
            foreignCurrency -> warnings += "Se detectó una moneda distinta de pesos. Ingresá manualmente un importe en ARS."
            distinct.size > 1 -> warnings += "Hay más de un total posible. Comprobá el importe del ticket."
            amount == null -> warnings += "No se pudo identificar un total único. Completá el importe."
        }
        val dates = datePattern.findAll(text).mapNotNull { match ->
            runCatching {
                LocalDate.parse(match.value.replace('-', '/'), DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT))
            }.getOrNull()
        }.distinct().toList()
        val date = dates.singleOrNull()?.format(DateTimeFormatter.ofPattern("dd/MM/uuuu"))
        if (date == null) warnings += "Revisá la fecha: no se identificó una fecha única válida."
        if (amount != null && amount.stripTrailingZeros().scale() > 0) warnings += "El ticket contiene centavos; la app registra pesos enteros."
        val merchant = lines.take(5).firstOrNull {
            it.length in 3..80 && it.any(Char::isLetter) &&
                !Regex("CUIT|CONSUMIDOR|TICKET|FACTURA|FECHA|\\bTOTAL\\b").containsMatchIn(normalized(it))
        }
        return ReceiptDraft(merchant, date, amount, warnings)
    }

    private fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").uppercase(Locale.ROOT)
}
