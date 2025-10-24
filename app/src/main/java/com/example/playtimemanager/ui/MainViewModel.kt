// 파일 경로: app/src/main/java/com/example/playtimemanager/ui/MainViewModel.kt

package com.example.playtimemanager.ui

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.playtimemanager.data.AppDatabase
import com.example.playtimemanager.data.DailyRecord
import com.example.playtimemanager.data.ActivityType
import com.example.playtimemanager.data.TimerPrefsKeys
import com.example.playtimemanager.data.TimerPrefsRepo
import com.example.playtimemanager.timer.TimerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

// '자동 앱 사용 기록' API를 통해 가져온 데이터를 담기 위한 전용 데이터 클래스.
// (예: appName="YouTube", totalTimeInSeconds=120)
data class AppUsageRecord(val appName: String, val totalTimeInSeconds: Int)

// 이 클래스는 앱의 '두뇌' 역할을 합니다. 모든 데이터 관리와 로직 처리를 담당합니다.
class MainViewModel(application: Application) : AndroidViewModel(application) {
    // '데이터 창고'인 Room 데이터베이스와 통신하기 위한 '창고지기'(DAO)를 준비합니다.
    private val recordDao = AppDatabase.getDatabase(application).recordDao()

    // ✅ DataStore (진행중 상태 보존)
    private val prefsRepo = TimerPrefsRepo(application)

    // "오늘"이 며칠인지 기억하는 변수입니다. 자정이 지나면 이 값과 실제 날짜를 비교합니다.
    private var currentLoadedDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    // --- UI State: 화면(View)에 보여줄 모든 데이터를 담는 '실시간 현황판'(StateFlow)들 ---
    // StateFlow는 값이 바뀌면 화면이 자동으로 업데이트되도록 만들어줍니다.

    // 설정 화면에서 선택된 아이콘 목록
    private val _selectedIcons = MutableStateFlow(ActivityType.values().toList())
    val selectedIcons: StateFlow<List<ActivityType>> = _selectedIcons.asStateFlow()

    // 목표 시간 (초 단위)
    private val _targetTime = MutableStateFlow(6 * 3600) // 6h
    val targetTime: StateFlow<Int> = _targetTime.asStateFlow()

    // isTracking: DataStore를 “진실의 원천”으로, UI용 StateFlow로 변환
    val isTracking: StateFlow<Boolean> =
        prefsRepo.isTrackingFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)


    // 오늘 하루 동안 수동으로 기록된 모든 내역 목록
    private val _dailyRecords = MutableStateFlow<List<DailyRecord>>(emptyList())
    val dailyRecords: StateFlow<List<DailyRecord>> = _dailyRecords.asStateFlow()

    // 달력 화면에 표시될 날짜별 목표 달성률 맵
    private val _monthlyRecords = MutableStateFlow<Map<LocalDate, Float>>(emptyMap())
    val monthlyRecords: StateFlow<Map<LocalDate, Float>> = _monthlyRecords.asStateFlow()

    // 자동으로 집계된 '앱 사용 기록' 목록
    private val _appUsageRecords = MutableStateFlow<List<AppUsageRecord>>(emptyList())
    val appUsageRecords: StateFlow<List<AppUsageRecord>> = _appUsageRecords.asStateFlow()

    // ✅ 현재 세션 표시 시간(초) = (elapsedRealtime - start) + accumulated 를 1초 틱으로 계산
    val currentSessionTime: StateFlow<Int> = combine(
        prefsRepo.isTrackingFlow,
        prefsRepo.startElapsedMsFlow,
        prefsRepo.accumulatedTodayMsFlow,
        tickerFlow(1000L) // 가벼운 틱 (표시용), 계산은 항상 elapsed 기반
    ) { tracking, startMs, accMs, _ ->
        if (!tracking || startMs == 0L) 0
        else (((SystemClock.elapsedRealtime() - startMs) + accMs) / 1000L).toInt()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ViewModel이 처음 생성될 때, 오늘 날짜의 모든 데이터를 불러옵니다.
    init {
        refreshAllDataForToday()
    }

    /**
     * '선-시작'을 위한 함수입니다.
     * MainScreen에서 START 버튼을 누르면 호출됩니다.
     */
    fun startActivity() {
        viewModelScope.launch {
            val now = SystemClock.elapsedRealtime()
            // DataStore 업데이트
            prefsRepo.setStartElapsed(now)
            prefsRepo.setAccumulated(0L)     // 새 세션 시작 시 누적은 0부터
            prefsRepo.setTracking(true)

            // Foreground Service 시작 (알림에 크로노미터 표시)
            val ctx = getApplication<Application>()
            val intent = Intent(ctx, TimerService::class.java).apply {
                putExtra(TimerService.EXTRA_START_ELAPSED, now)
                putExtra(TimerService.EXTRA_ACCUMULATED, 0L)
            }
            try {
                ContextCompat.startForegroundService(ctx, intent)
            } catch (e: Exception) {
                // ForegroundServiceStartNotAllowedException 등 방어
                prefsRepo.setTracking(false)
                prefsRepo.setStartElapsed(0L)
                // TODO: UI에 "알림 권한이 필요해요" 같은 안내 노출
            }
        }
    }

    /**
     * '후-선택'을 위한 함수입니다.
     * MainScreen에서 타이머가 작동 중일 때 버튼을 누르고, 활동 아이콘을 선택하면 호출됩니다.
     * @param activity 사용자가 선택한 활동 종류 (예: ActivityType.GAMING)
     */
    fun stopActivity(activity: ActivityType) {
        viewModelScope.launch(Dispatchers.IO) {
            // DataStore에서 현재 상태 읽기
            val isTracking = prefsRepo.isTrackingFlow.first()
            val startMs = prefsRepo.startElapsedMsFlow.first()
            val accMs = prefsRepo.accumulatedTodayMsFlow.first()

            if (isTracking && startMs > 0L) {
                val elapsedMs = (SystemClock.elapsedRealtime() - startMs) + accMs
                val seconds = (elapsedMs / 1000L).toInt()

                if (seconds > 0) {
                    val record = DailyRecord(
                        date = currentLoadedDate,
                        activity = activity,
                        seconds = seconds
                    )
                    recordDao.upsertRecord(record)
                }
            }

            // DataStore 리셋
            prefsRepo.setTracking(false)
            prefsRepo.setStartElapsed(0L)
            prefsRepo.setAccumulated(0L)

            // 서비스 중지
            val ctx = getApplication<Application>()
            ctx.stopService(Intent(ctx, TimerService::class.java))

            // UI 데이터 갱신
            loadDailyRecords()
            loadMonthlyRecords()
        }
    }

    /**
     * 자정 초기화 기능의 핵심 함수입니다.
     * MainActivity에서 앱이 화면에 다시 나타날 때마다 호출됩니다.
     */
    fun checkForDateChangeAndRefresh() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (today != currentLoadedDate) {
            currentLoadedDate = today
            // 자정 넘어가면 누적도 리셋 권장 (진행 중 끊김 방지를 원하면 정책에 맞춰 조정)
            viewModelScope.launch { prefsRepo.setAccumulated(0L) }
            refreshAllDataForToday()
        }
    }

    /** 모든 데이터 로딩 함수를 한 번에 호출하는 헬퍼 함수입니다. */
    private fun refreshAllDataForToday() {
        loadDailyRecords()
        loadMonthlyRecords()
        loadAppUsageStats()
    }

    // =======================================================================================
    // == API 연동: 자동 앱 사용 기록 조회 (UsageStatsManager)
    // =======================================================================================
    /**
     * 안드로이드 시스템 API를 사용하여 '오늘 하루'의 앱 사용 기록을 가져옵니다.
     * StatisticsScreen이 보일 때마다, 그리고 자정이 지날 때마다 호출됩니다.
     */
    fun loadAppUsageStats() {
        // UI를 멈추지 않도록 별도의 작업실(Dispatchers.IO)에서 실행합니다.
        viewModelScope.launch(Dispatchers.IO) {
            // 1. [API 호출 준비] 안드로이드 시스템에서 'UsageStatsManager'(보안실 직원)를 불러옵니다.
            val usageStatsManager = getApplication<Application>().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            // 2. [조회 시간 설정] 조회할 시간 범위를 설정합니다.
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            }
            val startTime = cal.timeInMillis           // 시작 시간: 오늘 0시 0분
            val endTime = System.currentTimeMillis()   // 종료 시간: 지금 이 순간

            // 3. [API 호출] 보안실 직원에게 "오늘 하루 동안의 일일 보고서('INTERVAL_DAILY')를 주세요!" 라고 공식적으로 요청합니다.
            // 이 한 줄이 API 연동의 가장 핵심적인 부분입니다.
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

            // 4. [데이터 가공 준비] 앱의 실제 이름(예: "YouTube")을 얻기 위해 'PackageManager'(주민센터)를 불러옵니다.
            val packageManager = getApplication<Application>().packageManager

            // 5. [데이터 가공] 시스템이 전달해준 보고서 뭉치(stats)를 한 장씩 분석하고 우리가 원하는 형태로 변환합니다.
            val appRecords = stats
                // 5-1. 필터링: 총 사용 시간이 1초(1000ms)를 넘는 의미 있는 기록만 남깁니다.
                .filter { it.totalTimeInForeground > 1000 }
                // 5-2. 변환: 각 시스템 기록(UsageStats)을 우리가 만든 AppUsageRecord 형태로 변환합니다.
                .mapNotNull {
                    try {
                        // 패키지 이름(예: "com.google.android.youtube")으로 앱 정보를 조회합니다.
                        val appInfo = packageManager.getApplicationInfo(it.packageName, 0)
                        // 앱 정보에서 사용자가 보는 실제 앱 이름(예: "YouTube")을 가져옵니다.
                        val appName = packageManager.getApplicationLabel(appInfo).toString()

                        // 실행 아이콘이 있고(사용자가 직접 실행 가능한 앱이고), 우리 앱 자신이 아닌 경우에만 목록에 포함시킵니다.
                        if (packageManager.getLaunchIntentForPackage(it.packageName) != null &&
                            it.packageName != getApplication<Application>().packageName
                        ) {
                            // 최종적으로 'AppUsageRecord' 객체를 만들어 반환합니다.
                            AppUsageRecord(appName, (it.totalTimeInForeground / 1000).toInt())
                        } else {
                            null // 조건에 맞지 않으면 목록에서 제외(null)합니다.
                        }
                    } catch (e: Exception) {
                        null // 앱이 삭제되었거나 정보를 가져올 수 없는 경우, 오류를 방지하고 목록에서 제외합니다.
                    }
                }

            // 6. [결과 업데이트] 완성된 최종 목록을 'appUsageRecords' 현황판에 업데이트하여 StatisticsScreen에 알립니다.
            _appUsageRecords.value = appRecords
        }
    }

    /** 설정 화면에서 목표 시간을 변경했을 때 호출됩니다. */
    fun updateTargetTime(newTimeInSeconds: Int) {
        _targetTime.value = newTimeInSeconds
        loadMonthlyRecords()
    }

    /** 설정 화면에서 보여줄 아이콘 목록을 변경했을 때 호출됩니다. */
    fun updateSelectedIcons(newIcons: List<ActivityType>) {
        _selectedIcons.value = newIcons
    }

    /** 통계 화면에서 수동 기록의 시간을 수정했을 때 호출됩니다. */
    fun overwriteActivityTimeForToday(activity: ActivityType, newTotalSeconds: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // 해당 활동의 오늘 기록을 모두 지우고,
            recordDao.deleteRecordsByActivityAndDate(activity.name, currentLoadedDate)
            if (newTotalSeconds > 0) {
                // 새로운 시간으로 기록을 하나 다시 만듭니다.
                val newRecord = DailyRecord(date = currentLoadedDate, activity = activity, seconds = newTotalSeconds)
                recordDao.upsertRecord(newRecord)
            }
            // 데이터가 변경되었으니 달력 현황판을 업데이트합니다.
            loadMonthlyRecords()
        }
    }

    /** '오늘'의 수동 기록을 DB에서 불러와 'dailyRecords' 현황판을 업데이트합니다. */
    private fun loadDailyRecords() {
        viewModelScope.launch {
            recordDao.getRecordsForDate(currentLoadedDate).collect { _dailyRecords.value = it }
        }
    }

    /** '이번 달'의 모든 기록을 DB에서 불러와 날짜별 달성률을 계산하고, 'monthlyRecords' 현황판을 업데이트합니다. */
    private fun loadMonthlyRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val startDate = LocalDate.of(year, month, 1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endDate = LocalDate.of(
                year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            ).format(DateTimeFormatter.ISO_LOCAL_DATE)

            val records = recordDao.getRecordsBetweenDates(startDate, endDate).first()
            val targetTimeFloat = _targetTime.value.toFloat()
            val grouped = records.groupBy { LocalDate.parse(it.date) }
                .mapValues { (_, dayRecords) ->
                    val totalSecondsForDay = dayRecords.sumOf { it.seconds }
                    if (targetTimeFloat > 0) totalSecondsForDay.toFloat() / targetTimeFloat else 0f
                }
            _monthlyRecords.value = grouped
        }
    }

    // 단순 1초 틱(표시용). 계산은 항상 elapsed 기반이라 정확도 보장.
    private fun tickerFlow(periodMs: Long) = flow {
        while (true) {
            emit(Unit)
            kotlinx.coroutines.delay(periodMs)
        }
    }

}