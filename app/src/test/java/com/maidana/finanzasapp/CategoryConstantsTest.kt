package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.model.CategoryConstants
import com.maidana.finanzasapp.ui.components.getCategoryIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryConstantsTest {

    @Test
    fun expenseCategories_areNotEmpty_andHaveNoDuplicates() {
        val categories = CategoryConstants.EXPENSE_CATEGORIES
        assertTrue(categories.isNotEmpty())
        assertEquals(categories.size, categories.toSet().size)
        assertTrue(categories.contains("Alimentación"))
        assertTrue(categories.contains("Vivienda"))
        assertTrue(categories.contains("Educación"))
        assertTrue(categories.contains("Tecnología"))
    }

    @Test
    fun incomeCategories_areNotEmpty_andHaveNoDuplicates() {
        val categories = CategoryConstants.INCOME_CATEGORIES
        assertTrue(categories.isNotEmpty())
        assertEquals(categories.size, categories.toSet().size)
        assertTrue(categories.contains("Salario"))
        assertTrue(categories.contains("Inversión"))
    }

    @Test
    fun getCategoryIcon_resolvesAllStandardCategories_withoutErrors() {
        val allCategories = CategoryConstants.EXPENSE_CATEGORIES + CategoryConstants.INCOME_CATEGORIES
        for (category in allCategories) {
            val iconInfo = getCategoryIcon(category)
            assertNotNull(iconInfo.icon)
            assertNotNull(iconInfo.color)
        }
    }
}
