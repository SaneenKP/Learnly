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
     * Refreshes course list adhering to DB-first policy:
     * - If DB already contains courses and forceRefresh is false, no API call is performed.
     * - If DB is empty, checks connectivity; if offline, API call is not performed.
     * - If online (or forceRefresh), fetches from API and caches in Room.
     */
    suspend fun refreshCourses(forceRefresh: Boolean = false)

    /**
     * Refreshes lessons for a given course:
     * - If DB already contains lessons for this course and forceRefresh is false, no API call is performed.
     * - If empty and online, fetches from API and caches in Room.
     */
    suspend fun refreshLessons(courseId: Long, forceRefresh: Boolean = false)

    /**
     * Marks a lesson as completed or incomplete.
     * Persisted immediately to local storage to support offline workflows.
     */
    suspend fun markLessonCompleted(
        courseId: Long,
        lessonId: Long,
        completed: Boolean = true
    )

    /**
     * Erases the entire local database (used for logout).
     */
    suspend fun clearAllData()
}
