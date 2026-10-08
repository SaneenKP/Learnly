package com.example.learningdashboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.learningdashboard.data.local.entity.LessonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {

    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY id ASC")
    fun observeLessons(courseId: Long): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons ORDER BY id ASC")
    fun observeAllLessons(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY id ASC")
    suspend fun getLessonsForCourse(courseId: Long): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    suspend fun getLessonById(lessonId: Long): LessonEntity?

    @Query("UPDATE lessons SET completed = :completed WHERE id = :lessonId")
    suspend fun updateLessonCompletion(lessonId: Long, completed: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLessonsIfNotExists(lessons: List<LessonEntity>)

    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun getLessonCount(): Int

    @Query("DELETE FROM lessons")
    suspend fun deleteAllLessons()
}
