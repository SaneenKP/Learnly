package com.example.learningdashboard.di

import android.content.Context
import com.example.learningdashboard.data.local.AppDatabase
import com.example.learningdashboard.data.local.dao.CourseDao
import com.example.learningdashboard.data.local.dao.LessonDao
import com.example.learningdashboard.data.remote.CourseApi
import com.example.learningdashboard.data.remote.FakeCourseApi
import com.example.learningdashboard.data.repository.CourseRepositoryImpl
import com.example.learningdashboard.domain.repository.CourseRepository

interface AppContainer {
    val database: AppDatabase
    val courseDao: CourseDao
    val lessonDao: LessonDao
    val courseApi: CourseApi
    val courseRepository: CourseRepository
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

    override val courseRepository: CourseRepository by lazy {
        CourseRepositoryImpl(
            courseDao = courseDao,
            lessonDao = lessonDao,
            courseApi = courseApi
        )
    }
}
