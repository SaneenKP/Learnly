package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.data.remote.FakeAuthApi
import com.example.learningdashboard.data.repository.AuthRepositoryImpl
import com.example.learningdashboard.data.util.NetworkManager
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.domain.repository.UserPreferencesRepository
import com.example.learningdashboard.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginUseCaseTest {

    private val fakeUserPreferences = FakeUserPreferencesRepository()
    private lateinit var fakeAuthApi: FakeAuthApi
    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setUp() {
        fakeAuthApi = FakeAuthApi(simulatedDelayMs = 0L)
        authRepository = AuthRepositoryImpl(fakeAuthApi)
    }

    @Test
    fun invoke_whenOffline_throwsOfflineActionNotAllowed() = runTest {
        val fakeNetwork = FakeNetworkManager(online = false)
        val useCase = LoginUseCaseImpl(authRepository, fakeNetwork, fakeUserPreferences)

        var thrown = false
        try {
            useCase(Constants.Auth.DEFAULT_EMAIL, Constants.Auth.DEFAULT_PASSWORD)
        } catch (e: AppError.BusinessError.OfflineActionNotAllowed) {
            thrown = true
        }
        assertTrue(thrown)
        assertFalse(fakeUserPreferences.isLoggedIn.first())
    }

    @Test
    fun invoke_withInvalidEmailFormat_throwsInvalidEmail() = runTest {
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = LoginUseCaseImpl(authRepository, fakeNetwork, fakeUserPreferences)

        var thrown = false
        try {
            useCase("invalid-format", Constants.Auth.DEFAULT_PASSWORD)
        } catch (e: AppError.BusinessError.InvalidEmail) {
            thrown = true
        }
        assertTrue(thrown)
        assertFalse(fakeUserPreferences.isLoggedIn.first())
    }

    @Test
    fun invoke_withShortPassword_throwsShortPassword() = runTest {
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = LoginUseCaseImpl(authRepository, fakeNetwork, fakeUserPreferences)

        var thrown = false
        try {
            useCase(Constants.Auth.DEFAULT_EMAIL, "123")
        } catch (e: AppError.BusinessError.ShortPassword) {
            thrown = true
        }
        assertTrue(thrown)
        assertFalse(fakeUserPreferences.isLoggedIn.first())
    }

    @Test
    fun invoke_withWrongCredentials_throwsInvalidCredentialsFromApi() = runTest {
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = LoginUseCaseImpl(authRepository, fakeNetwork, fakeUserPreferences)

        var thrown = false
        try {
            useCase("wrong@university.edu", "wrongpassword")
        } catch (e: AppError.BusinessError.InvalidCredentials) {
            thrown = true
        }
        assertTrue(thrown)
        assertFalse(fakeUserPreferences.isLoggedIn.first())
    }

    @Test
    fun invoke_whenApiServerUnavailable_throwsNetworkError() = runTest {
        fakeAuthApi.shouldSimulateError = true
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = LoginUseCaseImpl(authRepository, fakeNetwork, fakeUserPreferences)

        var thrown = false
        try {
            useCase(Constants.Auth.DEFAULT_EMAIL, Constants.Auth.DEFAULT_PASSWORD)
        } catch (e: AppError.NetworkError.ServerUnavailable) {
            thrown = true
        }
        assertTrue(thrown)
        assertFalse(fakeUserPreferences.isLoggedIn.first())
    }

    @Test
    fun invoke_withCorrectCredentials_succeedsAndPersistsLoginState() = runTest {
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = LoginUseCaseImpl(authRepository, fakeNetwork, fakeUserPreferences)

        useCase(Constants.Auth.DEFAULT_EMAIL, Constants.Auth.DEFAULT_PASSWORD)

        assertTrue(fakeUserPreferences.isLoggedIn.first())
    }

    private class FakeNetworkManager(private val online: Boolean) : NetworkManager {
        override val isOnline: Flow<Boolean> = MutableStateFlow(online)
        override fun isCurrentlyOnline(): Boolean = online
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val _isLoggedIn = MutableStateFlow(false)
        override val isLoggedIn: Flow<Boolean> = _isLoggedIn

        override suspend fun setLoggedIn(isLoggedIn: Boolean) {
            _isLoggedIn.value = isLoggedIn
        }
    }
}
