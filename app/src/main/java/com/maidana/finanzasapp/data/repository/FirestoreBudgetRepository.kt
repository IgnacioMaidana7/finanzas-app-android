package com.maidana.finanzasapp.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.maidana.finanzasapp.data.model.Budget
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreBudgetRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : BudgetRepository {

    override fun getBudgets(): Flow<List<Budget>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close(IllegalStateException("No hay una sesión de usuario activa."))
            return@callbackFlow
        }

        val query = firestore.collection("users")
            .document(uid)
            .collection("budgets")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(mapFirestoreException(error, "cargar los presupuestos"))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val budgets = snapshot.documents.mapNotNull { doc ->
                    doc.toBudget()
                }
                trySend(budgets)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun addBudget(budget: Budget): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            // Importante: NO se guardan spentAmount, percentage ni remainingAmount en Firestore
            val data = hashMapOf(
                "category" to budget.category,
                "limitAmount" to budget.limitAmount,
                "month" to budget.month,
                "year" to budget.year,
                "createdAt" to if (budget.createdAt > 0L) budget.createdAt else System.currentTimeMillis()
            )

            val collection = firestore.collection("users")
                .document(uid)
                .collection("budgets")

            if (budget.id.isNotBlank()) {
                collection.document(budget.id).set(data).await()
            } else {
                collection.add(data).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "guardar el presupuesto"))
        }
    }

    override suspend fun deleteBudget(id: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            firestore.collection("users")
                .document(uid)
                .collection("budgets")
                .document(id)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "eliminar el presupuesto"))
        }
    }

    private fun DocumentSnapshot.toBudget(): Budget? {
        return try {
            val category = getString("category").orEmpty()
            val limitNumber = get("limitAmount") as? Number
            val limitAmount = limitNumber?.toLong() ?: 0L
            val month = (get("month") as? Number)?.toInt() ?: 0
            val year = (get("year") as? Number)?.toInt() ?: 0
            val createdAt = getLong("createdAt") ?: (get("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis()

            Budget(
                id = id,
                category = category,
                limitAmount = limitAmount,
                month = month,
                year = year,
                createdAt = createdAt,
                spentAmount = 0L
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
                        "No tienes permisos suficientes para $action."
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        "El servicio no está disponible temporalmente o no hay conexión a internet."
                    FirebaseFirestoreException.Code.NOT_FOUND ->
                        "El recurso solicitado no fue encontrado."
                    else ->
                        "Error al $action (${e.code.name.lowercase()}). Inténtalo de nuevo."
                }
            }
            is FirebaseNetworkException ->
                "Error de conexión. Verifica tu conexión a internet e inténtalo nuevamente."
            else ->
                e.message?.takeIf { it.isNotBlank() && !it.contains("Exception") }
                    ?: "Ocurrió un error inesperado al $action. Inténtalo de nuevo."
        }
        return Exception(userMessage, e)
    }
}
