package com.maidana.finanzasapp.data.repository

import com.maidana.finanzasapp.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    fun isUserAuthenticated(): Boolean
    fun getCurrentUser(): User?
    suspend fun login(email: String, password: String): Result<User>

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): Result<User>

    // Sobrecarga de compatibilidad para llamadas previas
    suspend fun register(email: String, password: String, displayName: String = ""): Result<User> {
        val parts = displayName.trim().split(" ", limit = 2)
        val first = parts.getOrNull(0).orEmpty()
        val last = parts.getOrNull(1).orEmpty()
        return register(firstName = first, lastName = last, email = email, password = password)
    }

    suspend fun logout(): Result<Unit>
}
