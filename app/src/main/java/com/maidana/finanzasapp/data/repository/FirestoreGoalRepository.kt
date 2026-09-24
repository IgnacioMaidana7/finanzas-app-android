package com.maidana.finanzasapp.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.maidana.finanzasapp.data.model.GoalContribution
import com.maidana.finanzasapp.data.model.SavingsGoal
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreGoalRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : GoalRepository {

    override fun getGoals(): Flow<List<SavingsGoal>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close(IllegalStateException("No hay una sesión de usuario activa."))
            return@callbackFlow
        }

        val query = firestore.collection("users")
            .document(uid)
            .collection("goals")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(mapFirestoreException(error, "cargar los objetivos de ahorro"))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val goals = snapshot.documents.mapNotNull { it.toSavingsGoal() }
                trySend(goals)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    override fun getGoalById(id: String): Flow<SavingsGoal?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close(IllegalStateException("No hay una sesión de usuario activa."))
            return@callbackFlow
        }

        val docRef = firestore.collection("users")
            .document(uid)
            .collection("goals")
            .document(id)

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(mapFirestoreException(error, "cargar el detalle del objetivo"))
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toSavingsGoal())
            } else {
                trySend(null)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    override fun getContributions(goalId: String): Flow<List<GoalContribution>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close(IllegalStateException("No hay una sesión de usuario activa."))
            return@callbackFlow
        }

        val query = firestore.collection("users")
            .document(uid)
            .collection("goals")
            .document(goalId)
            .collection("contributions")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(mapFirestoreException(error, "cargar los aportes del objetivo"))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val contributions = snapshot.documents.mapNotNull { it.toGoalContribution(goalId) }
                trySend(contributions)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun addGoal(goal: SavingsGoal, initialAmount: Long): Result<String> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            val goalsCollection = firestore.collection("users")
                .document(uid)
                .collection("goals")

            val goalDocRef = if (goal.id.isNotBlank()) {
                goalsCollection.document(goal.id)
            } else {
                goalsCollection.document()
            }
            val goalId = goalDocRef.id

            val goalData = hashMapOf(
                "name" to goal.name.ifBlank { goal.title },
                "targetAmount" to goal.targetAmount,
                "savedAmount" to initialAmount,
                "targetDate" to goal.targetDate,
                "targetDateString" to goal.targetDateString,
                "createdAt" to if (goal.createdAt > 0L) goal.createdAt else System.currentTimeMillis()
            )

            if (initialAmount > 0L) {
                val batch = firestore.batch()
                batch.set(goalDocRef, goalData)

                val contribDocRef = goalDocRef.collection("contributions").document()
                val now = System.currentTimeMillis()
                val contribData = hashMapOf(
                    "amount" to initialAmount,
                    "date" to now,
                    "dateString" to goal.targetDateString.ifBlank { "Aporte inicial" },
                    "note" to "Aporte inicial",
                    "createdAt" to now
                )
                batch.set(contribDocRef, contribData)
                batch.commit().await()
            } else {
                goalDocRef.set(goalData).await()
            }

            Result.success(goalId)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "crear el objetivo de ahorro"))
        }
    }

    override suspend fun addContribution(goalId: String, contribution: GoalContribution): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            val goalDocRef = firestore.collection("users")
                .document(uid)
                .collection("goals")
                .document(goalId)

            val contribDocRef = goalDocRef.collection("contributions").document()

            val now = if (contribution.createdAt > 0L) contribution.createdAt else System.currentTimeMillis()
            val contribDate = if (contribution.date > 0L) contribution.date else now
            val contribData = hashMapOf(
                "amount" to contribution.amount,
                "date" to contribDate,
                "dateString" to contribution.dateString,
                "note" to contribution.note.ifBlank { "Aporte a la meta" },
                "createdAt" to now
            )

            // Transacción atómica en Firestore: crea el aporte e incrementa savedAmount en la meta
            firestore.runTransaction { tx ->
                val goalSnapshot = tx.get(goalDocRef)
                if (!goalSnapshot.exists()) {
                    throw IllegalStateException("El objetivo de ahorro no existe.")
                }
                val currentSaved = (goalSnapshot.get("savedAmount") as? Number)?.toLong() ?: 0L
                val newSaved = currentSaved + contribution.amount

                tx.set(contribDocRef, contribData)
                tx.update(goalDocRef, "savedAmount", newSaved)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "registrar el aporte"))
        }
    }

    override suspend fun deleteGoal(id: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            val goalDocRef = firestore.collection("users")
                .document(uid)
                .collection("goals")
                .document(id)

            // Obtener todos los aportes de la subcolección para borrarlos en lote
            val contributionsSnapshot = goalDocRef.collection("contributions").get().await()

            val batch = firestore.batch()
            for (doc in contributionsSnapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.delete(goalDocRef)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "eliminar el objetivo de ahorro"))
        }
    }

    private fun DocumentSnapshot.toSavingsGoal(): SavingsGoal? {
        return try {
            val name = getString("name") ?: getString("title").orEmpty()
            val targetNumber = get("targetAmount") as? Number
            val targetAmount = targetNumber?.toLong() ?: 0L
            val savedNumber = get("savedAmount") as? Number
            val savedAmount = savedNumber?.toLong() ?: 0L
            val targetDate = getLong("targetDate") ?: (get("targetDate") as? Number)?.toLong() ?: 0L
            val targetDateString = getString("targetDateString").orEmpty()
            val createdAt = getLong("createdAt") ?: (get("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis()

            SavingsGoal(
                id = id,
                name = name,
                targetAmount = targetAmount,
                savedAmount = savedAmount,
                targetDate = targetDate,
                targetDateString = targetDateString,
                createdAt = createdAt
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toGoalContribution(goalId: String): GoalContribution? {
        return try {
            val amountNumber = get("amount") as? Number
            val amount = amountNumber?.toLong() ?: 0L
            val date = getLong("date") ?: (get("date") as? Number)?.toLong() ?: 0L
            val dateString = getString("dateString").orEmpty()
            val note = getString("note").orEmpty()
            val createdAt = getLong("createdAt") ?: (get("createdAt") as? Number)?.toLong() ?: date

            GoalContribution(
                id = id,
                goalId = goalId,
                amount = amount,
                date = date,
                dateString = dateString,
                note = note,
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
