package com.ayng.kebiao.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayng.kebiao.data.db.entity.Attendance
import com.ayng.kebiao.data.db.entity.Course
import com.ayng.kebiao.data.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class ScheduleViewModel(private val repo: AppRepository) : ViewModel() {

    val courses: StateFlow<List<Course>> = repo.getAllCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Current week offset: 0 = this week, -1 = last week, +1 = next week */
    val currentWeekOffset = kotlinx.coroutines.flow.MutableStateFlow(0)

    fun recordAttendance(courseId: Long, date: Long, studentCount: Int, assistantCount: Int = 0, note: String? = null) {
        viewModelScope.launch {
            repo.insertAttendance(Attendance(courseId = courseId, date = date, studentCount = studentCount, assistantCount = assistantCount, note = note))
        }
    }

    fun addCourse(name: String, location: String, dayOfWeek: Int, startTime: String, endTime: String, durationHours: Float, isKindergarten: Boolean = false, colorHex: String = "") {
        viewModelScope.launch {
            repo.insertCourse(Course(name = name, location = location, dayOfWeek = dayOfWeek, startTime = startTime, endTime = endTime, durationHours = durationHours, isKindergarten = isKindergarten, colorHex = colorHex))
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch { repo.deleteCourse(course) }
    }

    fun dayOfWeekLabel(day: Int): String = when (day) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        7 -> "周日"
        else -> ""
    }

    fun getMondayOfWeek(weekOffset: Int): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
        return cal
    }

    fun getDateForDay(weekOffset: Int, dayOfWeek: Int): Long {
        val cal = getMondayOfWeek(weekOffset)
        cal.add(Calendar.DAY_OF_WEEK, dayOfWeek - Calendar.MONDAY)
        return cal.timeInMillis
    }

    fun monthLabel(weekOffset: Int): String {
        val cal = getMondayOfWeek(weekOffset)
        val startMonth = cal.get(Calendar.MONTH)
        cal.add(Calendar.DAY_OF_WEEK, 6)
        val endMonth = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        return if (startMonth == endMonth) {
            "${year}年${startMonth + 1}月"
        } else {
            "${year}年${startMonth + 1}-${endMonth + 1}月"
        }
    }

    class Factory(private val repo: AppRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ScheduleViewModel(repo) as T
    }
}
