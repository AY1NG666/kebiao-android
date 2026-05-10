package com.ayng.kebiao.data.parser

import android.content.Context
import android.net.Uri
import com.ayng.kebiao.data.db.entity.Attendance
import com.ayng.kebiao.data.db.entity.Course
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

object ExcelParser {

    data class ParseResult(
        val courses: List<Course>,
        val attendances: List<Attendance> = emptyList(),
        val errors: List<String>,
        val warnings: List<String>,
    )

    fun parse(context: Context, uri: Uri): ParseResult {
        val fileName = getFileName(context, uri) ?: ""
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

        try {
            val reader = BufferedReader(InputStreamReader(context.contentResolver.openInputStream(uri), "UTF-8"))
            var lineNum = 0
            var section = "none" // none | courses | attendance
            val coursesById = mutableMapOf<String, Long>() // name -> id mapping for attendance linking

            reader.useLines { lines ->
                lines.forEach { line ->
                    lineNum++
                    val trimmed = line.trim()
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

                    val cols = trimmed.split(",").map { it.trim().removeSurrounding("\"") }

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
                                val durationHours = cols[5].toFloat()
                                val isKindergarten = if (cols.size >= 7) {
                                    val v = cols[6]; v == "是" || v == "1" || v.equals("true", ignoreCase = true) || v == "幼儿园"
                                } else false
                                val colorHex = if (cols.size >= 8) cols[7] else ""

                                courses.add(Course(name = name, location = location, dayOfWeek = dayOfWeek, startTime = startTime, endTime = endTime, durationHours = durationHours, isKindergarten = isKindergarten, colorHex = colorHex))
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
                                val date = SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE).parse(dateStr)?.time ?: return@forEach
                                val studentCount = cols[3].toIntOrNull() ?: 0
                                val assistantCount = cols[4].toIntOrNull() ?: 0

                                // Find matching course by name to link attendance
                                val course = courses.find { it.name == courseName }
                                if (course != null) {
                                    attendances.add(Attendance(courseId = course.id, date = date, studentCount = studentCount, assistantCount = assistantCount))
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
        s.toIntOrNull()?.let { if (it in 1..7) return it }
        return when (s) {
            "周一" -> 1; "周二" -> 2; "周三" -> 3; "周四" -> 4
            "周五" -> 5; "周六" -> 6; "周日" -> 7
            else -> -1
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx)
        }
        return name ?: uri.lastPathSegment
    }
}
