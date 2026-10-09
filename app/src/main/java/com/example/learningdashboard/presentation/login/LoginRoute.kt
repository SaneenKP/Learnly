package com.example.learningdashboard.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.learningdashboard.di.AppContainer
import com.example.learningdashboard.di.LocalAppContainer
import com.example.learningdashboard.presentation.viewmodel.LoginViewModel

@Composable
fun LoginRoute(
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    appContainer: AppContainer = LocalAppContainer.current,
    viewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.provideFactory(
            loginUseCase = appContainer.loginUseCase,
            validateCredentialsUseCase = appContainer.validateCredentialsUseCase,
            observeNetworkStatusUseCase = appContainer.observeNetworkStatusUseCase
        )
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onNavigateToDashboard()
        }
    }

    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChanged,
        onPasswordChange = viewModel::onPasswordChanged,
        onEmailFocusChange = viewModel::onEmailFocusChanged,
        onPasswordFocusChange = viewModel::onPasswordFocusChanged,
        onLoginClick = viewModel::login,
        modifier = modifier
    )
}
