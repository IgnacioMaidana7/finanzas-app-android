package com.maidana.finanzasapp.data.repository

import com.maidana.finanzasapp.data.model.Category
import com.maidana.finanzasapp.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    /**
     * Emite la lista unificada de categorías (predefinidas del sistema + personalizadas del usuario).
     */
    fun getCategories(): Flow<List<Category>>

    /**
     * Crea una nueva categoría personalizada para el usuario actual.
     */
    suspend fun addCategory(name: String, type: TransactionType): Result<Category>

    /**
     * Actualiza el nombre de una categoría personalizada y sincroniza los movimientos y presupuestos asociados.
     */
    suspend fun updateCategory(id: String, newName: String, oldName: String = ""): Result<Unit>

    /**
     * Activa o desactiva (archiva) una categoría personalizada.
     */
    suspend fun setCategoryActive(id: String, isActive: Boolean): Result<Unit>
}
