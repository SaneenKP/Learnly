package com.example.learningdashboard.presentation.courses

import com.example.learningdashboard.domain.model.Course

sealed interface CourseListUiState {

    data object Loading : CourseListUiState

    data class Success(
        val courses: List<Course>,
        val userMessage: String? = null,
        val isRefreshing: Boolean = false
    ) : CourseListUiState

    data class Empty(
        val message: String = "No courses found."
    ) : CourseListUiState

    data class Error(
        val message: String
    ) : CourseListUiState
}
