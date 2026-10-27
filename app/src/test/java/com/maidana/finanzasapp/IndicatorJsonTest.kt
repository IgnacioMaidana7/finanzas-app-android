package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.remote.IndicatorJson
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.YearMonth

class IndicatorJsonTest {
    private val dollar = """[{"casa":"oficial","nombre":"Oficial","moneda":"USD","compra":null,"venta":1200.25,"fechaActualizacion":"2026-10-27T10:00:00Z","extra":true}]"""
    @Test fun dollarContractPreservesPrecisionAndOptionalBuy() {
        val quote = IndicatorJson.quotes(dollar).single()
        assertNull(quote.buy)
        assertEquals(BigDecimal("1200.25"), quote.sell)
        assertEquals(listOf(quote), IndicatorJson.quotes(IndicatorJson.encodeQuotes(listOf(quote))))
    }
    @Test fun unexpectedCurrencyAndBrokenDatesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { IndicatorJson.quotes(dollar.replace("USD", "EUR")) }
        assertThrows(Exception::class.java) { IndicatorJson.quotes(dollar.replace("2026-10-27T10:00:00Z", "ayer")) }
    }
    @Test fun inflationNormalizesSortsAndRoundTrips() {
        val series = IndicatorJson.inflation("""[{"fecha":"2026-03-31","valor":3},{"fecha":"2026-02-28","valor":2}]""")
        assertEquals(YearMonth.of(2026, 2), series.first().period)
        assertEquals(series, IndicatorJson.inflation(IndicatorJson.encodeInflation(series)))
    }
    @Test fun duplicatePeriodsAndInvalidRatesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { IndicatorJson.inflation("""[{"fecha":"2026-02-28","valor":2},{"fecha":"2026-02-01","valor":3}]""") }
        assertThrows(IllegalArgumentException::class.java) { IndicatorJson.inflation("""[{"fecha":"2026-02-28","valor":-100}]""") }
    }
}
