package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.data.util.NetworkManager
import com.example.learningdashboard.domain.error.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val validateCredentialsUseCase = ValidateCredentialsUseCaseImpl()

    @Test
    fun invoke_whenOffline_throwsOfflineActionNotAllowed() = runTest(testDispatcher) {
        val fakeNetwork = FakeNetworkManager(online = false)
        val useCase = LoginUseCaseImpl(fakeNetwork, validateCredentialsUseCase, testDispatcher)

        var thrown = false
        try {
            useCase("student@university.edu", "password123")
        } catch (e: AppError.BusinessError.OfflineActionNotAllowed) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun invoke_withInvalidEmailFormat_throwsInvalidEmail() = runTest(testDispatcher) {
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = LoginUseCaseImpl(fakeNetwork, validateCredentialsUseCase, testDispatcher)

        var thrown = false
        try {
            useCase("invalid-format", "password123")
        } catch (e: AppError.BusinessError.InvalidEmail) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun invoke_withShortPassword_throwsShortPassword() = runTest(testDispatcher) {
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = LoginUseCaseImpl(fakeNetwork, validateCredentialsUseCase, testDispatcher)

        var thrown = false
        try {
            useCase("student@university.edu", "123")
        } catch (e: AppError.BusinessError.ShortPassword) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun invoke_withWrongCredentials_throwsInvalidCredentials() = runTest(testDispatcher) {
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = LoginUseCaseImpl(fakeNetwork, validateCredentialsUseCase, testDispatcher)

        var thrown = false
        try {
            useCase("other@university.edu", "password123")
        } catch (e: AppError.BusinessError.InvalidCredentials) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun invoke_withCorrectCredentials_succeedsWithoutException() = runTest(testDispatcher) {
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = LoginUseCaseImpl(fakeNetwork, validateCredentialsUseCase, testDispatcher)

        // Should complete without throwing any exception
        useCase("student@university.edu", "password123")
    }

    private class FakeNetworkManager(private val online: Boolean) : NetworkManager {
        override val isOnline: Flow<Boolean> = MutableStateFlow(online)
        override fun isCurrentlyOnline(): Boolean = online
    }
}
