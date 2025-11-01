package com.example.studyplanner.data.plan

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AssessmentEntity::class, SubtaskEntity::class],
    version = 2   // <-- bumped from 1 to 2
)
@TypeConverters(LocalDateTimeConverters::class)
abstract class PlanDb : RoomDatabase() {
    abstract fun dao(): PlanDao

    companion object {
        @Volatile
        private var INSTANCE: PlanDb? = null

        // migration: add remark TEXT NOT NULL DEFAULT ''
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE assessments ADD COLUMN remark TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun get(context: Context): PlanDb =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, PlanDb::class.java, "plan.db")
                    .addMigrations(MIGRATION_1_2)   // <-- important
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
