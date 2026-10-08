package com.example.learningdashboard.domain.repository

import com.example.learningdashboard.data.remote.model.LoginResponseDto

/**
 * Repository interface for authentication operations.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): LoginResponseDto
}
