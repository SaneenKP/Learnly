package com.example.learningdashboard.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val isLoggedIn: Flow<Boolean>
    suspend fun setLoggedIn(isLoggedIn: Boolean)
}
