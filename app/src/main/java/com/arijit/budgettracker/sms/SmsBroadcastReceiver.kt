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

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SmsReceiver", "onReceive action=${intent.action}")
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Log.w("SmsReceiver", "No messages in intent")
            return
        }

        val sender = messages[0].displayOriginatingAddress ?: return
        val fullMessage = messages.joinToString("") { it.displayMessageBody ?: "" }
        Log.d("SmsReceiver", "From=$sender body=$fullMessage")
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
        val db = TransactionDatabase.getDatabase(context)
        val templates = db.smsTemplateDao().getActiveTemplates()
        Log.d("SmsReceiver", "Loaded ${templates.size} templates")

        val parsed = SmsParser.parse(smsContent, sender, templates)
        if (parsed == null) {
            Log.w("SmsReceiver", "Parse failed for sender=$sender")
            return
        }
        Log.d("SmsReceiver", "Parsed amount=${parsed.amount} type=${parsed.type} bank=${parsed.bankName}")

        val category = CategoryClassifier.classify(smsContent)
        val now = System.currentTimeMillis()

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
