package com.example.learningdashboard.data.repository

import com.example.learningdashboard.data.local.dao.CourseDao
import com.example.learningdashboard.data.local.dao.LessonDao
import com.example.learningdashboard.data.local.entity.CourseEntity
import com.example.learningdashboard.data.local.entity.LessonEntity
import com.example.learningdashboard.data.remote.CourseApi
import com.example.learningdashboard.data.remote.model.CourseDto
import com.example.learningdashboard.data.remote.model.LessonDto
import com.example.learningdashboard.domain.error.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseRepositoryImplTest {

    @Test
    fun observeCourses_dynamicallyDerivesProgressFromPersistedLessons() = runTest {
        val courseFlow = MutableStateFlow(
            listOf(
                CourseEntity(
                    id = 1L,
                    title = "Python Programming",
                    category = "Programming",
                    instructorId = 101L,
                    instructorName = "John Smith",
                    totalLessons = 20
                )
            )
        )
        val lessonFlow = MutableStateFlow(
            listOf(
                LessonEntity(id = 101L, courseId = 1L, title = "Lesson 1", completed = true),
                LessonEntity(id = 102L, courseId = 1L, title = "Lesson 2", completed = true),
                LessonEntity(id = 103L, courseId = 1L, title = "Lesson 3", completed = false),
                LessonEntity(id = 104L, courseId = 1L, title = "Lesson 4", completed = false)
            )
        )

        val fakeCourseDao = createFakeCourseDao(courseFlow)
        val fakeLessonDao = createFakeLessonDao(lessonFlow)
        val fakeApi = object : CourseApi {
            override suspend fun getCourses(): List<CourseDto> = emptyList()
            override suspend fun getLessons(courseId: Long): List<LessonDto> = emptyList()
        }

        val repository = CourseRepositoryImpl(fakeCourseDao, fakeLessonDao, fakeApi)

        // 2 of 4 lessons completed = 50% progress
        val initialCourses = repository.observeCourses().first()
        assertEquals(1, initialCourses.size)
        assertEquals(2, initialCourses.first().completedLessons)
        assertEquals(50, initialCourses.first().progress)

        // Mark 3rd lesson as complete -> repository updates Room DAO
        repository.markLessonCompleted(courseId = 1L, lessonId = 103L, completed = true)

        // 3 of 4 completed = 75% progress
        val updatedCourses = repository.observeCourses().first()
        assertEquals(3, updatedCourses.first().completedLessons)
        assertEquals(75, updatedCourses.first().progress)
    }

    @Test
    fun fetchAndStoreCourses_storesInLocalDatabase() = runTest {
        val courseFlow = MutableStateFlow<List<CourseEntity>>(emptyList())
        val lessonFlow = MutableStateFlow<List<LessonEntity>>(emptyList())
        val fakeCourseDao = createFakeCourseDao(courseFlow)
        val fakeLessonDao = createFakeLessonDao(lessonFlow)

        val fakeApi = object : CourseApi {
            override suspend fun getCourses(): List<CourseDto> = listOf(
                CourseDto(1L, "Python", "Prog", 101L, "John Smith", 20)
            )
            override suspend fun getLessons(courseId: Long): List<LessonDto> = listOf(
                LessonDto(101L, 1L, "Intro", false)
            )
        }

        val repository = CourseRepositoryImpl(fakeCourseDao, fakeLessonDao, fakeApi)
        repository.fetchAndStoreCourses()

        assertEquals(1, fakeCourseDao.getCourseCount())
        assertEquals(1, fakeLessonDao.getLessonCount())
    }

    @Test
    fun fetchAndStoreCourses_propagatesNetworkErrors() = runTest {
        val courseFlow = MutableStateFlow<List<CourseEntity>>(emptyList())
        val lessonFlow = MutableStateFlow<List<LessonEntity>>(emptyList())
        val fakeCourseDao = createFakeCourseDao(courseFlow)
        val fakeLessonDao = createFakeLessonDao(lessonFlow)

        val fakeApi = object : CourseApi {
            override suspend fun getCourses(): List<CourseDto> {
                throw AppError.NetworkError.ServerUnavailable()
            }
            override suspend fun getLessons(courseId: Long): List<LessonDto> = emptyList()
        }

        val repository = CourseRepositoryImpl(fakeCourseDao, fakeLessonDao, fakeApi)

        var thrown = false
        try {
            repository.fetchAndStoreCourses()
        } catch (e: AppError.NetworkError.ServerUnavailable) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun clearAllData_erasesAllCoursesAndLessons() = runTest {
        val courseFlow = MutableStateFlow(
            listOf(CourseEntity(1L, "Python", "Prog", 101L, "John", 20))
        )
        val lessonFlow = MutableStateFlow(
            listOf(LessonEntity(101L, 1L, "Lesson 1", true))
        )
        val fakeCourseDao = createFakeCourseDao(courseFlow)
        val fakeLessonDao = createFakeLessonDao(lessonFlow)
        val fakeApi = object : CourseApi {
            override suspend fun getCourses(): List<CourseDto> = emptyList()
            override suspend fun getLessons(courseId: Long): List<LessonDto> = emptyList()
        }

        val repository = CourseRepositoryImpl(fakeCourseDao, fakeLessonDao, fakeApi)
        repository.clearAllData()

        assertEquals(0, fakeCourseDao.getCourseCount())
        assertEquals(0, fakeLessonDao.getLessonCount())
    }

    @Test
    fun setAllLessonsCompletion_updatesAllLessonsForCourse() = runTest {
        val courseFlow = MutableStateFlow(
            listOf(CourseEntity(1L, "Python", "Dev", 1L, "John", 2))
        )
        val lessonFlow = MutableStateFlow(
            listOf(
                LessonEntity(101L, 1L, "Lesson 1", false),
                LessonEntity(102L, 1L, "Lesson 2", false),
                LessonEntity(201L, 2L, "Other Course Lesson", false)
            )
        )
        val fakeCourseDao = createFakeCourseDao(courseFlow)
        val fakeLessonDao = createFakeLessonDao(lessonFlow)
        val fakeApi = object : CourseApi {
            override suspend fun getCourses(): List<CourseDto> = emptyList()
            override suspend fun getLessons(courseId: Long): List<LessonDto> = emptyList()
        }

        val repository = CourseRepositoryImpl(fakeCourseDao, fakeLessonDao, fakeApi)
        repository.setAllLessonsCompletion(courseId = 1L, completed = true)

        val lessons = repository.observeLessons(1L).first()
        assertTrue(lessons.all { it.completed })
        // Verify other course was not affected
        val otherCourseLessons = repository.observeLessons(2L).first()
        assertTrue(otherCourseLessons.none { it.completed })
    }

    private fun createFakeCourseDao(courseFlow: MutableStateFlow<List<CourseEntity>>) = object : CourseDao {
        override fun observeCourses(): Flow<List<CourseEntity>> = courseFlow
        override fun observeCourse(courseId: Long): Flow<CourseEntity?> = MutableStateFlow(courseFlow.value.firstOrNull())
        override suspend fun getCourseById(courseId: Long): CourseEntity? = courseFlow.value.firstOrNull()
        override suspend fun insertCourses(courses: List<CourseEntity>) { courseFlow.value = courses }
        override suspend fun insertCourse(course: CourseEntity) { courseFlow.value = listOf(course) }
        override suspend fun getCourseCount(): Int = courseFlow.value.size
        override suspend fun deleteAllCourses() { courseFlow.value = emptyList() }
    }

    private fun createFakeLessonDao(lessonFlow: MutableStateFlow<List<LessonEntity>>) = object : LessonDao {
        override fun observeLessons(courseId: Long): Flow<List<LessonEntity>> = lessonFlow.map { list ->
            list.filter { it.courseId == courseId }
        }
        override fun observeAllLessons(): Flow<List<LessonEntity>> = lessonFlow
        override suspend fun getLessonsForCourse(courseId: Long): List<LessonEntity> = lessonFlow.value.filter { it.courseId == courseId }
        override suspend fun getLessonById(lessonId: Long): LessonEntity? = lessonFlow.value.firstOrNull { it.id == lessonId }
        override suspend fun updateLessonCompletion(lessonId: Long, completed: Boolean) {
            lessonFlow.value = lessonFlow.value.map {
                if (it.id == lessonId) it.copy(completed = completed) else it
            }
        }
        override suspend fun updateAllLessonsCompletionForCourse(courseId: Long, completed: Boolean) {
            lessonFlow.value = lessonFlow.value.map {
                if (it.courseId == courseId) it.copy(completed = completed) else it
            }
        }
        override suspend fun insertLessons(lessons: List<LessonEntity>) { lessonFlow.value = lessons }
        override suspend fun insertLessonsIfNotExists(lessons: List<LessonEntity>) { lessonFlow.value = lessons }
        override suspend fun getLessonCount(): Int = lessonFlow.value.size
        override suspend fun deleteAllLessons() { lessonFlow.value = emptyList() }
    }
}
