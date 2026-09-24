package com.maidana.finanzasapp.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maidana.finanzasapp.data.model.User
import com.maidana.finanzasapp.data.repository.AuthRepository
import com.maidana.finanzasapp.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Authenticated(val user: User) : AuthState
    data class Error(val message: String) : AuthState
}

data class AuthUiState(
    val authState: AuthState = AuthState.Idle,
    val currentUser: User? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            currentUser = authRepository.getCurrentUser(),
            authState = if (authRepository.isUserAuthenticated()) {
                authRepository.getCurrentUser()?.let { AuthState.Authenticated(it) } ?: AuthState.Idle
            } else {
                AuthState.Idle
            }
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { current ->
                    current.copy(
                        currentUser = user,
                        authState = if (user != null) {
                            AuthState.Authenticated(user)
                        } else {
                            AuthState.Idle
                        }
                    )
                }
            }
        }
    }

    fun isUserAuthenticated(): Boolean = authRepository.isUserAuthenticated()

    fun login(email: String, password: String) {
        if (_uiState.value.authState is AuthState.Loading) return

        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            _uiState.update { it.copy(authState = AuthState.Error("Por favor ingresa tu correo electrónico.")) }
            return
        }

        if (password.isBlank()) {
            _uiState.update { it.copy(authState = AuthState.Error("Por favor ingresa tu contraseña.")) }
            return
        }

        _uiState.update { it.copy(authState = AuthState.Loading) }

        viewModelScope.launch {
            val result = authRepository.login(trimmedEmail, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            currentUser = user,
                            authState = AuthState.Authenticated(user)
                        )
                    }
                },
                onFailure = { throwable ->
                    val message = throwable.message ?: "Error al iniciar sesión. Verifica tus credenciales."
                    _uiState.update { it.copy(authState = AuthState.Error(message)) }
                }
            )
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        if (_uiState.value.authState is AuthState.Loading) return

        val validationError = validateRegistrationInput(firstName, lastName, email, password, confirmPassword)
        if (validationError != null) {
            _uiState.update { it.copy(authState = AuthState.Error(validationError)) }
            return
        }

        _uiState.update { it.copy(authState = AuthState.Loading) }

        viewModelScope.launch {
            val result = authRepository.register(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                email = email.trim(),
                password = password
            )
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            currentUser = user,
                            authState = AuthState.Authenticated(user)
                        )
                    }
                },
                onFailure = { throwable ->
                    val message = throwable.message ?: "Error al crear la cuenta. Intenta nuevamente."
                    _uiState.update { it.copy(authState = AuthState.Error(message)) }
                }
            )
        }
    }

    // Sobrecarga de compatibilidad para código y tests existentes
    fun register(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        val parts = name.trim().split(" ", limit = 2)
        val first = parts.getOrNull(0).orEmpty()
        val last = parts.getOrNull(1).orEmpty()
        register(
            firstName = first,
            lastName = last,
            email = email,
            password = password,
            confirmPassword = confirmPassword
        )
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update {
                it.copy(
                    currentUser = null,
                    authState = AuthState.Idle
                )
            }
        }
    }

    fun clearError() {
        if (_uiState.value.authState is AuthState.Error) {
            _uiState.update { it.copy(authState = AuthState.Idle) }
        }
    }

    fun resetAuthState() {
        _uiState.update { it.copy(authState = AuthState.Idle) }
    }

    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()

        fun validateRegistrationInput(
            firstName: String,
            lastName: String,
            email: String,
            password: String,
            confirmPassword: String
        ): String? {
            if (firstName.trim().isBlank()) {
                return "El nombre es obligatorio."
            }
            if (lastName.trim().isBlank()) {
                return "El apellido es obligatorio."
            }
            val trimmedEmail = email.trim()
            if (trimmedEmail.isBlank()) {
                return "El correo electrónico es obligatorio."
            }
            if (!EMAIL_REGEX.matches(trimmedEmail)) {
                return "Ingresa un correo electrónico con formato válido (ej: usuario@correo.com)."
            }
            if (password.isBlank()) {
                return "La contraseña es obligatoria."
            }
            if (password.length < 8) {
                return "La contraseña debe tener al menos 8 caracteres."
            }
            if (password != confirmPassword) {
                return "Las contraseñas no coinciden."
            }
            return null
        }

        // Sobrecarga de retrocompatibilidad
        fun validateRegistrationInput(
            name: String,
            email: String,
            password: String,
            confirmPassword: String
        ): String? {
            if (name.trim().isBlank()) {
                return "El nombre es obligatorio."
            }
            val parts = name.trim().split(" ", limit = 2)
            val first = parts.getOrNull(0).orEmpty()
            val last = parts.getOrNull(1).orEmpty().ifBlank { first }
            return validateRegistrationInput(first, last, email, password, confirmPassword)
        }
    }
}
