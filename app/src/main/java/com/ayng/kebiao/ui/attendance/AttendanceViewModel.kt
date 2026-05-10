package com.ayng.kebiao.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayng.kebiao.data.db.entity.Attendance
import com.ayng.kebiao.data.db.entity.Course
import com.ayng.kebiao.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class AttendanceViewModel(private val repo: AppRepository) : ViewModel() {

    val currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))

    private val _attendances = MutableStateFlow<List<Attendance>>(emptyList())
    val attendances: StateFlow<List<Attendance>> = _attendances

    val courses: StateFlow<List<Course>> = repo.getAllCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadMonth()
    }

    fun goToMonth(year: Int, month: Int) {
        currentYear.value = year
        currentMonth.value = month
        loadMonth()
    }

    fun recordDayAttendance(date: Long, entries: List<DayEntry>) {
        viewModelScope.launch {
            for (entry in entries) {
                if (entry.course.isKindergarten && !entry.checked) continue
                val count = if (entry.course.isKindergarten) 0 else (entry.studentCountStr.toIntOrNull() ?: 0)
                val assistant = if (entry.course.isKindergarten) 0 else (entry.assistantCountStr.toIntOrNull() ?: 0)
                repo.insertAttendance(
                    Attendance(courseId = entry.course.id, date = date, studentCount = count, assistantCount = assistant)
                )
            }
            loadMonth()
        }
    }

    /** Custom mode: single entry, skips 0-student non-kindergarten */
    fun recordDayAttendanceCustom(date: Long, entries: List<DayEntry>) {
        viewModelScope.launch {
            for (entry in entries) {
                if (entry.course.isKindergarten && !entry.checked) continue
                val count = if (entry.course.isKindergarten) 0 else (entry.studentCountStr.toIntOrNull() ?: 0)
                // Skip non-kindergarten courses with 0 students
                if (!entry.course.isKindergarten && count <= 0) continue
                val assistant = if (entry.course.isKindergarten) 0 else (entry.assistantCountStr.toIntOrNull() ?: 0)
                repo.insertAttendance(
                    Attendance(courseId = entry.course.id, date = date, studentCount = count, assistantCount = assistant)
                )
            }
            loadMonth()
        }
    }

    fun updateAttendance(attendance: Attendance) {
        viewModelScope.launch {
            repo.updateAttendance(attendance)
            loadMonth()
        }
    }

    fun deleteAttendance(id: Long) {
        viewModelScope.launch {
            repo.deleteAttendance(id)
            loadMonth()
        }
    }

    fun getCourseName(courseId: Long): String {
        return courses.value.find { it.id == courseId }?.name ?: "未知课程"
    }

    private fun loadMonth() {
        viewModelScope.launch {
            val year = currentYear.value
            val month = currentMonth.value
            repo.getAttendanceByMonth(year, month).collect { list ->
                _attendances.value = list
            }
        }
    }

    class Factory(private val repo: AppRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AttendanceViewModel(repo) as T
    }
}
