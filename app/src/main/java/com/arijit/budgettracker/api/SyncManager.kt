package com.arijit.budgettracker.api

import android.content.Context
import android.util.Log
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.db.ExpenseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncManager(private val context: Context) {
    private val apiService = RetrofitClient.getApiService(context)
    private val expenseDao = ExpenseDatabase.Companion.getDatabase(context).expenseDao()

    suspend fun syncExpensesFromAPI() {
        try {
            withContext(Dispatchers.IO) {
                Log.d("SyncManager", "Starting sync from API...")
                
                // Gọi API /api/transactions để lấy danh sách giao dịch (bao gồm cả type)
                val response = apiService.getAllTransactions()
                
                if (response.isSuccessful && response.body() != null) {
                    val transactionResponses = response.body()!!
                    Log.d("SyncManager", "Fetched ${transactionResponses.size} transactions from API")
                    
                    // Xóa dữ liệu cũ
                    expenseDao.deleteAll()
                    
                    // Chuyển đổi TransactionResponse thành Expense entity
                    val expenses = transactionResponses.map { transactionResponse ->
                        Expense(
                            id = transactionResponse.id.toInt(),
                            amount = transactionResponse.amount.toDouble(),
                            name = transactionResponse.name,  // Transaction name (what user is paying for)
                            category = transactionResponse.categoryName ?: transactionResponse.name,  // Category name
                            type = transactionResponse.type, // Lấy type từ API response (INCOME hoặc EXPENSE)
                            timeStamp = transactionResponse.timeStamp,
                            synced = true
                        )
                    }
                    
                    // Lưu tất cả vào Room một lần
                    if (expenses.isNotEmpty()) {
                        expenseDao.insertExpenses(expenses)
                        Log.d("SyncManager", "Inserted ${expenses.size} expenses to Room DB")
                    }
                    
                    Log.d("SyncManager", "Sync completed successfully with ${expenses.size} transactions")
                } else {
                    Log.d("SyncManager", "API response failed: ${response.code()}")
                    Log.d("SyncManager", "Error body: ${response.errorBody()?.string()}")
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync failed: ${e.message}", e)
            e.printStackTrace()
        }
    }
}
