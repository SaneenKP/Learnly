package com.example.learningdashboard.data.remote

import android.content.Context
import com.example.learningdashboard.data.remote.model.CourseDto
import com.example.learningdashboard.data.remote.model.LessonDto
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.io.IOException

class FakeCourseApi(
    private val context: Context,
    private val networkDelayMs: Long = 1000L
) : CourseApi {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Flag to simulate remote API downtime or network disconnects for testing error states.
     */
    var shouldSimulateError: Boolean = false

    override suspend fun getCourses(): List<CourseDto> {
        if (networkDelayMs > 0) {
            delay(networkDelayMs)
        }
        if (shouldSimulateError) {
            throw IOException("Network error: Unable to reach courses server (503 Service Unavailable)")
        }

        return try {
            val jsonString = context.assets.open("courses.json").bufferedReader().use { it.readText() }
            json.decodeFromString<List<CourseDto>>(jsonString)
        } catch (e: Exception) {
            throw IOException("Failed to parse courses from remote mock: ${e.message}", e)
        }
    }

    override suspend fun getLessons(courseId: Long): List<LessonDto> {
        if (networkDelayMs > 0) {
            delay(networkDelayMs)
        }
        if (shouldSimulateError) {
            throw IOException("Network error: Unable to fetch lessons for course $courseId")
        }

        return try {
            val jsonString = context.assets.open("lessons.json").bufferedReader().use { it.readText() }
            val allLessons = json.decodeFromString<List<LessonDto>>(jsonString)
            allLessons.filter { it.courseId == courseId }
        } catch (e: Exception) {
            throw IOException("Failed to parse lessons from remote mock: ${e.message}", e)
        }
    }
}
