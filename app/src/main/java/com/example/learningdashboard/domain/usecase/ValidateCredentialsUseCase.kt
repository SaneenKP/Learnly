package com.example.learningdashboard.domain.usecase

import com.example.learningdashboard.util.Constants

interface ValidateCredentialsUseCase {
    fun validateEmail(email: String): String?
    fun validatePassword(password: String): String?
}

class ValidateCredentialsUseCaseImpl : ValidateCredentialsUseCase {

    private val emailRegex = Regex(Constants.Validation.EMAIL_REGEX_PATTERN)

    override fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isBlank() -> "Email cannot be empty"
            !emailRegex.matches(trimmed) -> "Please enter a valid email address (e.g. user@domain.com)"
            else -> null
        }
    }

    override fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password cannot be empty"
            password.length < Constants.Validation.MIN_PASSWORD_LENGTH ->
                "Password must be at least ${Constants.Validation.MIN_PASSWORD_LENGTH} characters"
            else -> null
        }
    }
}