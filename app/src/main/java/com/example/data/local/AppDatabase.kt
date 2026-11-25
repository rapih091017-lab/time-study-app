package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.LapEntity
import com.example.data.model.MasterItemEntity
import com.example.data.model.SessionEntity
import com.example.data.model.StopwatchElementEntity

@Database(
    entities = [
        SessionEntity::class,
        StopwatchElementEntity::class,
        LapEntity::class,
        MasterItemEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timeStudyDao(): TimeStudyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN sessionName TEXT NOT NULL DEFAULT 'Sesi 1'")
                db.execSQL("ALTER TABLE laps ADD COLUMN sessionName TEXT NOT NULL DEFAULT 'Sesi 1'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS master_items (" +
                            "category TEXT NOT NULL, " +
                            "value TEXT NOT NULL, " +
                            "PRIMARY KEY(category, value))"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stopwatch_elements ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS work_instructions (
                        id TEXT NOT NULL PRIMARY KEY,
                        type TEXT NOT NULL,
                        titleBanner TEXT NOT NULL,
                        processName TEXT NOT NULL,
                        documentNo TEXT NOT NULL,
                        date TEXT NOT NULL,
                        createdByName TEXT NOT NULL,
                        line TEXT NOT NULL,
                        machine TEXT NOT NULL,
                        building TEXT NOT NULL,
                        importantNotes TEXT NOT NULL,
                        stepsJson TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDraft INTEGER NOT NULL DEFAULT 0
                    )""".trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS work_instructions")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN observerName TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timestudy_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
