package com.example.learningdashboard.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponseDto(
    val token: String,
    val email: String,
    val success: Boolean
)
