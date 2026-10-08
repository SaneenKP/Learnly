package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

interface GetAuthStateUseCase {
    operator fun invoke(): Flow<Boolean>
}

class GetAuthStateUseCaseImpl(
    private val userPreferencesRepository: UserPreferencesRepository
) : GetAuthStateUseCase {
    override fun invoke(): Flow<Boolean> = userPreferencesRepository.isLoggedIn
}
