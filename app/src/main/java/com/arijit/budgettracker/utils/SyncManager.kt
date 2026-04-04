package com.arijit.budgettracker.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.arijit.budgettracker.api.ExpenseRequest
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.api.SmsTransactionRequest
import com.arijit.budgettracker.db.ExpenseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyncManager {

    suspend fun syncIfOnline(context: Context) {
        if (!isOnline(context)) return
        if (!TokenManager.isLoggedIn(context)) return

        withContext(Dispatchers.IO) {
            try {
                syncExpenses(context)
                syncSmsTransactions(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun syncExpenses(context: Context) {
        val dao = ExpenseDatabase.getDatabase(context).expenseDao()
        val unsynced = dao.getUnsyncedExpenses()
        if (unsynced.isEmpty()) return

        val requests = unsynced.map { expense ->
            ExpenseRequest(
                amount = expense.amount,
                category = expense.category,
                timeStamp = expense.timeStamp,
                type = expense.type,
                name = expense.name,
                source = expense.source
            )
        }

        val response = RetrofitClient.getApiService(context).syncExpenses(requests)
        if (response.isSuccessful) {
            dao.markAsSynced(unsynced.map { it.id })
        }
    }

    private suspend fun syncSmsTransactions(context: Context) {
        val smsDao = ExpenseDatabase.getDatabase(context).smsTransactionDao()
        val unsynced = smsDao.getUnsynced()
        if (unsynced.isEmpty()) return

        val requests = unsynced.map { sms ->
            SmsTransactionRequest(
                sender = sms.sender,
                rawContent = sms.rawContent,
                parsedAmount = sms.parsedAmount,
                parsedCategoryName = sms.parsedCategory,
                type = if (sms.type == "INCOME") "CREDIT" else "DEBIT",
                transactionTime = sms.transactionTime
            )
        }

        val response = RetrofitClient.getApiService(context).syncSmsTransactions(requests)
        if (response.isSuccessful) {
            smsDao.markAsSynced(unsynced.map { it.id })
        }
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
