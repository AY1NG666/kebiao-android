package com.ayng.kebiao.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ayng.kebiao.data.db.entity.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startTime")
    fun getAll(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE dayOfWeek = :day ORDER BY startTime")
    fun getByDay(day: Int): Flow<List<Course>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: Course): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<Course>)

    @Delete
    suspend fun delete(course: Course)

    @Query("DELETE FROM courses")
    suspend fun deleteAll()

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getById(id: Long): Course?
}
