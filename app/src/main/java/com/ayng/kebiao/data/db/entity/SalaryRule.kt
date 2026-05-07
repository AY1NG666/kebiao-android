package com.ayng.kebiao.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "salary_rules")
data class SalaryRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val minStudents: Int,
    val maxStudents: Int?,    // null = no upper limit
    val ratePerClass: Double, // per-class pay (¥)
)
