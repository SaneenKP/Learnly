package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.data.util.NetworkManager
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.domain.repository.CourseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

interface RefreshCoursesUseCase {
    suspend operator fun invoke(forceRefresh: Boolean = false)
}

class RefreshCoursesUseCaseImpl(
    private val repository: CourseRepository,
    private val networkManager: NetworkManager
) : RefreshCoursesUseCase {

    override suspend fun invoke(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
        val cachedCount = repository.getCourseCount()

        // DB-First policy: If cached courses exist and not forced, return immediately
        if (cachedCount > 0 && !forceRefresh) {
            return@withContext
        }

        // Offline handling
        if (!networkManager.isCurrentlyOnline()) {
            if (cachedCount == 0) {
                // When offline initially with empty DB, do not fetch from API
                return@withContext
            } else {
                throw AppError.NetworkError.NoInternet("Internet connection required to refresh courses.")
            }
        }

        // Simulated network delay kept inside UseCase rather than ViewModel
        delay(800)

        repository.fetchAndStoreCourses()
    }
}
