package com.example.learningdashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.learningdashboard.di.AppContainer
import com.example.learningdashboard.navigation.AppNavHost
import com.example.learningdashboard.ui.theme.LearningDashboardTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as LearningDashboardApplication).container

        setContent {
            LearningDashboardApp(appContainer = appContainer)
        }
    }
}

@Composable
fun LearningDashboardApp(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    LearningDashboardTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavHost(appContainer = appContainer)
        }
    }
}
