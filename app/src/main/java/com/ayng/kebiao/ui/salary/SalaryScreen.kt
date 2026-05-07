package com.ayng.kebiao.ui.salary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryScreen(vm: SalaryViewModel) {
    val breakdown by vm.breakdown.collectAsState()
    val year by vm.currentYear.collectAsState()
    val month by vm.currentMonth.collectAsState()

    // Animated total
    val rawTotal = breakdown?.total ?: 0.0
    val animatedTotal by animateFloatAsState(
        targetValue = rawTotal.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "salaryTotal",
    )
    val firstLoad = breakdown?.total == -1.0  // -1 signals initial loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("课时费工资") },
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Total salary card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "当月工资总额",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        )
                        Spacer(Modifier.height(4.dp))
                        if (firstLoad) {
                            Text(
                                text = "...",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            )
                        } else {
                            Text(
                                text = "¥%.2f".format(animatedTotal),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "共 ${breakdown?.classCount ?: 0} 节课",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            // Detail section — split by type
            if (breakdown != null && breakdown!!.details.isNotEmpty()) {
                val kinderDetails = breakdown!!.details.filter { it.isKindergarten }
                val normalDetails = breakdown!!.details.filter { !it.isKindergarten }
                val kinderTotal = kinderDetails.sumOf { it.rate }
                val normalTotal = normalDetails.sumOf { it.rate }
                // 幼儿园
                if (kinderDetails.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("幼儿园", fontWeight = FontWeight.SemiBold)
                            Text("小计 ¥%.2f".format(kinderTotal), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    itemsIndexed(kinderDetails) { index, detail ->
                        val dateFmt = remember { SimpleDateFormat("M月d日  EEEE", Locale.CHINESE) }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(detail.courseName, fontWeight = FontWeight.Medium)
                                    Text(dateFmt.format(java.util.Date(detail.date)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("¥%.2f".format(detail.rate), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                // 超能星球
                if (normalDetails.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("超能星球", fontWeight = FontWeight.SemiBold)
                            Text("小计 ¥%.2f".format(normalTotal), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    itemsIndexed(normalDetails) { index, detail ->
                        val dateFmt = remember { SimpleDateFormat("M月d日  EEEE", Locale.CHINESE) }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(detail.courseName, fontWeight = FontWeight.Medium)
                                    Text(dateFmt.format(java.util.Date(detail.date)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("¥%.2f".format(detail.rate), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            } else if (!firstLoad) {
                item {
                    Text(
                        text = "本月暂无出勤记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
