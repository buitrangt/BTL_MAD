package com.arijit.budgettracker.sms

import android.util.Log
import com.arijit.budgettracker.db.SmsTemplate

data class ParsedSms(
    val amount: Double,
    val type: String,
    val bankName: String
)

object SmsParser {

    private const val TAG = "SmsParser"

    private val BANK_KEYWORDS = listOf(
        "GD", "giao dich", "giao dịch", "so du", "số dư",
        "tai khoan", "tài khoản", "TK", "SD",
        "VND", "vnd", "VNĐ", "đ"
    )

    private val CREDIT_KEYWORDS = listOf(
        "nhan", "nhận", "da nhan", "đã nhận", "nhan duoc", "nhận được",
        "cong", "cộng", "ghi co", "ghi có",
        "chuyen den", "chuyển đến", "chuyen toi", "chuyển tới",
        "nap tien", "nạp tiền", "hoan tien", "hoàn tiền",
        "tien vao", "tiền vào", "thu nhap", "thu nhập",
        "luong", "lương",
        "+"
    )
    private val DEBIT_KEYWORDS = listOf(
        "thanh toan", "thanh toán", "chi tieu", "chi tiêu",
        "rut tien", "rút tiền", "ghi no", "ghi nợ",
        "tien ra", "tiền ra",
        "chuyen di", "chuyển đi", "chuyen khoan di", "chuyển khoản đi",
        "mua ", "tra goi", "trả gói",
        "-"
    )

    private val AMOUNT_REGEX = Regex("""(\d{1,3}(?:[.,]\d{3})+|\d{4,})""")

    fun parse(smsContent: String, sender: String, templates: List<SmsTemplate>): ParsedSms? {
        val bankName = identifyBank(smsContent, sender, templates) ?: sender

        if (!hasBankKeyword(smsContent)) {
            Log.d(TAG, "No bank keyword in: $smsContent")
            return null
        }

        val amount = extractAmount(smsContent) ?: run {
            Log.d(TAG, "No amount in: $smsContent")
            return null
        }
        if (amount < 1000) return null

        val type = detectType(smsContent)
        Log.d(TAG, "Parsed amount=$amount type=$type bank=$bankName from: $smsContent")
        return ParsedSms(amount, type, bankName)
    }

    private fun identifyBank(
        smsContent: String,
        sender: String,
        templates: List<SmsTemplate>
    ): String? {
        val match = templates.firstOrNull { template ->
            sender.contains(template.senderPattern, ignoreCase = true) ||
                smsContent.contains(template.senderPattern, ignoreCase = true)
        }
        return match?.bankName
    }

    private fun hasBankKeyword(smsContent: String): Boolean {
        return BANK_KEYWORDS.any { smsContent.contains(it, ignoreCase = true) }
    }

    private fun extractAmount(smsContent: String): Double? {
        val cleaned = AMOUNT_REGEX.find(smsContent)?.groupValues?.get(1) ?: return null
        return try {
            cleaned.replace(".", "").replace(",", "").toDouble()
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun detectType(smsContent: String): String {
        val lower = smsContent.lowercase()
        val creditIdx = CREDIT_KEYWORDS
            .mapNotNull { kw -> lower.indexOf(kw.lowercase()).takeIf { it >= 0 } }
            .minOrNull()
        val debitIdx = DEBIT_KEYWORDS
            .mapNotNull { kw -> lower.indexOf(kw.lowercase()).takeIf { it >= 0 } }
            .minOrNull()

        return when {
            creditIdx != null && (debitIdx == null || creditIdx < debitIdx) -> "INCOME"
            debitIdx != null -> "EXPENSE"
            else -> "EXPENSE"
        }
    }
}
