package com.example.studyplanner.data.plan

import android.content.Context
import androidx.room.*

@Database(entities = [AssessmentEntity::class, SubtaskEntity::class], version = 1)
@TypeConverters(LocalDateTimeConverters::class)
abstract class PlanDb : RoomDatabase() {
    abstract fun dao(): PlanDao

    companion object {
        @Volatile private var INSTANCE: PlanDb? = null
        fun get(context: Context): PlanDb =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, PlanDb::class.java, "plan.db")
                    .build().also { INSTANCE = it }
            }
    }
}
