package com.example.learningdashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.domain.usecase.LoginUseCase
import com.example.learningdashboard.domain.usecase.ObserveNetworkStatusUseCase
import com.example.learningdashboard.domain.usecase.ValidateCredentialsUseCase
import com.example.learningdashboard.presentation.error.toUiError
import com.example.learningdashboard.presentation.login.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val validateCredentialsUseCase: ValidateCredentialsUseCase,
    private val observeNetworkStatusUseCase: ObserveNetworkStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(isOnline = observeNetworkStatusUseCase.isCurrentlyOnline())
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeNetworkStatusUseCase.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
    }

    /**
     * Dynamically validates email formatting as the user types.
     */
    fun onEmailChanged(newEmail: String) {
        val error = validateCredentialsUseCase.validateEmail(newEmail)
        _uiState.update {
            it.copy(
                email = newEmail,
                emailError = error,
                errorMessage = null
            )
        }
    }

    /**
     * Dynamically validates password constraints as the user types.
     */
    fun onPasswordChanged(newPassword: String) {
        val error = validateCredentialsUseCase.validatePassword(newPassword)
        _uiState.update {
            it.copy(
                password = newPassword,
                passwordError = error,
                errorMessage = null
            )
        }
    }

    /**
     * Tracks email field focus to only display validation error when unfocused.
     */
    fun onEmailFocusChanged(isFocused: Boolean) {
        _uiState.update { current ->
            val wasFocused = current.emailHasBeenFocused || isFocused
            val lostFocus = current.emailHasLostFocus || (current.emailHasBeenFocused && !isFocused)
            current.copy(
                isEmailFocused = isFocused,
                emailHasBeenFocused = wasFocused,
                emailHasLostFocus = lostFocus
            )
        }
    }

    /**
     * Tracks password field focus to only display validation error when unfocused.
     */
    fun onPasswordFocusChanged(isFocused: Boolean) {
        _uiState.update { current ->
            val wasFocused = current.passwordHasBeenFocused || isFocused
            val lostFocus = current.passwordHasLostFocus || (current.passwordHasBeenFocused && !isFocused)
            current.copy(
                isPasswordFocused = isFocused,
                passwordHasBeenFocused = wasFocused,
                passwordHasLostFocus = lostFocus
            )
        }
    }

    /**
     * Triggers login through the domain [LoginUseCase].
     * Artificial delays, data calls and business credential validation execute on Dispatchers.IO inside [LoginUseCase].
     */
    fun login() {
        val current = _uiState.value
        val email = current.email.trim()
        val password = current.password

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, submitAttempted = true) }

            try {
                loginUseCase(email, password)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: AppError) {
                val isFieldError = e is AppError.BusinessError.ShortPassword ||
                    e is AppError.BusinessError.EmptyPassword ||
                    e is AppError.BusinessError.InvalidEmail ||
                    e is AppError.BusinessError.EmptyEmail

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (isFieldError) null else e.toUiError().message
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "An unexpected error occurred. Please try again."
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            loginUseCase: LoginUseCase,
            validateCredentialsUseCase: ValidateCredentialsUseCase,
            observeNetworkStatusUseCase: ObserveNetworkStatusUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(
                    loginUseCase = loginUseCase,
                    validateCredentialsUseCase = validateCredentialsUseCase,
                    observeNetworkStatusUseCase = observeNetworkStatusUseCase
                ) as T
            }
        }
    }
}
