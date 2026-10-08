package com.example.learningdashboard.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.learningdashboard.data.util.NetworkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val networkManager: NetworkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(isOnline = networkManager.isCurrentlyOnline())
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    init {
        viewModelScope.launch {
            networkManager.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    fun onEmailChanged(newEmail: String) {
        val error = validateEmail(newEmail)
        _uiState.update {
            it.copy(
                email = newEmail,
                emailError = error,
                errorMessage = null
            )
        }
    }

    fun onPasswordChanged(newPassword: String) {
        val error = validatePassword(newPassword)
        _uiState.update {
            it.copy(
                password = newPassword,
                passwordError = error,
                errorMessage = null
            )
        }
    }

    private fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isBlank() -> "Email cannot be empty"
            !emailRegex.matches(trimmed) -> "Please enter a valid email address (e.g. user@domain.com)"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password cannot be empty"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }

    fun login() {
        val current = _uiState.value
        if (!current.isOnline) {
            _uiState.update { it.copy(errorMessage = "Internet connection required to log in.") }
            return
        }

        val email = current.email.trim()
        val password = current.password

        val emailErr = validateEmail(email)
        val passwordErr = validatePassword(password)

        if (emailErr != null || passwordErr != null) {
            _uiState.update {
                it.copy(
                    emailError = emailErr,
                    passwordError = passwordErr,
                    errorMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, emailError = null, passwordError = null) }

            // Realistic dummy loading simulation
            delay(1200)

            if (email == REQUIRED_EMAIL && password == REQUIRED_PASSWORD) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Invalid credentials. Use student@university.edu / password123"
                    )
                }
            }
        }
    }

    companion object {
        const val REQUIRED_EMAIL = "student@university.edu"
        const val REQUIRED_PASSWORD = "password123"

        fun provideFactory(networkManager: NetworkManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoginViewModel(networkManager) as T
                }
            }
    }
}
