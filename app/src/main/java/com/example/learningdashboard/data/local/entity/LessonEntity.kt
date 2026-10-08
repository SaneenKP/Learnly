package com.example.learningdashboard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.learningdashboard.domain.model.Lesson

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: Long,
    val courseId: Long,
    val title: String,
    val completed: Boolean
) {
    fun toDomain(): Lesson = Lesson(
        id = id,
        courseId = courseId,
        title = title,
        completed = completed
    )
}
