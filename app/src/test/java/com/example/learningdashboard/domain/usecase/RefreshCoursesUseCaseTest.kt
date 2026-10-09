package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.data.util.NetworkManager
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.domain.model.Course
import com.example.learningdashboard.domain.model.Lesson
import com.example.learningdashboard.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshCoursesUseCaseTest {

    @Test
    fun invoke_whenDatabaseHasCachedCourses_doesNotFetchFromRemote() = runTest {
        val fakeRepo = FakeCourseRepository(cachedCount = 3)
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = RefreshCoursesUseCaseImpl(fakeRepo, fakeNetwork)

        useCase(forceRefresh = false)

        assertEquals(0, fakeRepo.fetchAndStoreCallCount)
    }

    @Test
    fun invoke_whenDatabaseEmptyAndOnline_fetchesFromRemote() = runTest {
        val fakeRepo = FakeCourseRepository(cachedCount = 0)
        val fakeNetwork = FakeNetworkManager(online = true)
        val useCase = RefreshCoursesUseCaseImpl(fakeRepo, fakeNetwork)

        useCase(forceRefresh = false)

        assertEquals(1, fakeRepo.fetchAndStoreCallCount)
    }

    @Test
    fun invoke_whenDatabaseEmptyAndOffline_doesNotFetchFromRemoteAndReturnsGracefully() = runTest {
        val fakeRepo = FakeCourseRepository(cachedCount = 0)
        val fakeNetwork = FakeNetworkManager(online = false)
        val useCase = RefreshCoursesUseCaseImpl(fakeRepo, fakeNetwork)

        useCase(forceRefresh = false)

        assertEquals(0, fakeRepo.fetchAndStoreCallCount)
    }

    @Test
    fun invoke_whenOfflineAndForceRefresh_throwsNoInternetError() = runTest {
        val fakeRepo = FakeCourseRepository(cachedCount = 2)
        val fakeNetwork = FakeNetworkManager(online = false)
        val useCase = RefreshCoursesUseCaseImpl(fakeRepo, fakeNetwork)

        var thrown = false
        try {
            useCase(forceRefresh = true)
        } catch (e: AppError.NetworkError.NoInternet) {
            thrown = true
        }
        assertTrue(thrown)
    }

    private class FakeCourseRepository(private var cachedCount: Int) : CourseRepository {
        var fetchAndStoreCallCount = 0

        override fun observeCourses(): Flow<List<Course>> = MutableStateFlow(emptyList())
        override fun observeCourse(courseId: Long): Flow<Course?> = MutableStateFlow(null)
        override fun observeLessons(courseId: Long): Flow<List<Lesson>> = MutableStateFlow(emptyList())
        override suspend fun getCourseCount(): Int = cachedCount
        override suspend fun getLessonCount(courseId: Long): Int = 0

        override suspend fun fetchAndStoreCourses() {
            fetchAndStoreCallCount++
        }

        override suspend fun fetchAndStoreLessons(courseId: Long) {}
        override suspend fun markLessonCompleted(courseId: Long, lessonId: Long, completed: Boolean) {}
        override suspend fun setAllLessonsCompletion(courseId: Long, completed: Boolean) {}
        override suspend fun clearAllData() {}
    }

    private class FakeNetworkManager(private val online: Boolean) : NetworkManager {
        override val isOnline: Flow<Boolean> = MutableStateFlow(online)
        override fun isCurrentlyOnline(): Boolean = online
    }
}
