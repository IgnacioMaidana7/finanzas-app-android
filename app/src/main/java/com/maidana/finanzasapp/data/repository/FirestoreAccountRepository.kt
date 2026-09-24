package com.maidana.finanzasapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.maidana.finanzasapp.data.model.Account
import com.maidana.finanzasapp.data.model.AccountType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreAccountRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AccountRepository {

    override fun getAccounts(): Flow<List<Account>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close(IllegalStateException("No hay una sesión de usuario activa."))
            return@callbackFlow
        }

        val collection = firestore.collection("users")
            .document(uid)
            .collection("accounts")

        val registration = collection
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(mapFirestoreException(error, "cargar las cuentas"))
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents.orEmpty()
                if (docs.isEmpty()) {
                    // Cuentas por defecto si es usuario nuevo o aún no se inicializaron
                    trySend(Account.DEFAULT_ACCOUNTS)
                    CoroutineScope(Dispatchers.IO).launch {
                        ensureDefaultAccounts(uid)
                    }
                    return@addSnapshotListener
                }

                val accounts = docs.mapNotNull { doc -> doc.toAccount() }
                if (accounts.isEmpty()) {
                    trySend(Account.DEFAULT_ACCOUNTS)
                } else {
                    trySend(accounts)
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun ensureDefaultAccounts(uid: String): Result<Unit> {
        return try {
            val accountsCollection = firestore.collection("users")
                .document(uid)
                .collection("accounts")

            val cashDocRef = accountsCollection.document(Account.DEFAULT_CASH_ID)
            val digitalDocRef = accountsCollection.document(Account.DEFAULT_DIGITAL_ID)

            val cashSnap = cashDocRef.get().await()
            val digitalSnap = digitalDocRef.get().await()

            val batch = firestore.batch()
            var needsCommit = false

            if (!cashSnap.exists()) {
                val cashData = hashMapOf(
                    "name" to Account.DEFAULT_CASH_ACCOUNT.name,
                    "type" to Account.DEFAULT_CASH_ACCOUNT.type.name,
                    "initialBalance" to Account.DEFAULT_CASH_ACCOUNT.initialBalance,
                    "isActive" to true,
                    "createdAt" to System.currentTimeMillis()
                )
                batch.set(cashDocRef, cashData, SetOptions.merge())
                needsCommit = true
            }

            if (!digitalSnap.exists()) {
                val digitalData = hashMapOf(
                    "name" to Account.DEFAULT_DIGITAL_ACCOUNT.name,
                    "type" to Account.DEFAULT_DIGITAL_ACCOUNT.type.name,
                    "initialBalance" to Account.DEFAULT_DIGITAL_ACCOUNT.initialBalance,
                    "isActive" to true,
                    "createdAt" to System.currentTimeMillis() + 10L
                )
                batch.set(digitalDocRef, digitalData, SetOptions.merge())
                needsCommit = true
            }

            if (needsCommit) {
                batch.commit().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "inicializar las cuentas por defecto"))
        }
    }

    override suspend fun getAccount(id: String): Result<Account?> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            val doc = firestore.collection("users")
                .document(uid)
                .collection("accounts")
                .document(id)
                .get()
                .await()

            if (!doc.exists()) {
                Result.success(null)
            } else {
                Result.success(doc.toAccount())
            }
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "obtener la cuenta"))
        }
    }

    override suspend fun updateInitialBalance(id: String, initialBalance: Long): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            if (initialBalance < 0L) {
                return Result.failure(IllegalArgumentException("El saldo inicial no puede ser negativo."))
            }

            firestore.collection("users")
                .document(uid)
                .collection("accounts")
                .document(id)
                .update("initialBalance", initialBalance)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "actualizar el saldo inicial"))
        }
    }

    override suspend fun updateInitialBalances(balances: Map<String, Long>): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            val batch = firestore.batch()
            val collection = firestore.collection("users").document(uid).collection("accounts")

            balances.forEach { (accountId, balance) ->
                if (balance < 0L) {
                    throw IllegalArgumentException("El saldo inicial no puede ser negativo.")
                }
                val docRef = collection.document(accountId)
                batch.update(docRef, "initialBalance", balance)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "actualizar los saldos iniciales"))
        }
    }

    override suspend fun addAccount(account: Account): Result<Account> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            val cleanName = account.name.trim()
            if (cleanName.isBlank()) {
                return Result.failure(IllegalArgumentException("El nombre de la cuenta es obligatorio."))
            }
            if (account.initialBalance < 0L) {
                return Result.failure(IllegalArgumentException("El saldo inicial no puede ser negativo."))
            }

            val data = hashMapOf(
                "name" to cleanName,
                "type" to account.type.name,
                "initialBalance" to account.initialBalance,
                "isActive" to account.isActive,
                "createdAt" to if (account.createdAt > 0L) account.createdAt else System.currentTimeMillis()
            )

            val collection = firestore.collection("users").document(uid).collection("accounts")
            val docRef = if (account.id.isNotBlank()) {
                collection.document(account.id).set(data).await()
                collection.document(account.id)
            } else {
                collection.add(data).await()
            }

            Result.success(account.copy(id = docRef.id, name = cleanName))
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "crear la cuenta"))
        }
    }

    override suspend fun updateAccount(account: Account): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay una sesión de usuario activa."))

            val cleanName = account.name.trim()
            if (cleanName.isBlank()) {
                return Result.failure(IllegalArgumentException("El nombre de la cuenta es obligatorio."))
            }

            firestore.collection("users")
                .document(uid)
                .collection("accounts")
                .document(account.id)
                .update(
                    mapOf(
                        "name" to cleanName,
                        "type" to account.type.name,
                        "isActive" to account.isActive,
                        "initialBalance" to account.initialBalance
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e, "actualizar la cuenta"))
        }
    }

    private fun DocumentSnapshot.toAccount(): Account? {
        return try {
            val name = getString("name").orEmpty()
            if (name.isBlank()) return null

            val typeStr = getString("type") ?: AccountType.CASH.name
            val type = try {
                AccountType.valueOf(typeStr)
            } catch (_: Exception) {
                AccountType.CASH
            }

            val initialBalance = getLong("initialBalance")
                ?: (get("initialBalance") as? Number)?.toLong()
                ?: 0L

            val isActive = getBoolean("isActive") ?: true
            val createdAt = getLong("createdAt")
                ?: (get("createdAt") as? Number)?.toLong()
                ?: System.currentTimeMillis()

            Account(
                id = id,
                name = name,
                type = type,
                initialBalance = initialBalance,
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
            is IllegalArgumentException -> e.message ?: "Datos de cuenta inválidos."
            is IllegalStateException -> e.message ?: "Estado inválido."
            else -> "Ocurrió un error inesperado al $action."
        }
        return Exception(userMessage, e)
    }
}
