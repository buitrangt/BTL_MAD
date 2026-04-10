package com.arijit.budgettracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_transactions")
data class SmsTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expenseId: Int,
    val sender: String,
    val rawContent: String,
    val parsedAmount: Double,
    val parsedCategory: String,
    val type: String = "EXPENSE",
    val status: String = "CONFIRMED",
    val transactionTime: Long,
    val synced: Boolean = false
)
