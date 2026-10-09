package com.example.learningdashboard.presentation.viewmodel

import com.example.learningdashboard.domain.model.Course
import com.example.learningdashboard.domain.usecase.GetCoursesUseCase
import com.example.learningdashboard.domain.usecase.LogoutUseCase
import com.example.learningdashboard.domain.usecase.ObserveNetworkStatusUseCase
import com.example.learningdashboard.domain.usecase.RefreshCoursesUseCase
import com.example.learningdashboard.presentation.courses.CourseListUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CourseListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val courseFlow = MutableStateFlow<List<Course>>(emptyList())
    private val fakeNetworkUseCase = FakeObserveNetworkStatusUseCase(initialOnline = true)

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
            logoutUseCase = fakeLogout,
            observeNetworkStatusUseCase = fakeNetworkUseCase
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
    fun logout_whenOnline_invokesLogoutUseCaseAndTriggersNavigationCallback() = runTest {
        fakeNetworkUseCase.onlineFlow.value = true
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
            logoutUseCase = fakeLogout,
            observeNetworkStatusUseCase = fakeNetworkUseCase
        )

        var loggedOutCallbackFired = false
        viewModel.logout {
            loggedOutCallbackFired = true
        }

        advanceUntilIdle()

        assertTrue(logoutCalled)
        assertTrue(loggedOutCallbackFired)
        assertFalse(viewModel.showNetworkDialog.value)
    }

    @Test
    fun logout_whenOffline_blocksLogoutAndShowsNetworkDialogFor3Seconds() = runTest {
        fakeNetworkUseCase.onlineFlow.value = false
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
            logoutUseCase = fakeLogout,
            observeNetworkStatusUseCase = fakeNetworkUseCase
        )

        var loggedOutCallbackFired = false
        viewModel.logout {
            loggedOutCallbackFired = true
        }

        // Advance initial call without draining 3-second delay
        testScheduler.runCurrent()

        // Logout was blocked: useCase not invoked, callback not fired
        assertFalse(logoutCalled)
        assertFalse(loggedOutCallbackFired)

        // Dialog is shown
        assertTrue(viewModel.showNetworkDialog.value)

        // Fast forward 2.9s -> still showing
        advanceTimeBy(2900L)
        assertTrue(viewModel.showNetworkDialog.value)

        // Fast forward another 200ms -> auto-dismissed
        advanceTimeBy(200L)
        assertFalse(viewModel.showNetworkDialog.value)
    }

    @Test
    fun logout_whenOffline_showsDialogAgainOnRepeatedAttempt() = runTest {
        fakeNetworkUseCase.onlineFlow.value = false
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
            logoutUseCase = fakeLogout,
            observeNetworkStatusUseCase = fakeNetworkUseCase
        )

        // 1st offline logout attempt
        viewModel.logout {}
        testScheduler.runCurrent()
        assertTrue(viewModel.showNetworkDialog.value)

        // Wait 3 seconds for it to dismiss
        advanceTimeBy(3001L)
        assertFalse(viewModel.showNetworkDialog.value)

        // 2nd offline logout attempt -> dialog is shown AGAIN
        viewModel.logout {}
        testScheduler.runCurrent()
        assertTrue(viewModel.showNetworkDialog.value)

        // Wait 3 seconds for it to dismiss again
        advanceTimeBy(3001L)
        assertFalse(viewModel.showNetworkDialog.value)

        // Logout was never executed
        assertFalse(logoutCalled)
    }

    private class FakeObserveNetworkStatusUseCase(
        initialOnline: Boolean
    ) : ObserveNetworkStatusUseCase {
        val onlineFlow = MutableStateFlow(initialOnline)
        override val isOnline: Flow<Boolean> = onlineFlow
        override fun isCurrentlyOnline(): Boolean = onlineFlow.value
    }
}
