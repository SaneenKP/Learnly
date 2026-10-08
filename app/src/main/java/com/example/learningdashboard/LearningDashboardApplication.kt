package com.example.learningdashboard

import android.app.Application
import com.example.learningdashboard.di.AppContainer
import com.example.learningdashboard.di.DefaultAppContainer

class LearningDashboardApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
