package com.example.learningdashboard.data.remote

import com.example.learningdashboard.data.remote.model.CourseDto
import com.example.learningdashboard.data.remote.model.LessonDto

interface CourseApi {
    suspend fun getCourses(): List<CourseDto>
    suspend fun getLessons(courseId: Long): List<LessonDto>
}
