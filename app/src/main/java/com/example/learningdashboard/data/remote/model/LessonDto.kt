package com.example.learningdashboard.data.remote.model

import com.example.learningdashboard.data.local.entity.LessonEntity
import com.example.learningdashboard.domain.model.Lesson
import kotlinx.serialization.Serializable

@Serializable
data class LessonDto(
    val id: Long,
    val courseId: Long,
    val title: String,
    val completed: Boolean
) {
    fun toEntity(): LessonEntity = LessonEntity(
        id = id,
        courseId = courseId,
        title = title,
        completed = completed
    )

    fun toDomain(): Lesson = Lesson(
        id = id,
        courseId = courseId,
        title = title,
        completed = completed
    )
}
