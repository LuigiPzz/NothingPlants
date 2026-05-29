package com.example.nothingplants.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(entities = [Plant::class, WateringLog::class, Reminder::class, ShoppingItem::class, InventoryItem::class, NotificationHistory::class], version = 12, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun reminderDao(): ReminderDao
    abstract fun shoppingInventoryDao(): ShoppingInventoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN adoptionDate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE plants ADD COLUMN aiSummary TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watering_logs ADD COLUMN logType TEXT NOT NULL DEFAULT 'WATERING'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `plantId` INTEGER NOT NULL, `type` TEXT NOT NULL, `dueDate` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, FOREIGN KEY(`plantId`) REFERENCES `plants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN room TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN originalImageUri TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_plantId ON reminders(plantId)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Controlla se la colonna originalImageUri esiste già nella tabella plants
                val cursor = db.query("PRAGMA table_info(plants)")
                var hasOriginalImageUri = false
                try {
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex != -1) {
                        while (cursor.moveToNext()) {
                            if (cursor.getString(nameIndex) == "originalImageUri") {
                                hasOriginalImageUri = true
                                break
                            }
                        }
                    }
                } finally {
                    cursor.close()
                }

                if (!hasOriginalImageUri) {
                    db.execSQL("ALTER TABLE plants ADD COLUMN originalImageUri TEXT")
                }

                // Crea l'indice se non esiste già
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_plantId ON reminders(plantId)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watering_logs ADD COLUMN luxValue REAL")
                db.execSQL("ALTER TABLE watering_logs ADD COLUMN compassDirection TEXT")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN potDiameter INTEGER")
                db.execSQL("ALTER TABLE watering_logs ADD COLUMN newPotDiameter INTEGER")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shopping_list` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `isPurchased` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `inventory` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `levelPercent` INTEGER NOT NULL, `description` TEXT)"
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notification_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `message` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nothing_plants_db"
                )
                .addMigrations(
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
