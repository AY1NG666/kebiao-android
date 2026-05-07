package com.ayng.kebiao.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ayng.kebiao.data.db.entity.SalaryRule
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryRuleDao {

    @Query("SELECT * FROM salary_rules ORDER BY minStudents")
    fun getAll(): Flow<List<SalaryRule>>

    @Query("SELECT * FROM salary_rules ORDER BY minStudents")
    suspend fun getAllSync(): List<SalaryRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: SalaryRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<SalaryRule>)

    @Update
    suspend fun update(rule: SalaryRule)

    @Query("DELETE FROM salary_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM salary_rules")
    suspend fun deleteAll()
}
