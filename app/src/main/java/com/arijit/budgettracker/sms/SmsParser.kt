package com.arijit.budgettracker.sms

import com.arijit.budgettracker.db.SmsTemplate

data class ParsedSms(
    val amount: Double,
    val type: String,
    val bankName: String
)

object SmsParser {

    private val BANK_KEYWORDS = listOf("GD", "giao dich", "so du", "tai khoan", "TK", "SD")
    private val GENERIC_AMOUNT_REGEX = Regex("""(\d{1,3}(?:[.,]\d{3})+)""")

    fun parse(smsContent: String, sender: String, templates: List<SmsTemplate>): ParsedSms? {
        val matchedTemplates = templates.filter { template ->
            sender.contains(template.senderPattern, ignoreCase = true) ||
            smsContent.contains(template.senderPattern, ignoreCase = true)
        }

        for (template in matchedTemplates) {
            val regex = Regex(template.amountRegex)
            val match = regex.find(smsContent) ?: continue
            val rawAmount = match.groupValues[1]
            val amount = cleanAmount(rawAmount) ?: continue
            if (amount <= 0) continue

            val type = if (template.type == "CREDIT") "INCOME" else "EXPENSE"
            return ParsedSms(amount, type, template.bankName)
        }

        return tryGenericParse(smsContent, sender)
    }

    private fun tryGenericParse(smsContent: String, sender: String): ParsedSms? {
        val hasBankKeyword = BANK_KEYWORDS.any { keyword ->
            smsContent.contains(keyword, ignoreCase = true)
        }
        if (!hasBankKeyword) return null

        val match = GENERIC_AMOUNT_REGEX.find(smsContent) ?: return null
        val amount = cleanAmount(match.groupValues[1]) ?: return null
        if (amount < 1000) return null

        return ParsedSms(amount, "EXPENSE", sender)
    }

    private fun cleanAmount(raw: String): Double? {
        return try {
            val cleaned = raw.replace(".", "").replace(",", "")
            cleaned.toDouble()
        } catch (e: NumberFormatException) {
            null
        }
    }
}
