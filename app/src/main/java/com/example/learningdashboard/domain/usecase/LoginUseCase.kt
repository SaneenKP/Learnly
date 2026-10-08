package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.data.util.NetworkManager
import com.example.learningdashboard.domain.error.AppError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

interface LoginUseCase {
    suspend operator fun invoke(email: String, password: String)
}

class LoginUseCaseImpl(
    private val networkManager: NetworkManager,
    private val validateCredentialsUseCase: ValidateCredentialsUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LoginUseCase {

    override suspend fun invoke(email: String, password: String) = withContext(ioDispatcher) {
        if (!networkManager.isCurrentlyOnline()) {
            throw AppError.BusinessError.OfflineActionNotAllowed("Internet connection required to log in.")
        }

        val trimmedEmail = email.trim()
        val emailError = validateCredentialsUseCase.validateEmail(trimmedEmail)
        if (emailError != null) {
            if (trimmedEmail.isBlank()) throw AppError.BusinessError.EmptyEmail()
            else throw AppError.BusinessError.InvalidEmail()
        }

        val passwordError = validateCredentialsUseCase.validatePassword(password)
        if (passwordError != null) {
            if (password.isBlank()) throw AppError.BusinessError.EmptyPassword()
            else throw AppError.BusinessError.ShortPassword()
        }

        // Dummy network latency handled in UseCase layer, keeping ViewModel clean
        delay(1200)

        if (trimmedEmail != REQUIRED_EMAIL || password != REQUIRED_PASSWORD) {
            throw AppError.BusinessError.InvalidCredentials()
        }
    }

    companion object {
        const val REQUIRED_EMAIL = "student@university.edu"
        const val REQUIRED_PASSWORD = "password123"
    }
}
