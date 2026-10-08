package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.data.util.NetworkManager
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

interface LoginUseCase {
    suspend operator fun invoke(email: String, password: String)
}

class LoginUseCaseImpl(
    private val networkManager: NetworkManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val simulatedDelayMs: Long = 1200L
) : LoginUseCase {

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

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
            password.length < 6 -> throw AppError.BusinessError.ShortPassword()
        }

        // Simulated network latency on Dispatchers.IO
        if (simulatedDelayMs > 0) {
            delay(simulatedDelayMs)
        }

        if (trimmedEmail != REQUIRED_EMAIL || password != REQUIRED_PASSWORD) {
            throw AppError.BusinessError.InvalidCredentials()
        }

        // Persist logged-in state to DataStore
        userPreferencesRepository.setLoggedIn(true)
    }

    companion object {
        const val REQUIRED_EMAIL = "student@university.edu"
        const val REQUIRED_PASSWORD = "password123"
    }
}
