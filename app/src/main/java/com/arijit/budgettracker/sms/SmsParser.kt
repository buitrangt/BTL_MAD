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
    private const val MIN_AMOUNT = 1000.0
    private const val MAX_AMOUNT = 10_000_000_000.0

    private val BANK_KEYWORDS = listOf(
        "GD", "giao dich", "giao dịch", "so du", "số dư",
        "tai khoan", "tài khoản", "TK", "SD",
        "VND", "VNĐ"
    )

    private val CREDIT_KEYWORDS = listOf(
        "nhan", "nhận", "da nhan", "đã nhận", "nhan duoc", "nhận được",
        "cong", "cộng", "ghi co", "ghi có",
        "chuyen den", "chuyển đến", "chuyen toi", "chuyển tới",
        "nap tien", "nạp tiền", "hoan tien", "hoàn tiền",
        "tien vao", "tiền vào", "thu nhap", "thu nhập",
        "luong", "lương"
    )

    private val DEBIT_KEYWORDS = listOf(
        "thanh toan", "thanh toán", "chi tieu", "chi tiêu",
        "rut tien", "rút tiền", "ghi no", "ghi nợ",
        "tien ra", "tiền ra",
        "chuyen di", "chuyển đi", "chuyen khoan di", "chuyển khoản đi",
        "mua", "tra goi", "trả gói"
    )

    // Ưu tiên số có hậu tố tiền tệ; capture dấu +/- ngay trước (nếu có).
    private val SIGNED_AMOUNT_WITH_CURRENCY = Regex(
        """([+\-])?\s*(\d{1,3}(?:[.,]\d{3})+|\d+)\s*(?:VND|vnd|VNĐ|vnđ|đ|Đ)(?![A-Za-zÀ-ỹ])"""
    )

    // Fallback: số có định dạng nghìn VN (1.234.567 hoặc 1,234,567), tránh match trong chuỗi dài.
    private val FORMATTED_AMOUNT = Regex(
        """(?<![\d.,])([+\-])?\s*(\d{1,3}(?:\.\d{3})+|\d{1,3}(?:,\d{3})+)(?![.,]?\d)"""
    )

    fun parse(smsContent: String, sender: String, templates: List<SmsTemplate>): ParsedSms? {
        val bankName = identifyBank(smsContent, sender, templates) ?: sender

        if (!hasBankKeyword(smsContent)) {
            Log.d(TAG, "No bank keyword in: $smsContent")
            return null
        }

        val (amount, signHint) = extractAmountWithSign(smsContent) ?: run {
            Log.d(TAG, "No amount in: $smsContent")
            return null
        }
        if (amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
            Log.d(TAG, "Amount $amount out of range, skip: $smsContent")
            return null
        }

        val type = detectType(smsContent, signHint)
        Log.d(TAG, "Parsed amount=$amount type=$type bank=$bankName sign=$signHint")
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
        return BANK_KEYWORDS.any { kw -> containsWord(smsContent, kw) >= 0 }
    }

    /**
     * Trích amount + dấu +/- kề bên. Ưu tiên số có hậu tố tiền tệ,
     * fallback về số có dấu phân cách nghìn. Trả (amount, signHint?).
     */
    private fun extractAmountWithSign(smsContent: String): Pair<Double, String?>? {
        SIGNED_AMOUNT_WITH_CURRENCY.find(smsContent)?.let { m ->
            val sign = m.groupValues[1].takeIf { it.isNotBlank() }
            val num = parseNumber(m.groupValues[2])
            if (num != null) return num to sign
        }
        return FORMATTED_AMOUNT.findAll(smsContent)
            .mapNotNull { m ->
                val sign = m.groupValues[1].takeIf { it.isNotBlank() }
                val n = parseNumber(m.groupValues[2]) ?: return@mapNotNull null
                n to sign
            }
            .filter { it.first in MIN_AMOUNT..MAX_AMOUNT }
            .maxByOrNull { it.first }
    }

    private fun parseNumber(raw: String): Double? = try {
        raw.replace(".", "").replace(",", "").toDouble()
    } catch (e: NumberFormatException) {
        null
    }

    private fun detectType(smsContent: String, signHint: String?): String {
        if (signHint == "+") return "INCOME"
        if (signHint == "-") return "EXPENSE"

        val creditIdx = CREDIT_KEYWORDS
            .mapNotNull { kw -> containsWord(smsContent, kw).takeIf { it >= 0 } }
            .minOrNull()
        val debitIdx = DEBIT_KEYWORDS
            .mapNotNull { kw -> containsWord(smsContent, kw).takeIf { it >= 0 } }
            .minOrNull()

        return when {
            creditIdx != null && (debitIdx == null || creditIdx < debitIdx) -> "INCOME"
            debitIdx != null -> "EXPENSE"
            else -> "EXPENSE"
        }
    }

    /**
     * So khớp từ khóa với ranh giới phi-chữ-cái (hỗ trợ tiếng Việt có dấu).
     * Trả vị trí xuất hiện đầu tiên, hoặc -1 nếu không tìm thấy.
     */
    private fun containsWord(text: String, word: String): Int {
        if (word.isEmpty()) return -1
        val lower = text.lowercase()
        val kw = word.lowercase()
        var from = 0
        while (from <= lower.length - kw.length) {
            val found = lower.indexOf(kw, from)
            if (found < 0) return -1
            val beforeOk = found == 0 || !lower[found - 1].isLetter()
            val afterIdx = found + kw.length
            val afterOk = afterIdx == lower.length || !lower[afterIdx].isLetter()
            if (beforeOk && afterOk) return found
            from = found + 1
        }
        return -1
    }
}
