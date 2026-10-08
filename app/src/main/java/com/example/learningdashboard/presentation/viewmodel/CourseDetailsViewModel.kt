package com.example.learningdashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.domain.usecase.GetCourseDetailsUseCase
import com.example.learningdashboard.domain.usecase.RefreshCourseDetailsUseCase
import com.example.learningdashboard.domain.usecase.ToggleLessonCompletionUseCase
import com.example.learningdashboard.presentation.details.CourseDetailsUiState
import com.example.learningdashboard.presentation.error.toUiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseDetailsViewModel(
    private val courseId: Long,
    private val getCourseDetailsUseCase: GetCourseDetailsUseCase,
    private val refreshCourseDetailsUseCase: RefreshCourseDetailsUseCase,
    private val toggleLessonCompletionUseCase: ToggleLessonCompletionUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CourseDetailsUiState> = combine(
        getCourseDetailsUseCase.observeCourse(courseId),
        getCourseDetailsUseCase.observeLessons(courseId),
        _isLoading,
        _errorMessage
    ) { course, lessons, isLoading, error ->
        when {
            isLoading -> CourseDetailsUiState.Loading
            course != null -> CourseDetailsUiState.Success(
                course = course,
                lessons = lessons
            )
            error != null -> CourseDetailsUiState.Error(error)
            else -> CourseDetailsUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CourseDetailsUiState.Loading
    )

    init {
        // DB-First: Loads cached lessons; only hits remote API if not cached locally
        refresh(forceRefresh = false)
    }

    fun refresh(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                refreshCourseDetailsUseCase(courseId, forceRefresh = forceRefresh)
            } catch (e: AppError) {
                if (uiState.value !is CourseDetailsUiState.Success) {
                    _errorMessage.value = e.toUiError().message
                }
            } catch (e: Exception) {
                if (uiState.value !is CourseDetailsUiState.Success) {
                    _errorMessage.value = "Failed to load course details"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleLessonCompletion(lessonId: Long, completed: Boolean) {
        viewModelScope.launch {
            try {
                toggleLessonCompletionUseCase(courseId, lessonId, completed)
            } catch (e: AppError) {
                _errorMessage.value = e.toUiError().message
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update lesson: ${e.message}"
            }
        }
    }

    companion object {
        fun provideFactory(
            courseId: Long,
            getCourseDetailsUseCase: GetCourseDetailsUseCase,
            refreshCourseDetailsUseCase: RefreshCourseDetailsUseCase,
            toggleLessonCompletionUseCase: ToggleLessonCompletionUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CourseDetailsViewModel(
                    courseId = courseId,
                    getCourseDetailsUseCase = getCourseDetailsUseCase,
                    refreshCourseDetailsUseCase = refreshCourseDetailsUseCase,
                    toggleLessonCompletionUseCase = toggleLessonCompletionUseCase
                ) as T
            }
        }
    }
}
