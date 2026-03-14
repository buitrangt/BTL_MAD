package com.arijit.budgettracker.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.arijit.budgettracker.api.ExpenseRequest
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.db.ExpenseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                        timeStamp = expense.timeStamp
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

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
