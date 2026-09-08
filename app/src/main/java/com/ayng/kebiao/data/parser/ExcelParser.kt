package com.ayng.kebiao.data.parser

import android.content.Context
import android.net.Uri
import com.ayng.kebiao.data.db.entity.Attendance
import com.ayng.kebiao.data.db.entity.Course
import com.ayng.kebiao.data.db.entity.DEFAULT_KINDERGARTEN_RATE
import com.ayng.kebiao.data.validation.isValidTimeRange
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ExcelParser {

    data class ParseResult(
        val courses: List<Course>,
        val attendances: List<Attendance> = emptyList(),
        val errors: List<String>,
        val warnings: List<String>,
    )

    fun parse(context: Context, uri: Uri): ParseResult {
        val fileName = (getFileName(context, uri) ?: "").lowercase(Locale.ROOT)
        return when {
            fileName.endsWith(".csv") -> parseCsv(context, uri)
            fileName.endsWith(".xlsx") || fileName.endsWith(".xls") -> ParseResult(emptyList(), emptyList(), listOf("不支持 Excel 文件，请用 CSV 格式重新导入"), emptyList())
            else -> ParseResult(emptyList(), emptyList(), listOf("不支持的文件格式：$fileName，请使用 CSV 文件"), emptyList())
        }
    }

    private fun parseCsv(context: Context, uri: Uri): ParseResult {
        val courses = mutableListOf<Course>()
        val attendances = mutableListOf<Attendance>()
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var tempCourseId = 1L  // temporary unique IDs for linking attendances to courses

        try {
            val reader = BufferedReader(InputStreamReader(context.contentResolver.openInputStream(uri), "UTF-8"))
            var lineNum = 0
            var section = "none" // none | courses | attendance
            reader.useLines { lines ->
                lines.forEach { line ->
                    lineNum++
                    val rawLine = line.removePrefix("\uFEFF")
                    val trimmed = rawLine.trim()
                    if (trimmed.isEmpty()) return@forEach

                    // Detect section headers
                    when {
                        trimmed.startsWith("=== 课表数据备份") || trimmed.startsWith("===") -> {
                            section = "none"; return@forEach
                        }
                        trimmed.startsWith("--- 课程表 ---") -> {
                            section = "courses"; return@forEach
                        }
                        trimmed.startsWith("--- 出勤记录 ---") -> {
                            section = "attendance"; return@forEach
                        }
                        trimmed.startsWith("--- 汇总 ---") -> {
                            section = "none"; return@forEach
                        }
                    }

                    val cols = parseCsvLine(rawLine)

                    when (section) {
                        "courses" -> {
                            if (cols.size < 6) return@forEach
                            // Skip header
                            if (cols[0] == "课程名称") return@forEach

                            try {
                                val name = cols[0]
                                val location = cols[1]
                                val dayOfWeek = parseDayOfWeek(cols[2])
                                if (dayOfWeek == -1) {
                                    warnings.add("第${lineNum}行：无法识别星期「${cols[2]}」，已跳过")
                                    return@forEach
                                }
                                val startTime = cols[3]
                                val endTime = cols[4]
                                if (!isValidTimeRange(startTime, endTime)) {
                                    warnings.add("第${lineNum}行：无效时间范围，已跳过")
                                    return@forEach
                                }
                                val durationHours = cols[5].toFloat()
                                if (!durationHours.isFinite() || durationHours <= 0f) {
                                    warnings.add("第${lineNum}行：课时必须是正数，已跳过")
                                    return@forEach
                                }
                                val isKindergarten = if (cols.size >= 7) {
                                    val v = cols[6]; v == "是" || v == "1" || v.equals("true", ignoreCase = true) || v == "幼儿园"
                                } else false
                                val colorHex = if (cols.size >= 8) cols[7] else ""
                                val normalizedColor = if (colorHex.isBlank() || COLOR_PATTERN.matches(colorHex)) {
                                    colorHex
                                } else {
                                    warnings.add("第${lineNum}行：颜色格式无效，已使用默认颜色")
                                    ""
                                }

                                val kindergartenRate = if (cols.size >= 9 && cols[8].isNotBlank()) {
                                    cols[8].toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
                                        ?: run {
                                            warnings.add("第${lineNum}行：幼儿园金额无效，已使用默认金额")
                                            DEFAULT_KINDERGARTEN_RATE
                                        }
                                } else {
                                    DEFAULT_KINDERGARTEN_RATE
                                }

                                courses.add(Course(id = tempCourseId++, name = name, location = location, dayOfWeek = dayOfWeek, startTime = startTime, endTime = endTime, durationHours = durationHours, isKindergarten = isKindergarten, kindergartenRate = kindergartenRate, colorHex = normalizedColor))
                            } catch (_: NumberFormatException) {
                                warnings.add("第${lineNum}行：数字格式错误，已跳过")
                            }
                        }

                        "attendance" -> {
                            if (cols.size < 5) return@forEach
                            if (cols[0] == "日期") return@forEach

                            try {
                                val dateStr = cols[0]
                                val courseName = cols[1]
                                val date = parseIsoDate(dateStr) ?: run {
                                    warnings.add("第${lineNum}行：日期格式错误，已跳过")
                                    return@forEach
                                }
                                val studentCount = cols[3].toIntOrNull()?.takeIf { it >= 0 } ?: run {
                                    warnings.add("第${lineNum}行：学生人数无效，已跳过")
                                    return@forEach
                                }
                                val assistantCount = cols[4].toIntOrNull()?.takeIf { it >= 0 } ?: run {
                                    warnings.add("第${lineNum}行：助教人数无效，已跳过")
                                    return@forEach
                                }

                                // Find matching course by name to link attendance
                                val course = courses.find { it.name == courseName }
                                if (course != null) {
                                    val note = cols.getOrNull(7)?.takeIf { it.isNotBlank() }
                                    attendances.add(Attendance(courseId = course.id, date = date, studentCount = studentCount, assistantCount = assistantCount, note = note))
                                } else if (courseName.isNotBlank() && courseName != "未知") {
                                    warnings.add("第${lineNum}行：找不到课程「$courseName」，请先导入课程")
                                }
                            } catch (_: Exception) {
                                warnings.add("第${lineNum}行：日期格式错误，已跳过")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            errors.add("读取文件失败：${e.message}")
        }

        return ParseResult(courses, attendances, errors, warnings)
    }

    /** Parse day from number (1-7) or Chinese name (周一/周二...) */
    private fun parseDayOfWeek(s: String): Int {
        val value = s.trim()
        value.toIntOrNull()?.let { if (it in 1..7) return it }
        return when (value) {
            "周一", "星期一" -> 1; "周二", "星期二" -> 2
            "周三", "星期三" -> 3; "周四", "星期四" -> 4
            "周五", "星期五" -> 5; "周六", "星期六" -> 6
            "周日", "星期日", "星期天" -> 7
            else -> -1
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < line.length) {
            val character = line[index]
            when {
                character == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                character == '"' -> inQuotes = !inQuotes
                character == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.setLength(0)
                }
                else -> current.append(character)
            }
            index++
        }
        result.add(current.toString().trim())
        return result
    }

    private fun parseIsoDate(value: String): Long? {
        val text = value.trim()
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE).apply { isLenient = false }
        val position = ParsePosition(0)
        val parsed = formatter.parse(text, position) ?: return null
        if (position.index != text.length) return null
        return Calendar.getInstance().apply {
            time = parsed
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private val COLOR_PATTERN = Regex("#[0-9A-Fa-f]{6}")

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx)
        }
        return name ?: uri.lastPathSegment
    }
}
