package com.maidana.finanzasapp

import com.maidana.finanzasapp.domain.ReceiptParser
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ReceiptParserTest {
    @Test fun selectsTotalInsteadOfSubtotalCashOrChangeAndPreservesCents() {
        val draft = ReceiptParser.parse("SUPERMERCADO CENTRO\n27/10/2026\nSUBTOTAL 1.500,00\nDESCUENTO 265,44\nTOTAL $ 1.234,56\nEFECTIVO 2.000,00\nVUELTO 765,44")
        assertEquals(BigDecimal("1234.56"), draft.amount)
        assertEquals("27/10/2026", draft.date)
        assertEquals("SUPERMERCADO CENTRO", draft.merchant)
        assertTrue(draft.warnings.any { it.contains("centavos") })
    }
    @Test fun ambiguousTotalsRequireManualSelection() {
        val draft = ReceiptParser.parse("COMERCIO\nTOTAL 1000,00\nTOTAL A PAGAR 900,00")
        assertNull(draft.amount); assertTrue(draft.warnings.any { it.contains("más de un total") })
    }
    @Test fun totalOnNextLineIsSupported() {
        assertEquals(BigDecimal("2500.00"), ReceiptParser.parse("COMERCIO\nTOTAL\n$ 2.500,00").amount)
    }
    @Test fun foreignCurrencyIsNeverImportedAsPesos() {
        for (currency in listOf("USD", "EUR", "U\$S", "US\$")) assertNull(ReceiptParser.parse("COMERCIO\nTOTAL $currency 500,00").amount)
    }
    @Test fun invalidOrAmbiguousDatesAreNotGuessed() {
        assertNull(ReceiptParser.parse("COMERCIO\n31/02/2026\nTOTAL 10").date)
        assertNull(ReceiptParser.parse("COMERCIO\n27/10/2026\n28/10/2026\nTOTAL 10").date)
    }
    @Test fun noTotalDoesNotChooseTheLargestNumberOrCuit() {
        assertNull(ReceiptParser.parse("COMERCIO\nCUIT 30123456789\nEFECTIVO 20000\nSUBTOTAL 10000").amount)
        assertNull(ReceiptParser.parse("COMERCIO\nTOTAL IVA 1000\nTOTAL PRODUCTOS 42").amount)
    }
    @Test fun emptyTextFailsWithManualFallback() {
        assertThrows(IllegalArgumentException::class.java) { ReceiptParser.parse(" \n ") }
    }
}
