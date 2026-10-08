package com.example.learningdashboard.data.remote.model

import com.example.learningdashboard.data.local.entity.CourseEntity
import com.example.learningdashboard.domain.model.Course
import kotlinx.serialization.Serializable

@Serializable
data class CourseDto(
    val id: Long,
    val title: String,
    val category: String,
    val instructorId: Long,
    val instructorName: String,
    val totalLessons: Int
) {
    fun toEntity(): CourseEntity = CourseEntity(
        id = id,
        title = title,
        category = category,
        instructorId = instructorId,
        instructorName = instructorName,
        totalLessons = totalLessons
    )

    fun toDomain(completedLessons: Int = 0, progress: Int = 0): Course = Course(
        id = id,
        title = title,
        category = category,
        instructor = instructorName,
        totalLessons = totalLessons,
        completedLessons = completedLessons,
        progress = progress
    )
}
