package com.example.learningdashboard.presentation.viewmodel

import com.example.learningdashboard.data.remote.FakeAuthApi
import com.example.learningdashboard.data.repository.AuthRepositoryImpl
import com.example.learningdashboard.data.util.NetworkManager
import com.example.learningdashboard.domain.repository.UserPreferencesRepository
import com.example.learningdashboard.domain.usecase.LoginUseCaseImpl
import com.example.learningdashboard.domain.usecase.ObserveNetworkStatusUseCaseImpl
import com.example.learningdashboard.domain.usecase.ValidateCredentialsUseCaseImpl
import com.example.learningdashboard.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeNetwork = FakeTestNetworkManager(initialOnline = true)
    private val fakeUserPreferences = FakeUserPreferencesRepository()
    private val validateCredentialsUseCase = ValidateCredentialsUseCaseImpl()
    private lateinit var fakeAuthApi: FakeAuthApi
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthApi = FakeAuthApi(simulatedDelayMs = 0L)
        authRepository = AuthRepositoryImpl(fakeAuthApi)
        val loginUseCase = LoginUseCaseImpl(
            authRepository = authRepository,
            networkManager = fakeNetwork,
            userPreferencesRepository = fakeUserPreferences
        )
        val observeNetworkUseCase = ObserveNetworkStatusUseCaseImpl(fakeNetwork)

        viewModel = LoginViewModel(
            loginUseCase = loginUseCase,
            validateCredentialsUseCase = validateCredentialsUseCase,
            observeNetworkStatusUseCase = observeNetworkUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_whenOffline_disablesSubmissionAndShowsOfflineError() = runTest {
        fakeNetwork.setOnline(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isOnline)
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.login()
        waitForLoginCompletion()

        assertTrue(viewModel.uiState.value.errorMessage?.contains("Internet connection required") == true)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun login_blankFields_showsFieldSpecificErrors() = runTest {
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("")
        viewModel.login()
        advanceUntilIdle()

        assertEquals("Email cannot be empty", viewModel.uiState.value.emailError)
        assertEquals("Password cannot be empty", viewModel.uiState.value.passwordError)
        assertEquals("Email cannot be empty", viewModel.uiState.value.displayEmailError)
        assertEquals("Password cannot be empty", viewModel.uiState.value.displayPasswordError)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun login_invalidEmailFormat_showsValidationError() = runTest {
        viewModel.onEmailChanged("invalid-email")
        viewModel.onPasswordChanged("password123")
        viewModel.login()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.emailError)
        assertNotNull(viewModel.uiState.value.displayEmailError)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun login_shortPassword_showsValidationError() = runTest {
        viewModel.onEmailChanged("student@university.edu")
        viewModel.onPasswordChanged("123")
        viewModel.login()
        advanceUntilIdle()

        assertEquals("Password must be at least 6 characters", viewModel.uiState.value.passwordError)
        assertEquals("Password must be at least 6 characters", viewModel.uiState.value.displayPasswordError)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun login_incorrectCredentials_showsInvalidCredentialsError() = runTest {
        viewModel.onEmailChanged("wrong@university.edu")
        viewModel.onPasswordChanged("wrongpassword")
        viewModel.login()

        waitForLoginCompletion()

        assertTrue(viewModel.uiState.value.errorMessage?.contains("Invalid credentials") == true)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun login_validCredentials_setsSuccessTrue() = runTest {
        viewModel.onEmailChanged("student@university.edu")
        viewModel.onPasswordChanged("password123")
        viewModel.login()

        waitForLoginCompletion()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    private fun waitForLoginCompletion() {
        testDispatcher.scheduler.advanceUntilIdle()
        var count = 0
        while (viewModel.uiState.value.isLoading && count < 50) {
            Thread.sleep(10)
            testDispatcher.scheduler.advanceUntilIdle()
            count++
        }
    }

    @Test
    fun validationErrors_onlyDisplayedAfterFieldsAreUnfocused() {
        // Initially no errors shown
        assertNull(viewModel.uiState.value.displayEmailError)
        assertNull(viewModel.uiState.value.displayPasswordError)

        // 1. User focuses on email and types invalid email
        viewModel.onEmailFocusChanged(isFocused = true)
        viewModel.onEmailChanged("invalid")

        // While email field is focused, error is suppressed
        assertNotNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.displayEmailError)

        // 2. User unfocuses email field
        viewModel.onEmailFocusChanged(isFocused = false)

        // Error is now visible after field lost focus
        assertEquals(
            "Please enter a valid email address (e.g. user@domain.com)",
            viewModel.uiState.value.displayEmailError
        )

        // 3. User focuses on password and types short password
        viewModel.onPasswordFocusChanged(isFocused = true)
        viewModel.onPasswordChanged("123")

        // While password field is focused, error is suppressed
        assertNotNull(viewModel.uiState.value.passwordError)
        assertNull(viewModel.uiState.value.displayPasswordError)

        // 4. User unfocuses password field
        viewModel.onPasswordFocusChanged(isFocused = false)

        // Error is now visible after field lost focus
        assertEquals(
            "Password must be at least 6 characters",
            viewModel.uiState.value.displayPasswordError
        )

        // 5. User refocuses and types valid values
        viewModel.onEmailFocusChanged(isFocused = true)
        viewModel.onEmailChanged("student@university.edu")
        viewModel.onEmailFocusChanged(isFocused = false)

        viewModel.onPasswordFocusChanged(isFocused = true)
        viewModel.onPasswordChanged("password123")
        viewModel.onPasswordFocusChanged(isFocused = false)

        assertNull(viewModel.uiState.value.displayEmailError)
        assertNull(viewModel.uiState.value.displayPasswordError)
    }

    @Test
    fun canSubmit_dynamicallyTracksFieldValidityAndConnectivity() {
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onEmailChanged("user@example.com")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onPasswordChanged("123")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onPasswordChanged("password123")
        assertTrue(viewModel.uiState.value.canSubmit)

        viewModel.onEmailChanged("invalid")
        assertFalse(viewModel.uiState.value.canSubmit)
    }

    private class FakeTestNetworkManager(initialOnline: Boolean) : NetworkManager {
        private val _isOnline = MutableStateFlow(initialOnline)
        override val isOnline: Flow<Boolean> = _isOnline
        override fun isCurrentlyOnline(): Boolean = _isOnline.value

        fun setOnline(online: Boolean) {
            _isOnline.value = online
        }
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val _isLoggedIn = MutableStateFlow(false)
        override val isLoggedIn: Flow<Boolean> = _isLoggedIn

        override suspend fun setLoggedIn(isLoggedIn: Boolean) {
            _isLoggedIn.value = isLoggedIn
        }
    }
}
