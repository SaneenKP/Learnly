package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.domain.repository.CourseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface LogoutUseCase {
    suspend operator fun invoke()
}

class LogoutUseCaseImpl(
    private val repository: CourseRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LogoutUseCase {

    override suspend fun invoke() = withContext(ioDispatcher) {
        repository.clearAllData()
    }
}
