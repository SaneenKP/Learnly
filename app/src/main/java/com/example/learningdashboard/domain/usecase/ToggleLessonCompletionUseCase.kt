package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.domain.repository.CourseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ToggleLessonCompletionUseCase {
    suspend operator fun invoke(courseId: Long, lessonId: Long, completed: Boolean)
}

class ToggleLessonCompletionUseCaseImpl(
    private val repository: CourseRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ToggleLessonCompletionUseCase {

    override suspend fun invoke(courseId: Long, lessonId: Long, completed: Boolean) = withContext(ioDispatcher) {
        repository.markLessonCompleted(courseId, lessonId, completed)
    }
}
