package com.example.learningdashboard.data.repository

import com.example.learningdashboard.data.local.dao.CourseDao
import com.example.learningdashboard.data.local.dao.LessonDao
import com.example.learningdashboard.data.remote.CourseApi
import com.example.learningdashboard.data.util.NetworkManager
import com.example.learningdashboard.domain.model.Course
import com.example.learningdashboard.domain.model.Lesson
import com.example.learningdashboard.domain.repository.CourseRepository
import com.example.learningdashboard.domain.util.ProgressCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.IOException

class CourseRepositoryImpl(
    private val courseDao: CourseDao,
    private val lessonDao: LessonDao,
    private val courseApi: CourseApi,
    private val networkManager: NetworkManager
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

    override suspend fun refreshCourses(forceRefresh: Boolean) {
        val cachedCount = courseDao.getCourseCount()

        // DB-First Strategy: If courses already exist locally and forceRefresh is false, return immediately
        if (cachedCount > 0 && !forceRefresh) {
            return
        }

        // Check network availability before initiating remote API call
        if (!networkManager.isCurrentlyOnline()) {
            if (cachedCount == 0) {
                // When offline and DB has no data, do not attempt to fetch from API
                return
            } else {
                throw IOException("No internet connection available to refresh courses.")
            }
        }

        // Fetch latest courses from remote API
        val remoteCourses = courseApi.getCourses()
        courseDao.insertCourses(remoteCourses.map { it.toEntity() })

        // Pre-cache lessons for each course if not already stored, preserving existing user progress
        remoteCourses.forEach { course ->
            try {
                val remoteLessons = courseApi.getLessons(course.id)
                lessonDao.insertLessonsIfNotExists(remoteLessons.map { it.toEntity() })
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun refreshLessons(courseId: Long, forceRefresh: Boolean) {
        val cachedLessons = lessonDao.getLessonsForCourse(courseId)

        // DB-First Strategy for lessons
        if (cachedLessons.isNotEmpty() && !forceRefresh) {
            return
        }

        if (!networkManager.isCurrentlyOnline()) {
            if (cachedLessons.isEmpty()) {
                return
            } else {
                throw IOException("No internet connection available to refresh lessons.")
            }
        }

        val remoteLessons = courseApi.getLessons(courseId)
        lessonDao.insertLessonsIfNotExists(remoteLessons.map { it.toEntity() })
    }
    override suspend fun markLessonCompleted(
        courseId: Long,
        lessonId: Long,
        completed: Boolean
    ) {
        lessonDao.updateLessonCompletion(lessonId, completed)
    }

    override suspend fun clearAllData() {
        lessonDao.deleteAllLessons()
        courseDao.deleteAllCourses()
    }
}
