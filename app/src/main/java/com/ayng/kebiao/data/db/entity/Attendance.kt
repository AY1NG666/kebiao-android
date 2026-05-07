package com.ayng.kebiao.data.db.entity

import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@androidx.compose.runtime.Immutable
@Entity(
    tableName = "attendances",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId"), Index("date")]
)
data class Attendance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long,
    val date: Long,              // epoch millis at start of day
    val studentCount: Int,
    val assistantCount: Int = 0, // 助教人数
    val note: String? = null,
)
