package com.example.learningdashboard.presentation.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.learningdashboard.presentation.components.CourseCard
import com.example.learningdashboard.presentation.components.EmptyState
import com.example.learningdashboard.presentation.components.ErrorState
import com.example.learningdashboard.presentation.components.LoadingState

@Composable
fun CourseListRoute(
    viewModel: CourseListViewModel,
    onCourseClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CourseListScreen(
        state = state,
        onCourseClick = onCourseClick,
        onRetry = viewModel::refresh,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    state: CourseListUiState,
    onCourseClick: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                is CourseListUiState.Loading -> {
                    LoadingState()
                }

                is CourseListUiState.Empty -> {
                    EmptyState(
                        message = "No courses found. Try refreshing.",
                        onRefresh = onRetry
                    )
                }

                is CourseListUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = onRetry
                    )
                }

                is CourseListUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Display non-blocking banner if network refresh failed while offline
                        if (state.userMessage != null) {
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = "Offline Mode: Showing cached courses.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items = state.courses, key = { it.id }) { course ->
                                CourseCard(
                                    course = course,
                                    onContinueClick = onCourseClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
