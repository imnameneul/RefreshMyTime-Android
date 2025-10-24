// 파일 경로: app/src/main/java/com/example/playtimemanager/data/RecordDao.kt

package com.example.playtimemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Upsert
    suspend fun upsertRecord(record: DailyRecord)

    @Delete
    suspend fun deleteRecord(record: DailyRecord)

    // --- 변경점: 새로운 삭제 명령어 추가 ---
    @Query("DELETE FROM daily_records WHERE activity = :activityName AND date = :date")
    suspend fun deleteRecordsByActivityAndDate(activityName: String, date: String)

    @Query("SELECT * FROM daily_records WHERE date = :date ORDER BY id DESC")
    fun getRecordsForDate(date: String): Flow<List<DailyRecord>>

    @Query("SELECT * FROM daily_records WHERE date BETWEEN :startDate AND :endDate")
    fun getRecordsBetweenDates(startDate: String, endDate: String): Flow<List<DailyRecord>>
}