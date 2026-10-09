package com.example.learningdashboard.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.learningdashboard.di.AppContainer
import com.example.learningdashboard.di.LocalAppContainer
import com.example.learningdashboard.presentation.courses.CourseListRoute
import com.example.learningdashboard.presentation.details.CourseDetailsRoute
import com.example.learningdashboard.presentation.login.LoginRoute
import com.example.learningdashboard.util.Constants

object AppRoutes {
    const val LOGIN = Constants.Navigation.ROUTE_LOGIN
    const val COURSES = Constants.Navigation.ROUTE_COURSES
    const val COURSE_DETAILS = Constants.Navigation.ROUTE_COURSE_DETAILS

    fun courseDetails(courseId: Long): String = Constants.Navigation.courseDetailsRoute(courseId)
}

@Composable
fun AppNavHost(
    appContainer: AppContainer = LocalAppContainer.current,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val isUserLoggedIn by appContainer.getAuthStateUseCase()
        .collectAsStateWithLifecycle(initialValue = null)

    // Wait until DataStore emits stored login state to determine initial startDestination
    if (isUserLoggedIn == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (isUserLoggedIn == true) AppRoutes.COURSES else AppRoutes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(AppRoutes.LOGIN) {
            LoginRoute(
                onNavigateToDashboard = {
                    navController.navigate(AppRoutes.COURSES) {
                        popUpTo(AppRoutes.LOGIN) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(AppRoutes.COURSES) {
            CourseListRoute(
                onCourseClick = { courseId ->
                    navController.navigate(AppRoutes.courseDetails(courseId))
                },
                onLogout = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            route = AppRoutes.COURSE_DETAILS,
            arguments = listOf(
                navArgument(Constants.Navigation.ARG_COURSE_ID) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong(Constants.Navigation.ARG_COURSE_ID) ?: return@composable
            CourseDetailsRoute(
                courseId = courseId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
