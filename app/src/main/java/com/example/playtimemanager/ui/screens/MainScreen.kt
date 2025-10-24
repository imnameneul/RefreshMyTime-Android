// 파일 경로: app/src/main/java/com/example/playtimemanager/ui/screens/MainScreen.kt

package com.example.playtimemanager.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playtimemanager.data.ActivityType
import java.util.concurrent.TimeUnit

@Composable
fun MainScreen(
    targetTime: Int,
    totalUsedTime: Int, // 오늘 하루 총 사용 시간
    currentSessionTime: Int, // 현재 측정 중인 시간
    selectedIcons: List<ActivityType>,
    isTracking: Boolean,
    onStartActivity: () -> Unit,
    onStopActivity: (ActivityType) -> Unit
) {
    var showActivitySelector by remember { mutableStateOf(false) }

    if (showActivitySelector) {
        ActivitySelectorDialog(
            activities = selectedIcons,
            onActivitySelected = { activity ->
                onStopActivity(activity)
                showActivitySelector = false
            },
            onDismiss = {
                showActivitySelector = false
            }
        )
    }

    val displayTime = if (isTracking) currentSessionTime else totalUsedTime
    val progress = if (targetTime > 0) ((totalUsedTime + currentSessionTime).toFloat() / targetTime.toFloat()) else 0f
    val progressAngle = progress * 360f

    val progressColor = when {
        progress <= 1f -> Color(0xFF10b981)
        progress <= 2f -> Color(0xFFf59e0b)
        else -> Color(0xFFef4444)
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isTracking) {
                        showActivitySelector = true //멈출때
                    } else {
                        onStartActivity()  //시작할 떄
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = progressAngle,
                    useCenter = false,
                    style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatTime(displayTime),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatTime(targetTime),
                    fontSize = 24.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (isTracking) {
                    Text(
                        text = "기록 중...",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "START",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val hours = TimeUnit.SECONDS.toHours(seconds.toLong())
    val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong()) % 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}