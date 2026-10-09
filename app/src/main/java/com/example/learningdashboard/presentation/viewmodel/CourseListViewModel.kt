package com.example.learningdashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.domain.usecase.GetCoursesUseCase
import com.example.learningdashboard.domain.usecase.LogoutUseCase
import com.example.learningdashboard.domain.usecase.ObserveNetworkStatusUseCase
import com.example.learningdashboard.domain.usecase.RefreshCoursesUseCase
import com.example.learningdashboard.presentation.courses.CourseListUiState
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

class CourseListViewModel(
    private val getCoursesUseCase: GetCoursesUseCase,
    private val refreshCoursesUseCase: RefreshCoursesUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val observeNetworkStatusUseCase: ObserveNetworkStatusUseCase
) : ViewModel() {

    private val _isInitialLoading = MutableStateFlow(true)
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _showNetworkDialog = MutableStateFlow(false)
    val showNetworkDialog: StateFlow<Boolean> = _showNetworkDialog.asStateFlow()

    private var networkDialogJob: Job? = null

    val isOnline: StateFlow<Boolean> = observeNetworkStatusUseCase.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = observeNetworkStatusUseCase.isCurrentlyOnline()
        )

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
     * If network is offline, logout is not allowed and the 3-second network unavailable dialog is displayed.
     */
    fun logout(onLoggedOut: () -> Unit) {
        if (!observeNetworkStatusUseCase.isCurrentlyOnline()) {
            showNetworkUnavailableDialog()
            return
        }

        viewModelScope.launch {
            try {
                logoutUseCase()
            } catch (_: Exception) {
            }
            onLoggedOut()
        }
    }

    /**
     * Displays the network unavailable dialog and schedules automatic dismissal after 3 seconds.
     * Can be invoked repeatedly upon subsequent offline logout attempts.
     */
    fun showNetworkUnavailableDialog() {
        _showNetworkDialog.value = true
        networkDialogJob?.cancel()
        networkDialogJob = viewModelScope.launch {
            delay(Constants.Network.DIALOG_AUTO_DISMISS_DELAY_MS)
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
            getCoursesUseCase: GetCoursesUseCase,
            refreshCoursesUseCase: RefreshCoursesUseCase,
            logoutUseCase: LogoutUseCase,
            observeNetworkStatusUseCase: ObserveNetworkStatusUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CourseListViewModel(
                    getCoursesUseCase = getCoursesUseCase,
                    refreshCoursesUseCase = refreshCoursesUseCase,
                    logoutUseCase = logoutUseCase,
                    observeNetworkStatusUseCase = observeNetworkStatusUseCase
                ) as T
            }
        }
    }
}
