package com.example.learningdashboard.presentation.error

import com.example.learningdashboard.domain.error.AppError

/**
 * UI-specific representation of errors, keeping ViewModels clean and decoupled
 * from low-level network or database exceptions.
 */
sealed interface UiError {
    val message: String

    data class Message(override val message: String) : UiError
}

/**
 * Transforms domain-level [AppError] instances into user-friendly UI presentation errors.
 */
fun AppError.toUiError(): UiError = UiError.Message(
    when (this) {
        is AppError.NetworkError.NoInternet -> "No internet connection. Please reconnect to continue."
        is AppError.NetworkError.ServerUnavailable -> "Server is temporarily unavailable. Please try again later."
        is AppError.NetworkError.ParseError -> "Failed to process data from the server."
        is AppError.NetworkError.Unknown -> message
        is AppError.DataError.DatabaseError -> "Local storage issue encountered."
        is AppError.DataError.NotFound -> "Requested item not found."
        is AppError.BusinessError -> message
    }
)
