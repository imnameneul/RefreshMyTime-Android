package com.example.playtimemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "daily_records")
@TypeConverters(ActivityTypeConverter::class)
data class DailyRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val activity: ActivityType,
    val seconds: Int
)