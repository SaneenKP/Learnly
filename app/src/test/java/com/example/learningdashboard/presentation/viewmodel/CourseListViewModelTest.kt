package com.example.learningdashboard.presentation.viewmodel

import com.example.learningdashboard.domain.model.Course
import com.example.learningdashboard.domain.usecase.GetCoursesUseCase
import com.example.learningdashboard.domain.usecase.LogoutUseCase
import com.example.learningdashboard.domain.usecase.RefreshCoursesUseCase
import com.example.learningdashboard.presentation.courses.CourseListUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CourseListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val courseFlow = MutableStateFlow<List<Course>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoading_showsLoadingStateBeforeEmittingCourses() = runTest {
        val fakeGetCourses = object : GetCoursesUseCase {
            override fun invoke(): Flow<List<Course>> = courseFlow
        }
        val fakeRefreshCourses = object : RefreshCoursesUseCase {
            override suspend fun invoke(forceRefresh: Boolean) {
                // Simulate emission upon refresh completion
                courseFlow.value = listOf(
                    Course(1L, "Python Programming", "Programming", "John Smith", 20, 10, 50)
                )
            }
        }
        val fakeLogout = object : LogoutUseCase {
            override suspend fun invoke() {}
        }

        val viewModel = CourseListViewModel(
            getCoursesUseCase = fakeGetCourses,
            refreshCoursesUseCase = fakeRefreshCourses,
            logoutUseCase = fakeLogout
        )

        // Start collecting in backgroundScope so WhileSubscribed is active
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // Initially in Loading state while fetching from API/DB
        assertEquals(CourseListUiState.Loading, viewModel.uiState.value)

        // Process coroutine tasks
        advanceUntilIdle()

        // Transitions to Success with loaded courses
        assertTrue(viewModel.uiState.value is CourseListUiState.Success)
        val success = viewModel.uiState.value as CourseListUiState.Success
        assertEquals(1, success.courses.size)
        assertEquals("Python Programming", success.courses.first().title)
    }

    @Test
    fun logout_invokesLogoutUseCaseAndTriggersNavigationCallback() = runTest {
        val fakeGetCourses = object : GetCoursesUseCase {
            override fun invoke(): Flow<List<Course>> = courseFlow
        }
        val fakeRefreshCourses = object : RefreshCoursesUseCase {
            override suspend fun invoke(forceRefresh: Boolean) {}
        }
        var logoutCalled = false
        val fakeLogout = object : LogoutUseCase {
            override suspend fun invoke() {
                logoutCalled = true
            }
        }

        val viewModel = CourseListViewModel(
            getCoursesUseCase = fakeGetCourses,
            refreshCoursesUseCase = fakeRefreshCourses,
            logoutUseCase = fakeLogout
        )

        var loggedOutCallbackFired = false
        viewModel.logout {
            loggedOutCallbackFired = true
        }

        advanceUntilIdle()

        assertTrue(logoutCalled)
        assertTrue(loggedOutCallbackFired)
    }
}
