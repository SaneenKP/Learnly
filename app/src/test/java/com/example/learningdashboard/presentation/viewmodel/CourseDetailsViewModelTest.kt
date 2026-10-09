package com.example.learningdashboard.presentation.viewmodel

import com.example.learningdashboard.domain.model.Course
import com.example.learningdashboard.domain.model.Lesson
import com.example.learningdashboard.domain.usecase.GetCourseDetailsUseCase
import com.example.learningdashboard.domain.usecase.ObserveNetworkStatusUseCase
import com.example.learningdashboard.domain.usecase.RefreshCourseDetailsUseCase
import com.example.learningdashboard.domain.usecase.SetAllLessonsCompletionUseCase
import com.example.learningdashboard.domain.usecase.ToggleLessonCompletionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class CourseDetailsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeGetCourseDetailsUseCase = FakeGetCourseDetailsUseCase()
    private val fakeRefreshCourseDetailsUseCase = FakeRefreshCourseDetailsUseCase()
    private val fakeToggleLessonCompletionUseCase = FakeToggleLessonCompletionUseCase()
    private val fakeSetAllLessonsCompletionUseCase = FakeSetAllLessonsCompletionUseCase()
    private val fakeObserveNetworkStatusUseCase = FakeObserveNetworkStatusUseCase(initialOnline = true)

    private lateinit var viewModel: CourseDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CourseDetailsViewModel(
            courseId = 1L,
            getCourseDetailsUseCase = fakeGetCourseDetailsUseCase,
            refreshCourseDetailsUseCase = fakeRefreshCourseDetailsUseCase,
            toggleLessonCompletionUseCase = fakeToggleLessonCompletionUseCase,
            setAllLessonsCompletionUseCase = fakeSetAllLessonsCompletionUseCase,
            observeNetworkStatusUseCase = fakeObserveNetworkStatusUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectAllLessons_delegatesToSetAllLessonsCompletionUseCaseWithTrue() = runTest {
        viewModel.selectAllLessons()
        advanceUntilIdle()

        assertEquals(1L, fakeSetAllLessonsCompletionUseCase.lastCourseId)
        assertTrue(fakeSetAllLessonsCompletionUseCase.lastCompleted == true)
    }

    @Test
    fun clearAllLessons_delegatesToSetAllLessonsCompletionUseCaseWithFalse() = runTest {
        viewModel.clearAllLessons()
        advanceUntilIdle()

        assertEquals(1L, fakeSetAllLessonsCompletionUseCase.lastCourseId)
        assertFalse(fakeSetAllLessonsCompletionUseCase.lastCompleted == true)
    }

    @Test
    fun toggleLessonCompletion_delegatesToToggleUseCase() = runTest {
        viewModel.toggleLessonCompletion(lessonId = 101L, completed = true)
        advanceUntilIdle()

        assertEquals(1L, fakeToggleLessonCompletionUseCase.lastCourseId)
        assertEquals(101L, fakeToggleLessonCompletionUseCase.lastLessonId)
        assertTrue(fakeToggleLessonCompletionUseCase.lastCompleted == true)
    }

    @Test
    fun init_whenOffline_showsNetworkDialogAndAutoDismissesAfter3Seconds() = runTest {
        val offlineNetworkUseCase = FakeObserveNetworkStatusUseCase(initialOnline = false)
        val offlineViewModel = CourseDetailsViewModel(
            courseId = 1L,
            getCourseDetailsUseCase = fakeGetCourseDetailsUseCase,
            refreshCourseDetailsUseCase = fakeRefreshCourseDetailsUseCase,
            toggleLessonCompletionUseCase = fakeToggleLessonCompletionUseCase,
            setAllLessonsCompletionUseCase = fakeSetAllLessonsCompletionUseCase,
            observeNetworkStatusUseCase = offlineNetworkUseCase
        )

        // Dialog is shown initially because device is offline
        assertTrue(offlineViewModel.showNetworkDialog.value)

        // Fast forward 2.9 seconds -> still showing
        advanceTimeBy(2900L)
        assertTrue(offlineViewModel.showNetworkDialog.value)

        // Fast forward another 200ms (total > 3000ms) -> auto-dismissed
        advanceTimeBy(200L)
        assertFalse(offlineViewModel.showNetworkDialog.value)
    }

    @Test
    fun manualDismissNetworkDialog_immediatelyHidesDialog() = runTest {
        viewModel.showNetworkUnavailableDialog()
        assertTrue(viewModel.showNetworkDialog.value)

        viewModel.dismissNetworkDialog()
        assertFalse(viewModel.showNetworkDialog.value)
    }

    private class FakeObserveNetworkStatusUseCase(
        initialOnline: Boolean
    ) : ObserveNetworkStatusUseCase {
        val onlineFlow = MutableStateFlow(initialOnline)
        override val isOnline: Flow<Boolean> = onlineFlow
        override fun isCurrentlyOnline(): Boolean = onlineFlow.value
    }

    private class FakeGetCourseDetailsUseCase : GetCourseDetailsUseCase {
        override fun observeCourse(courseId: Long): Flow<Course?> = MutableStateFlow(
            Course(1L, "Python", "Dev", "John", 2, 0, 0)
        )
        override fun observeLessons(courseId: Long): Flow<List<Lesson>> = MutableStateFlow(
            listOf(Lesson(101L, 1L, "L1", false), Lesson(102L, 1L, "L2", false))
        )
    }

    private class FakeRefreshCourseDetailsUseCase : RefreshCourseDetailsUseCase {
        override suspend fun invoke(courseId: Long, forceRefresh: Boolean) {}
    }

    private class FakeToggleLessonCompletionUseCase : ToggleLessonCompletionUseCase {
        var lastCourseId: Long? = null
        var lastLessonId: Long? = null
        var lastCompleted: Boolean? = null

        override suspend fun invoke(courseId: Long, lessonId: Long, completed: Boolean) {
            lastCourseId = courseId
            lastLessonId = lessonId
            lastCompleted = completed
        }
    }

    private class FakeSetAllLessonsCompletionUseCase : SetAllLessonsCompletionUseCase {
        var lastCourseId: Long? = null
        var lastCompleted: Boolean? = null

        override suspend fun invoke(courseId: Long, completed: Boolean) {
            lastCourseId = courseId
            lastCompleted = completed
        }
    }
}
