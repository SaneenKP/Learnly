package com.example.learningdashboard.domain.usecase

interface ValidateCredentialsUseCase {
    fun validateEmail(email: String): String?
    fun validatePassword(password: String): String?
}

class ValidateCredentialsUseCaseImpl : ValidateCredentialsUseCase {

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

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
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }
}