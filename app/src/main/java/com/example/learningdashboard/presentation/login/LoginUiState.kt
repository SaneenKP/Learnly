package com.example.learningdashboard.presentation.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isEmailFocused: Boolean = false,
    val isPasswordFocused: Boolean = false,
    val emailHasBeenFocused: Boolean = false,
    val passwordHasBeenFocused: Boolean = false,
    val emailHasLostFocus: Boolean = false,
    val passwordHasLostFocus: Boolean = false,
    val submitAttempted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isOnline: Boolean = true
) {
    /**
     * Email error is only displayed once the email textField has lost focus
     * (or submission was attempted). While actively focused, the error is hidden.
     */
    val displayEmailError: String?
        get() = if (!isEmailFocused && (emailHasLostFocus || submitAttempted)) emailError else null

    /**
     * Password error is only displayed once the password textField has lost focus
     * (or submission was attempted). While actively focused, the error is hidden.
     */
    val displayPasswordError: String?
        get() = if (!isPasswordFocused && (passwordHasLostFocus || submitAttempted)) passwordError else null

    val canSubmit: Boolean
        get() = isOnline && !isLoading && email.isNotBlank() && password.isNotBlank() && emailError == null && passwordError == null
}
