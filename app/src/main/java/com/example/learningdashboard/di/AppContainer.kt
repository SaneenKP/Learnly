package com.example.learningdashboard.di

import android.content.Context
import com.example.learningdashboard.data.local.AppDatabase
import com.example.learningdashboard.data.local.dao.CourseDao
import com.example.learningdashboard.data.local.dao.LessonDao
import com.example.learningdashboard.data.local.datastore.UserPreferencesRepositoryImpl
import com.example.learningdashboard.data.remote.AuthApi
import com.example.learningdashboard.data.remote.CourseApi
import com.example.learningdashboard.data.remote.FakeAuthApi
import com.example.learningdashboard.data.remote.FakeCourseApi
import com.example.learningdashboard.data.repository.AuthRepositoryImpl
import com.example.learningdashboard.data.repository.CourseRepositoryImpl
import com.example.learningdashboard.data.util.DefaultNetworkManager
import com.example.learningdashboard.data.util.NetworkManager
import com.example.learningdashboard.domain.repository.AuthRepository
import com.example.learningdashboard.domain.repository.CourseRepository
import com.example.learningdashboard.domain.repository.UserPreferencesRepository
import com.example.learningdashboard.domain.usecase.GetAuthStateUseCase
import com.example.learningdashboard.domain.usecase.GetAuthStateUseCaseImpl
import com.example.learningdashboard.domain.usecase.GetCourseDetailsUseCase
import com.example.learningdashboard.domain.usecase.GetCourseDetailsUseCaseImpl
import com.example.learningdashboard.domain.usecase.GetCoursesUseCase
import com.example.learningdashboard.domain.usecase.GetCoursesUseCaseImpl
import com.example.learningdashboard.domain.usecase.LoginUseCase
import com.example.learningdashboard.domain.usecase.LoginUseCaseImpl
import com.example.learningdashboard.domain.usecase.LogoutUseCase
import com.example.learningdashboard.domain.usecase.LogoutUseCaseImpl
import com.example.learningdashboard.domain.usecase.ObserveNetworkStatusUseCase
import com.example.learningdashboard.domain.usecase.ObserveNetworkStatusUseCaseImpl
import com.example.learningdashboard.domain.usecase.RefreshCourseDetailsUseCase
import com.example.learningdashboard.domain.usecase.RefreshCourseDetailsUseCaseImpl
import com.example.learningdashboard.domain.usecase.RefreshCoursesUseCase
import com.example.learningdashboard.domain.usecase.RefreshCoursesUseCaseImpl
import com.example.learningdashboard.domain.usecase.ToggleLessonCompletionUseCase
import com.example.learningdashboard.domain.usecase.ToggleLessonCompletionUseCaseImpl
import com.example.learningdashboard.domain.usecase.ValidateCredentialsUseCase
import com.example.learningdashboard.domain.usecase.ValidateCredentialsUseCaseImpl

interface AppContainer {
    val database: AppDatabase
    val courseDao: CourseDao
    val lessonDao: LessonDao
    val courseApi: CourseApi
    val authApi: AuthApi
    val networkManager: NetworkManager
    val courseRepository: CourseRepository
    val authRepository: AuthRepository
    val userPreferencesRepository: UserPreferencesRepository

    // Domain Use Cases
    val getAuthStateUseCase: GetAuthStateUseCase
    val validateCredentialsUseCase: ValidateCredentialsUseCase
    val loginUseCase: LoginUseCase
    val observeNetworkStatusUseCase: ObserveNetworkStatusUseCase
    val getCoursesUseCase: GetCoursesUseCase
    val refreshCoursesUseCase: RefreshCoursesUseCase
    val getCourseDetailsUseCase: GetCourseDetailsUseCase
    val refreshCourseDetailsUseCase: RefreshCourseDetailsUseCase
    val toggleLessonCompletionUseCase: ToggleLessonCompletionUseCase
    val logoutUseCase: LogoutUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val courseDao: CourseDao by lazy {
        database.courseDao()
    }

    override val lessonDao: LessonDao by lazy {
        database.lessonDao()
    }

    override val courseApi: CourseApi by lazy {
        FakeCourseApi(context)
    }

    override val authApi: AuthApi by lazy {
        FakeAuthApi()
    }

    override val networkManager: NetworkManager by lazy {
        DefaultNetworkManager(context)
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepositoryImpl(context)
    }

    override val courseRepository: CourseRepository by lazy {
        CourseRepositoryImpl(
            courseDao = courseDao,
            lessonDao = lessonDao,
            courseApi = courseApi
        )
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            authApi = authApi
        )
    }

    override val getAuthStateUseCase: GetAuthStateUseCase by lazy {
        GetAuthStateUseCaseImpl(
            userPreferencesRepository = userPreferencesRepository
        )
    }

    override val validateCredentialsUseCase: ValidateCredentialsUseCase by lazy {
        ValidateCredentialsUseCaseImpl()
    }

    override val loginUseCase: LoginUseCase by lazy {
        LoginUseCaseImpl(
            authRepository = authRepository,
            networkManager = networkManager,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    override val observeNetworkStatusUseCase: ObserveNetworkStatusUseCase by lazy {
        ObserveNetworkStatusUseCaseImpl(
            networkManager = networkManager
        )
    }

    override val getCoursesUseCase: GetCoursesUseCase by lazy {
        GetCoursesUseCaseImpl(
            repository = courseRepository
        )
    }

    override val refreshCoursesUseCase: RefreshCoursesUseCase by lazy {
        RefreshCoursesUseCaseImpl(
            repository = courseRepository,
            networkManager = networkManager
        )
    }

    override val getCourseDetailsUseCase: GetCourseDetailsUseCase by lazy {
        GetCourseDetailsUseCaseImpl(
            repository = courseRepository
        )
    }

    override val refreshCourseDetailsUseCase: RefreshCourseDetailsUseCase by lazy {
        RefreshCourseDetailsUseCaseImpl(
            repository = courseRepository,
            networkManager = networkManager
        )
    }

    override val toggleLessonCompletionUseCase: ToggleLessonCompletionUseCase by lazy {
        ToggleLessonCompletionUseCaseImpl(
            repository = courseRepository
        )
    }

    override val logoutUseCase: LogoutUseCase by lazy {
        LogoutUseCaseImpl(
            repository = courseRepository,
            userPreferencesRepository = userPreferencesRepository
        )
    }
}
