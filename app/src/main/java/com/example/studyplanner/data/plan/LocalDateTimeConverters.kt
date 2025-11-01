package com.example.studyplanner.data.plan

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class LocalDateTimeConverters {
    @TypeConverter
    fun toLocalDateTime(epochMillis: Long?): LocalDateTime? =
        epochMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }

    @TypeConverter
    fun fromLocalDateTime(dt: LocalDateTime?): Long? =
        dt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
}
