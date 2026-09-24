package com.maidana.finanzasapp

import androidx.compose.ui.text.AnnotatedString
import com.maidana.finanzasapp.ui.components.CurrencyUtils
import com.maidana.finanzasapp.ui.components.ThousandsSeparatorVisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Test

class ThousandsSeparatorVisualTransformationTest {

    private val transformation = ThousandsSeparatorVisualTransformation()

    @Test
    fun formatDigits_auditRequiredCases() {
        assertEquals("0", ThousandsSeparatorVisualTransformation.formatDigits("0"))
        assertEquals("1", ThousandsSeparatorVisualTransformation.formatDigits("1"))
        assertEquals("999", ThousandsSeparatorVisualTransformation.formatDigits("999"))
        assertEquals("1.000", ThousandsSeparatorVisualTransformation.formatDigits("1000"))
        assertEquals("10.000", ThousandsSeparatorVisualTransformation.formatDigits("10000"))
        assertEquals("999.999", ThousandsSeparatorVisualTransformation.formatDigits("999999"))
        assertEquals("1.000.000", ThousandsSeparatorVisualTransformation.formatDigits("1000000"))
        assertEquals("123.456.789", ThousandsSeparatorVisualTransformation.formatDigits("123456789"))
    }

    @Test
    fun formatAmount_auditRequiredCases() {
        assertEquals("0", ThousandsSeparatorVisualTransformation.formatAmount(0L))
        assertEquals("1", ThousandsSeparatorVisualTransformation.formatAmount(1L))
        assertEquals("999", ThousandsSeparatorVisualTransformation.formatAmount(999L))
        assertEquals("1.000", ThousandsSeparatorVisualTransformation.formatAmount(1000L))
        assertEquals("10.000", ThousandsSeparatorVisualTransformation.formatAmount(10000L))
        assertEquals("999.999", ThousandsSeparatorVisualTransformation.formatAmount(999999L))
        assertEquals("1.000.000", ThousandsSeparatorVisualTransformation.formatAmount(1000000L))
        assertEquals("123.456.789", ThousandsSeparatorVisualTransformation.formatAmount(123456789L))
    }

    @Test
    fun currencyUtils_auditRequiredCases() {
        assertEquals("$ 0", CurrencyUtils.format(0L))
        assertEquals("$ 1", CurrencyUtils.format(1L))
        assertEquals("$ 999", CurrencyUtils.format(999L))
        assertEquals("$ 1.000", CurrencyUtils.format(1000L))
        assertEquals("$ 10.000", CurrencyUtils.format(10000L))
        assertEquals("$ 999.999", CurrencyUtils.format(999999L))
        assertEquals("$ 1.000.000", CurrencyUtils.format(1000000L))
        assertEquals("$ 123.456.789", CurrencyUtils.format(123456789L))
    }

    @Test
    fun formatDigits_variousLengths_formattedCorrectly() {
        assertEquals("", ThousandsSeparatorVisualTransformation.formatDigits(""))
        assertEquals("5", ThousandsSeparatorVisualTransformation.formatDigits("5"))
        assertEquals("50", ThousandsSeparatorVisualTransformation.formatDigits("50"))
        assertEquals("500", ThousandsSeparatorVisualTransformation.formatDigits("500"))
        assertEquals("1.000", ThousandsSeparatorVisualTransformation.formatDigits("1000"))
        assertEquals("10.000", ThousandsSeparatorVisualTransformation.formatDigits("10000"))
        assertEquals("100.000", ThousandsSeparatorVisualTransformation.formatDigits("100000"))
        assertEquals("1.000.000", ThousandsSeparatorVisualTransformation.formatDigits("1000000"))
        assertEquals("12.500.000", ThousandsSeparatorVisualTransformation.formatDigits("12500000"))
    }

    @Test
    fun formatAmount_numericValues_formattedCorrectly() {
        assertEquals("0", ThousandsSeparatorVisualTransformation.formatAmount(0L))
        assertEquals("1.000", ThousandsSeparatorVisualTransformation.formatAmount(1000L))
        assertEquals("10.000", ThousandsSeparatorVisualTransformation.formatAmount(10000L))
        assertEquals("100.000", ThousandsSeparatorVisualTransformation.formatAmount(100000L))
        assertEquals("1.000.000", ThousandsSeparatorVisualTransformation.formatAmount(1000000L))
        assertEquals("12.500.000", ThousandsSeparatorVisualTransformation.formatAmount(12500000L))
        assertEquals("-1.500", ThousandsSeparatorVisualTransformation.formatAmount(-1500L))
    }

    @Test
    fun filter_emptyText_returnsEmptyWithIdentityMapping() {
        val result = transformation.filter(AnnotatedString(""))
        assertEquals("", result.text.text)
        assertEquals(0, result.offsetMapping.originalToTransformed(0))
        assertEquals(0, result.offsetMapping.transformedToOriginal(0))
    }

    @Test
    fun filter_fourDigits_offsetMappingCorrect() {
        // "1000" -> "1.000"
        val result = transformation.filter(AnnotatedString("1000"))
        assertEquals("1.000", result.text.text)

        val mapping = result.offsetMapping
        // originalToTransformed:
        assertEquals(0, mapping.originalToTransformed(0)) // before '1'
        assertEquals(2, mapping.originalToTransformed(1)) // after '1' -> after '.' in transformed
        assertEquals(3, mapping.originalToTransformed(2)) // after first '0'
        assertEquals(4, mapping.originalToTransformed(3)) // after second '0'
        assertEquals(5, mapping.originalToTransformed(4)) // after third '0' (end)

        // transformedToOriginal:
        assertEquals(0, mapping.transformedToOriginal(0)) // before '1'
        assertEquals(1, mapping.transformedToOriginal(1)) // between '1' and '.'
        assertEquals(1, mapping.transformedToOriginal(2)) // after '.'
        assertEquals(2, mapping.transformedToOriginal(3)) // after first '0'
        assertEquals(3, mapping.transformedToOriginal(4)) // after second '0'
        assertEquals(4, mapping.transformedToOriginal(5)) // after third '0' (end)
    }

    @Test
    fun filter_sevenDigits_offsetMappingCorrect() {
        // "1000000" -> "1.000.000"
        val result = transformation.filter(AnnotatedString("1000000"))
        assertEquals("1.000.000", result.text.text)

        val mapping = result.offsetMapping
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(2, mapping.originalToTransformed(1)) // after '1' -> after first '.'
        assertEquals(6, mapping.originalToTransformed(4)) // after '1000' -> after second '.'
        assertEquals(9, mapping.originalToTransformed(7)) // end

        assertEquals(0, mapping.transformedToOriginal(0))
        assertEquals(1, mapping.transformedToOriginal(1))
        assertEquals(1, mapping.transformedToOriginal(2))
        assertEquals(4, mapping.transformedToOriginal(5))
        assertEquals(4, mapping.transformedToOriginal(6))
        assertEquals(7, mapping.transformedToOriginal(9))
    }

    @Test
    fun currencyUtils_format_producesArgentineFormat() {
        assertEquals("$ 0", CurrencyUtils.format(0L))
        assertEquals("$ 1.000", CurrencyUtils.format(1000L))
        assertEquals("$ 10.000", CurrencyUtils.format(10000L))
        assertEquals("$ 100.000", CurrencyUtils.format(100000L))
        assertEquals("$ 1.000.000", CurrencyUtils.format(1000000L))
        assertEquals("$ 12.500.000", CurrencyUtils.format(12500000L))

        assertEquals("+ $ 1.500", CurrencyUtils.formatWithSign(1500L, isIncome = true))
        assertEquals("- $ 1.500", CurrencyUtils.formatWithSign(1500L, isIncome = false))
    }

    @Test
    fun filter_sequentialDeletion_doesNotThrow() {
        // Simular borrado secuencial de dígitos desde "1000" hasta vacío ""
        val steps = listOf("1000", "100", "10", "1", "")
        for (text in steps) {
            val result = transformation.filter(AnnotatedString(text))
            // Verificar que no arroja excepción al transformar y mapear todas las posiciones
            for (i in 0..text.length) {
                val trans = result.offsetMapping.originalToTransformed(i)
                val back = result.offsetMapping.transformedToOriginal(trans)
                assertEquals(i, back)
            }
        }
    }

    @Test
    fun filter_outOfBoundsOffsets_clampedSafely() {
        val result = transformation.filter(AnnotatedString("1000"))
        // Offset mapping con valores negativos o mayores que la longitud no debe arrojar excepción
        assertEquals(0, result.offsetMapping.originalToTransformed(-5))
        assertEquals(5, result.offsetMapping.originalToTransformed(100))
        assertEquals(0, result.offsetMapping.transformedToOriginal(-5))
        assertEquals(4, result.offsetMapping.transformedToOriginal(100))
    }
}

