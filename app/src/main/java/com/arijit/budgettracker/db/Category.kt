package com.arijit.budgettracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val icon: String = "", // optional, for future use
    val type: String = "both", // "expense", "income", or "both"
    val description: String = "", // category note/description
    val createdAt: Long = System.currentTimeMillis() // for sorting newly created first
)
