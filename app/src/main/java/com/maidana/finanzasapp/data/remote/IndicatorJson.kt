package com.maidana.finanzasapp.data.remote

import com.maidana.finanzasapp.domain.DollarQuote
import com.maidana.finanzasapp.domain.MonthlyInflation
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

object IndicatorJson {
    fun quotes(json: String): List<DollarQuote> {
        val array = JSONArray(json)
        require(array.length() in 1..100) { "Respuesta de cotizaciones no válida." }
        val values = (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            require(row.getString("moneda") == "USD") { "Moneda no admitida." }
            val market = row.getString("casa").trim()
            val name = row.getString("nombre").trim()
            require(market.isNotEmpty() && name.isNotEmpty())
            DollarQuote(market, name, positive(row, "compra"), positive(row, "venta"),
                Instant.parse(row.getString("fechaActualizacion")))
        }
        require(values.map { it.market }.distinct().size == values.size) { "Mercados duplicados." }
        return values
    }

    fun inflation(json: String): List<MonthlyInflation> {
        val array = JSONArray(json)
        require(array.length() in 1..5000) { "Respuesta de inflación no válida." }
        val values = (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            val period = YearMonth.from(LocalDate.parse(row.getString("fecha")))
            val percent = row.get("valor").toString().toBigDecimal()
            require(percent > BigDecimal("-100") && percent.abs() < BigDecimal("100000"))
            MonthlyInflation(period, percent)
        }
        require(values.map { it.period }.distinct().size == values.size) { "Meses duplicados." }
        return values.sortedBy { it.period }
    }

    fun encodeQuotes(values: List<DollarQuote>): String = JSONArray().apply {
        values.forEach { quote -> put(JSONObject().apply {
            put("casa", quote.market); put("nombre", quote.name); put("moneda", "USD")
            put("compra", quote.buy?.toPlainString() ?: JSONObject.NULL)
            put("venta", quote.sell?.toPlainString() ?: JSONObject.NULL)
            put("fechaActualizacion", quote.updatedAt.toString())
        }) }
    }.toString()

    fun encodeInflation(values: List<MonthlyInflation>): String = JSONArray().apply {
        values.forEach { point -> put(JSONObject().apply {
            put("fecha", point.period.atEndOfMonth().toString()); put("valor", point.percent.toPlainString())
        }) }
    }.toString()

    private fun positive(row: JSONObject, field: String): BigDecimal? =
        if (row.isNull(field)) null else row.get(field).toString().toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
}
