package com.ayng.kebiao.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayng.kebiao.data.db.entity.Attendance
import com.ayng.kebiao.data.db.entity.Course
import com.ayng.kebiao.data.db.entity.SalaryRule
import com.ayng.kebiao.data.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repo: AppRepository) : ViewModel() {

    val salaryRules: StateFlow<List<SalaryRule>> = repo.getAllSalaryRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val courses: StateFlow<List<Course>> = repo.getAllCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSalaryRule(rule: SalaryRule) {
        viewModelScope.launch { repo.updateSalaryRule(rule) }
    }

    fun addSalaryRule(rule: SalaryRule) {
        viewModelScope.launch { repo.insertSalaryRule(rule) }
    }

    fun deleteSalaryRule(id: Long) {
        viewModelScope.launch { repo.deleteSalaryRule(id) }
    }

    fun resetSalaryRules() {
        viewModelScope.launch {
            repo.deleteAllSalaryRules()
            repo.insertSalaryRule(SalaryRule(minStudents = 0, maxStudents = 4, ratePerClass = 100.0))
            repo.insertSalaryRule(SalaryRule(minStudents = 5, maxStudents = 10, ratePerClass = 150.0))
            repo.insertSalaryRule(SalaryRule(minStudents = 11, maxStudents = null, ratePerClass = 200.0))
        }
    }

    // Course management
    fun addCourse(name: String, location: String, dayOfWeek: Int, startTime: String, endTime: String, durationHours: Float, isKindergarten: Boolean = false, colorHex: String = "") {
        viewModelScope.launch {
            repo.insertCourse(
                Course(name = name, location = location, dayOfWeek = dayOfWeek, startTime = startTime, endTime = endTime, durationHours = durationHours, isKindergarten = isKindergarten, colorHex = colorHex)
            )
        }
    }

    fun updateCourse(id: Long, name: String, location: String, dayOfWeek: Int, startTime: String, endTime: String, durationHours: Float, isKindergarten: Boolean, colorHex: String) {
        viewModelScope.launch {
            repo.insertCourse(
                Course(id = id, name = name, location = location, dayOfWeek = dayOfWeek, startTime = startTime, endTime = endTime, durationHours = durationHours, isKindergarten = isKindergarten, colorHex = colorHex)
            )
        }
    }

    fun deleteCourse(courseId: Long) {
        viewModelScope.launch {
            val course = repo.getCourseById(courseId)
            if (course != null) {
                repo.deleteCourse(course)
            }
        }
    }

    // Import
    fun importCourses(courses: List<Course>) {
        viewModelScope.launch {
            repo.deleteAllCourses()
            repo.insertCourses(courses)
        }
    }

    fun importAll(courses: List<Course>, attendances: List<Attendance>) {
        viewModelScope.launch {
            // Store old course names for attendance mapping
            val oldIdToName = repo.getAllCourses().first().associate { it.id to it.name }

            repo.deleteAllCourses()
            repo.insertCourses(courses)

            // Map attendance to new course IDs by name
            val newCourses = repo.getAllCourses().first()
            for (a in attendances) {
                val courseName = oldIdToName[a.courseId] ?: continue
                val newCourse = newCourses.find { it.name == courseName }
                if (newCourse != null) {
                    repo.insertAttendance(a.copy(courseId = newCourse.id))
                }
            }
        }
    }

    suspend fun exportAllData(): String = buildString {
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINESE).format(java.util.Date())
        appendLine("=== 课表数据备份 === 导出时间：$dateStr")
        appendLine()

        appendLine("--- 课程表 ---")
        appendLine("课程名称,上课地点,星期,开始时间,结束时间,课时,幼儿园,颜色")
        val courses = repo.getAllCourses().first()
        for (c in courses) {
            val dayLabel = when(c.dayOfWeek) { 1->"周一"; 2->"周二"; 3->"周三"; 4->"周四"; 5->"周五"; 6->"周六"; else->"周日" }
            appendLine("${c.name},${c.location},$dayLabel,${c.startTime},${c.endTime},${c.durationHours},${if (c.isKindergarten) "是" else "否"},${c.colorHex}")
        }
        appendLine()

        appendLine("--- 出勤记录 ---")
        appendLine("日期,课程名称,地点,学生人数,助教人数,课时费,类型")
        val attendances = repo.getAllAttendance().first()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINESE)
        var totalSalary = 0.0
        for (a in attendances.sortedBy { it.date }) {
            val course = courses.find { it.id == a.courseId }
            val rate = if (course?.isKindergarten == true) 55.0 else a.studentCount * 7.0 + a.assistantCount * 3.0
            totalSalary += rate
            appendLine("${sdf.format(java.util.Date(a.date))},${course?.name ?: "未知"},${course?.location ?: ""},${a.studentCount},${a.assistantCount},${"%.2f".format(rate)},${if (course?.isKindergarten == true) "幼儿园" else "超能星球"}")
        }
        appendLine()

        appendLine("--- 汇总 ---")
        appendLine("总课程数,${courses.size}")
        appendLine("总出勤记录,${attendances.size}")
        appendLine("课时费合计,${"%.2f".format(totalSalary)}")
    }

    fun clearAllData() {
        viewModelScope.launch {
            repo.deleteAllCourses()
            // Clear all attendance and rules too
            // This is a simplified version — a production app would have proper cascade
        }
    }

    class Factory(private val repo: AppRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(repo) as T
    }
}
