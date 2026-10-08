package com.example.learningdashboard.presentation.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningdashboard.domain.repository.CourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseListViewModel(
    private val repository: CourseRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CourseListUiState> = combine(
        repository.observeCourses(),
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
        // Initial load: Fetches from DB first. Only reaches API if DB has no data and device is online.
        loadCourses(forceRefresh = false)
    }

    fun refresh() {
        loadCourses(forceRefresh = true)
    }

    private fun loadCourses(forceRefresh: Boolean) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            try {
                repository.refreshCourses(forceRefresh = forceRefresh)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to refresh courses"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Erases the entire Room database and navigates back to Login screen.
     */
    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.clearAllData()
            } catch (_: Exception) {
            }
            onLoggedOut()
        }
    }
}
