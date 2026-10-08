package com.example.learningdashboard.data.repository

import com.example.learningdashboard.data.local.dao.CourseDao
import com.example.learningdashboard.data.local.dao.LessonDao
import com.example.learningdashboard.data.local.entity.CourseEntity
import com.example.learningdashboard.data.local.entity.LessonEntity
import com.example.learningdashboard.data.remote.CourseApi
import com.example.learningdashboard.data.remote.model.CourseDto
import com.example.learningdashboard.data.remote.model.LessonDto
import com.example.learningdashboard.data.util.NetworkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
        val fakeNetwork = FakeTestNetworkManager(online = true)

        val repository = CourseRepositoryImpl(fakeCourseDao, fakeLessonDao, fakeApi, fakeNetwork)

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
    fun refreshCourses_whenDatabaseHasData_doesNotCallApi() = runTest {
        val courseFlow = MutableStateFlow(
            listOf(
                CourseEntity(1L, "Python", "Prog", 101L, "John", 20)
            )
        )
        val lessonFlow = MutableStateFlow<List<LessonEntity>>(emptyList())
        val fakeCourseDao = createFakeCourseDao(courseFlow)
        val fakeLessonDao = createFakeLessonDao(lessonFlow)

        var apiCalled = false
        val fakeApi = object : CourseApi {
            override suspend fun getCourses(): List<CourseDto> {
                apiCalled = true
                return emptyList()
            }
            override suspend fun getLessons(courseId: Long): List<LessonDto> = emptyList()
        }
        val fakeNetwork = FakeTestNetworkManager(online = true)

        val repository = CourseRepositoryImpl(fakeCourseDao, fakeLessonDao, fakeApi, fakeNetwork)
        repository.refreshCourses(forceRefresh = false)

        // DB had data, so API should not be called
        assertTrue(!apiCalled)
    }

    @Test
    fun refreshCourses_whenDatabaseEmptyAndOffline_doesNotCallApi() = runTest {
        val courseFlow = MutableStateFlow<List<CourseEntity>>(emptyList())
        val lessonFlow = MutableStateFlow<List<LessonEntity>>(emptyList())
        val fakeCourseDao = createFakeCourseDao(courseFlow)
        val fakeLessonDao = createFakeLessonDao(lessonFlow)

        var apiCalled = false
        val fakeApi = object : CourseApi {
            override suspend fun getCourses(): List<CourseDto> {
                apiCalled = true
                return emptyList()
            }
            override suspend fun getLessons(courseId: Long): List<LessonDto> = emptyList()
        }
        val fakeNetwork = FakeTestNetworkManager(online = false) // Device is OFFLINE

        val repository = CourseRepositoryImpl(fakeCourseDao, fakeLessonDao, fakeApi, fakeNetwork)
        repository.refreshCourses(forceRefresh = false)

        // Offline and DB empty: API should NOT be called
        assertTrue(!apiCalled)
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
        val fakeNetwork = FakeTestNetworkManager(online = true)

        val repository = CourseRepositoryImpl(fakeCourseDao, fakeLessonDao, fakeApi, fakeNetwork)
        repository.clearAllData()

        assertEquals(0, fakeCourseDao.getCourseCount())
        assertEquals(0, fakeLessonDao.getLessonCount())
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
        override fun observeLessons(courseId: Long): Flow<List<LessonEntity>> = lessonFlow
        override fun observeAllLessons(): Flow<List<LessonEntity>> = lessonFlow
        override suspend fun getLessonsForCourse(courseId: Long): List<LessonEntity> = lessonFlow.value
        override suspend fun getLessonById(lessonId: Long): LessonEntity? = lessonFlow.value.firstOrNull { it.id == lessonId }
        override suspend fun updateLessonCompletion(lessonId: Long, completed: Boolean) {
            lessonFlow.value = lessonFlow.value.map {
                if (it.id == lessonId) it.copy(completed = completed) else it
            }
        }
        override suspend fun insertLessons(lessons: List<LessonEntity>) { lessonFlow.value = lessons }
        override suspend fun insertLessonsIfNotExists(lessons: List<LessonEntity>) { lessonFlow.value = lessons }
        override suspend fun getLessonCount(): Int = lessonFlow.value.size
        override suspend fun deleteAllLessons() { lessonFlow.value = emptyList() }
    }

    private class FakeTestNetworkManager(private val online: Boolean) : NetworkManager {
        override val isOnline: Flow<Boolean> = MutableStateFlow(online)
        override fun isCurrentlyOnline(): Boolean = online
    }
}
