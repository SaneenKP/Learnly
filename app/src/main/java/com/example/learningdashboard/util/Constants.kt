package com.example.learningdashboard.util

/**
 * Centralized application constants categorized into logical groupings.
 */
object Constants {

    object Auth {
        const val DEFAULT_EMAIL = "testing@gmail.com"
        const val DEFAULT_PASSWORD = "testing123"
        const val LOGIN_DELAY_MS = 1200L
        const val MOCK_AUTH_TOKEN = "mock_jwt_token_learnly_dashboard"
    }

    object Validation {
        const val MIN_PASSWORD_LENGTH = 6
        const val EMAIL_REGEX_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    }

    object Database {
        const val DATABASE_NAME = "learning_dashboard.db"
        const val DATABASE_VERSION = 1
        const val TABLE_COURSES = "courses"
        const val TABLE_LESSONS = "lessons"
    }

    object Preferences {
        const val DATASTORE_NAME = "user_preferences"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    object Remote {
        const val ASSET_COURSES_JSON = "courses.json"
        const val ASSET_LESSONS_JSON = "lessons.json"
        const val NETWORK_DELAY_MS = 600L
    }

    object Navigation {
        const val ROUTE_LOGIN = "login"
        const val ROUTE_COURSES = "courses"
        const val ROUTE_COURSE_DETAILS = "course/{courseId}"
        const val ARG_COURSE_ID = "courseId"

        fun courseDetailsRoute(courseId: Long): String = "course/$courseId"
    }

    object Network {
        const val DIALOG_AUTO_DISMISS_DELAY_MS = 3000L
        const val NETWORK_UNAVAILABLE_TITLE = "No Internet Connection"
        const val NETWORK_UNAVAILABLE_MESSAGE = "Network is not available. Please check your connection."
    }
}
