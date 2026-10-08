package com.example.learningdashboard.data.repository

import com.example.learningdashboard.data.local.dao.CourseDao
import com.example.learningdashboard.data.local.dao.LessonDao
import com.example.learningdashboard.data.remote.CourseApi
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.domain.model.Course
import com.example.learningdashboard.domain.model.Lesson
import com.example.learningdashboard.domain.repository.CourseRepository
import com.example.learningdashboard.domain.util.ProgressCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CourseRepositoryImpl(
    private val courseDao: CourseDao,
    private val lessonDao: LessonDao,
    private val courseApi: CourseApi
) : CourseRepository {

    override fun observeCourses(): Flow<List<Course>> {
        return combine(
            courseDao.observeCourses(),
            lessonDao.observeAllLessons()
        ) { courses, allLessons ->
            courses.map { courseEntity ->
                val courseLessons = allLessons.filter { it.courseId == courseEntity.id }
                val completedCount = courseLessons.count { it.completed }
                val totalCount = if (courseLessons.isNotEmpty()) courseLessons.size else courseEntity.totalLessons
                val progress = ProgressCalculator.calculateProgress(completedCount, totalCount)

                courseEntity.toDomain(
                    completedLessons = completedCount,
                    progress = progress
                )
            }
        }
    }

    override fun observeCourse(courseId: Long): Flow<Course?> {
        return combine(
            courseDao.observeCourse(courseId),
            lessonDao.observeLessons(courseId)
        ) { courseEntity, lessons ->
            courseEntity?.let { entity ->
                val completedCount = lessons.count { it.completed }
                val totalCount = if (lessons.isNotEmpty()) lessons.size else entity.totalLessons
                val progress = ProgressCalculator.calculateProgress(completedCount, totalCount)

                entity.toDomain(
                    completedLessons = completedCount,
                    progress = progress
                )
            }
        }
    }

    override fun observeLessons(courseId: Long): Flow<List<Lesson>> {
        return lessonDao.observeLessons(courseId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCourseCount(): Int {
        return try {
            courseDao.getCourseCount()
        } catch (e: Exception) {
            throw AppError.DataError.DatabaseError("Failed to query course count", e)
        }
    }

    override suspend fun getLessonCount(courseId: Long): Int {
        return try {
            lessonDao.getLessonsForCourse(courseId).size
        } catch (e: Exception) {
            throw AppError.DataError.DatabaseError("Failed to query lesson count for course $courseId", e)
        }
    }

    override suspend fun fetchAndStoreCourses() {
        val remoteCourses = try {
            courseApi.getCourses()
        } catch (e: AppError.NetworkError) {
            throw e
        } catch (e: Exception) {
            throw AppError.NetworkError.Unknown("Failed to fetch courses from network", e)
        }

        try {
            courseDao.insertCourses(remoteCourses.map { it.toEntity() })
        } catch (e: Exception) {
            throw AppError.DataError.DatabaseError("Failed to store courses in database", e)
        }

        remoteCourses.forEach { course ->
            try {
                val remoteLessons = courseApi.getLessons(course.id)
                lessonDao.insertLessonsIfNotExists(remoteLessons.map { it.toEntity() })
            } catch (e: AppError.NetworkError) {
                // Secondary non-fatal remote lesson prefetch error; continue with other courses
            } catch (e: Exception) {
                throw AppError.DataError.DatabaseError("Failed to persist lessons for course ${course.id}", e)
            }
        }
    }

    override suspend fun fetchAndStoreLessons(courseId: Long) {
        val remoteLessons = try {
            courseApi.getLessons(courseId)
        } catch (e: AppError.NetworkError) {
            throw e
        } catch (e: Exception) {
            throw AppError.NetworkError.Unknown("Failed to fetch lessons for course $courseId", e)
        }

        try {
            lessonDao.insertLessonsIfNotExists(remoteLessons.map { it.toEntity() })
        } catch (e: Exception) {
            throw AppError.DataError.DatabaseError("Failed to store lessons for course $courseId", e)
        }
    }

    override suspend fun markLessonCompleted(
        courseId: Long,
        lessonId: Long,
        completed: Boolean
    ) {
        try {
            lessonDao.updateLessonCompletion(lessonId, completed)
        } catch (e: Exception) {
            throw AppError.DataError.DatabaseError("Failed to update lesson $lessonId completion status", e)
        }
    }

    override suspend fun clearAllData() {
        try {
            lessonDao.deleteAllLessons()
            courseDao.deleteAllCourses()
        } catch (e: Exception) {
            throw AppError.DataError.DatabaseError("Failed to clear local database", e)
        }
    }
}
