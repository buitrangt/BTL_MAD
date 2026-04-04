package com.arijit.budgettracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Expense::class, SmsTemplate::class, SmsTransactionEntity::class],
    version = 3,
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
                db.execSQL("ALTER TABLE expenses ADD COLUMN name TEXT")
                db.execSQL("ALTER TABLE expenses ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sms_templates (
                        id INTEGER NOT NULL PRIMARY KEY,
                        senderPattern TEXT NOT NULL,
                        amountRegex TEXT NOT NULL,
                        type TEXT NOT NULL,
                        bankName TEXT NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        version INTEGER NOT NULL DEFAULT 1
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sms_transactions (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        expenseId INTEGER NOT NULL,
                        sender TEXT NOT NULL,
                        rawContent TEXT NOT NULL,
                        parsedAmount REAL NOT NULL,
                        parsedCategory TEXT NOT NULL,
                        type TEXT NOT NULL DEFAULT 'EXPENSE',
                        status TEXT NOT NULL DEFAULT 'CONFIRMED',
                        transactionTime INTEGER NOT NULL,
                        synced INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .enableMultiInstanceInvalidation()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
