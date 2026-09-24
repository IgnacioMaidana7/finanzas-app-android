package com.maidana.finanzasapp.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * VisualTransformation que formatea una cadena de dígitos agregando separadores
 * de miles (puntos) con formato argentino (ej. 1000 -> 1.000, 1000000 -> 1.000.000).
 *
 * Mantiene intacto el valor del estado interno (solo dígitos) y provee un
 * OffsetMapping bidireccional exacto para que el cursor no sufra desfasajes.
 */
class ThousandsSeparatorVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formatted = formatDigits(originalText)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clampedOffset = offset.coerceIn(0, originalText.length)
                val length = originalText.length
                val remaining = length - clampedOffset
                val totalDots = if (length > 0) (length - 1) / 3 else 0
                val dotsAfter = if (remaining > 0) (remaining - 1) / 3 else 0
                val dotsBefore = totalDots - dotsAfter
                return clampedOffset + dotsBefore
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clampedOffset = offset.coerceIn(0, formatted.length)
                var dotsBefore = 0
                for (i in 0 until clampedOffset) {
                    if (formatted[i] == '.') {
                        dotsBefore++
                    }
                }
                return (clampedOffset - dotsBefore).coerceIn(0, originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }

    companion object {
        /**
         * Formatea una cadena de dígitos insertando puntos de miles desde la derecha.
         * Si la cadena contiene caracteres no numéricos, solo procesa dígitos preservando el resto.
         */
        fun formatDigits(digits: String): String {
            if (digits.isEmpty()) return ""
            val cleanDigits = digits.filter { it.isDigit() }
            if (cleanDigits.isEmpty()) return digits

            val length = cleanDigits.length
            val sb = StringBuilder(length + (length / 3))
            for (i in 0 until length) {
                sb.append(cleanDigits[i])
                val remaining = length - 1 - i
                if (remaining > 0 && remaining % 3 == 0) {
                    sb.append('.')
                }
            }
            return sb.toString()
        }

        /**
         * Formatea un importe Long a representación numérica con separadores de miles argentinos (sin símbolo $).
         * Ej: 12500000 -> "12.500.000"
         */
        fun formatAmount(amount: Long): String {
            val isNegative = amount < 0
            val absStr = kotlin.math.abs(amount).toString()
            val formatted = formatDigits(absStr)
            return if (isNegative) "-$formatted" else formatted
        }
    }
}
