package com.example.learningdashboard.domain.error

/**
 * Root sealed class representing errors across the application architecture.
 *
 * Specific subtypes are isolated by architecture layer:
 * - [NetworkError]: Thrown and mapped in the API / remote layer.
 * - [DataError]: Thrown and mapped in the Repository / database layer.
 * - [BusinessError]: Thrown and mapped in the Domain / UseCase layer.
 */
sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * Errors originating from the Network / API layer.
     */
    sealed class NetworkError(
        message: String,
        cause: Throwable? = null
    ) : AppError(message, cause) {
        data class NoInternet(
            override val message: String = "No internet connection available."
        ) : NetworkError(message)

        data class ServerUnavailable(
            override val message: String = "Server unavailable (503). Please try again later."
        ) : NetworkError(message)

        data class ParseError(
            override val message: String = "Failed to parse remote server response.",
            override val cause: Throwable? = null
        ) : NetworkError(message, cause)

        data class Unknown(
            override val message: String = "An unexpected network error occurred.",
            override val cause: Throwable? = null
        ) : NetworkError(message, cause)
    }

    /**
     * Errors originating from the Data / Persistence / Repository layer.
     */
    sealed class DataError(
        message: String,
        cause: Throwable? = null
    ) : AppError(message, cause) {
        data class DatabaseError(
            override val message: String = "Local database operation failed.",
            override val cause: Throwable? = null
        ) : DataError(message, cause)

        data class NotFound(
            override val message: String = "Requested data was not found in local cache."
        ) : DataError(message)
    }

    /**
     * Errors originating from Domain / Business Logic (UseCases).
     */
    sealed class BusinessError(
        message: String,
        cause: Throwable? = null
    ) : AppError(message, cause) {
        data class InvalidCredentials(
            override val message: String = "Invalid credentials. Use student@university.edu / password123"
        ) : BusinessError(message)

        data class InvalidEmail(
            override val message: String = "Please enter a valid email address (e.g. user@domain.com)"
        ) : BusinessError(message)

        data class EmptyEmail(
            override val message: String = "Email cannot be empty"
        ) : BusinessError(message)

        data class ShortPassword(
            override val message: String = "Password must be at least 6 characters"
        ) : BusinessError(message)

        data class EmptyPassword(
            override val message: String = "Password cannot be empty"
        ) : BusinessError(message)

        data class OfflineActionNotAllowed(
            override val message: String = "Internet connection required to perform this action."
        ) : BusinessError(message)

        data class CourseNotFound(
            override val message: String = "Course not found."
        ) : BusinessError(message)
    }
}
