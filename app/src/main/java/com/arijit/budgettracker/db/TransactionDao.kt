package com.arijit.budgettracker.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(expense: Expense)

    @Insert
    suspend fun insertTransactionAndGetId(expense: Expense): Long

    @Insert
    suspend fun insertTransactions(expenses: List<Expense>)

    @Update
    suspend fun updateTransaction(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Expense?

    @Query("SELECT * FROM expenses WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: Long): Expense?

    @Query("SELECT * FROM expenses WHERE amount = :amount AND category = :category AND timestamp = :timeStamp LIMIT 1")
    suspend fun getOneByIdentity(amount: Double, category: String, timeStamp: Long): Expense?

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<Expense>>

    @Query("""
        SELECT *
        FROM expenses
        ORDER BY
          CASE WHEN localCreatedAt IS NULL OR localCreatedAt = 0 THEN timestamp ELSE localCreatedAt END DESC,
          id DESC
        LIMIT 8
    """)
    fun getLatest8Expenses(): LiveData<List<Expense>>

    @Delete
    suspend fun deleteTransaction(expense: Expense)

    @Query("DELETE FROM expenses WHERE remoteId = :remoteId")
    suspend fun deleteByRemoteId(remoteId: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Query("SELECT * FROM expenses WHERE synced = 0")
    suspend fun getUnsyncedTransactions(): List<Expense>

    @Query("UPDATE expenses SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Int>)

    @Query("SELECT COUNT(*) FROM expenses WHERE category = :categoryName")
    suspend fun getTransactionCountByCategory(categoryName: String): Int

    @Query("UPDATE expenses SET category = :newCategoryName WHERE category = :oldCategoryName")
    suspend fun updateTransactionCategoryName(oldCategoryName: String, newCategoryName: String)

    @Query("SELECT COUNT(*) FROM expenses WHERE amount = :amount AND category = :category AND note = :note AND type = :type AND timestamp = :timeStamp")
    suspend fun countBySignature(
        amount: Double,
        category: String,
        note: String,
        type: String,
        timeStamp: Long
    ): Int

    @Query("SELECT COUNT(*) FROM expenses WHERE amount = :amount AND category = :category AND timestamp = :timeStamp")
    suspend fun countByIdentity(
        amount: Double,
        category: String,
        timeStamp: Long
    ): Int

    @Query("UPDATE expenses SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: Int, remoteId: Long)
}
