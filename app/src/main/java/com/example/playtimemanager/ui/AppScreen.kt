// 파일 경로: app/src/main/java/com/example/playtimemanager/ui/AppScreen.kt

package com.example.playtimemanager.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.playtimemanager.ui.screens.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppScreen(viewModel: MainViewModel) {
    val pagerState = rememberPagerState(initialPage = 1) { 4 }

    val selectedIcons by viewModel.selectedIcons.collectAsState()
    val targetTime by viewModel.targetTime.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val currentSessionTime by viewModel.currentSessionTime.collectAsState()
    val dailyRecords by viewModel.dailyRecords.collectAsState()
    val monthlyRecords by viewModel.monthlyRecords.collectAsState()
    val appUsageRecords by viewModel.appUsageRecords.collectAsState()
    val totalUsedTime = dailyRecords.sumOf { it.seconds }

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (page) {
                0 -> SettingsScreen(
                    selectedIcons = selectedIcons,
                    targetTime = targetTime,
                    onUpdateIcons = viewModel::updateSelectedIcons,
                    onUpdateTargetTime = viewModel::updateTargetTime
                )
                1 -> MainScreen(
                    targetTime = targetTime,
                    totalUsedTime = totalUsedTime,
                    currentSessionTime = currentSessionTime,
                    selectedIcons = selectedIcons,
                    isTracking = isTracking,
                    onStartActivity = viewModel::startActivity,
                    onStopActivity = viewModel::stopActivity
                )
                2 -> StatisticsScreen(
                    dailyRecords = dailyRecords,
                    appUsageRecords = appUsageRecords,
                    onUpdateRecord = viewModel::overwriteActivityTimeForToday,
                    onRefresh = viewModel::loadAppUsageStats
                )//바뀐 데이터 전달
                3 -> CalendarScreen(
                    monthlyRecords = monthlyRecords,
                    targetTime = targetTime
                )//바뀐 데이터 전달
            }
        }
    }
}