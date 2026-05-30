package com.arijit.budgettracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.db.TransactionDatabase
import com.arijit.budgettracker.db.SmsTransactionEntity
import com.arijit.budgettracker.utils.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Lắng nghe SMS đến từ hệ thống, bóc tách giao dịch ngân hàng và tự động lưu vào sổ chi tiêu.
 * Đây là lớp điều phối chính của luồng "SMS Auto Thu/Chi".
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    // Hệ điều hành gọi hàm này mỗi khi có SMS mới đến
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SmsReceiver", "onReceive action=${intent.action}")
        // Chỉ xử lý đúng sự kiện nhận SMS
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // Gộp các phần của một SMS dài thành nội dung đầy đủ
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Log.w("SmsReceiver", "No messages in intent")
            return
        }

        val sender = messages[0].displayOriginatingAddress ?: return
        val fullMessage = messages.joinToString("") { it.displayMessageBody ?: "" }
        Log.d("SmsReceiver", "From=$sender body=$fullMessage")
        if (fullMessage.isBlank()) return

        // Chuyển sang xử lý bất đồng bộ (DB, mạng) để không chặn luồng chính
        val pendingResult = goAsync()

        val handler = kotlinx.coroutines.CoroutineExceptionHandler { _, e -> e.printStackTrace() }
        CoroutineScope(Dispatchers.IO + handler).launch {
            try {
                processSms(context, sender, fullMessage)
            } finally {
                pendingResult.finish()
            }
        }
    }

    // Xử lý 1 SMS: bóc tách -> phân loại -> lưu DB -> thông báo -> đồng bộ server
    private suspend fun processSms(context: Context, sender: String, smsContent: String) {
        val db = TransactionDatabase.getDatabase(context)
        // Lấy danh sách mẫu (template) ngân hàng để nhận diện
        val templates = db.smsTemplateDao().getActiveTemplates()
        Log.d("SmsReceiver", "Loaded ${templates.size} templates")

        // Bóc tách số tiền/loại giao dịch; null nghĩa là không phải SMS ngân hàng
        val parsed = SmsParser.parse(smsContent, sender, templates)
        if (parsed == null) {
            Log.w("SmsReceiver", "Parse failed for sender=$sender")
            return
        }
        Log.d("SmsReceiver", "Parsed amount=${parsed.amount} type=${parsed.type} bank=${parsed.bankName}")

        // Đoán danh mục chi tiêu từ nội dung SMS
        val category = CategoryClassifier.classify(smsContent)
        val now = System.currentTimeMillis()

        // Tạo bản ghi chi tiêu (chưa đồng bộ) và lưu vào Room DB
        val expense = Expense(
            amount = parsed.amount,
            category = category,
            timeStamp = now,
            localCreatedAt = now,
            synced = false,
            type = parsed.type.lowercase(),
            name = "SMS - ${parsed.bankName}"
        )
        val expenseId = db.expenseDao().insertTransactionAndGetId(expense)
        Log.d("SmsReceiver", "Inserted expense id=$expenseId amount=${parsed.amount}")

        // Lưu thêm bản ghi SMS gốc (audit) liên kết với giao dịch vừa tạo
        val smsTransaction = SmsTransactionEntity(
            expenseId = expenseId.toInt(),
            sender = sender,
            rawContent = smsContent,
            parsedAmount = parsed.amount,
            parsedCategory = category,
            type = parsed.type,
            transactionTime = now
        )
        db.smsTransactionDao().insert(smsTransaction)

        // Hiện thông báo đẩy để người dùng biết đã ghi nhận giao dịch
        SmsNotificationHelper.showTransactionNotification(
            context, parsed.amount, parsed.type, category, parsed.bankName
        )

        // Đẩy dữ liệu lên server nếu đang có mạng
        SyncManager.syncIfOnline(context)
    }
}
