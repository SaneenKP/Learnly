package com.example.learningdashboard.data.remote

import android.content.Context
import com.example.learningdashboard.data.remote.model.CourseDto
import com.example.learningdashboard.data.remote.model.LessonDto
import com.example.learningdashboard.data.remote.model.LoginRequestDto
import com.example.learningdashboard.data.remote.model.LoginResponseDto
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.util.Constants
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

// ============================================================================
// API INTERFACES
// ============================================================================

/**
 * Remote API interface for user authentication.
 */
interface AuthApi {
    suspend fun login(request: LoginRequestDto): LoginResponseDto
}

/**
 * Remote API interface for courses and lessons data.
 */
interface CourseApi {
    suspend fun getCourses(): List<CourseDto>
    suspend fun getLessons(courseId: Long): List<LessonDto>
}

/**
 * Unified application API contract combining all feature endpoints.
 */
interface AppLoginViewApi : AuthApi, CourseApi

// ============================================================================
// FAKE / MOCK API IMPLEMENTATIONS
// ============================================================================

/**
 * Fake implementation of [AuthApi] simulating remote authentication with network delay
 * and credential validation against default credentials.
 */
class FakeAuthApi(
    private val simulatedDelayMs: Long = Constants.Auth.LOGIN_DELAY_MS
) : AuthApi {

    /**
     * Flag to simulate remote server errors / downtime for testing.
     */
    var shouldSimulateError: Boolean = false

    override suspend fun login(request: LoginRequestDto): LoginResponseDto {
        if (shouldSimulateError) {
            throw AppError.NetworkError.ServerUnavailable("Authentication server is temporarily unreachable.")
        }

        if (simulatedDelayMs > 0) {
            delay(simulatedDelayMs)
        }

        val trimmedEmail = request.email.trim()
        val password = request.password

        return if (trimmedEmail == Constants.Auth.DEFAULT_EMAIL && password == Constants.Auth.DEFAULT_PASSWORD) {
            LoginResponseDto(
                token = Constants.Auth.MOCK_AUTH_TOKEN,
                email = trimmedEmail,
                success = true
            )
        } else {
            throw AppError.BusinessError.InvalidCredentials()
        }
    }
}

/**
 * Fake implementation of [CourseApi] simulating remote courses and lessons
 * by loading data from local JSON assets.
 */
class FakeCourseApi(
    private val context: Context
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
        if (shouldSimulateError) {
            throw AppError.NetworkError.ServerUnavailable()
        }

        return try {
            val jsonString = context.assets.open(Constants.Remote.ASSET_COURSES_JSON).bufferedReader().use { it.readText() }
            json.decodeFromString<List<CourseDto>>(jsonString)
        } catch (e: Exception) {
            throw AppError.NetworkError.ParseError("Failed to parse courses from remote mock", e)
        }
    }

    override suspend fun getLessons(courseId: Long): List<LessonDto> {
        if (shouldSimulateError) {
            throw AppError.NetworkError.ServerUnavailable("Server error: Unable to fetch lessons for course $courseId")
        }

        return try {
            val jsonString = context.assets.open(Constants.Remote.ASSET_LESSONS_JSON).bufferedReader().use { it.readText() }
            val allLessons = json.decodeFromString<List<LessonDto>>(jsonString)
            allLessons.filter { it.courseId == courseId }
        } catch (e: Exception) {
            throw AppError.NetworkError.ParseError("Failed to parse lessons from remote mock", e)
        }
    }
}
