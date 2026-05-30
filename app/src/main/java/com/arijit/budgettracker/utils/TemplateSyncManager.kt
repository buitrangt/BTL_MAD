package com.arijit.budgettracker.utils

import android.content.Context
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.api.SmsTemplateDto
import com.arijit.budgettracker.db.TransactionDatabase
import com.arijit.budgettracker.db.SmsTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tải danh sách mẫu (template) ngân hàng từ server về lưu vào Room
 * để SmsParser dùng nhận diện SMS.
 */
object TemplateSyncManager {

    // Gọi API lấy template rồi ghi đè/cập nhật vào DB cục bộ
    suspend fun syncTemplates(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.getApiService(context).getSmsTemplates()
                if (response.isSuccessful) {
                    val templates = response.body()?.map { dto ->
                        SmsTemplate(
                            id = dto.id,
                            senderPattern = dto.senderPattern,
                            amountRegex = dto.amountRegex,
                            type = dto.type,
                            bankName = dto.bankName,
                            isActive = true,
                            version = dto.version
                        )
                    } ?: return@withContext

                    val dao = TransactionDatabase.getDatabase(context).smsTemplateDao()
                    dao.upsertAll(templates)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
