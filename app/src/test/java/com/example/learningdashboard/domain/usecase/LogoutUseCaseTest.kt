package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.domain.model.Course
import com.example.learningdashboard.domain.model.Lesson
import com.example.learningdashboard.domain.repository.CourseRepository
import com.example.learningdashboard.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LogoutUseCaseTest {

    @Test
    fun invoke_clearsDatabaseAndResetsDataStoreLoginState() = runTest {
        val fakeRepo = FakeCourseRepository()
        val fakeUserPreferences = FakeUserPreferencesRepository(initialLoggedIn = true)

        val logoutUseCase = LogoutUseCaseImpl(fakeRepo, fakeUserPreferences)

        logoutUseCase()

        assertEquals(1, fakeRepo.clearAllDataCallCount)
        assertFalse(fakeUserPreferences.isLoggedIn.first())
    }

    private class FakeCourseRepository : CourseRepository {
        var clearAllDataCallCount = 0

        override fun observeCourses(): Flow<List<Course>> = MutableStateFlow(emptyList())
        override fun observeCourse(courseId: Long): Flow<Course?> = MutableStateFlow(null)
        override fun observeLessons(courseId: Long): Flow<List<Lesson>> = MutableStateFlow(emptyList())
        override suspend fun getCourseCount(): Int = 0
        override suspend fun getLessonCount(courseId: Long): Int = 0
        override suspend fun fetchAndStoreCourses() {}
        override suspend fun fetchAndStoreLessons(courseId: Long) {}
        override suspend fun markLessonCompleted(courseId: Long, lessonId: Long, completed: Boolean) {}

        override suspend fun clearAllData() {
            clearAllDataCallCount++
        }
    }

    private class FakeUserPreferencesRepository(initialLoggedIn: Boolean) : UserPreferencesRepository {
        private val _isLoggedIn = MutableStateFlow(initialLoggedIn)
        override val isLoggedIn: Flow<Boolean> = _isLoggedIn

        override suspend fun setLoggedIn(isLoggedIn: Boolean) {
            _isLoggedIn.value = isLoggedIn
        }
    }
}
