package com.example.playtimemanager.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Jetpack DataStore(Preferences) 기반 타이머 상태 저장소
 * - is_tracking: 타이머 실행 여부
 * - start_elapsed_ms: SystemClock.elapsedRealtime() 기준 시작 시각(ms)
 * - accumulated_today_ms: 오늘 날짜 기준 누적 시간(ms)
 *
 * Room(DB)은 완료된 기록 저장용, DataStore는 "진행 중 상태" 유지/복원용으로 사용.
 */

// 앱 전역에서 context.timerDataStore 로 접근할 확장 프로퍼티
val Context.timerDataStore by preferencesDataStore(name = "timer_prefs")

// 저장 키
object TimerPrefsKeys {
    val IS_TRACKING = booleanPreferencesKey("is_tracking")
    val START_ELAPSED_MS = longPreferencesKey("start_elapsed_ms")
    val ACCUMULATED_TODAY_MS = longPreferencesKey("accumulated_today_ms")
}

// 레포지토리: ViewModel 등에서 사용
class TimerPrefsRepo(private val context: Context) {

    /** 전체 Preferences 스트림 */
    val data: Flow<Preferences> = context.timerDataStore.data

    /** 개별 필드 스트림 */
    val isTrackingFlow: Flow<Boolean> =
        data.map { it[TimerPrefsKeys.IS_TRACKING] ?: false }

    val startElapsedMsFlow: Flow<Long> =
        data.map { it[TimerPrefsKeys.START_ELAPSED_MS] ?: 0L }

    val accumulatedTodayMsFlow: Flow<Long> =
        data.map { it[TimerPrefsKeys.ACCUMULATED_TODAY_MS] ?: 0L }

    /** 저장 메서드 */
    suspend fun setTracking(isTracking: Boolean) {
        context.timerDataStore.edit { prefs ->
            prefs[TimerPrefsKeys.IS_TRACKING] = isTracking
        }
    }

    suspend fun setStartElapsed(ms: Long) {
        context.timerDataStore.edit { prefs ->
            prefs[TimerPrefsKeys.START_ELAPSED_MS] = ms
        }
    }

    suspend fun setAccumulated(ms: Long) {
        context.timerDataStore.edit { prefs ->
            prefs[TimerPrefsKeys.ACCUMULATED_TODAY_MS] = ms
        }
    }

    /** 누적 시간에 델타 추가(옵션) */
    suspend fun addToAccumulated(deltaMs: Long) {
        if (deltaMs == 0L) return
        context.timerDataStore.edit { prefs ->
            val cur = prefs[TimerPrefsKeys.ACCUMULATED_TODAY_MS] ?: 0L
            prefs[TimerPrefsKeys.ACCUMULATED_TODAY_MS] = (cur + deltaMs).coerceAtLeast(0L)
        }
    }

    /** 모두 초기화 (STOP/자정 리셋 등에 사용) */
    suspend fun clearAll() {
        context.timerDataStore.edit { prefs ->
            prefs[TimerPrefsKeys.IS_TRACKING] = false
            prefs[TimerPrefsKeys.START_ELAPSED_MS] = 0L
            prefs[TimerPrefsKeys.ACCUMULATED_TODAY_MS] = 0L
        }
    }
}
