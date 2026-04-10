package com.arijit.budgettracker.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SmsTransactionDao {
    @Insert
    suspend fun insert(smsTransaction: SmsTransactionEntity)

    @Query("SELECT * FROM sms_transactions WHERE synced = 0")
    suspend fun getUnsynced(): List<SmsTransactionEntity>

    @Query("UPDATE sms_transactions SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Int>)
}
