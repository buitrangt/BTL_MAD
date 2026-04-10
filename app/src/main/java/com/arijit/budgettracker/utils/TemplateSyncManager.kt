package com.arijit.budgettracker.utils

import android.content.Context
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.api.SmsTemplateDto
import com.arijit.budgettracker.db.ExpenseDatabase
import com.arijit.budgettracker.db.SmsTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TemplateSyncManager {

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

                    val dao = ExpenseDatabase.getDatabase(context).smsTemplateDao()
                    dao.upsertAll(templates)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
