package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.data.util.NetworkManager
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.domain.repository.CourseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

interface RefreshCourseDetailsUseCase {
    suspend operator fun invoke(courseId: Long, forceRefresh: Boolean = false)
}

class RefreshCourseDetailsUseCaseImpl(
    private val repository: CourseRepository,
    private val networkManager: NetworkManager
) : RefreshCourseDetailsUseCase {

    override suspend fun invoke(courseId: Long, forceRefresh: Boolean) = withContext(Dispatchers.IO) {
        val cachedCount = repository.getLessonCount(courseId)

        // DB-First check
        if (cachedCount > 0 && !forceRefresh) {
            return@withContext
        }

        // Connectivity check
        if (!networkManager.isCurrentlyOnline()) {
            if (cachedCount == 0) {
                return@withContext
            } else {
                throw AppError.NetworkError.NoInternet("Internet connection required to refresh lessons.")
            }
        }

        // Dummy network delay in UseCase
        delay(600)

        repository.fetchAndStoreLessons(courseId)
    }
}
