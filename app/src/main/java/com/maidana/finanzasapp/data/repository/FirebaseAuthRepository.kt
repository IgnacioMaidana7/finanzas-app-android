package com.maidana.finanzasapp.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.maidana.finanzasapp.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthException(
    val userFriendlyMessage: String,
    cause: Throwable? = null
) : Exception(userFriendlyMessage, cause)

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        var firestoreListener: ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            firestoreListener?.remove()
            firestoreListener = null

            if (firebaseUser == null) {
                trySend(null)
            } else {
                // Emitir inmediatamente con la información en memoria de Firebase Auth
                trySend(firebaseUser.toDomainUser())

                // Escuchar el perfil en Firestore /users/{uid} para reflejar cambios en tiempo real
                try {
                    firestoreListener = firestore.collection("users")
                        .document(firebaseUser.uid)
                        .addSnapshotListener { snapshot, _ ->
                            if (snapshot != null && snapshot.exists()) {
                                trySend(snapshot.toDomainUser(firebaseUser))
                            } else {
                                trySend(firebaseUser.toDomainUser())
                            }
                        }
                } catch (_: Exception) {
                    // Si falla el listener de Firestore se conserva la información de Auth
                }
            }
        }

        firebaseAuth.addAuthStateListener(authListener)
        awaitClose {
            firestoreListener?.remove()
            firebaseAuth.removeAuthStateListener(authListener)
        }
    }

    override fun isUserAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override fun getCurrentUser(): User? {
        return firebaseAuth.currentUser?.toDomainUser()
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).awaitResult()
            val firebaseUser = authResult.user
                ?: return Result.failure(AuthException("No se pudo obtener la información del usuario."))

            // Intentar enriquecer con Firestore si el documento ya existe
            val domainUser = try {
                val doc = firestore.collection("users").document(firebaseUser.uid).get().awaitResult()
                if (doc != null && doc.exists()) {
                    doc.toDomainUser(firebaseUser)
                } else {
                    firebaseUser.toDomainUser()
                }
            } catch (_: Exception) {
                firebaseUser.toDomainUser()
            }

            Result.success(domainUser)
        } catch (e: Throwable) {
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): Result<User> {
        return try {
            val cleanFirst = firstName.trim()
            val cleanLast = lastName.trim()
            val cleanEmail = email.trim()
            val fullName = "$cleanFirst $cleanLast".trim()

            // 1. Crear el usuario en Firebase Authentication
            val authResult = firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password).awaitResult()
            val firebaseUser = authResult.user
                ?: return Result.failure(AuthException("No se pudo crear la cuenta de usuario."))

            // 2. Actualizar displayName en Firebase Auth
            if (fullName.isNotBlank()) {
                val profileUpdates = userProfileChangeRequest {
                    this.displayName = fullName
                }
                try {
                    firebaseUser.updateProfile(profileUpdates).awaitResult()
                } catch (_: Throwable) {
                    // No bloqueante
                }
            }

            // 3. Forzar obtención del ID token para que el cliente interno de Firestore
            // reciba las credenciales autenticadas antes de cualquier consulta
            try {
                firebaseUser.getIdToken(true).awaitResult()
            } catch (_: Throwable) {
                // Continuar si la recarga forzada falla
            }

            // 4. Crear el documento /users/{uid} en Firestore
            val userDoc = hashMapOf(
                "firstName" to cleanFirst,
                "lastName" to cleanLast,
                "email" to cleanEmail,
                "displayName" to fullName,
                "createdAt" to System.currentTimeMillis()
            )

            try {
                firestore.collection("users").document(firebaseUser.uid).set(userDoc).awaitResult()
            } catch (e: Throwable) {
                // Si la escritura en Firestore falla no rompemos el registro pero aseguramos compatibilidad
            }

            val user = User(
                id = firebaseUser.uid,
                email = cleanEmail,
                firstName = cleanFirst,
                lastName = cleanLast,
                displayName = fullName
            )
            Result.success(user)
        } catch (e: Throwable) {
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(mapAuthException(e))
        }
    }

    private fun FirebaseUser.toDomainUser(): User {
        val dispName = displayName.orEmpty()
        val parts = dispName.trim().split(" ", limit = 2)
        val first = parts.getOrNull(0).orEmpty()
        val last = parts.getOrNull(1).orEmpty()
        val name = dispName.ifBlank {
            email?.substringBefore("@") ?: "Usuario"
        }
        return User(
            id = uid,
            email = email.orEmpty(),
            firstName = first,
            lastName = last,
            displayName = name
        )
    }

    private fun DocumentSnapshot.toDomainUser(firebaseUser: FirebaseUser): User {
        val fName = getString("firstName").orEmpty().trim()
        val lName = getString("lastName").orEmpty().trim()
        val legacyName = (getString("name") ?: getString("fullName") ?: getString("displayName")).orEmpty().trim()

        val resolvedFirstName = when {
            fName.isNotBlank() -> fName
            legacyName.isNotBlank() -> legacyName.substringBefore(" ")
            else -> firebaseUser.displayName?.trim()?.substringBefore(" ").orEmpty()
        }

        val resolvedLastName = when {
            lName.isNotBlank() -> lName
            legacyName.isNotBlank() && legacyName.contains(" ") -> legacyName.substringAfter(" ")
            else -> ""
        }

        val resolvedDisplayName = when {
            fName.isNotBlank() || lName.isNotBlank() -> "$resolvedFirstName $resolvedLastName".trim()
            legacyName.isNotBlank() -> legacyName
            else -> firebaseUser.displayName.orEmpty().ifBlank {
                firebaseUser.email?.substringBefore("@") ?: "Usuario"
            }
        }

        return User(
            id = id,
            email = getString("email") ?: firebaseUser.email.orEmpty(),
            firstName = resolvedFirstName,
            lastName = resolvedLastName,
            displayName = resolvedDisplayName
        )
    }

    private fun mapAuthException(throwable: Throwable): AuthException {
        val friendlyMessage = when (throwable) {
            is FirebaseAuthInvalidCredentialsException -> {
                when (throwable.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "El formato del correo electrónico no es válido."
                    "ERROR_WRONG_PASSWORD" -> "La contraseña ingresada es incorrecta."
                    else -> "Credenciales inválidas. Verifica tu correo y contraseña."
                }
            }
            is FirebaseAuthInvalidUserException -> {
                "No existe una cuenta registrada con este correo electrónico o fue deshabilitada."
            }
            is FirebaseAuthUserCollisionException -> {
                "Este correo electrónico ya se encuentra registrado. Inicia sesión o utiliza otro."
            }
            is FirebaseAuthWeakPasswordException -> {
                "La contraseña es muy débil. Debe contener al menos 8 caracteres."
            }
            is FirebaseNetworkException -> {
                "No se pudo conectar con el servidor. Revisa tu conexión a internet e intenta nuevamente."
            }
            is AuthException -> throwable.userFriendlyMessage
            else -> {
                "Ocurrió un error inesperado al procesar la solicitud. Por favor intenta más tarde."
            }
        }
        return AuthException(friendlyMessage, throwable)
    }
}

/**
 * Extensión para suspender y esperar la resolución de un Task de Google Play Services
 * sin requerir dependencias adicionales.
 */
private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) {
            continuation.resumeWithException(exception)
        }
    }
    addOnCanceledListener {
        if (continuation.isActive) {
            continuation.cancel()
        }
    }
}
