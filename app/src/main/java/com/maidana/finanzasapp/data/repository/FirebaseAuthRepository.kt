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
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toDomainUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
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

            Result.success(firebaseUser.toDomainUser())
        } catch (e: Throwable) {
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun register(email: String, password: String, displayName: String): Result<User> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).awaitResult()
            val firebaseUser = authResult.user
                ?: return Result.failure(AuthException("No se pudo crear la cuenta de usuario."))

            // Actualizar nombre de perfil si fue provisto
            if (displayName.isNotBlank()) {
                val profileUpdates = userProfileChangeRequest {
                    this.displayName = displayName.trim()
                }
                try {
                    firebaseUser.updateProfile(profileUpdates).awaitResult()
                } catch (_: Throwable) {
                    // Si falla la actualización del displayName no bloqueamos el registro exitoso
                }
            }

            Result.success(firebaseUser.toDomainUser())
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
        val name = displayName.orEmpty().ifBlank {
            email?.substringBefore("@") ?: "Usuario"
        }
        return User(
            id = uid,
            email = email.orEmpty(),
            displayName = name
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
