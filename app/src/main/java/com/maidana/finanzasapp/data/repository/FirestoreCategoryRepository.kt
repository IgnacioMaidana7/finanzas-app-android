package com.maidana.finanzasapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.maidana.finanzasapp.data.model.Category
import com.maidana.finanzasapp.data.model.CategoryConstants
import com.maidana.finanzasapp.data.model.TransactionType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreCategoryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : CategoryRepository {

    override fun getCategories(): Flow<List<Category>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close(IllegalStateException("No hay una sesión de usuario activa."))
            return@callbackFlow
        }

        val query = firestore.collection("users")
            .document(uid)
            .collection("categories")
            .orderBy("createdAt", Query.Direction.ASCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(mapFirestoreException(error, "cargar las categorías"))
                return@addSnapshotListener
            }

            val customCategories = snapshot?.documents?.mapNotNull { doc ->
                doc.toCategory()
            }.orEmpty()

            val all = CategoryConstants.ALL_PREDEFINED_CATEGORIES + customCategories
            trySend(all)
        }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun addCategory(name: String, type: TransactionType): Result<Category> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            val cleanName = name.trim()
            if (cleanName.isBlank()) {
                return Result.failure(IllegalArgumentException("El nombre de la categoría es obligatorio."))
            }

            // Validar que no duplique categorías predefinidas del mismo tipo
            val duplicatePredefined = CategoryConstants.ALL_PREDEFINED_CATEGORIES.any {
                it.type == type && it.name.equals(cleanName, ignoreCase = true)
            }
            if (duplicatePredefined) {
                return Result.failure(IllegalArgumentException("Ya existe una categoría predefinida con el nombre '$cleanName'."))
            }

            // Validar que no duplique categorías personalizadas activas del usuario para este tipo
            val existingSnapshot = firestore.collection("users")
                .document(uid)
                .collection("categories")
                .whereEqualTo("type", type.name)
                .get()
                .await()

            val duplicateCustom = existingSnapshot.documents.any { doc ->
                val docName = doc.getString("name").orEmpty().trim()
                val docActive = doc.getBoolean("isActive") ?: true
                docActive && docName.equals(cleanName, ignoreCase = true)
            }
            if (duplicateCustom) {
                return Result.failure(IllegalArgumentException("Ya tienes una categoría activa llamada '$cleanName'."))
            }

            val now = System.currentTimeMillis()
            val data = hashMapOf(
                "name" to cleanName,
                "type" to type.name,
                "isPredefined" to false,
                "isActive" to true,
                "createdAt" to now
            )

            val docRef = firestore.collection("users")
                .document(uid)
                .collection("categories")
                .add(data)
                .await()

            val category = Category(
                id = docRef.id,
                name = cleanName,
                type = type,
                isPredefined = false,
                isActive = true,
                createdAt = now
            )

            Result.success(category)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "crear la categoría"))
        }
    }

    override suspend fun updateCategory(id: String, newName: String, oldName: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            if (id.startsWith("predefined_")) {
                return Result.failure(IllegalArgumentException("No se pueden modificar las categorías predefinidas del sistema."))
            }

            val cleanNewName = newName.trim()
            if (cleanNewName.isBlank()) {
                return Result.failure(IllegalArgumentException("El nombre de la categoría es obligatorio."))
            }

            val docRef = firestore.collection("users")
                .document(uid)
                .collection("categories")
                .document(id)

            val currentDoc = docRef.get().await()
            if (!currentDoc.exists()) {
                return Result.failure(IllegalArgumentException("La categoría no existe."))
            }

            val currentTypeStr = currentDoc.getString("type") ?: TransactionType.GASTO.name
            val currentType = try {
                TransactionType.valueOf(currentTypeStr)
            } catch (_: Exception) {
                TransactionType.GASTO
            }

            // Validar que no duplique predefinidas
            val duplicatePredefined = CategoryConstants.ALL_PREDEFINED_CATEGORIES.any {
                it.type == currentType && it.name.equals(cleanNewName, ignoreCase = true)
            }
            if (duplicatePredefined) {
                return Result.failure(IllegalArgumentException("Ya existe una categoría predefinida con el nombre '$cleanNewName'."))
            }

            // Validar duplicados con otras categorías personalizadas
            val existingSnapshot = firestore.collection("users")
                .document(uid)
                .collection("categories")
                .whereEqualTo("type", currentType.name)
                .get()
                .await()

            val duplicateCustom = existingSnapshot.documents.any { doc ->
                doc.id != id &&
                (doc.getBoolean("isActive") ?: true) &&
                doc.getString("name").orEmpty().trim().equals(cleanNewName, ignoreCase = true)
            }
            if (duplicateCustom) {
                return Result.failure(IllegalArgumentException("Ya tienes otra categoría activa con el nombre '$cleanNewName'."))
            }

            val previousName = oldName.ifBlank { currentDoc.getString("name").orEmpty() }.trim()

            // 1. Actualizar el documento de la categoría
            docRef.update("name", cleanNewName).await()

            // 2. Sincronizar el nombre en movimientos históricos y presupuestos asociados
            if (previousName.isNotBlank() && !previousName.equals(cleanNewName, ignoreCase = true)) {
                syncCategoryNameInTransactionsAndBudgets(uid, id, previousName, cleanNewName)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "actualizar la categoría"))
        }
    }

    override suspend fun setCategoryActive(id: String, isActive: Boolean): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            if (id.startsWith("predefined_")) {
                return Result.failure(IllegalArgumentException("No se pueden archivar las categorías predefinidas del sistema."))
            }

            firestore.collection("users")
                .document(uid)
                .collection("categories")
                .document(id)
                .update("isActive", isActive)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            val action = if (isActive) "reactivar la categoría" else "archivar la categoría"
            Result.failure(mapFirestoreException(e, action))
        }
    }

    private suspend fun syncCategoryNameInTransactionsAndBudgets(
        uid: String,
        categoryId: String,
        oldName: String,
        newName: String
    ) {
        try {
            // Actualizar transacciones que coincidan por categoryId o por nombre previo
            val txCollection = firestore.collection("users").document(uid).collection("transactions")
            val txByCatId = txCollection.whereEqualTo("categoryId", categoryId).get().await()
            val txByName = if (oldName.isNotBlank()) {
                txCollection.whereEqualTo("category", oldName).get().await().documents
            } else {
                emptyList()
            }

            val allTxDocs = (txByCatId.documents + txByName).distinctBy { it.id }
            if (allTxDocs.isNotEmpty()) {
                allTxDocs.chunked(500).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { doc ->
                        batch.update(doc.reference, mapOf("category" to newName, "categoryId" to categoryId))
                    }
                    batch.commit().await()
                }
            }

            // Actualizar presupuestos que coincidan por categoryId o por nombre previo
            val budgetCollection = firestore.collection("users").document(uid).collection("budgets")
            val bByCatId = budgetCollection.whereEqualTo("categoryId", categoryId).get().await()
            val bByName = if (oldName.isNotBlank()) {
                budgetCollection.whereEqualTo("category", oldName).get().await().documents
            } else {
                emptyList()
            }

            val allBudgetDocs = (bByCatId.documents + bByName).distinctBy { it.id }
            if (allBudgetDocs.isNotEmpty()) {
                allBudgetDocs.chunked(500).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { doc ->
                        batch.update(doc.reference, mapOf("category" to newName, "categoryId" to categoryId))
                    }
                    batch.commit().await()
                }
            }
        } catch (_: Exception) {
            // No bloqueante: la actualización principal de la categoría ya fue confirmada
        }
    }

    private fun DocumentSnapshot.toCategory(): Category? {
        return try {
            val name = getString("name").orEmpty()
            if (name.isBlank()) return null

            val typeStr = getString("type") ?: TransactionType.GASTO.name
            val type = try {
                TransactionType.valueOf(typeStr)
            } catch (_: Exception) {
                TransactionType.GASTO
            }

            val isActive = getBoolean("isActive") ?: true
            val isPredefined = getBoolean("isPredefined") ?: false
            val createdAt = getLong("createdAt") ?: System.currentTimeMillis()

            Category(
                id = id,
                name = name,
                type = type,
                isPredefined = isPredefined,
                isActive = isActive,
                createdAt = createdAt
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun mapFirestoreException(e: Exception, action: String): Exception {
        val userMessage = when (e) {
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        "No tienes permisos para $action. Verifica tu sesión."
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        "El servicio de base de datos no está disponible. Revisa tu conexión."
                    else ->
                        "Error al $action (${e.code.name})."
                }
            }
            is IllegalArgumentException -> e.message ?: "Datos de categoría inválidos."
            is IllegalStateException -> e.message ?: "Estado inválido."
            else -> "Ocurrió un error inesperado al $action."
        }
        return Exception(userMessage, e)
    }
}
