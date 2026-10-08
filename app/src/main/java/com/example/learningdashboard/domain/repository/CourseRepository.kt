package com.example.learningdashboard.domain.repository

import com.example.learningdashboard.domain.model.Course
import com.example.learningdashboard.domain.model.Lesson
import kotlinx.coroutines.flow.Flow

interface CourseRepository {

    /**
     * Observes the active list of courses as a continuous stream from the local database.
     * Calculated progress and completed lesson counts are derived from persisted lessons.
     */
    fun observeCourses(): Flow<List<Course>>

    /**
     * Observes a specific course by its identifier.
     */
    fun observeCourse(courseId: Long): Flow<Course?>

    /**
     * Observes the lessons for a given course.
     */
    fun observeLessons(courseId: Long): Flow<List<Lesson>>

    /**
     * Refreshes course list from remote source and updates local database cache.
     */
    suspend fun refreshCourses()

    /**
     * Refreshes lessons for a given course from remote source and updates local database cache.
     */
    suspend fun refreshLessons(courseId: Long)

    /**
     * Marks a lesson as completed or incomplete.
     * Persisted immediately to local storage to support offline workflows.
     */
    suspend fun markLessonCompleted(
        courseId: Long,
        lessonId: Long,
        completed: Boolean = true
    )
}
