// 파일 경로: app/src/main/java/com/example/playtimemanager/ui/screens/CalendarScreen.kt

package com.example.playtimemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    monthlyRecords: Map<LocalDate, Float>,
    targetTime: Int // targetTime은 이제 직접 사용하지 않지만, 변경 시 재계산을 위해 남겨둡니다.
) {
    val currentMonth = YearMonth.now()
    val startMonth = currentMonth.minusMonths(10)
    val endMonth = currentMonth.plusMonths(10)
    val firstDayOfWeek = firstDayOfWeekFromLocale()

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    HorizontalCalendar(
        state = state,
        dayContent = { day ->
            Day(day, progress = monthlyRecords[day.date] ?: 0f)
        }
    )
}

@Composable
private fun Day(day: CalendarDay, progress: Float) {
    // --- 변경점: 새로운 색상 규칙 적용 ---
    // progress는 (사용 시간 / 목표 시간) 비율입니다. (예: 0.6 = 60%, 1.0 = 100%)
    val color = when {
        progress <= 0f -> Color.Transparent // 기록 없음
        progress > 1.0f -> Color.Red.copy(alpha = 0.6f) // 100% 초과 시 빨간색
        progress > 0.6f -> Color.Yellow.copy(alpha = 0.6f) // 60% 초과 시 노란색
        else -> Color.Green.copy(alpha = 0.6f) // 0~60%는 초록색
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f) // 정사각형 모양 유지
            .padding(4.dp)
            .background(color = color),
        contentAlignment = Alignment.Center
    ) {
        Text(text = day.date.dayOfMonth.toString())
    }
}