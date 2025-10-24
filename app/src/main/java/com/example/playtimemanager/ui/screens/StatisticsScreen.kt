// 파일 경로: app/src/main/java/com/example/playtimemanager/ui/screens/StatisticsScreen.kt

package com.example.playtimemanager.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.playtimemanager.data.ActivityType
import com.example.playtimemanager.data.DailyRecord
import com.example.playtimemanager.ui.AppUsageRecord
import java.util.concurrent.TimeUnit

@Composable
fun StatisticsScreen(
    dailyRecords: List<DailyRecord>,
    appUsageRecords: List<AppUsageRecord>,
    onUpdateRecord: (activity: ActivityType, newTotalSeconds: Int) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    //xml에서의 출입증 확인
    var hasPermission by remember { mutableStateOf(hasUsageStatsPermission(context)) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            onRefresh()
        }
    }

    //출입증 없으면 안내화면 보여주고 실행 정지
    if (!hasPermission) {
        PermissionRequestScreen(
            onGoToSettings = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            onRefresh = {
                hasPermission = hasUsageStatsPermission(context)
            }
        )
        return
    }

    val aggregatedData = remember(dailyRecords, appUsageRecords) {
        val combinedData = mutableMapOf<String, Pair<Int, ActivityType?>>()
        dailyRecords.filter { it.seconds > 0 }.forEach { record ->
            val key = record.activity.displayName
            val currentSeconds = combinedData[key]?.first ?: 0
            combinedData[key] = Pair(currentSeconds + record.seconds, record.activity)
        }
        appUsageRecords.forEach { appRecord ->
            val key = appRecord.appName
            val currentSeconds = combinedData[key]?.first ?: 0
            combinedData[key] = Pair(currentSeconds + appRecord.totalTimeInSeconds, null)
        }
        combinedData.toList().sortedByDescending { it.second.first }
    }

    val totalSeconds = remember(aggregatedData) {
        aggregatedData.sumOf { it.second.first }
    }

    var editingRecord by remember { mutableStateOf<Pair<ActivityType, Int>?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog && editingRecord != null) {
        TimeEditDialog(
            activity = editingRecord!!.first,
            initialSeconds = editingRecord!!.second,
            onDismiss = { showEditDialog = false },
            onSave = { newTotalSeconds ->
                onUpdateRecord(editingRecord!!.first, newTotalSeconds)
                showEditDialog = false
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("총 사용 시간", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatTimeWithUnits(totalSeconds),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onRefresh) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "새로고침")
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        if (totalSeconds == 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "아직 기록된 활동이 없습니다.\n홈 화면에서 활동을 시작하거나,\n다른 앱을 사용해보세요!",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            StatisticsChartAndLegend(data = aggregatedData, totalSeconds = totalSeconds)
            Spacer(modifier = Modifier.height(24.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(items = aggregatedData, key = { (name, _) -> name }) { (name, dataPair) ->
                    val (seconds, activityType) = dataPair
                    ActivityStatItem(
                        name = name,
                        activityType = activityType,
                        seconds = seconds,
                        onEditClick = {
                            if (activityType != null) {
                                editingRecord = Pair(activityType, seconds)
                                showEditDialog = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

//검문소 함수
@Composable
private fun PermissionRequestScreen(onGoToSettings: () -> Unit, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "앱 사용 기록을 불러오려면\n특별한 권한이 필요합니다.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "아래 버튼을 눌러 '사용 정보 접근' 설정 화면으로 이동한 후,\n'PlayTimeManager' 앱을 찾아 권한을 허용해주세요.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onGoToSettings) {
            Text("설정 화면으로 이동")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onRefresh) {
            Text("권한 확인 / 새로고침")
        }
    }
}

@Composable
private fun StatisticsChartAndLegend(data: List<Pair<String, Pair<Int, ActivityType?>>>, totalSeconds: Int) {
    val chartColors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFBB86FC),
        Color(0xFF3700B3), Color(0xFF018786), Color(0xFFCF6679),
        Color.Gray, Color.LightGray
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                data.forEachIndexed { index, (_, dataPair) ->
                    val (seconds, _) = dataPair
                    val sweepAngle = (seconds.toFloat() / totalSeconds.toFloat()) * 360f
                    drawArc(
                        color = chartColors[index % chartColors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - 2f,
                        useCenter = false,
                        style = Stroke(width = 25.dp.toPx(), cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }
        }
        Spacer(modifier = Modifier.width(24.dp))
        LazyColumn(modifier = Modifier.height(150.dp)) {
            itemsIndexed(items = data, key = { _, (name, _) -> name }) { index, (name, _) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(chartColors[index % chartColors.size]))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ActivityStatItem(
    name: String,
    activityType: ActivityType?,
    seconds: Int,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (activityType != null) {
            ActivityIcon(type = activityType, modifier = Modifier.size(32.dp))
        } else {
            Icon(imageVector = Icons.Default.Apps, contentDescription = "App Usage", modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = formatTimeWithUnits(seconds), style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = onEditClick, enabled = activityType != null) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "수정",
                tint = if (activityType != null) LocalContentColor.current else Color.Transparent
            )
        }
    }
}

private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun formatTimeWithUnits(totalSeconds: Int): String {
    if (totalSeconds < 0) return "0초"
    val hours = TimeUnit.SECONDS.toHours(totalSeconds.toLong())
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds.toLong()) % 60
    val seconds = totalSeconds % 60
    val parts = mutableListOf<String>()
    if (hours > 0) parts.add("${hours}시간")
    if (minutes > 0) parts.add("${minutes}분")
    if (seconds > 0 || parts.isEmpty()) parts.add("${seconds}초")
    return parts.joinToString(" ")
}