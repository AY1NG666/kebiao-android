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
import com.ayng.kebiao.data.db.entity.SalaryRule
import com.ayng.kebiao.data.parser.ExcelParser
import com.ayng.kebiao.data.update.UpdateCheckResult
import com.ayng.kebiao.data.update.UpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val courses by vm.courses.collectAsState()
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<com.ayng.kebiao.data.db.entity.Course?>(null) }

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

            // ── Update Check ──
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "版本更新",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                UpdateSection()
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
fun UpdateSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager(context) }

    var checking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var downloading by remember { mutableStateOf(false) }

    val currentVersion = remember { updateManager.getCurrentVersion() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Current version
        Text(
            text = "当前版本：$currentVersion",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Check update button
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Button(
                onClick = {
                    checking = true
                    checkResult = null
                    scope.launch {
                        val result = updateManager.checkForUpdate()
                        checkResult = result
                        checking = false
                    }
                },
                enabled = !checking,
            ) {
                Text(if (checking) "检查中..." else "检查更新")
            }
        }

        // Progress
        if (checking) {
            androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Result
        checkResult?.let { result ->
            if (result.error != null) {
                Text(
                    text = "检查失败：${result.error}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else if (result.hasUpdate && result.updateInfo != null) {
                val info = result.updateInfo
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "发现新版本 v${info.versionName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (info.releaseNotes.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = info.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                downloading = true
                                updateManager.downloadAndInstall(info) {
                                    Toast.makeText(context, "正在后台下载更新...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !downloading,
                        ) {
                            Text(if (downloading) "下载中..." else "立即更新")
                        }
                    }
                }
            } else {
                Text(
                    text = "已是最新版本 ✓",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
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
