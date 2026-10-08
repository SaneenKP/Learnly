package com.example.learningdashboard.data.repository

import com.example.learningdashboard.data.remote.AuthApi
import com.example.learningdashboard.data.remote.model.LoginRequestDto
import com.example.learningdashboard.data.remote.model.LoginResponseDto
import com.example.learningdashboard.domain.error.AppError
import com.example.learningdashboard.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val authApi: AuthApi
) : AuthRepository {

    override suspend fun login(email: String, password: String): LoginResponseDto = withContext(Dispatchers.IO) {
        try {
            authApi.login(LoginRequestDto(email = email, password = password))
        } catch (e: AppError) {
            throw e
        } catch (e: Exception) {
            throw AppError.NetworkError.Unknown("Authentication failed due to unexpected error", e)
        }
    }
}
