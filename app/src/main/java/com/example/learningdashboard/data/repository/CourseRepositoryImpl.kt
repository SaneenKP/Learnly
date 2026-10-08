package com.example.learningdashboard.data.repository

import com.example.learningdashboard.data.local.dao.CourseDao
import com.example.learningdashboard.data.local.dao.LessonDao
import com.example.learningdashboard.data.remote.CourseApi
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

    override suspend fun refreshCourses() {
        // Fetch latest courses from remote API
        val remoteCourses = courseApi.getCourses()
        courseDao.insertCourses(remoteCourses.map { it.toEntity() })

        // Pre-cache lessons for each course if not already stored, preserving existing user progress
        remoteCourses.forEach { course ->
            try {
                val remoteLessons = courseApi.getLessons(course.id)
                lessonDao.insertLessonsIfNotExists(remoteLessons.map { it.toEntity() })
            } catch (_: Exception) {
                // Non-fatal if lessons for an individual course cannot be refreshed during list sync
            }
        }
    }

    override suspend fun refreshLessons(courseId: Long) {
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
}
