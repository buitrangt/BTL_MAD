package com.arijit.budgettracker.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.arijit.budgettracker.api.ExpenseRequest
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.db.ExpenseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

object SyncManager {

    suspend fun syncIfOnline(context: Context) {
        if (!isOnline(context)) return
        if (!TokenManager.isLoggedIn(context)) return

        withContext(Dispatchers.IO) {
            try {
                val dao = ExpenseDatabase.getDatabase(context).expenseDao()
                val unsynced = dao.getUnsyncedExpenses()
                if (unsynced.isEmpty()) return@withContext

                val requests = unsynced.map { expense ->
                    ExpenseRequest(
                        amount = expense.amount,
                        category = expense.category,
                        timeStamp = expense.timeStamp,
                        note = expense.note,
                        type = expense.type
                    )
                }

                val response = RetrofitClient.getApiService(context).syncExpenses(requests)
                if (response.isSuccessful) {
                    dao.markAsSynced(unsynced.map { it.id })
                }
            } catch (e: Exception) {
                // Sync failed silently — will retry next time
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteExpenseIfOnline(context: Context, expense: Expense): Boolean {
        if (!isOnline(context)) return false
        if (!TokenManager.isLoggedIn(context)) return false

        return withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(context)
                val remoteRes = api.getAllExpenses()
                if (!remoteRes.isSuccessful) return@withContext false

                val target = remoteRes.body()?.firstOrNull {
                    almostEqual(it.amount, expense.amount) &&
                        it.timeStamp == expense.timeStamp &&
                        it.category == expense.category
                } ?: return@withContext true

                api.deleteExpense(target.id).isSuccessful
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun updateExpenseIfOnline(context: Context, oldExpense: Expense, newExpense: Expense): Boolean {
        if (!isOnline(context)) return false
        if (!TokenManager.isLoggedIn(context)) return false

        return withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(context)
                val remoteRes = api.getAllExpenses()
                if (!remoteRes.isSuccessful) return@withContext false

                val request = ExpenseRequest(
                    amount = newExpense.amount,
                    category = newExpense.category,
                    timeStamp = newExpense.timeStamp,
                    note = newExpense.note,
                    type = newExpense.type
                )

                val target = remoteRes.body()?.firstOrNull {
                    almostEqual(it.amount, oldExpense.amount) &&
                        it.timeStamp == oldExpense.timeStamp &&
                        it.category == oldExpense.category
                }

                if (target != null) {
                    api.updateExpense(target.id, request).isSuccessful
                } else {
                    false
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun almostEqual(a: Double, b: Double): Boolean {
        return abs(a - b) < 0.0001
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
