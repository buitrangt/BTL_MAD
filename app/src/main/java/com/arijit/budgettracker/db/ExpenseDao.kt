package com.arijit.budgettracker.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: Expense)

    @Insert
    suspend fun insertExpenses(expenses: List<Expense>)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpensesFlow(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE timestamp <= (CAST(strftime('%s','now') AS INTEGER) * 1000) ORDER BY timestamp DESC LIMIT 3")
    fun getLatest8Expenses(): LiveData<List<Expense>>

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Query("SELECT * FROM expenses WHERE synced = 0")
    suspend fun getUnsyncedExpenses(): List<Expense>

    @Query("UPDATE expenses SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Int>)

    @Query("SELECT COUNT(*) FROM expenses WHERE category = :categoryName")
    suspend fun getExpenseCountByCategory(categoryName: String): Int

    @Query("UPDATE expenses SET category = :newCategoryName WHERE category = :oldCategoryName")
    suspend fun updateExpenseCategoryName(oldCategoryName: String, newCategoryName: String)
}
