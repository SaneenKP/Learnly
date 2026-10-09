package com.example.learningdashboard.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.learningdashboard.presentation.components.AppProgressBar
import com.example.learningdashboard.presentation.components.ErrorState
import com.example.learningdashboard.presentation.components.LessonItem
import com.example.learningdashboard.presentation.components.LoadingState
import com.example.learningdashboard.presentation.components.NetworkUnavailableDialog
import com.example.learningdashboard.presentation.viewmodel.CourseDetailsViewModel

@Composable
fun CourseDetailsRoute(
    viewModel: CourseDetailsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailsScreen(
    state: CourseDetailsUiState,
    showNetworkDialog: Boolean = false,
    onDismissNetworkDialog: () -> Unit = {},
    onToggleLesson: (Long, Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showNetworkDialog) {
        NetworkUnavailableDialog(onDismiss = onDismissNetworkDialog)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state) {
                is CourseDetailsUiState.Loading -> {
                    LoadingState()
                }

                is CourseDetailsUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = onRetry
                    )
                }

                is CourseDetailsUiState.Success -> {
                    val course = state.course
                    val lessons = state.lessons

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = course.title,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Instructor: ${course.instructor}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                AppProgressBar(
                                    progressPercent = course.progress,
                                    showLabel = true,
                                    label = "Overall Progress (${course.completedLessons}/${course.totalLessons} Completed)"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Lesson actions header with Select All and Clear All buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Course Lessons (${lessons.size})",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = onSelectAll,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Select All",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                TextButton(
                                    onClick = onClearAll,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Clear All",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items = lessons, key = { it.id }) { lesson ->
                                LessonItem(
                                    lesson = lesson,
                                    onToggleCompletion = onToggleLesson
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
