package com.ayng.kebiao.ui.attendance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayng.kebiao.data.db.entity.Attendance
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(vm: AttendanceViewModel) {
    val attendances by vm.attendances.collectAsState()
    val courses by vm.courses.collectAsState()
    val year by vm.currentYear.collectAsState()
    val month by vm.currentMonth.collectAsState()
    val classCount = attendances.size

    var showRecordDialog by remember { mutableStateOf(false) }
    var editingAttendance by remember { mutableStateOf<Attendance?>(null) }
    var deletingId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("出勤记录") },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val m = month - 1
                            if (m < 0) vm.goToMonth(year - 1, 11)
                            else vm.goToMonth(year, m)
                        }) { Icon(Icons.Filled.ChevronLeft, "上一月") }
                        Text(text = "${year}年${month + 1}月", fontWeight = FontWeight.Medium)
                        IconButton(onClick = {
                            val m = month + 1
                            if (m > 11) vm.goToMonth(year + 1, 0)
                            else vm.goToMonth(year, m)
                        }) { Icon(Icons.Filled.ChevronRight, "下一月") }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showRecordDialog = true }) {
                Icon(Icons.Filled.Add, "录入出勤")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "本月已上课：${classCount} 节",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            if (attendances.isEmpty()) {
                item {
                    Text(
                        text = "本月暂无出勤记录\n点击右下角 + 录入",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            items(attendances, key = { it.id }) { att ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = vm.getCourseName(att.courseId),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = rememberDateFormat(att.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (!att.note.isNullOrBlank()) {
                                    Text(
                                        text = " · ${att.note}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                        }
                        val isKinderCourse = courses.find { it.id == att.courseId }?.isKindergarten == true
                        if (!isKinderCourse && att.studentCount > 0) {
                            Text(
                                text = "${att.studentCount}人",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        IconButton(onClick = { editingAttendance = att }) {
                            Icon(Icons.Filled.Edit, "编辑", tint = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(onClick = { deletingId = att.id }) {
                            Icon(Icons.Filled.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    // Record dialog
    if (showRecordDialog) {
        RecordAttendanceDialog(
            courses = courses,
            existingAttendances = attendances,
            onDismiss = { showRecordDialog = false },
            onConfirm = { date, entries, note ->
                vm.recordDayAttendanceCustom(date, entries, note)
                showRecordDialog = false
            },
        )
    }

    // Edit dialog
    editingAttendance?.let { att ->
        EditAttendanceDialog(
            attendance = att,
            courses = courses,
            onDismiss = { editingAttendance = null },
            onConfirm = { updated ->
                vm.updateAttendance(updated)
                editingAttendance = null
            },
        )
    }

    // Delete confirmation
    deletingId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text("删除记录") },
            text = { Text("确定要删除这条出勤记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteAttendance(id)
                    deletingId = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingId = null }) { Text("取消") }
            },
        )
    }
}

@Composable
fun rememberDateFormat(millis: Long): String {
    val fmt = remember { SimpleDateFormat("yyyy年M月d日  EEEE", Locale.CHINESE) }
    return fmt.format(java.util.Date(millis))
}

/** Try multiple date formats */
private fun parseDateFlexible(s: String): java.util.Date? {
    val formats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE),
        SimpleDateFormat("yyyy/MM/dd", Locale.CHINESE),
        SimpleDateFormat("yyyy.MM.dd", Locale.CHINESE),
        SimpleDateFormat("yyyy年M月d日", Locale.CHINESE),
        SimpleDateFormat("M月d日", Locale.CHINESE),
    )
    for (fmt in formats) {
        try {
            val d = fmt.parse(s)
            if (d != null) {
                if (s.length <= 6) {
                    val cal = Calendar.getInstance()
                    cal.time = d
                    cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
                    return cal.time
                }
                return d
            }
        } catch (_: Exception) {}
    }
    return null
}

data class DayEntry(
    val course: com.ayng.kebiao.data.db.entity.Course,
    var studentCountStr: String = "",
    var assistantCountStr: String = "0",
    var checked: Boolean = true,
    val alreadyRecorded: Boolean = false,
)

@Composable
fun RecordAttendanceDialog(
    courses: List<com.ayng.kebiao.data.db.entity.Course>,
    existingAttendances: List<Attendance>,
    onDismiss: () -> Unit,
    onConfirm: (date: Long, entries: List<DayEntry>, note: String?) -> Unit,
) {
    var mode by remember { mutableStateOf(0) } // 0=课表, 1=自定义

    // Schedule mode state
    var dateStr by remember {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE)
        mutableStateOf(fmt.format(java.util.Date()))
    }
    val parsedDate = parseDateFlexible(dateStr)
    val dayOfWeek = parsedDate?.let {
        val cal = Calendar.getInstance()
        cal.time = it
        cal.get(Calendar.DAY_OF_WEEK)
    } ?: -1
    val ourDay = when (dayOfWeek) {
        Calendar.MONDAY -> 1; Calendar.TUESDAY -> 2; Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4; Calendar.FRIDAY -> 5; Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7; else -> -1
    }
    val todayCourses = courses.filter { it.dayOfWeek == ourDay }
    val dateMillis = parsedDate?.time ?: 0L
    val recordedCourseIds = existingAttendances
        .filter { isSameDay(it.date, dateMillis) }
        .map { it.courseId }.toSet()
    var scheduleEntries by remember(todayCourses, recordedCourseIds) {
        mutableStateOf(todayCourses.map {
            DayEntry(course = it, alreadyRecorded = it.id in recordedCourseIds, checked = it.id !in recordedCourseIds)
        })
    }
    val dayLabel = when (ourDay) { 1->"周一";2->"周二";3->"周三";4->"周四";5->"周五";6->"周六";7->"周日"; else->"" }
    val dateValid = parsedDate != null

    // Custom mode state
    var customDateStr by remember {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE)
        mutableStateOf(fmt.format(java.util.Date()))
    }
    val customParsedDate = parseDateFlexible(customDateStr)
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var customStudentCount by remember { mutableStateOf("") }
    var customAssistantCount by remember { mutableStateOf("0") }
    var customKinderChecked by remember { mutableStateOf(false) }
    var customStartH by remember { mutableStateOf("") }
    var customStartM by remember { mutableStateOf("") }
    var customEndH by remember { mutableStateOf("") }
    var customEndM by remember { mutableStateOf("") }

    val selectedCourse = courses.find { it.id == selectedCourseId }

    // === DIALOG ===
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("录入出勤") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Mode switch
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("课表出勤") })
                    FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("自定义出勤") })
                }

                if (mode == 0) {
                    // ===== SCHEDULE MODE =====
                    OutlinedTextField(
                        value = dateStr, onValueChange = { dateStr = it },
                        label = { Text("日期") },
                        placeholder = { Text("如 2026-05-06 或 5月6日") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        isError = dateStr.isNotBlank() && !dateValid,
                        supportingText = if (dateStr.isNotBlank() && !dateValid) {{ Text("日期格式不对") }} else null,
                    )
                    if (courses.isEmpty()) {
                        Text("⚠️ 还没有课程！", color = MaterialTheme.colorScheme.error)
                    } else if (!dateValid) {
                        Text("请输入有效日期", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (todayCourses.isEmpty()) {
                        Text("${dayLabel}暂无课程，可切换到自定义出勤", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        HorizontalDivider()
                        Text("${dayLabel} 共${todayCourses.size}节课", fontWeight = FontWeight.SemiBold)
                        todayCourses.forEachIndexed { idx, course ->
                            val entry = scheduleEntries.getOrNull(idx) ?: return@forEachIndexed
                            val isRecorded = entry.alreadyRecorded
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isRecorded) MaterialTheme.colorScheme.surfaceVariant
                                    else if (course.isKindergarten) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surface
                                ),
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(course.name, fontWeight = FontWeight.Medium)
                                            if (isRecorded) {
                                                Spacer(Modifier.width(6.dp))
                                                Text("已录入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Text(course.location, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("${course.startTime}-${course.endTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (isRecorded) {
                                        Text("此课程已录入，无需重复", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    } else if (course.isKindergarten) {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("幼儿园 ¥55/节", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                            Switch(
                                                checked = entry.checked,
                                                onCheckedChange = {
                                                    val newList = scheduleEntries.toMutableList()
                                                    newList[idx] = entry.copy(checked = it)
                                                    scheduleEntries = newList
                                                }
                                            )
                                        }
                                    } else {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = entry.studentCountStr, onValueChange = { v ->
                                                val newList = scheduleEntries.toMutableList()
                                                newList[idx] = entry.copy(studentCountStr = v.filter { c -> c.isDigit() })
                                                scheduleEntries = newList
                                            }, label = { Text("学生") }, singleLine = true, modifier = Modifier.weight(1f))
                                            OutlinedTextField(value = entry.assistantCountStr, onValueChange = { v ->
                                                val newList = scheduleEntries.toMutableList()
                                                newList[idx] = entry.copy(assistantCountStr = v.filter { c -> c.isDigit() })
                                                scheduleEntries = newList
                                            }, label = { Text("助教") }, singleLine = true, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ===== CUSTOM MODE =====
                    OutlinedTextField(
                        value = customDateStr, onValueChange = { customDateStr = it },
                        label = { Text("日期") },
                        placeholder = { Text("如 2026-05-06 或 5月6日") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        isError = customDateStr.isNotBlank() && customParsedDate == null,
                    )

                    Text("选择课程", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    courses.forEach { course ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedCourseId = course.id
                                if (course.isKindergarten) { customStudentCount = ""; customKinderChecked = false }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCourseId == course.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surface
                            ),
                        ) {
                            Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(course.name, fontWeight = FontWeight.Medium)
                                    Text("${course.location}  ${course.startTime}-${course.endTime}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (selectedCourseId == course.id) {
                                    Icon(Icons.Filled.Edit, "已选", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    if (selectedCourse != null) {
                        HorizontalDivider()
                        if (selectedCourse!!.isKindergarten) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("到课（¥55/节）", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = customKinderChecked, onCheckedChange = { customKinderChecked = it })
                            }
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = customStudentCount, onValueChange = { customStudentCount = it.filter { c -> c.isDigit() } },
                                    label = { Text("学生") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = customAssistantCount, onValueChange = { customAssistantCount = it.filter { c -> c.isDigit() } },
                                    label = { Text("助教") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                        }
                        // Custom time
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("时间", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(value = customStartH, onValueChange = { customStartH = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("时") }, singleLine = true, modifier = Modifier.width(56.dp))
                            Text(":"); OutlinedTextField(value = customStartM, onValueChange = { customStartM = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("分") }, singleLine = true, modifier = Modifier.width(56.dp))
                            Text("→"); OutlinedTextField(value = customEndH, onValueChange = { customEndH = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("时") }, singleLine = true, modifier = Modifier.width(56.dp))
                            Text(":"); OutlinedTextField(value = customEndM, onValueChange = { customEndM = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("分") }, singleLine = true, modifier = Modifier.width(56.dp))
                            Text("留空=原课时间", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        },
        confirmButton = {
            val canConfirm = if (mode == 0) {
                todayCourses.isNotEmpty() && dateValid && scheduleEntries.any { entry ->
                    if (entry.alreadyRecorded) false
                    else entry.course.isKindergarten && entry.checked || (!entry.course.isKindergarten && entry.studentCountStr.isNotBlank())
                }
            } else {
                selectedCourseId != null && customParsedDate != null &&
                (selectedCourse?.isKindergarten == true && customKinderChecked || (selectedCourse?.isKindergarten == false && customStudentCount.isNotBlank()))
            }
            TextButton(
                onClick = {
                    if (mode == 0) {
                        val date = parsedDate?.time ?: System.currentTimeMillis()
                        onConfirm(date, scheduleEntries.filter { !it.alreadyRecorded }, null)
                    } else {
                        val date = customParsedDate?.time ?: System.currentTimeMillis()
                        val course = selectedCourse!!
                        val sc = if (course.isKindergarten) 0 else (customStudentCount.toIntOrNull() ?: 0)
                        val ac = if (course.isKindergarten) 0 else (customAssistantCount.toIntOrNull() ?: 0)
                        // Build time note
                        var note: String? = null
                        if (customStartH.isNotEmpty() || customEndH.isNotEmpty()) {
                            val sh = customStartH.ifBlank { "00" }.padStart(2, '0')
                            val sm = customStartM.ifBlank { "00" }.padStart(2, '0')
                            val eh = customEndH.ifBlank { "00" }.padStart(2, '0')
                            val em = customEndM.ifBlank { "00" }.padStart(2, '0')
                            note = "$sh:$sm-$eh:$em"
                        }
                        // Create a single DayEntry for VM
                        val entry = DayEntry(course = course, studentCountStr = sc.toString(), assistantCountStr = ac.toString(), checked = true)
                        onConfirm(date, listOf(entry), note)
                    }
                },
                enabled = canConfirm,
            ) { Text("确认录入") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun isSameDay(millis1: Long, millis2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun EditAttendanceDialog(
    attendance: Attendance,
    courses: List<com.ayng.kebiao.data.db.entity.Course>,
    onDismiss: () -> Unit,
    onConfirm: (Attendance) -> Unit,
) {
    val course = courses.find { it.id == attendance.courseId }
    val isKinder = course?.isKindergarten == true
    var studentCountStr by remember { mutableStateOf(attendance.studentCount.toString()) }
    var assistantCountStr by remember { mutableStateOf(attendance.assistantCount.toString()) }
    // Parse note as custom time fields
    val noteParts = attendance.note?.split("-") ?: emptyList()
    var editStartH by remember { mutableStateOf(if (noteParts.size >= 1) noteParts[0].substringBefore(":").takeIf { it.isNotEmpty() } ?: "" else "") }
    var editStartM by remember { mutableStateOf(if (noteParts.size >= 1) noteParts[0].substringAfter(":").takeIf { it.isNotEmpty() } ?: "" else "") }
    var editEndH by remember { mutableStateOf(if (noteParts.size >= 2) noteParts[1].substringBefore(":").takeIf { it.isNotEmpty() } ?: "" else "") }
    var editEndM by remember { mutableStateOf(if (noteParts.size >= 2) noteParts[1].substringAfter(":").takeIf { it.isNotEmpty() } ?: "" else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑出勤") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("课程：${course?.name ?: "未知"}", style = MaterialTheme.typography.bodyLarge)
                Text("日期：${rememberDateFormat(attendance.date)}", style = MaterialTheme.typography.bodySmall)
                if (isKinder) {
                    Text("幼儿园课程 ¥55/节", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                } else {
                    OutlinedTextField(value = studentCountStr, onValueChange = { studentCountStr = it.filter { c -> c.isDigit() } },
                        label = { Text("上课人数") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = assistantCountStr, onValueChange = { assistantCountStr = it.filter { c -> c.isDigit() } },
                        label = { Text("助教人数") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                // Custom time edit
                HorizontalDivider()
                Text("自定义时间", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(value = editStartH, onValueChange = { editStartH = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("时") }, singleLine = true, modifier = Modifier.width(56.dp))
                    Text(":"); OutlinedTextField(value = editStartM, onValueChange = { editStartM = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("分") }, singleLine = true, modifier = Modifier.width(56.dp))
                    Text("→"); OutlinedTextField(value = editEndH, onValueChange = { editEndH = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("时") }, singleLine = true, modifier = Modifier.width(56.dp))
                    Text(":"); OutlinedTextField(value = editEndM, onValueChange = { editEndM = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("分") }, singleLine = true, modifier = Modifier.width(56.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val count = if (isKinder) 0 else (studentCountStr.toIntOrNull() ?: return@TextButton)
                    val assistant = if (isKinder) 0 else (assistantCountStr.toIntOrNull() ?: 0)
                    var newNote: String? = null
                    if (editStartH.isNotEmpty() || editEndH.isNotEmpty()) {
                        val sh = editStartH.ifBlank { "00" }.padStart(2, '0')
                        val sm = editStartM.ifBlank { "00" }.padStart(2, '0')
                        val eh = editEndH.ifBlank { "00" }.padStart(2, '0')
                        val em = editEndM.ifBlank { "00" }.padStart(2, '0')
                        newNote = "$sh:$sm-$eh:$em"
                    }
                    onConfirm(attendance.copy(studentCount = count, assistantCount = assistant, note = newNote))
                },
                enabled = isKinder || studentCountStr.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
