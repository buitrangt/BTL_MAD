package com.arijit.budgettracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Expense::class, SmsTemplate::class, SmsTransactionEntity::class],
    version = 5,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun smsTemplateDao(): SmsTemplateDao
    abstract fun smsTransactionDao(): SmsTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN type TEXT NOT NULL DEFAULT 'EXPENSE'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN name TEXT NOT NULL DEFAULT 'Transaction'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add missing source column on expenses
                try {
                    db.execSQL("ALTER TABLE expenses ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'")
                } catch (_: Exception) {}

                // Create sms_templates
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS sms_templates (" +
                        "id INTEGER NOT NULL PRIMARY KEY, " +
                        "senderPattern TEXT NOT NULL, " +
                        "amountRegex TEXT NOT NULL, " +
                        "type TEXT NOT NULL, " +
                        "bankName TEXT NOT NULL, " +
                        "isActive INTEGER NOT NULL DEFAULT 1, " +
                        "version INTEGER NOT NULL DEFAULT 1)"
                )

                // Create sms_transactions
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS sms_transactions (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "expenseId INTEGER NOT NULL, " +
                        "sender TEXT NOT NULL, " +
                        "rawContent TEXT NOT NULL, " +
                        "parsedAmount REAL NOT NULL, " +
                        "parsedCategory TEXT NOT NULL, " +
                        "type TEXT NOT NULL DEFAULT 'EXPENSE', " +
                        "status TEXT NOT NULL DEFAULT 'CONFIRMED', " +
                        "transactionTime INTEGER NOT NULL, " +
                        "synced INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
