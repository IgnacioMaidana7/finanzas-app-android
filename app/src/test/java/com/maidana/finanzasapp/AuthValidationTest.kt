package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.model.User
import com.maidana.finanzasapp.data.repository.AuthRepository
import com.maidana.finanzasapp.ui.screens.auth.AuthState
import com.maidana.finanzasapp.ui.screens.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAuthRepository : AuthRepository {
    var shouldFailLogin = false
    var shouldFailRegister = false
    private val _currentUserFlow = MutableStateFlow<User?>(null)
    var currentUserMock: User? = null
        set(value) {
            field = value
            _currentUserFlow.value = value
        }

    override val currentUser: Flow<User?> = _currentUserFlow

    override fun isUserAuthenticated(): Boolean = currentUserMock != null

    override fun getCurrentUser(): User? = currentUserMock

    override suspend fun login(email: String, password: String): Result<User> {
        return if (shouldFailLogin) {
            Result.failure(Exception("Credenciales inválidas."))
        } else {
            val user = User(
                id = "test_uid",
                email = email,
                firstName = "Usuario",
                lastName = "Test",
                displayName = "Usuario Test"
            )
            currentUserMock = user
            _currentUserFlow.value = user
            Result.success(user)
        }
    }

    override suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): Result<User> {
        return if (shouldFailRegister) {
            Result.failure(Exception("Este correo electrónico ya se encuentra registrado."))
        } else {
            val user = User(
                id = "new_uid",
                email = email,
                firstName = firstName,
                lastName = lastName,
                displayName = "$firstName $lastName".trim()
            )
            currentUserMock = user
            _currentUserFlow.value = user
            Result.success(user)
        }
    }

    override suspend fun register(email: String, password: String, displayName: String): Result<User> {
        val parts = displayName.trim().split(" ", limit = 2)
        val first = parts.getOrNull(0).orEmpty()
        val last = parts.getOrNull(1).orEmpty()
        return register(firstName = first, lastName = last, email = email, password = password)
    }

    override suspend fun logout(): Result<Unit> {
        currentUserMock = null
        _currentUserFlow.value = null
        return Result.success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthValidationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun validateRegistration_emptyName_returnsError() {
        val error = AuthViewModel.validateRegistrationInput(
            name = "",
            email = "test@finanzasapp.com",
            password = "password123",
            confirmPassword = "password123"
        )
        assertEquals("El nombre es obligatorio.", error)
    }

    @Test
    fun validateRegistration_emptyFirstName_returnsError() {
        val error = AuthViewModel.validateRegistrationInput(
            firstName = "",
            lastName = "Maidana",
            email = "test@finanzasapp.com",
            password = "password123",
            confirmPassword = "password123"
        )
        assertEquals("El nombre es obligatorio.", error)
    }

    @Test
    fun validateRegistration_emptyLastName_returnsError() {
        val error = AuthViewModel.validateRegistrationInput(
            firstName = "Ignacio",
            lastName = "   ",
            email = "test@finanzasapp.com",
            password = "password123",
            confirmPassword = "password123"
        )
        assertEquals("El apellido es obligatorio.", error)
    }

    @Test
    fun validateRegistration_emptyEmail_returnsError() {
        val error = AuthViewModel.validateRegistrationInput(
            firstName = "Ignacio",
            lastName = "Maidana",
            email = "   ",
            password = "password123",
            confirmPassword = "password123"
        )
        assertEquals("El correo electrónico es obligatorio.", error)
    }

    @Test
    fun validateRegistration_invalidEmailFormat_returnsError() {
        val error = AuthViewModel.validateRegistrationInput(
            firstName = "Ignacio",
            lastName = "Maidana",
            email = "correo-invalido",
            password = "password123",
            confirmPassword = "password123"
        )
        assertEquals("Ingresa un correo electrónico con formato válido (ej: usuario@correo.com).", error)
    }

    @Test
    fun validateRegistration_shortPassword_returnsError() {
        val error = AuthViewModel.validateRegistrationInput(
            firstName = "Ignacio",
            lastName = "Maidana",
            email = "ignacio@test.com",
            password = "1234",
            confirmPassword = "1234"
        )
        assertEquals("La contraseña debe tener al menos 8 caracteres.", error)
    }

    @Test
    fun validateRegistration_mismatchedPasswords_returnsError() {
        val error = AuthViewModel.validateRegistrationInput(
            firstName = "Ignacio",
            lastName = "Maidana",
            email = "ignacio@test.com",
            password = "password123",
            confirmPassword = "password456"
        )
        assertEquals("Las contraseñas no coinciden.", error)
    }

    @Test
    fun validateRegistration_validInputs_returnsNull() {
        val error = AuthViewModel.validateRegistrationInput(
            firstName = "Ignacio",
            lastName = "Maidana",
            email = "ignacio@test.com",
            password = "password123",
            confirmPassword = "password123"
        )
        assertNull(error)
    }

    @Test
    fun fakeAuthRepository_initialState_isNotAuthenticated() {
        val repository = FakeAuthRepository()
        assertFalse(repository.isUserAuthenticated())
        assertNull(repository.getCurrentUser())
    }

    @Test
    fun authViewModel_withBlankEmail_setsErrorState() {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.login("", "12345678")

        val state = viewModel.uiState.value.authState
        assertTrue(state is AuthState.Error)
        assertEquals("Por favor ingresa tu correo electrónico.", (state as AuthState.Error).message)
    }

    @Test
    fun authViewModel_withBlankPassword_setsErrorState() {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.login("test@test.com", "")

        val state = viewModel.uiState.value.authState
        assertTrue(state is AuthState.Error)
        assertEquals("Por favor ingresa tu contraseña.", (state as AuthState.Error).message)
    }

    @Test
    fun authViewModel_clearError_resetsToIdle() {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.login("", "")
        assertTrue(viewModel.uiState.value.authState is AuthState.Error)

        viewModel.clearError()
        assertEquals(AuthState.Idle, viewModel.uiState.value.authState)
    }

    @Test
    fun authViewModel_successfulLogin_setsAuthenticatedState() {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.login("usuario@test.com", "password123")

        val state = viewModel.uiState.value.authState
        assertTrue(state is AuthState.Authenticated)
        assertEquals("usuario@test.com", (state as AuthState.Authenticated).user.email)
        assertTrue(viewModel.isUserAuthenticated())
    }

    @Test
    fun authViewModel_failedLogin_setsErrorState() {
        val repository = FakeAuthRepository().apply { shouldFailLogin = true }
        val viewModel = AuthViewModel(repository)

        viewModel.login("usuario@test.com", "wrongpassword")

        val state = viewModel.uiState.value.authState
        assertTrue(state is AuthState.Error)
        assertEquals("Credenciales inválidas.", (state as AuthState.Error).message)
    }

    @Test
    fun authViewModel_successfulRegister_setsAuthenticatedState() {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.register(
            firstName = "Ignacio",
            lastName = "Maidana",
            email = "nuevo@test.com",
            password = "password123",
            confirmPassword = "password123"
        )

        val state = viewModel.uiState.value.authState
        assertTrue(state is AuthState.Authenticated)
        val user = (state as AuthState.Authenticated).user
        assertEquals("nuevo@test.com", user.email)
        assertEquals("Ignacio", user.firstName)
        assertEquals("Maidana", user.lastName)
        assertEquals("Ignacio", user.greetingName)
        assertEquals("Ignacio Maidana", user.displayName)
    }

    @Test
    fun authViewModel_failedRegister_setsErrorState() {
        val repository = FakeAuthRepository().apply { shouldFailRegister = true }
        val viewModel = AuthViewModel(repository)

        viewModel.register(
            firstName = "Usuario",
            lastName = "Existente",
            email = "existente@test.com",
            password = "password123",
            confirmPassword = "password123"
        )

        val state = viewModel.uiState.value.authState
        assertTrue(state is AuthState.Error)
        assertEquals("Este correo electrónico ya se encuentra registrado.", (state as AuthState.Error).message)
    }

    @Test
    fun authViewModel_logout_clearsUserAndResetsState() {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.login("usuario@test.com", "password123")
        assertTrue(viewModel.uiState.value.authState is AuthState.Authenticated)

        viewModel.logout()
        assertEquals(AuthState.Idle, viewModel.uiState.value.authState)
        assertNull(viewModel.uiState.value.currentUser)
        assertFalse(viewModel.isUserAuthenticated())
    }
}
