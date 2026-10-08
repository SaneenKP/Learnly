package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.domain.model.Course
import com.example.learningdashboard.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow

interface GetCoursesUseCase {
    operator fun invoke(): Flow<List<Course>>
}

class GetCoursesUseCaseImpl(
    private val repository: CourseRepository
) : GetCoursesUseCase {
    override fun invoke(): Flow<List<Course>> {
        return repository.observeCourses()
    }
}
