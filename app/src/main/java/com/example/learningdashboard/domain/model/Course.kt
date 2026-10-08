package com.example.learningdashboard.domain.model

data class Course(
    val id: Long,
    val title: String,
    val category: String,
    val instructor: String,
    val totalLessons: Int,
    val completedLessons: Int,
    val progress: Int,
    val nextLesson: String? = null
)
