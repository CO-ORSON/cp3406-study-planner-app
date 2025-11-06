package com.example.studyplanner.data.plan

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AssessmentEntity::class, SubtaskEntity::class],
    version = 3
)
@TypeConverters(LocalDateTimeConverters::class)
abstract class PlanDb : RoomDatabase() {
    abstract fun dao(): PlanDao

    companion object {
        @Volatile private var INSTANCE: PlanDb? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN remark TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): PlanDb =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PlanDb::class.java,
                    "plan.db"
                )
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
