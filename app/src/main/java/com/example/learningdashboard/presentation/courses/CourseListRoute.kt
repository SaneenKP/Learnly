package com.example.learningdashboard.presentation.courses

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.learningdashboard.di.AppContainer
import com.example.learningdashboard.di.LocalAppContainer
import com.example.learningdashboard.presentation.viewmodel.CourseListViewModel

@Composable
fun CourseListRoute(
    onCourseClick: (Long) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    appContainer: AppContainer = LocalAppContainer.current,
    viewModel: CourseListViewModel = viewModel(
        factory = CourseListViewModel.provideFactory(
            getCoursesUseCase = appContainer.getCoursesUseCase,
            refreshCoursesUseCase = appContainer.refreshCoursesUseCase,
            logoutUseCase = appContainer.logoutUseCase,
            observeNetworkStatusUseCase = appContainer.observeNetworkStatusUseCase
        )
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showNetworkDialog by viewModel.showNetworkDialog.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    CourseListScreen(
        state = state,
        showNetworkDialog = showNetworkDialog,
        isOnline = isOnline,
        onCourseClick = onCourseClick,
        onRetry = viewModel::refresh,
        onOfflineLogoutAttempt = viewModel::showNetworkUnavailableDialog,
        onDismissNetworkDialog = viewModel::dismissNetworkDialog,
        onLogoutClick = {
            viewModel.logout(onLoggedOut = onLogout)
        },
        modifier = modifier
    )
}
