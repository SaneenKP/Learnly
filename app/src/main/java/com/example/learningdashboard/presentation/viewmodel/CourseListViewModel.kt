package com.example.learningdashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.domain.usecase.GetCoursesUseCase
import com.example.learningdashboard.domain.usecase.LogoutUseCase
import com.example.learningdashboard.domain.usecase.RefreshCoursesUseCase
import com.example.learningdashboard.presentation.courses.CourseListUiState
import com.example.learningdashboard.presentation.error.toUiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseListViewModel(
    private val getCoursesUseCase: GetCoursesUseCase,
    private val refreshCoursesUseCase: RefreshCoursesUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _isInitialLoading = MutableStateFlow(true)
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CourseListUiState> = combine(
        getCoursesUseCase(),
        _isInitialLoading,
        _isRefreshing,
        _errorMessage
    ) { courses, isInitialLoading, isRefreshing, error ->
        when {
            // Show initial loading while fetching details from the API on course screen
            isInitialLoading -> CourseListUiState.Loading
            courses.isNotEmpty() -> {
                CourseListUiState.Success(
                    courses = courses,
                    userMessage = error,
                    isRefreshing = isRefreshing
                )
            }
            isRefreshing -> CourseListUiState.Loading
            error != null -> CourseListUiState.Error(error)
            else -> CourseListUiState.Empty(
                message = "No courses cached locally. Connect to internet and refresh to download courses."
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CourseListUiState.Loading
    )

    init {
        // Initial load: displays initial loading indicator while fetching details from the API
        loadCourses(isInitial = true, forceRefresh = false)
    }

    fun refresh() {
        loadCourses(isInitial = false, forceRefresh = true)
    }

    private fun loadCourses(isInitial: Boolean, forceRefresh: Boolean) {
        viewModelScope.launch {
            if (isInitial) {
                _isInitialLoading.value = true
            } else {
                _isRefreshing.value = true
            }
            _errorMessage.value = null

            try {
                refreshCoursesUseCase(forceRefresh = forceRefresh)
            } catch (e: AppError) {
                _errorMessage.value = e.toUiError().message
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh courses: ${e.message}"
            } finally {
                _isInitialLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Erases the entire Room database and updates DataStore login state via domain LogoutUseCase.
     */
    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            try {
                logoutUseCase()
            } catch (_: Exception) {
            }
            onLoggedOut()
        }
    }

    companion object {
        fun provideFactory(
            getCoursesUseCase: GetCoursesUseCase,
            refreshCoursesUseCase: RefreshCoursesUseCase,
            logoutUseCase: LogoutUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CourseListViewModel(
                    getCoursesUseCase = getCoursesUseCase,
                    refreshCoursesUseCase = refreshCoursesUseCase,
                    logoutUseCase = logoutUseCase
                ) as T
            }
        }
    }
}
