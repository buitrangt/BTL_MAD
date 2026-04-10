package com.arijit.budgettracker.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SmsTemplateDao {
    @Query("SELECT * FROM sms_templates WHERE isActive = 1")
    suspend fun getActiveTemplates(): List<SmsTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(templates: List<SmsTemplate>)

    @Query("DELETE FROM sms_templates")
    suspend fun deleteAll()
}
