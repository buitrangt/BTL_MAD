package com.arijit.budgettracker.utils

import android.content.Context
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyPrefs {
    private const val PREFS_NAME = "budget_prefs"
    private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
    private const val DEFAULT_SYMBOL = "đ"

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        DecimalFormat("#,##0", symbols)
    }

    fun getSymbol(context: Context): String {
        // App fixed to VND
        return DEFAULT_SYMBOL
    }

    fun setSymbol(context: Context, symbol: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
    }

    /**
     * Format amount as Vietnamese currency: "22.000đ"
     */
    fun format(amount: Double): String {
        return "${formatter.format(amount)}$DEFAULT_SYMBOL"
    }

    fun format(amount: Long): String {
        return "${formatter.format(amount)}$DEFAULT_SYMBOL"
    }
}
