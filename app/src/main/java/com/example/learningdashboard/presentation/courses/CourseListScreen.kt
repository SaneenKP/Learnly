package com.example.learningdashboard.presentation.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.learningdashboard.presentation.components.CourseCard
import com.example.learningdashboard.presentation.components.EmptyState
import com.example.learningdashboard.presentation.components.ErrorState
import com.example.learningdashboard.presentation.components.LoadingState
import com.example.learningdashboard.presentation.viewmodel.CourseListViewModel

@Composable
fun CourseListRoute(
    viewModel: CourseListViewModel,
    onCourseClick: (Long) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CourseListScreen(
        state = state,
        onCourseClick = onCourseClick,
        onRetry = viewModel::refresh,
        onLogoutClick = {
            viewModel.logout(onLoggedOut = onLogout)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    state: CourseListUiState,
    onCourseClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Logging out will erase all cached course and lesson data from the local database. Continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Dashboard") },
                actions = {
                    IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    TextButton(onClick = { showLogoutDialog = true }) {
                        Text(
                            text = "Logout",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                        message = state.message,
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
                        // Dummy loading progress indicator when refreshing from API
                        if (state.isRefreshing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        // Display non-blocking banner if network refresh failed while offline
                        if (state.userMessage != null) {
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = state.userMessage,
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
