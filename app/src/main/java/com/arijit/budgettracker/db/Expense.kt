package com.arijit.budgettracker.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val remoteId: Long? = null,
    val amount: Double,
    val name: String,  // Transaction name (what user is paying for)
    val category: String,  // Category name
    val note: String = "",
    val type: String = "expense",  // "income" hoặc "expense"
    @ColumnInfo(name = "timestamp") val timeStamp: Long = System.currentTimeMillis(),
    val localCreatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
