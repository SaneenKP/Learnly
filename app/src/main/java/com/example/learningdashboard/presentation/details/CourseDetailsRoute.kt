package com.example.learningdashboard.presentation.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.learningdashboard.di.AppContainer
import com.example.learningdashboard.di.LocalAppContainer
import com.example.learningdashboard.presentation.viewmodel.CourseDetailsViewModel

@Composable
fun CourseDetailsRoute(
    courseId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    appContainer: AppContainer = LocalAppContainer.current,
    viewModel: CourseDetailsViewModel = viewModel(
        key = "course_details_$courseId",
        factory = CourseDetailsViewModel.provideFactory(
            courseId = courseId,
            getCourseDetailsUseCase = appContainer.getCourseDetailsUseCase,
            refreshCourseDetailsUseCase = appContainer.refreshCourseDetailsUseCase,
            toggleLessonCompletionUseCase = appContainer.toggleLessonCompletionUseCase,
            setAllLessonsCompletionUseCase = appContainer.setAllLessonsCompletionUseCase,
            observeNetworkStatusUseCase = appContainer.observeNetworkStatusUseCase
        )
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showNetworkDialog by viewModel.showNetworkDialog.collectAsStateWithLifecycle()

    CourseDetailsScreen(
        state = state,
        showNetworkDialog = showNetworkDialog,
        onDismissNetworkDialog = viewModel::dismissNetworkDialog,
        onToggleLesson = viewModel::toggleLessonCompletion,
        onSelectAll = viewModel::selectAllLessons,
        onClearAll = viewModel::clearAllLessons,
        onBackClick = onBackClick,
        onRetry = viewModel::refresh,
        modifier = modifier
    )
}
