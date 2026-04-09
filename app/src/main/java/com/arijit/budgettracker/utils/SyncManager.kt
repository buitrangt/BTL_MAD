package com.arijit.budgettracker.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.arijit.budgettracker.api.ExpenseRequest
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.api.TransactionResponse
import com.arijit.budgettracker.db.Category
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
                val db = ExpenseDatabase.getDatabase(context)
                val dao = db.expenseDao()
                val categoryDao = db.categoryDao()
                val api = RetrofitClient.getApiService(context)

                val unsynced = dao.getUnsyncedExpenses()
                if (unsynced.isNotEmpty()) {
                    val requests = unsynced.map { expense ->
                        ExpenseRequest(
                            amount = expense.amount,
                            category = expense.category,
                            timeStamp = expense.timeStamp,
                            note = expense.note.takeIf { it.isNotBlank() },
                            type = expense.type
                        )
                    }

                    val response = api.syncExpenses(requests)
                    if (response.isSuccessful) {
                        dao.markAsSynced(unsynced.map { it.id })
                        // Backfill remoteId so future updates target the correct server row.
                        response.body().orEmpty().forEach { remote ->
                            val normalizedTs = normalizeTimestamp(remote.timeStamp)
                            val local = dao.getOneByIdentity(remote.amount, remote.category, normalizedTs)
                            if (local != null && local.remoteId == null) {
                                dao.setRemoteId(local.id, remote.id)
                            }
                        }
                    }
                }

                // Pull server transactions to local so cloud data appears after login/new install.
                // Using /api/transactions avoids null category issues and preserves note/type.
                val remoteTxRes = api.getAllTransactions()
                if (remoteTxRes.isSuccessful) {
                    remoteTxRes.body().orEmpty().forEach { tx ->
                        upsertFromTransaction(dao, tx)
                    }
                }

                // Pull server categories to local for category picker.
                val remoteCategoriesRes = api.getAllCategories()
                if (remoteCategoriesRes.isSuccessful) {
                    remoteCategoriesRes.body().orEmpty().forEach { remote ->
                        val exists = categoryDao.countByName(remote.name) > 0
                        if (!exists) {
                            categoryDao.insertCategory(
                                Category(
                                    name = remote.name,
                                    description = remote.note ?: "",
                                    type = "both"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Sync failed silently — will retry next time
                e.printStackTrace()
            }
        }
    }

    private suspend fun upsertFromTransaction(dao: com.arijit.budgettracker.db.ExpenseDao, tx: TransactionResponse) {
        val normalizedTimestamp = normalizeTimestamp(tx.timeStamp)
        val category = tx.categoryName ?: tx.name
        val normalizedType = tx.type.trim().lowercase().ifEmpty { "expense" }

        // 1) Prefer matching by remoteId (stable).
        val existingByRemote = dao.getByRemoteId(tx.id)
        if (existingByRemote != null) {
            dao.updateExpense(
                existingByRemote.copy(
                    amount = tx.amount,
                    name = tx.name,
                    category = category,
                    note = tx.note ?: "",
                    type = normalizedType,
                    timeStamp = normalizedTimestamp,
                    synced = true
                )
            )
            return
        }

        // 2) If this was created locally, attach remoteId to it.
        val existingByIdentity = dao.getOneByIdentity(tx.amount, category, normalizedTimestamp)
        if (existingByIdentity != null) {
            dao.updateExpense(
                existingByIdentity.copy(
                    remoteId = tx.id,
                    amount = tx.amount,
                    name = tx.name,
                    note = tx.note ?: "",
                    type = normalizedType,
                    synced = true
                )
            )
            return
        }

        // 3) Otherwise insert as server-pulled row.
        dao.insertExpense(
            Expense(
                remoteId = tx.id,
                amount = tx.amount,
                name = tx.name,
                category = category,
                note = tx.note ?: "",
                type = normalizedType,
                timeStamp = normalizedTimestamp,
                localCreatedAt = 0L,
                synced = true
            )
        )
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
                val request = ExpenseRequest(
                    amount = newExpense.amount,
                    category = newExpense.category,
                    timeStamp = newExpense.timeStamp,
                    note = newExpense.note.takeIf { it.isNotBlank() },
                    type = newExpense.type
                )

                val targetId = newExpense.remoteId
                    ?: oldExpense.remoteId
                    ?: run {
                        val remoteRes = api.getAllExpenses()
                        if (!remoteRes.isSuccessful) return@withContext false
                        remoteRes.body()?.firstOrNull {
                            almostEqual(it.amount, oldExpense.amount) &&
                                it.timeStamp == oldExpense.timeStamp &&
                                it.category == oldExpense.category
                        }?.id
                    }

                if (targetId != null) api.updateExpense(targetId, request).isSuccessful else false
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun almostEqual(a: Double, b: Double): Boolean {
        return abs(a - b) < 0.0001
    }

    private fun normalizeTimestamp(timeStamp: Long): Long {
        return if (timeStamp in 1..9_999_999_999L) timeStamp * 1000 else timeStamp
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
