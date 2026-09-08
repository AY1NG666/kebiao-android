package com.ayng.kebiao.data.db.entity

import androidx.compose.runtime.Stable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

const val DEFAULT_KINDERGARTEN_RATE: Double = 55.0

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
    val isKindergarten: Boolean = false,
    @ColumnInfo(defaultValue = "55.0")
    val kindergartenRate: Double = DEFAULT_KINDERGARTEN_RATE, // 幼儿园每节金额
    val colorHex: String = "",           // 自定义颜色，如 "#4F46E5"，空则自动
) {
    val effectiveKindergartenRate: Double
        get() = kindergartenRate.takeIf { it.isFinite() && it > 0.0 } ?: DEFAULT_KINDERGARTEN_RATE
}
