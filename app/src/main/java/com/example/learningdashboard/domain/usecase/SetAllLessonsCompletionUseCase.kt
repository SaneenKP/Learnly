package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.domain.repository.CourseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UseCase to mark all lessons of a course as completed or uncompleted in batch.
 */
interface SetAllLessonsCompletionUseCase {
    suspend operator fun invoke(courseId: Long, completed: Boolean)
}

class SetAllLessonsCompletionUseCaseImpl(
    private val repository: CourseRepository
) : SetAllLessonsCompletionUseCase {

    override suspend fun invoke(courseId: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        repository.setAllLessonsCompletion(courseId, completed)
    }
}
