package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.data.util.NetworkManager
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseTest {

    private val fakeUserPreferences = FakeUserPreferencesRepository()

    @Test
    fun invoke_whenOffline_throwsOfflineActionNotAllowed() = runTest {
        val fakeNetwork = FakeNetworkManager(online = false)
        val useCase = LoginUseCaseImpl(fakeNetwork, fakeUserPreferences)

        var thrown = false
        try {
            useCase("student@university.edu", "password123")
        } catch (e: AppError.BusinessError.OfflineActionNotAllowed) {
            thrown = true
        }
        assertTrue(thrown)
        assertFalse(fakeUserPreferences.isLoggedIn.first())
    }

    @Test
    fun invoke_withInvalidEmailFormat_throwsInvalidEmail() = runTest {
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = LoginUseCaseImpl(fakeNetwork, fakeUserPreferences)

        var thrown = false
        try {
            useCase("invalid-format", "password123")
        } catch (e: AppError.BusinessError.InvalidEmail) {
            thrown = true
        }
        assertTrue(thrown)
        assertFalse(fakeUserPreferences.isLoggedIn.first())
    }

    @Test
    fun invoke_withShortPassword_throwsShortPassword() = runTest {
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = LoginUseCaseImpl(fakeNetwork, fakeUserPreferences)

        var thrown = false
        try {
            useCase("student@university.edu", "123")
        } catch (e: AppError.BusinessError.ShortPassword) {
            thrown = true
        }
        assertTrue(thrown)
        assertFalse(fakeUserPreferences.isLoggedIn.first())
    }

    @Test
    fun invoke_withWrongCredentials_throwsInvalidCredentials() = runTest {
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = LoginUseCaseImpl(fakeNetwork, fakeUserPreferences)

        var thrown = false
        try {
            useCase("other@university.edu", "password123")
        } catch (e: AppError.BusinessError.InvalidCredentials) {
            thrown = true
        }
        assertTrue(thrown)
        assertFalse(fakeUserPreferences.isLoggedIn.first())
    }

    @Test
    fun invoke_withCorrectCredentials_succeedsAndPersistsLoginState() = runTest {
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = LoginUseCaseImpl(fakeNetwork, fakeUserPreferences)

        useCase("student@university.edu", "password123")

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
