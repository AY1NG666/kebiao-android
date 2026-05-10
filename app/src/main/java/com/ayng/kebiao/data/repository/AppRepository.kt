package com.ayng.kebiao.data.repository

import com.ayng.kebiao.data.db.dao.AttendanceDao
import com.ayng.kebiao.data.db.dao.CourseDao
import com.ayng.kebiao.data.db.dao.SalaryRuleDao
import com.ayng.kebiao.data.db.entity.Attendance
import com.ayng.kebiao.data.db.entity.Course
import com.ayng.kebiao.data.db.entity.SalaryRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class AppRepository(
    private val courseDao: CourseDao,
    private val attendanceDao: AttendanceDao,
    private val salaryRuleDao: SalaryRuleDao,
) {
    // Data version: increments on any attendance change, used for smart salary refresh
    private val _dataVersion = MutableStateFlow(0)
    val dataVersion: StateFlow<Int> = _dataVersion

    private fun bumpVersion() { _dataVersion.value++ }

    // ── Courses ──

    fun getAllCourses(): Flow<List<Course>> = courseDao.getAll()

    fun getCoursesByDay(day: Int): Flow<List<Course>> = courseDao.getByDay(day)

    suspend fun insertCourse(course: Course): Long = courseDao.insert(course)

    suspend fun insertCourses(courses: List<Course>) = courseDao.insertAll(courses)

    suspend fun deleteCourse(course: Course) = courseDao.delete(course)

    suspend fun deleteAllCourses() = courseDao.deleteAll()

    suspend fun getCourseById(id: Long): Course? = courseDao.getById(id)

    // ── Attendance ──

    fun getAllAttendance(): Flow<List<Attendance>> = attendanceDao.getAll()

    fun getAttendanceByMonth(year: Int, month: Int): Flow<List<Attendance>> {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return attendanceDao.getByMonth(start, end)
    }

    suspend fun insertAttendance(attendance: Attendance): Long {
        val id = attendanceDao.insert(attendance)
        bumpVersion()
        return id
    }

    suspend fun updateAttendance(attendance: Attendance) {
        attendanceDao.update(attendance)
        bumpVersion()
    }

    suspend fun deleteAttendance(id: Long) {
        attendanceDao.deleteById(id)
        bumpVersion()
    }

    suspend fun deleteAllAttendances() {
        attendanceDao.deleteAll()
        bumpVersion()
    }

    // ── Salary Rules ──

    fun getAllSalaryRules(): Flow<List<SalaryRule>> = salaryRuleDao.getAll()

    suspend fun getAllSalaryRulesSync(): List<SalaryRule> = salaryRuleDao.getAllSync()

    suspend fun insertSalaryRule(rule: SalaryRule): Long = salaryRuleDao.insert(rule)

    suspend fun updateSalaryRule(rule: SalaryRule) = salaryRuleDao.update(rule)

    suspend fun deleteSalaryRule(id: Long) = salaryRuleDao.deleteById(id)

    suspend fun deleteAllSalaryRules() = salaryRuleDao.deleteAll()

    // ── Salary Calculation ──
    // 普通课: 学生 × ¥7 + 助教 × ¥3
    // 幼儿园课: 固定 ¥55/节

    suspend fun calculateMonthlySalary(year: Int, month: Int): SalaryBreakdown {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis

        val attendances = attendanceDao.getByMonth(start, end)
        val list = attendances.first()

        val details = mutableListOf<SalaryDetail>()
        var total = 0.0

        for (a in list) {
            val course = courseDao.getById(a.courseId)
            val rate = if (course?.isKindergarten == true) {
                55.0
            } else {
                a.studentCount * 7.0 + a.assistantCount * 3.0
            }
            total += rate
            details.add(
                SalaryDetail(
                    attendanceId = a.id,
                    date = a.date,
                    courseName = course?.name ?: "未知课程",
                    studentCount = a.studentCount,
                    assistantCount = a.assistantCount,
                    isKindergarten = course?.isKindergarten == true,
                    rate = rate,
                )
            )
        }

        return SalaryBreakdown(
            year = year,
            month = month,
            total = total,
            classCount = list.size,
            details = details,
        )
    }
}

@androidx.compose.runtime.Immutable
data class SalaryBreakdown(
    val year: Int,
    val month: Int,
    val total: Double,
    val classCount: Int,
    val details: List<SalaryDetail>,
)

@androidx.compose.runtime.Immutable
data class SalaryDetail(
    val attendanceId: Long,
    val date: Long,
    val courseName: String,
    val studentCount: Int,
    val assistantCount: Int,
    val isKindergarten: Boolean,
    val rate: Double,
)
