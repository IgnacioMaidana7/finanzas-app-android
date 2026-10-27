package com.maidana.finanzasapp.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.maidana.finanzasapp.data.model.Transaction
import com.maidana.finanzasapp.data.model.TransactionType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreTransactionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : TransactionRepository {

    override fun getTransactions(): Flow<List<Transaction>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close(IllegalStateException("No hay una sesión de usuario activa."))
            return@callbackFlow
        }

        val query = firestore.collection("users")
            .document(uid)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(mapFirestoreException(error, "cargar los movimientos"))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val transactions = snapshot.documents.mapNotNull { doc ->
                    doc.toTransaction()
                }
                trySend(transactions)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun addTransaction(transaction: Transaction): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            if (transaction.expectedOwnerUid != null && transaction.expectedOwnerUid != uid) {
                return Result.failure(IllegalStateException("La sesión cambió. Volvé a revisar el gasto."))
            }

            val desc = transaction.description.ifBlank { transaction.title }
            val data = hashMapOf(
                "type" to transaction.type.name,
                "amount" to transaction.amount,
                "category" to transaction.category,
                "categoryId" to transaction.categoryId,
                "accountId" to transaction.accountId,
                "accountName" to transaction.accountName,
                "description" to desc,
                "title" to desc,
                "date" to transaction.date,
                "createdAt" to if (transaction.createdAt > 0L) transaction.createdAt else System.currentTimeMillis()
            )

            val collection = firestore.collection("users")
                .document(uid)
                .collection("transactions")

            if (transaction.id.isNotBlank()) {
                collection.document(transaction.id).set(data).await()
            } else {
                collection.add(data).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "guardar el movimiento"))
        }
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            firestore.collection("users")
                .document(uid)
                .collection("transactions")
                .document(id)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "eliminar el movimiento"))
        }
    }

    private fun DocumentSnapshot.toTransaction(): Transaction? {
        return try {
            val typeStr = getString("type") ?: TransactionType.GASTO.name
            val type = try {
                TransactionType.valueOf(typeStr)
            } catch (_: Exception) {
                TransactionType.GASTO
            }
            val amountNumber = get("amount") as? Number
            val amount = amountNumber?.toLong() ?: 0L
            val category = getString("category").orEmpty()
            val categoryId = getString("categoryId").orEmpty()
            val accountId = getString("accountId").orEmpty()
            val accountName = getString("accountName").orEmpty()
            val desc = getString("description") ?: getString("title").orEmpty()
            val date = getLong("date") ?: (get("date") as? Number)?.toLong() ?: System.currentTimeMillis()
            val createdAt = getLong("createdAt") ?: (get("createdAt") as? Number)?.toLong() ?: date

            Transaction(
                id = id,
                title = desc,
                description = desc,
                amount = amount,
                type = type,
                category = category,
                categoryId = categoryId,
                accountId = accountId,
                accountName = accountName,
                date = date,
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
