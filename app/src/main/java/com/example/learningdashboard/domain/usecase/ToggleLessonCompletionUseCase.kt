package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.domain.repository.CourseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ToggleLessonCompletionUseCase {
    suspend operator fun invoke(courseId: Long, lessonId: Long, completed: Boolean)
}

class ToggleLessonCompletionUseCaseImpl(
    private val repository: CourseRepository
) : ToggleLessonCompletionUseCase {

    override suspend fun invoke(courseId: Long, lessonId: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        repository.markLessonCompleted(courseId, lessonId, completed)
    }
}
