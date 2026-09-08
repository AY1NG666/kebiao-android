package com.ayng.kebiao.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayng.kebiao.data.validation.isValidTimeRange
import com.ayng.kebiao.data.validation.parseTimeMinutes
import com.ayng.kebiao.data.db.entity.DEFAULT_KINDERGARTEN_RATE
import com.ayng.kebiao.ui.parseColorSafe

private fun calcDuration(start: String, end: String): String {
    val startMinutes = parseTimeMinutes(start) ?: return ""
    val endMinutes = parseTimeMinutes(end) ?: return ""
    if (endMinutes <= startMinutes) return ""
    val hours = (endMinutes - startMinutes) / 60f
    return if (hours == hours.toInt().toFloat()) "${hours.toInt()}" else "%.1f".format(hours)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCourseDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, location: String, dayOfWeek: Int, startTime: String, endTime: String, durationHours: Float, isKindergarten: Boolean, kindergartenRate: Double, colorHex: String) -> Unit,
    initialName: String = "",
    initialLocation: String = "",
    initialDay: Int = 1,
    initialStart: String = "",
    initialEnd: String = "",
    initialKinder: Boolean = false,
    initialKindergartenRate: Double = DEFAULT_KINDERGARTEN_RATE,
    initialColor: String = "",
) {
    var name by remember { mutableStateOf(initialName) }
    var location by remember { mutableStateOf(initialLocation) }
    var selectedDay by remember { mutableIntStateOf(initialDay) }
    var startH by remember { mutableStateOf(if (initialStart.length >= 2) initialStart.substring(0, 2) else "") }
    var startM by remember { mutableStateOf(if (initialStart.length >= 5) initialStart.substring(3, 5) else "") }
    var endH by remember { mutableStateOf(if (initialEnd.length >= 2) initialEnd.substring(0, 2) else "") }
    var endM by remember { mutableStateOf(if (initialEnd.length >= 5) initialEnd.substring(3, 5) else "") }
    var isKindergarten by remember { mutableStateOf(initialKinder) }
    var kindergartenRateText by remember { mutableStateOf(formatCourseRate(initialKindergartenRate)) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var showColorPalette by remember { mutableStateOf(false) }

    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    val startTime = if (startH.isNotBlank()) "${startH.padStart(2, '0')}:${startM.padStart(2, '0')}" else ""
    val endTime = if (endH.isNotBlank()) "${endH.padStart(2, '0')}:${endM.padStart(2, '0')}" else ""
    val durationStr = remember(startTime, endTime) { calcDuration(startTime, endTime) }
    val validTime = startH.isNotBlank() && startM.isNotBlank() &&
        endH.isNotBlank() && endM.isNotBlank() &&
        isValidTimeRange(startTime, endTime)
    val kindergartenRate = kindergartenRateText.toDoubleOrNull()
    val validKindergartenRate = !isKindergarten || (kindergartenRate?.isFinite() == true && kindergartenRate > 0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isBlank()) "添加课程" else "编辑课程") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("课程名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("上课地点") },
                    placeholder = { Text("例如：欧阳修、木马森林或自定义地点") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("星期", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    days.forEachIndexed { index, day ->
                        FilterChip(selected = selectedDay == index + 1, onClick = { selectedDay = index + 1 }, label = { Text(day) })
                    }
                }

                // Time inputs with fixed ":"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("开始时间", style = MaterialTheme.typography.labelSmall)
                        TimeFields(h = startH, m = startM, onH = { if (it.length <= 2) startH = it.filter { c -> c.isDigit() } }, onM = { if (it.length <= 2) startM = it.filter { c -> c.isDigit() } })
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("结束时间", style = MaterialTheme.typography.labelSmall)
                        TimeFields(h = endH, m = endM, onH = { if (it.length <= 2) endH = it.filter { c -> c.isDigit() } }, onM = { if (it.length <= 2) endM = it.filter { c -> c.isDigit() } })
                    }
                }

                // Auto-calculated duration
                OutlinedTextField(
                    value = durationStr,
                    onValueChange = {},
                    label = { Text("课时（小时）") },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { if (durationStr.isNotBlank()) Text("自动计算") },
                )

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("幼儿园课程", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isKindergarten, onCheckedChange = { isKindergarten = it })
                }

                if (isKindergarten) {
                    OutlinedTextField(
                        value = kindergartenRateText,
                        onValueChange = { value ->
                            if (value.length <= 12 && value.count { it == '.' } <= 1 && value.all { it.isDigit() || it == '.' }) {
                                kindergartenRateText = value
                            }
                        },
                        label = { Text("幼儿园每节金额（元）") },
                        placeholder = { Text(formatCourseRate(DEFAULT_KINDERGARTEN_RATE)) },
                        supportingText = if (!validKindergartenRate) {
                            { Text("请输入大于 0 的有效金额") }
                        } else null,
                        isError = !validKindergartenRate,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }

                // Color picker — click to open palette
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("卡片颜色", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    val previewColor = parseColorSafe(selectedColor) ?: Color(0xFFCBD5E1)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(previewColor, CircleShape)
                            .clickable { showColorPalette = true }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (selectedColor.isEmpty()) "点击选色" else selectedColor,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                        location,
                        selectedDay,
                        startTime,
                        endTime,
                        durationStr.toFloatOrNull() ?: 1.5f,
                        isKindergarten,
                        kindergartenRate ?: DEFAULT_KINDERGARTEN_RATE,
                        selectedColor,
                    )
                },
                enabled = name.isNotBlank() && location.isNotBlank() && validTime && durationStr.isNotBlank() && validKindergartenRate,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )

    // Color palette dialog
    if (showColorPalette) {
        ColorPaletteDialog(
            current = selectedColor,
            onDismiss = { showColorPalette = false },
            onSelect = { selectedColor = it; showColorPalette = false },
        )
    }
}

fun formatCourseRate(value: Double): String = String.format(java.util.Locale.ROOT, "%.2f", value)

@Composable
private fun TimeFields(
    h: String,
    m: String,
    onH: (String) -> Unit,
    onM: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = h,
            onValueChange = { v -> if (v.length <= 2) onH(v.filter { it.isDigit() }) },
            label = { Text("时") },
            placeholder = { Text("09") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Text(" : ", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = m,
            onValueChange = { v -> if (v.length <= 2) onM(v.filter { it.isDigit() }) },
            label = { Text("分") },
            placeholder = { Text("00") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

private val paletteColors: List<String> = listOf(
    "#F44336","#E53935","#D32F2F","#C62828","#B71C1C","#FF5722","#E64A19","#D84315",
    "#FF9800","#F57C00","#EF6C00","#E65100","#FFC107","#FFA000","#FF8F00","#FF6F00",
    "#FFEB3B","#FDD835","#FBC02D","#F9A825","#CDDC39","#C0CA33","#AFB42B","#9E9D24",
    "#8BC34A","#689F38","#558B2F","#33691E","#4CAF50","#388E3C","#2E7D32","#1B5E20",
    "#009688","#00796B","#00695C","#004D40","#00BCD4","#0097A7","#00838F","#006064",
    "#03A9F4","#0288D1","#0277BD","#01579B","#2196F3","#1976D2","#1565C0","#0D47A1",
    "#3F51B5","#303F9F","#283593","#1A237E","#673AB7","#512DA8","#4527A0","#311B92",
    "#9C27B0","#7B1FA2","#6A1B9A","#4A148C","#E91E63","#C2185B","#AD1457","#880E4F",
    "#EF5350","#FF7043","#FFA726","#66BB6A","#26A69A","#26C6DA","#42A5F5","#5C6BC0",
    "#AB47BC","#EC407A","#78909C","#546E7A","#37474F","#263238","#607D8B","#455A64",
    "#795548","#5D4037","#4E342E","#3E2723","#000000","#424242","#616161","#9E9E9E",
    "#BDBDBD","#E0E0E0","#CFD8DC","#FFFFFF",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPaletteDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Reset option
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFE2E8F0), CircleShape)
                            .clickable { onSelect("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", color = Color(0xFF64748B), fontSize = 14.sp)
                    }

                    paletteColors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = current == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color, CircleShape)
                                .clickable { onSelect(hex) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
