package com.example.learningdashboard.presentation.viewmodel

import com.example.learningdashboard.data.util.NetworkManager
import com.example.learningdashboard.domain.usecase.LoginUseCaseImpl
import com.example.learningdashboard.domain.usecase.ObserveNetworkStatusUseCaseImpl
import com.example.learningdashboard.domain.usecase.ValidateCredentialsUseCaseImpl
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeNetwork = FakeTestNetworkManager(initialOnline = true)
    private val validateCredentialsUseCase = ValidateCredentialsUseCaseImpl()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val loginUseCase = LoginUseCaseImpl(
            networkManager = fakeNetwork,
            validateCredentialsUseCase = validateCredentialsUseCase,
            ioDispatcher = testDispatcher
        )
        val observeNetworkUseCase = ObserveNetworkStatusUseCaseImpl(fakeNetwork)

        viewModel = LoginViewModel(
            loginUseCase = loginUseCase,
            validateCredentialsUseCase = validateCredentialsUseCase,
            observeNetworkStatusUseCase = observeNetworkUseCase,
            dispatcher = testDispatcher
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
        advanceUntilIdle()

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
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun login_invalidEmailFormat_showsValidationError() = runTest {
        viewModel.onEmailChanged("invalid-email")
        viewModel.onPasswordChanged("password123")
        viewModel.login()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.emailError)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun login_shortPassword_showsValidationError() = runTest {
        viewModel.onEmailChanged("student@university.edu")
        viewModel.onPasswordChanged("123")
        viewModel.login()
        advanceUntilIdle()

        assertEquals("Password must be at least 6 characters", viewModel.uiState.value.passwordError)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun login_incorrectCredentials_showsInvalidCredentialsError() = runTest {
        viewModel.onEmailChanged("wrong@university.edu")
        viewModel.onPasswordChanged("wrongpassword")
        viewModel.login()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage?.contains("Invalid credentials") == true)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun login_validCredentials_setsSuccessTrue() = runTest {
        viewModel.onEmailChanged("student@university.edu")
        viewModel.onPasswordChanged("password123")
        viewModel.login()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onEmailChanged_updatesValidationErrorDynamically() {
        assertEquals(null, viewModel.uiState.value.emailError)

        viewModel.onEmailChanged("invalid-email")
        assertEquals(
            "Please enter a valid email address (e.g. user@domain.com)",
            viewModel.uiState.value.emailError
        )

        viewModel.onEmailChanged("user@example.com")
        assertEquals(null, viewModel.uiState.value.emailError)

        viewModel.onEmailChanged("")
        assertEquals("Email cannot be empty", viewModel.uiState.value.emailError)
    }

    @Test
    fun onPasswordChanged_updatesValidationErrorDynamically() {
        assertEquals(null, viewModel.uiState.value.passwordError)

        viewModel.onPasswordChanged("123")
        assertEquals(
            "Password must be at least 6 characters",
            viewModel.uiState.value.passwordError
        )

        viewModel.onPasswordChanged("123456")
        assertEquals(null, viewModel.uiState.value.passwordError)

        viewModel.onPasswordChanged("")
        assertEquals("Password cannot be empty", viewModel.uiState.value.passwordError)
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
}
