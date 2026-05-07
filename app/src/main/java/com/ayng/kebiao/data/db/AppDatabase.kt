package com.ayng.kebiao.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ayng.kebiao.data.db.dao.AttendanceDao
import com.ayng.kebiao.data.db.dao.CourseDao
import com.ayng.kebiao.data.db.dao.SalaryRuleDao
import com.ayng.kebiao.data.db.entity.Attendance
import com.ayng.kebiao.data.db.entity.Course
import com.ayng.kebiao.data.db.entity.SalaryRule

@Database(
    entities = [Course::class, Attendance::class, SalaryRule::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun salaryRuleDao(): SalaryRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun build(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kebiao.db"
                )
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
        }
    }
}
