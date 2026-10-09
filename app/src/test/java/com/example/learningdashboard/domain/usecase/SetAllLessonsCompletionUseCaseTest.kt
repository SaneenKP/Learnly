package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.domain.model.Course
import com.example.learningdashboard.domain.model.Lesson
import com.example.learningdashboard.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetAllLessonsCompletionUseCaseTest {

    @Test
    fun invoke_selectAll_marksAllLessonsCompleted() = runTest {
        val fakeRepo = FakeCourseRepository()
        val useCase = SetAllLessonsCompletionUseCaseImpl(fakeRepo)

        useCase(courseId = 1L, completed = true)

        assertEquals(1L, fakeRepo.lastBatchCourseId)
        assertTrue(fakeRepo.lastBatchCompleted == true)
    }

    @Test
    fun invoke_clearAll_marksAllLessonsIncomplete() = runTest {
        val fakeRepo = FakeCourseRepository()
        val useCase = SetAllLessonsCompletionUseCaseImpl(fakeRepo)

        useCase(courseId = 1L, completed = false)

        assertEquals(1L, fakeRepo.lastBatchCourseId)
        assertFalse(fakeRepo.lastBatchCompleted == true)
    }

    private class FakeCourseRepository : CourseRepository {
        var lastBatchCourseId: Long? = null
        var lastBatchCompleted: Boolean? = null

        override fun observeCourses(): Flow<List<Course>> = MutableStateFlow(emptyList())
        override fun observeCourse(courseId: Long): Flow<Course?> = MutableStateFlow(null)
        override fun observeLessons(courseId: Long): Flow<List<Lesson>> = MutableStateFlow(emptyList())
        override suspend fun getCourseCount(): Int = 0
        override suspend fun getLessonCount(courseId: Long): Int = 0
        override suspend fun fetchAndStoreCourses() {}
        override suspend fun fetchAndStoreLessons(courseId: Long) {}
        override suspend fun markLessonCompleted(courseId: Long, lessonId: Long, completed: Boolean) {}

        override suspend fun setAllLessonsCompletion(courseId: Long, completed: Boolean) {
            lastBatchCourseId = courseId
            lastBatchCompleted = completed
        }

        override suspend fun clearAllData() {}
    }
}
