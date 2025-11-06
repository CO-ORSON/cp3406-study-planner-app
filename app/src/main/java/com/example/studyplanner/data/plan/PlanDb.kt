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

        // Keep your 1→2 migration (harmless with destructive fallback)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assessments ADD COLUMN remark TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): PlanDb =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,     // app context
                    PlanDb::class.java,
                    "plan.db"
                )
                    // During dev, prefer **destructive** rebuild on any mismatch
                    .fallbackToDestructiveMigration()             // wipe & rebuild on upgrade gaps
                    .fallbackToDestructiveMigrationOnDowngrade()  // wipe on accidental version down
                    .addMigrations(MIGRATION_1_2)                 // ok to keep
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
