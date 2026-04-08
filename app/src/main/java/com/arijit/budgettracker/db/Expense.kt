package com.arijit.budgettracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val name: String,  // Transaction name (what user is paying for)
    val category: String,  // Category name
    val type: String = "EXPENSE",  // "INCOME" hoặc "EXPENSE"
    val timeStamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    val type: String = "EXPENSE",
    val name: String? = null,
    val source: String = "MANUAL"
)
