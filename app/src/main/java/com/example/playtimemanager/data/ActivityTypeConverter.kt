package com.example.playtimemanager.data

import androidx.room.TypeConverter

class ActivityTypeConverter {
    @TypeConverter
    fun fromActivityType(value: ActivityType): String = value.name

    @TypeConverter
    fun toActivityType(value: String): ActivityType = ActivityType.valueOf(value)
}