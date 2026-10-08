package com.example.learningdashboard.domain.repository

import com.example.learningdashboard.domain.model.Course
import com.example.learningdashboard.domain.model.Lesson
import kotlinx.coroutines.flow.Flow

/**
 * Pure data repository interface responsible strictly for data access, persistence,
 * and remote fetching. Business rules and orchestration are delegated to UseCases.
 */
interface CourseRepository {

    /**
     * Observes the active list of courses as a continuous stream from local persistence.
     */
    fun observeCourses(): Flow<List<Course>>

    /**
     * Observes a single course by its ID.
     */
    fun observeCourse(courseId: Long): Flow<Course?>

    /**
     * Observes the lessons belonging to a specific course.
     */
    fun observeLessons(courseId: Long): Flow<List<Lesson>>

    /**
     * Returns the total count of courses cached locally.
     */
    suspend fun getCourseCount(): Int

    /**
     * Returns the count of lessons cached locally for a specific course.
     */
    suspend fun getLessonCount(courseId: Long): Int

    /**
     * Fetches courses and default lessons from remote API and persists them locally.
     */
    suspend fun fetchAndStoreCourses()

    /**
     * Fetches lessons for a specific course from remote API and persists them locally.
     */
    suspend fun fetchAndStoreLessons(courseId: Long)

    /**
     * Updates completion status of a lesson in local storage.
     */
    suspend fun markLessonCompleted(
        courseId: Long,
        lessonId: Long,
        completed: Boolean
    )

    /**
     * Erases all cached course and lesson data from local storage.
     */
    suspend fun clearAllData()
}
