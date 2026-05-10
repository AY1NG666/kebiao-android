package com.ayng.kebiao.ui.settings

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ayng.kebiao.data.parser.ExcelParser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val courses by vm.courses.collectAsState()
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<com.ayng.kebiao.data.db.entity.Course?>(null) }
    var showChangelog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Course Management ──
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "课程管理",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { showAddCourseDialog = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("＋课程", maxLines = 1)
                    }
                    ImportButton(vm, Modifier.weight(1f))
                    ExportButton(vm, Modifier.weight(1f))
                }
            }

            item {
                Text(
                    text = "现有 ${courses.size} 门课程：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items(courses, key = { it.id }) { course ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = course.name, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${course.location}  |  周${course.dayOfWeek} ${course.startTime}-${course.endTime}  |  ${course.durationHours}h",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { editingCourse = course }) {
                            Icon(Icons.Filled.Edit, "编辑", tint = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(onClick = { vm.deleteCourse(course.id) }) {
                            Icon(Icons.Filled.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Changelog
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TextButton(onClick = { showChangelog = true }) {
                    Text("更新日志  v3.1", color = MaterialTheme.colorScheme.primary)
                }
            }

            // Bottom spacer
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    // Add course dialog
    if (showAddCourseDialog) {
        AddCourseDialog(
            onDismiss = { showAddCourseDialog = false },
            onConfirm = { name, location, day, start, end, duration, isKinder, color ->
                vm.addCourse(name, location, day, start, end, duration, isKinder, color)
                showAddCourseDialog = false
            },
        )
    }

    // Edit course dialog
    editingCourse?.let { course ->
        AddCourseDialog(
            initialName = course.name,
            initialLocation = course.location,
            initialDay = course.dayOfWeek,
            initialStart = course.startTime,
            initialEnd = course.endTime,
            initialKinder = course.isKindergarten,
            initialColor = course.colorHex,
            onDismiss = { editingCourse = null },
            onConfirm = { name, location, day, start, end, duration, isKinder, color ->
                vm.updateCourse(course.id, name, location, day, start, end, duration, isKinder, color)
                editingCourse = null
            },
        )
        }

    // Changelog dialog
    if (showChangelog) {
        val log = listOf(
            "v3.1" to listOf("修复 Color.parseColor 崩溃", "修复周历 locale 兼容", "修复工资加载静默失败", "修复 Excel 误导入", "UI 升级暖橙主题", "清理废弃代码"),
            "v3.0" to listOf("两端版本号统一为 3.0", "出勤列表底部留白防FAB遮挡", "自定义出勤按名称去重"),
            "v1.5.3" to listOf("标题固定不随内容滚动", "设置页新增更新日志入口"),
            "v1.5.2" to listOf("工资页新增炎梦分组（蓝色）"),
            "v1.5.1" to listOf("自定义出勤支持修改上课时间", "出勤列表自定义记录显示时间标签"),
            "v1.5.0" to listOf("录入页新增课表/自定义双模式切换", "自定义模式解决补课调课问题"),
            "v1.4.0" to listOf("新增应用图标", "出勤卡片删除按钮"),
            "v1.3.0" to listOf("修复出勤记录无法删除"),
            "v1.2.x" to listOf("导入功能全面修复(iCloud/编码/UTType)", "工资动画对齐Android", "课表手势修复"),
            "v1.1.0" to listOf("课程卡片纯展示", "日期滑动切周", "出勤自动识别日期", "工资页简约设计", "设置页卡片化"),
        )
        AlertDialog(
            onDismissRequest = { showChangelog = false },
            title = { Text("更新日志") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    log.forEach { (ver, items) ->
                        Text(ver, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                        items.forEach { item ->
                            Text("  • $item", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showChangelog = false }) { Text("关闭") } }
        )
    }
}

@Composable
fun ImportButton(vm: SettingsViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showResult by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val result = ExcelParser.parse(context, uri)
        if (result.errors.isNotEmpty()) {
            showResult = "导入失败：${result.errors.joinToString("; ")}"
        } else if (result.courses.isEmpty()) {
            showResult = "未解析到任何课程数据"
        } else {
            if (result.attendances.isNotEmpty()) {
                vm.importAll(result.courses, result.attendances)
            } else {
                vm.importCourses(result.courses)
            }
            val warnMsg = if (result.warnings.isNotEmpty()) {
                "\n警告：${result.warnings.joinToString("; ")}"
            } else ""
            val attMsg = if (result.attendances.isNotEmpty()) " + ${result.attendances.size}条出勤记录" else ""
            showResult = "成功导入 ${result.courses.size} 门课程${attMsg}！$warnMsg"
        }
    }

    OutlinedButton(onClick = { launcher.launch("*/*") }, modifier = modifier) {
        Text("导入", maxLines = 1)
    }

    showResult?.let { msg ->
        AlertDialog(
            onDismissRequest = { showResult = null },
            title = { Text("导入结果") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { showResult = null }) { Text("确定") }
            },
        )
    }
}

@Composable
fun ExportButton(vm: SettingsViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    OutlinedButton(onClick = {
        scope.launch {
            try {
                val csv = vm.exportAllData()
                val dateStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.CHINESE).format(java.util.Date())
                val file = java.io.File(context.cacheDir, "课表备份_${dateStr}.csv")
                file.writeText(csv)
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "导出备份"))
            } catch (e: Exception) {
                Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }, modifier = modifier) {
        Text("导出", maxLines = 1)
    }
}
