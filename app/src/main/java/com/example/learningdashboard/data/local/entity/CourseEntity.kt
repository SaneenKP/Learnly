package com.example.learningdashboard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.learningdashboard.domain.model.Course
import com.example.learningdashboard.util.Constants

@Entity(tableName = Constants.Database.TABLE_COURSES)
data class CourseEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val category: String,
    val instructorId: Long,
    val instructorName: String,
    val totalLessons: Int
) {
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
