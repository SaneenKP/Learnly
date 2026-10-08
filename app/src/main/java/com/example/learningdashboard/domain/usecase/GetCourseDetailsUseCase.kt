package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.domain.model.Course
import com.example.learningdashboard.domain.model.Lesson
import com.example.learningdashboard.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow

interface GetCourseDetailsUseCase {
    fun observeCourse(courseId: Long): Flow<Course?>
    fun observeLessons(courseId: Long): Flow<List<Lesson>>
}

class GetCourseDetailsUseCaseImpl(
    private val repository: CourseRepository
) : GetCourseDetailsUseCase {

    override fun observeCourse(courseId: Long): Flow<Course?> {
        return repository.observeCourse(courseId)
    }

    override fun observeLessons(courseId: Long): Flow<List<Lesson>> {
        return repository.observeLessons(courseId)
    }
}
