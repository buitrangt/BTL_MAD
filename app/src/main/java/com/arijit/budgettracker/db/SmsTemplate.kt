package com.arijit.budgettracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_templates")
data class SmsTemplate(
    @PrimaryKey val id: Long,
    val senderPattern: String,
    val amountRegex: String,
    val type: String,
    val bankName: String,
    val isActive: Boolean = true,
    val version: Int = 1
)
