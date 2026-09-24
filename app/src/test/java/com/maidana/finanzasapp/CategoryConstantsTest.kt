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

    @Test
    fun predefinedCategories_haveValidIdsAndProperties() {
        val expenseCats = CategoryConstants.PREDEFINED_EXPENSE_CATEGORIES
        assertEquals(CategoryConstants.EXPENSE_CATEGORIES.size, expenseCats.size)
        assertTrue(expenseCats.all { it.isPredefined && it.isActive && it.id.startsWith("predefined_gasto_") })

        val incomeCats = CategoryConstants.PREDEFINED_INCOME_CATEGORIES
        assertEquals(CategoryConstants.INCOME_CATEGORIES.size, incomeCats.size)
        assertTrue(incomeCats.all { it.isPredefined && it.isActive && it.id.startsWith("predefined_ingreso_") })

        val all = CategoryConstants.ALL_PREDEFINED_CATEGORIES
        assertEquals(expenseCats.size + incomeCats.size, all.size)
        // All IDs must be distinct
        assertEquals(all.size, all.map { it.id }.toSet().size)
    }
}
