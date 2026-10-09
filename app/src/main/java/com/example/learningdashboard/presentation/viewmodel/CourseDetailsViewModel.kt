package com.example.learningdashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.domain.usecase.GetCourseDetailsUseCase
import com.example.learningdashboard.domain.usecase.ObserveNetworkStatusUseCase
import com.example.learningdashboard.domain.usecase.RefreshCourseDetailsUseCase
import com.example.learningdashboard.domain.usecase.SetAllLessonsCompletionUseCase
import com.example.learningdashboard.domain.usecase.ToggleLessonCompletionUseCase
import com.example.learningdashboard.presentation.details.CourseDetailsUiState
import com.example.learningdashboard.presentation.error.toUiError
import com.example.learningdashboard.util.Constants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class CourseDetailsViewModel(
    private val courseId: Long,
    private val getCourseDetailsUseCase: GetCourseDetailsUseCase,
    private val refreshCourseDetailsUseCase: RefreshCourseDetailsUseCase,
    private val toggleLessonCompletionUseCase: ToggleLessonCompletionUseCase,
    private val setAllLessonsCompletionUseCase: SetAllLessonsCompletionUseCase,
    private val observeNetworkStatusUseCase: ObserveNetworkStatusUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _showNetworkDialog = MutableStateFlow(false)
    val showNetworkDialog: StateFlow<Boolean> = _showNetworkDialog.asStateFlow()

    private var networkDialogJob: Job? = null

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

        // Show network unavailable dialog for 3 seconds if initially offline
        if (!observeNetworkStatusUseCase.isCurrentlyOnline()) {
            showNetworkUnavailableDialog()
        }

        // Show network unavailable dialog whenever connectivity is lost while on screen
        viewModelScope.launch {
            observeNetworkStatusUseCase.isOnline.collect { online ->
                if (!online) {
                    showNetworkUnavailableDialog()
                }
            }
        }
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

    fun selectAllLessons() {
        viewModelScope.launch {
            try {
                setAllLessonsCompletionUseCase(courseId, completed = true)
            } catch (e: AppError) {
                _errorMessage.value = e.toUiError().message
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update lessons: ${e.message}"
            }
        }
    }

    fun clearAllLessons() {
        viewModelScope.launch {
            try {
                setAllLessonsCompletionUseCase(courseId, completed = false)
            } catch (e: AppError) {
                _errorMessage.value = e.toUiError().message
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update lessons: ${e.message}"
            }
        }
    }

    /**
     * Displays the network unavailable dialog and schedules automatic dismissal after 3 seconds.
     */
    fun showNetworkUnavailableDialog() {
        _showNetworkDialog.value = true
        networkDialogJob?.cancel()
        networkDialogJob = viewModelScope.launch {
            delay(Constants.Network.DIALOG_AUTO_DISMISS_DELAY_MS.milliseconds)
            _showNetworkDialog.value = false
        }
    }

    /**
     * Manually dismisses the network unavailable dialog.
     */
    fun dismissNetworkDialog() {
        networkDialogJob?.cancel()
        _showNetworkDialog.value = false
    }

    companion object {
        fun provideFactory(
            courseId: Long,
            getCourseDetailsUseCase: GetCourseDetailsUseCase,
            refreshCourseDetailsUseCase: RefreshCourseDetailsUseCase,
            toggleLessonCompletionUseCase: ToggleLessonCompletionUseCase,
            setAllLessonsCompletionUseCase: SetAllLessonsCompletionUseCase,
            observeNetworkStatusUseCase: ObserveNetworkStatusUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CourseDetailsViewModel(
                    courseId = courseId,
                    getCourseDetailsUseCase = getCourseDetailsUseCase,
                    refreshCourseDetailsUseCase = refreshCourseDetailsUseCase,
                    toggleLessonCompletionUseCase = toggleLessonCompletionUseCase,
                    setAllLessonsCompletionUseCase = setAllLessonsCompletionUseCase,
                    observeNetworkStatusUseCase = observeNetworkStatusUseCase
                ) as T
            }
        }
    }
}
