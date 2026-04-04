package com.arijit.budgettracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.db.ExpenseDatabase
import com.arijit.budgettracker.db.SmsTransactionEntity
import com.arijit.budgettracker.utils.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: return
        val fullMessage = messages.joinToString("") { it.displayMessageBody ?: "" }
        if (fullMessage.isBlank()) return

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

    private suspend fun processSms(context: Context, sender: String, smsContent: String) {
        val db = ExpenseDatabase.getDatabase(context)
        val templates = db.smsTemplateDao().getActiveTemplates()

        val parsed = SmsParser.parse(smsContent, sender, templates) ?: return

        val category = CategoryClassifier.classify(smsContent)
        val now = System.currentTimeMillis()

        val expense = Expense(
            amount = parsed.amount,
            category = category,
            timeStamp = now,
            synced = false,
            type = parsed.type,
            name = "SMS - ${parsed.bankName}",
            source = "SMS"
        )
        val expenseId = db.expenseDao().insertExpenseAndGetId(expense)

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

        SmsNotificationHelper.showTransactionNotification(
            context, parsed.amount, parsed.type, category, parsed.bankName
        )

        SyncManager.syncIfOnline(context)
    }
}
