package com.example.learningdashboard.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.learningdashboard.domain.repository.CourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseDetailsViewModel(
    private val courseId: Long,
    private val repository: CourseRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CourseDetailsUiState> = combine(
        repository.observeCourse(courseId),
        repository.observeLessons(courseId),
        _errorMessage
    ) { course, lessons, error ->
        when {
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
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                repository.refreshLessons(courseId)
            } catch (e: Exception) {
                if (uiState.value !is CourseDetailsUiState.Success) {
                    _errorMessage.value = e.message ?: "Failed to load course details"
                }
            }
        }
    }

    fun toggleLessonCompletion(lessonId: Long, completed: Boolean) {
        viewModelScope.launch {
            try {
                repository.markLessonCompleted(courseId, lessonId, completed)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update lesson: ${e.message}"
            }
        }
    }

    companion object {
        fun provideFactory(
            courseId: Long,
            repository: CourseRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CourseDetailsViewModel(courseId, repository) as T
            }
        }
    }
}
