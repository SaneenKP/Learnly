package com.example.learningdashboard.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.learningdashboard.di.AppContainer
import com.example.learningdashboard.presentation.courses.CourseListRoute
import com.example.learningdashboard.presentation.courses.CourseListViewModel
import com.example.learningdashboard.presentation.details.CourseDetailsRoute
import com.example.learningdashboard.presentation.details.CourseDetailsViewModel
import com.example.learningdashboard.presentation.login.LoginRoute
import com.example.learningdashboard.presentation.login.LoginViewModel

object AppRoutes {
    const val LOGIN = "login"
    const val COURSES = "courses"
    const val COURSE_DETAILS = "course/{courseId}"

    fun courseDetails(courseId: Long): String = "course/$courseId"
}

@Composable
fun AppNavHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.LOGIN,
        modifier = modifier
    ) {
        composable(AppRoutes.LOGIN) {
            val loginViewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.provideFactory(appContainer.networkManager)
            )
            LoginRoute(
                viewModel = loginViewModel,
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
            val courseListViewModel: CourseListViewModel = viewModel {
                CourseListViewModel(appContainer.courseRepository)
            }
            CourseListRoute(
                viewModel = courseListViewModel,
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
                navArgument("courseId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            val courseDetailsViewModel: CourseDetailsViewModel = viewModel(
                factory = CourseDetailsViewModel.provideFactory(courseId, appContainer.courseRepository)
            )
            CourseDetailsRoute(
                viewModel = courseDetailsViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
