package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.domain.repository.CourseRepository
import com.example.learningdashboard.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface LogoutUseCase {
    suspend operator fun invoke()
}

class LogoutUseCaseImpl(
    private val repository: CourseRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : LogoutUseCase {

    override suspend fun invoke() = withContext(Dispatchers.IO) {
        repository.clearAllData()
        userPreferencesRepository.setLoggedIn(false)
    }
}
