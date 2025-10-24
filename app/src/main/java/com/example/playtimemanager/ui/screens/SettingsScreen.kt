// 파일 경로: app/src/main/java/com/example/playtimemanager/ui/screens/SettingsScreen.kt

package com.example.playtimemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playtimemanager.data.ActivityType
import java.util.concurrent.TimeUnit

@Composable
fun SettingsScreen(
    selectedIcons: List<ActivityType>,
    targetTime: Int,
    onUpdateIcons: (List<ActivityType>) -> Unit,
    onUpdateTargetTime: (Int) -> Unit
) {
    // '없음'을 제외한 모든 활동 목록을 가져옵니다.
    val allActivities = remember { ActivityType.values().filter { it != ActivityType.EMPTY } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "설정",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        // --- 기존 목표 시간 설정 UI ---
        Text(
            text = "목표 시간 설정",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formatTimeSetting(targetTime),
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = targetTime.toFloat(),
            onValueChange = { newValue ->
                // 30분(1800초) 단위로 값이 바뀌도록 설정합니다.
                val snappedValue = ((newValue / 1800).toInt() * 1800)
                onUpdateTargetTime(snappedValue)
            },
            valueRange = 1800f..(24 * 3600f), // 30분 ~ 24시간
            steps = 47, // (24 * 2) - 1
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // --- 새로 추가된 활동 아이콘 선택 UI ---
        Text(
            text = "활동 아이콘 선택",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4), // 아이콘을 한 줄에 4개씩 보여줍니다.
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(allActivities) { activity ->
                val isSelected = selectedIcons.contains(activity)
                val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(backgroundColor)
                        .clickable {
                            val newList = selectedIcons.toMutableList()
                            if (isSelected) {
                                // 이미 선택된 아이콘이면 목록에서 제거
                                newList.remove(activity)
                            } else {
                                // 선택되지 않은 아이콘이면 목록에 추가
                                newList.add(activity)
                            }
                            onUpdateIcons(newList) // 변경된 목록을 ViewModel에 알립니다.
                        }
                        .padding(vertical = 12.dp)
                ) {
                    ActivityIcon(
                        type = activity,
                        modifier = Modifier.size(32.dp),
                        tint = contentColor // 선택 상태에 따라 아이콘 색상 변경
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activity.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor
                    )
                }
            }
        }
    }
}

// 시간을 "X시간 Y분" 형식으로 보여주는 함수
private fun formatTimeSetting(seconds: Int): String {
    val hours = TimeUnit.SECONDS.toHours(seconds.toLong())
    val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong()) % 60
    if (hours > 0) {
        return String.format("%d시간 %02d분", hours, minutes)
    }
    return String.format("%d분", minutes)
}