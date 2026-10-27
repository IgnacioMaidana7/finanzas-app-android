package com.maidana.finanzasapp

import com.maidana.finanzasapp.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth

class FinancialCalculationsTest {
    private val quote = DollarQuote("oficial", "Oficial", BigDecimal("1100"), BigDecimal("1200"), Instant.EPOCH)
    private val base = YearMonth.of(2026, 1)
    private val points = listOf(MonthlyInflation(base.plusMonths(1), BigDecimal("2")), MonthlyInflation(base.plusMonths(2), BigDecimal("3")))

    @Test fun dollarEstimateUsesSellInsteadOfBuy() {
        assertEquals(BigDecimal("100.00"), FinancialCalculations.dollarsForPesos(BigDecimal("120000"), quote))
    }
    @Test fun missingSellNeverDividesByZero() {
        assertThrows(IllegalArgumentException::class.java) { FinancialCalculations.dollarsForPesos(BigDecimal.TEN, quote.copy(sell = null)) }
        assertThrows(IllegalArgumentException::class.java) { FinancialCalculations.dollarsForPesos(BigDecimal.TEN, quote.copy(sell = BigDecimal.ZERO)) }
    }
    @Test fun inflationCompoundsOnlyAfterBaseMonth() {
        val result = FinancialCalculations.adjustForInflation(BigDecimal("100000"), base, base.plusMonths(2), points.reversed())
        assertEquals(BigDecimal("105060.00"), result.adjusted)
        assertEquals(BigDecimal("5.06"), result.accumulatedPercent)
    }
    @Test fun missingMonthRejectsPartialCalculation() {
        assertThrows(IllegalArgumentException::class.java) { FinancialCalculations.adjustForInflation(BigDecimal.TEN, base, base.plusMonths(2), points.takeLast(1)) }
    }
    @Test fun duplicateMonthsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { FinancialCalculations.adjustForInflation(BigDecimal.TEN, base, base.plusMonths(1), points + points.first()) }
    }
    @Test fun sameMonthHasNoInflationAndBackwardsRangeIsInvalid() {
        assertEquals(BigDecimal("100.00"), FinancialCalculations.adjustForInflation(BigDecimal("100"), base, base, emptyList()).adjusted)
        assertThrows(IllegalArgumentException::class.java) { FinancialCalculations.adjustForInflation(BigDecimal.TEN, base, base.minusMonths(1), points) }
    }
    @Test fun deflationIsValidButMinusOneHundredIsNot() {
        val point = MonthlyInflation(base.plusMonths(1), BigDecimal("-2"))
        assertEquals(BigDecimal("98.00"), FinancialCalculations.adjustForInflation(BigDecimal("100"), base, point.period, listOf(point)).adjusted)
        assertThrows(IllegalArgumentException::class.java) { FinancialCalculations.adjustForInflation(BigDecimal.TEN, base, point.period, listOf(point.copy(percent = BigDecimal("-100")))) }
    }
    @Test fun decimalAmountsHandleLocalAndInternationalFormatsWithoutTruncation() {
        for (input in listOf("1.234,56", "1234.56", "1,234.56", "$ 1234,56")) assertEquals(BigDecimal("1234.56"), DecimalAmounts.parse(input))
        assertEquals(BigDecimal("1234"), DecimalAmounts.parse("1.234"))
        for (input in listOf("12.34.56", "NaN", "-1", "1,2,3", "1e10")) assertNull(DecimalAmounts.parse(input))
    }
}
