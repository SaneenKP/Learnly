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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseListViewModel(
    private val getCoursesUseCase: GetCoursesUseCase,
    private val refreshCoursesUseCase: RefreshCoursesUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CourseListUiState> = combine(
        getCoursesUseCase(),
        _isRefreshing,
        _errorMessage
    ) { courses, isRefreshing, error ->
        when {
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
        // Initial load: fetches from DB first. If empty, UseCase attempts remote fetch if online.
        loadCourses(forceRefresh = false)
    }

    fun refresh() {
        loadCourses(forceRefresh = true)
    }

    private fun loadCourses(forceRefresh: Boolean) {
        viewModelScope.launch(dispatcher) {
            _isRefreshing.value = true
            _errorMessage.value = null

            try {
                refreshCoursesUseCase(forceRefresh = forceRefresh)
            } catch (e: AppError) {
                _errorMessage.value = e.toUiError().message
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh courses: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Erases the entire Room database via domain LogoutUseCase and invokes navigation callback.
     */
    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch(dispatcher) {
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
            logoutUseCase: LogoutUseCase,
            dispatcher: CoroutineDispatcher = Dispatchers.Main
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CourseListViewModel(
                    getCoursesUseCase = getCoursesUseCase,
                    refreshCoursesUseCase = refreshCoursesUseCase,
                    logoutUseCase = logoutUseCase,
                    dispatcher = dispatcher
                ) as T
            }
        }
    }
}
