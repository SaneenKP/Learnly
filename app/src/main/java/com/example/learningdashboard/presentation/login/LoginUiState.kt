package com.example.learningdashboard.presentation.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isOnline: Boolean = true
) {
    val canSubmit: Boolean
        get() = isOnline && !isLoading && email.isNotBlank() && password.isNotBlank() && emailError == null && passwordError == null
}
