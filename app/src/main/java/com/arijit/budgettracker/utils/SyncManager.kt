package com.arijit.budgettracker.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.arijit.budgettracker.api.TransactionRequest
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.api.TransactionResponse
import com.arijit.budgettracker.db.Category
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.db.TransactionDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Lớp quản lý đồng bộ dữ liệu giữa thiết bị và máy chủ.
 * Thuộc luồng chức năng: Quản lý giao dịch (Thêm/Sửa/Xóa giao dịch ngoại tuyến và trực tuyến).
 */
object SyncManager {

    // 1. Đồng bộ các giao dịch mới thêm/sửa/xóa từ local lên máy chủ và ngược lại
    suspend fun syncIfOnline(context: Context) {
        if (!isOnline(context)) return
        if (!TokenManager.isLoggedIn(context)) return

        withContext(Dispatchers.IO) {
            try {
                val db = TransactionDatabase.getDatabase(context)
                val dao = db.expenseDao()
                val categoryDao = db.categoryDao()
                val api = RetrofitClient.getApiService(context)

                val unsynced = dao.getUnsyncedTransactions()
                if (unsynced.isNotEmpty()) {
                    val requests = unsynced.map { expense ->
                        TransactionRequest(
                            name = expense.name,
                            amount = expense.amount,
                            categoryName = expense.category,
                            timeStamp = expense.timeStamp,
                            note = expense.note.takeIf { it.isNotBlank() },
                            type = expense.type
                        )
                    }

                    val response = api.syncTransactions(requests)
                    if (response.isSuccessful) {
                        dao.markAsSynced(unsynced.map { it.id })
                        // Backfill remoteId so future updates target the correct server row.
                        response.body().orEmpty().forEach { remote ->
                            val normalizedTs = normalizeTimestamp(remote.timeStamp)
                            val local = dao.getOneByIdentity(remote.amount, remote.category ?: "", normalizedTs)
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

    // Thêm mới hoặc cập nhật 1 giao dịch lấy từ server vào DB cục bộ (tránh trùng lặp)
    private suspend fun upsertFromTransaction(dao: com.arijit.budgettracker.db.TransactionDao, tx: TransactionResponse) {
        val normalizedTimestamp = normalizeTimestamp(tx.timeStamp)
        val category = tx.category ?: tx.name
        val normalizedType = tx.type.trim().lowercase().ifEmpty { "expense" }

        // 1) Prefer matching by remoteId (stable).
        val existingByRemote = dao.getByRemoteId(tx.id)
        if (existingByRemote != null) {
            dao.updateTransaction(
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
            dao.updateTransaction(
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
        dao.insertTransaction(
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

    // 2. Logic gọi API xóa giao dịch trên máy chủ
    suspend fun deleteTransactionIfOnline(context: Context, expense: Expense): Boolean {
        if (!isOnline(context)) return false
        if (!TokenManager.isLoggedIn(context)) return false

        return withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(context)
                val remoteRes = api.getAllTransactions()
                if (!remoteRes.isSuccessful) return@withContext false

                val target = remoteRes.body()?.firstOrNull {
                    almostEqual(it.amount, expense.amount) &&
                        it.timeStamp == expense.timeStamp &&
                        it.category == expense.category
                } ?: return@withContext true

                api.deleteTransaction(target.id).isSuccessful
            } catch (_: Exception) {
                false
            }
        }
    }

    // 3. Logic gọi API cập nhật (sửa) giao dịch trên máy chủ
    suspend fun updateTransactionIfOnline(context: Context, oldExpense: Expense, newExpense: Expense): Boolean {
        if (!isOnline(context)) return false
        if (!TokenManager.isLoggedIn(context)) return false

        return withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(context)
                val request = TransactionRequest(
                    name = newExpense.name,
                    amount = newExpense.amount,
                    categoryName = newExpense.category,
                    timeStamp = newExpense.timeStamp,
                    note = newExpense.note.takeIf { it.isNotBlank() },
                    type = newExpense.type
                )

                val targetId = newExpense.remoteId
                    ?: oldExpense.remoteId
                    ?: run {
                        val remoteRes = api.getAllTransactions()
                        if (!remoteRes.isSuccessful) return@withContext false
                        remoteRes.body()?.firstOrNull {
                            almostEqual(it.amount, oldExpense.amount) &&
                                it.timeStamp == oldExpense.timeStamp &&
                                it.category == oldExpense.category
                        }?.id
                    }

                if (targetId != null) api.updateTransaction(targetId, request).isSuccessful else false
            } catch (_: Exception) {
                false
            }
        }
    }

    // So sánh 2 số tiền gần bằng nhau (tránh sai số dấu phẩy động)
    private fun almostEqual(a: Double, b: Double): Boolean {
        return abs(a - b) < 0.0001
    }

    // Chuẩn hóa mốc thời gian về mili-giây (server có thể trả về giây)
    private fun normalizeTimestamp(timeStamp: Long): Long {
        return if (timeStamp in 1..9_999_999_999L) timeStamp * 1000 else timeStamp
    }

    // Kiểm tra thiết bị có đang kết nối Internet không
    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
