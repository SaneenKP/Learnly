package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.data.util.NetworkManager
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.domain.repository.AuthRepository
import com.example.learningdashboard.domain.repository.UserPreferencesRepository
import com.example.learningdashboard.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface LoginUseCase {
    suspend operator fun invoke(email: String, password: String)
}

class LoginUseCaseImpl(
    private val authRepository: AuthRepository,
    private val networkManager: NetworkManager,
    private val userPreferencesRepository: UserPreferencesRepository
) : LoginUseCase {

    private val emailRegex = Regex(Constants.Validation.EMAIL_REGEX_PATTERN)

    override suspend fun invoke(email: String, password: String) = withContext(Dispatchers.IO) {
        if (!networkManager.isCurrentlyOnline()) {
            throw AppError.BusinessError.OfflineActionNotAllowed("Internet connection required to log in.")
        }

        val trimmedEmail = email.trim()
        when {
            trimmedEmail.isBlank() -> throw AppError.BusinessError.EmptyEmail()
            !emailRegex.matches(trimmedEmail) -> throw AppError.BusinessError.InvalidEmail()
        }

        when {
            password.isBlank() -> throw AppError.BusinessError.EmptyPassword()
            password.length < Constants.Validation.MIN_PASSWORD_LENGTH -> throw AppError.BusinessError.ShortPassword()
        }

        // Call the remote login API through AuthRepository
        authRepository.login(trimmedEmail, password)

        // On successful authentication, persist logged-in state to DataStore
        userPreferencesRepository.setLoggedIn(true)
    }
}
