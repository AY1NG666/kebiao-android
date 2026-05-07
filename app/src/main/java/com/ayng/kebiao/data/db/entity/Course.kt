package com.ayng.kebiao.data.db.entity

import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.PrimaryKey

@androidx.compose.runtime.Immutable
@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val location: String,
    val dayOfWeek: Int,        // 1=Mon ... 7=Sun
    val startTime: String,     // "HH:mm"
    val endTime: String,       // "HH:mm"
    val durationHours: Float,  // e.g. 1.5
    val isKindergarten: Boolean = false,  // 幼儿园课程：固定55元/节
    val colorHex: String = "",           // 自定义颜色，如 "#4F46E5"，空则自动
)
