package com.maidana.finanzasapp.data.repository

import com.maidana.finanzasapp.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    fun isUserAuthenticated(): Boolean
    fun getCurrentUser(): User?
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, displayName: String = ""): Result<User>
    suspend fun logout(): Result<Unit>
}
