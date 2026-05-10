package com.ayng.kebiao.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ayng.kebiao.data.db.entity.Attendance
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Query("""
        SELECT * FROM attendances
        ORDER BY date DESC
    """)
    fun getAll(): Flow<List<Attendance>>

    @Query("""
        SELECT * FROM attendances
        WHERE date >= :startOfMonth AND date < :startOfNextMonth
        ORDER BY date
    """)
    fun getByMonth(startOfMonth: Long, startOfNextMonth: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendances WHERE courseId = :courseId ORDER BY date DESC")
    fun getByCourse(courseId: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendances WHERE id = :id")
    suspend fun getById(id: Long): Attendance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attendance: Attendance): Long

    @Update
    suspend fun update(attendance: Attendance)

    @Delete
    suspend fun delete(attendance: Attendance)

    @Query("DELETE FROM attendances WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM attendances")
    suspend fun deleteAll()
}
