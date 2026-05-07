package com.ayng.kebiao.ui.schedule

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayng.kebiao.data.db.entity.Course
import com.ayng.kebiao.ui.theme.Primary
import com.ayng.kebiao.ui.theme.Secondary
import com.ayng.kebiao.ui.theme.SecondaryContainer
import com.ayng.kebiao.ui.theme.Tertiary
import com.ayng.kebiao.ui.theme.TertiaryContainer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(vm: ScheduleViewModel) {
    val courses by vm.courses.collectAsState()
    val weekOffset by vm.currentWeekOffset.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf<Course?>(null) }
    var showAddCourseDialog by remember { mutableStateOf(false) }

    // Cache week calculation - only recompute when weekOffset changes
    val weekDays = remember(weekOffset) {
        val monday = vm.getMondayOfWeek(weekOffset)
        (0..6).map { offset ->
            val cal = monday.clone() as Calendar
            cal.add(Calendar.DAY_OF_WEEK, offset)
            cal
        }
    }
    val today = remember { Calendar.getInstance() }

    // Pre-group courses by dayOfWeek once
    val coursesByDay = remember(courses) { courses.groupBy { it.dayOfWeek } }
    val dateFmt = remember { SimpleDateFormat("MM/dd", Locale.CHINESE) }
    val dayLabels = remember { listOf("周一","周二","周三","周四","周五","周六","周日") }
    val dayShortLabels = remember { listOf("一","二","三","四","五","六","日") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("课表", style = MaterialTheme.typography.titleLarge)
                        Text(
                            vm.monthLabel(weekOffset),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.currentWeekOffset.value = weekOffset - 1 }) {
                        Icon(Icons.Filled.ChevronLeft, "上一周")
                    }
                    IconButton(onClick = { vm.currentWeekOffset.value = weekOffset + 1 }) {
                        Icon(Icons.Filled.ChevronRight, "下一周")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCourseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Add, "添加课程", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Day headers row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    weekDays.forEach { day ->
                        val isToday = day.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                                day.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                        val dayOfWeek = day.get(Calendar.DAY_OF_WEEK)
                        val ourDay = when (dayOfWeek) {
                            Calendar.MONDAY -> 1; Calendar.TUESDAY -> 2; Calendar.WEDNESDAY -> 3
                            Calendar.THURSDAY -> 4; Calendar.FRIDAY -> 5; Calendar.SATURDAY -> 6
                            else -> 7
                        }
                        val label = when (ourDay) { 1->"一"; 2->"二"; 3->"三"; 4->"四"; 5->"五"; 6->"六"; else->"日" }
                        val dateNum = day.get(Calendar.DAY_OF_MONTH)

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .then(
                                        if (isToday) Modifier.background(Primary, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$dateNum",
                                    fontSize = 13.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            // Courses for each day
            weekDays.forEach { day ->
                val dayOfWeek = day.get(Calendar.DAY_OF_WEEK)
                val ourDay = when (dayOfWeek) {
                    Calendar.MONDAY -> 1; Calendar.TUESDAY -> 2; Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4; Calendar.FRIDAY -> 5; Calendar.SATURDAY -> 6
                    else -> 7
                }
                val dayCourses = coursesByDay[ourDay].orEmpty()
                val label = when (ourDay) { 1->"周一"; 2->"周二"; 3->"周三"; 4->"周四"; 5->"周五"; 6->"周六"; else->"周日" }
                val dateMillis = day.timeInMillis

                item(key = "day_$ourDay") {
                    val dateStr = remember(dateMillis) { dateFmt.format(Date(dateMillis)) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "$label  $dateStr",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.weight(1f))
                        if (dayCourses.isNotEmpty()) {
                            Text(
                                "${dayCourses.size}节",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (dayCourses.isEmpty()) {
                    item(key = "empty_$ourDay") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("休息", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    items(dayCourses, key = { it.id }) { course ->
                        val courseColor = if (course.colorHex.isNotBlank()) Color(android.graphics.Color.parseColor(course.colorHex)) else if (course.isKindergarten) Secondary else Tertiary
                        val courseBg = if (course.colorHex.isNotBlank()) Color(android.graphics.Color.parseColor(course.colorHex)).copy(alpha = 0.12f) else if (course.isKindergarten) SecondaryContainer else TertiaryContainer

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                    ,
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = courseBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Color indicator bar
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(courseColor)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            course.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${course.startTime}-${course.endTime}  |  ${course.location}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { showDeleteConfirm = course }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.Delete, "删除", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    // Delete confirmation
    showDeleteConfirm?.let { course ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除课程") },
            text = { Text("确定要删除「${course.name}」吗？") },
            confirmButton = {
                TextButton(onClick = { vm.deleteCourse(course); showDeleteConfirm = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            },
        )
    }

    // Add course dialog
    if (showAddCourseDialog) {
        com.ayng.kebiao.ui.settings.AddCourseDialog(
            onDismiss = { showAddCourseDialog = false },
            onConfirm = { name, location, day, start, end, duration, isKinder, color ->
                vm.addCourse(name, location, day, start, end, duration, isKinder, color)
                showAddCourseDialog = false
            },
        )
    }
}

@Composable
private fun rememberDateFormat(millis: Long): String {
    val fmt = remember { SimpleDateFormat("MM/dd", Locale.CHINESE) }
    return fmt.format(Date(millis))
}
