package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DeckEntity::class,
        FlashcardEntity::class,
        StudyLogEntity::class,
        DailyStreakEntity::class,
        QuizResultEntity::class,
        FolderEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviseDao(): ReviseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `folders` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`colorHex` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE `decks` ADD COLUMN `folderId` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_nextReviewDate` ON `flashcards` (`nextReviewDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_logs_timestamp` ON `study_logs` (`timestamp`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `flashcards` ADD COLUMN `lastRating` TEXT NOT NULL DEFAULT ''")
            }
        }

        // DB v5: sync fields (stable uuid, last-write-wins timestamp, soft-delete
        // flag) on every syncable entity. Existing rows get backfilled uuids so
        // nothing is ever pushed with an empty identity.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `decks` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `decks` ADD COLUMN `updatedAtMillis` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `decks` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE `flashcards` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `flashcards` ADD COLUMN `updatedAtMillis` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `flashcards` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE `study_logs` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `study_logs` ADD COLUMN `updatedAtMillis` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `study_logs` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE `daily_streaks` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `daily_streaks` ADD COLUMN `updatedAtMillis` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `daily_streaks` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE `quiz_results` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `quiz_results` ADD COLUMN `updatedAtMillis` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `quiz_results` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE `folders` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `folders` ADD COLUMN `updatedAtMillis` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `folders` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")

                // Backfill identities: random for autoincrement rows (they only
                // exist on one device), deterministic for streak days so both
                // devices agree on the uuid of the same calendar day.
                db.execSQL("UPDATE `decks` SET `uuid` = lower(hex(randomblob(16))) WHERE `uuid` = ''")
                db.execSQL("UPDATE `flashcards` SET `uuid` = lower(hex(randomblob(16))) WHERE `uuid` = ''")
                db.execSQL("UPDATE `study_logs` SET `uuid` = lower(hex(randomblob(16))) WHERE `uuid` = ''")
                db.execSQL("UPDATE `daily_streaks` SET `uuid` = 'streak-' || `dateString` WHERE `uuid` = ''")
                db.execSQL("UPDATE `quiz_results` SET `uuid` = lower(hex(randomblob(16))) WHERE `uuid` = ''")
                db.execSQL("UPDATE `folders` SET `uuid` = lower(hex(randomblob(16))) WHERE `uuid` = ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reviseiq_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
