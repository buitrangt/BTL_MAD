package com.arijit.budgettracker.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.db.ExpenseDatabase
import com.arijit.budgettracker.utils.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.max

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val expenseDao = ExpenseDatabase.Companion.getDatabase(application).expenseDao()
    // Use a stable in-memory sort to avoid "LIMIT + mixed seconds/millis" glitches.
    private val _recentExpenses = MutableLiveData<List<Expense>>(emptyList())
    val recentExpenses: LiveData<List<Expense>> = _recentExpenses
    val allExpenses: LiveData<List<Expense>> = expenseDao.getAllExpensesFlow().asLiveData()
    
    private val _todayAmount = MutableLiveData<Double>()
    val todayAmount: LiveData<Double> = _todayAmount
    private val _weekAmount = MutableLiveData<Double>()
    val weekAmount: LiveData<Double> = _weekAmount
    private val _monthAmount = MutableLiveData<Double>()
    val monthAmount: LiveData<Double> = _monthAmount
    
    // Month summary: tính tất cả các giao dịch trong tháng (không phân loại income/expense)
    private val _monthIncome = MutableLiveData<Double>()
    val monthIncome: LiveData<Double> = _monthIncome
    private val _monthExpense = MutableLiveData<Double>()
    val monthExpense: LiveData<Double> = _monthExpense
    private val _monthSavings = MutableLiveData<Double>()
    val monthSavings: LiveData<Double> = _monthSavings

    init {
        viewModelScope.launch(Dispatchers.Default) {
            expenseDao.getAllExpensesFlow().collect { expenses ->
                val now = System.currentTimeMillis()
                val startOfDay = getStartOfDay(now)
                val endOfDay = startOfDay + (24 * 60 * 60 * 1000) // +1 day
                val startOfWeek = getStartOfWeek(now)
                val endOfWeek = startOfWeek + (7 * 24 * 60 * 60 * 1000) // +7 days
                val startOfMonth = getStartOfMonth(now)
                
                // Calculate end of month (start of next month)
                val nextMonthCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok")).apply {
                    timeInMillis = startOfMonth
                    add(Calendar.MONTH, 1)
                }
                val endOfMonth = nextMonthCal.timeInMillis

                // Some existing data may store timestamps in seconds; normalize to millis.
                fun tsMillis(ts: Long): Long = if (ts in 1..9_999_999_999L) ts * 1000 else ts

                val recent = expenses
                    .sortedByDescending { e ->
                        max(tsMillis(e.localCreatedAt), tsMillis(e.timeStamp))
                    }
                    .take(8)
                _recentExpenses.postValue(recent)

                // Home cards are "spending" cards, so only count expense type.
                _todayAmount.postValue(
                    expenses
                    .filter { it.type == "expense" }
                    .filter { tsMillis(it.timeStamp) >= startOfDay && tsMillis(it.timeStamp) < endOfDay }
                    .sumOf { it.amount }
                )
                _weekAmount.postValue(
                    expenses
                    .filter { it.type == "expense" }
                    .filter { tsMillis(it.timeStamp) >= startOfWeek && tsMillis(it.timeStamp) < endOfWeek }
                    .sumOf { it.amount }
                )
                _monthAmount.postValue(
                    expenses
                    .filter { it.type == "expense" }
                    .filter { tsMillis(it.timeStamp) >= startOfMonth && tsMillis(it.timeStamp) < endOfMonth }
                    .sumOf { it.amount }
                )
                
                // Tính chi tiêu tháng (chỉ expense)
                val monthlyExpenseAmount = expenses
                    .filter { it.type == "expense" }
                    .filter { tsMillis(it.timeStamp) >= startOfMonth && tsMillis(it.timeStamp) < endOfMonth }
                    .sumOf { it.amount }
                _monthExpense.postValue(monthlyExpenseAmount)
                
                // Tính thu nhập tháng (chỉ income)
                val monthlyIncomeAmount = expenses
                    .filter { it.type == "income" }
                    .filter { tsMillis(it.timeStamp) >= startOfMonth && tsMillis(it.timeStamp) < endOfMonth }
                    .sumOf { it.amount }
                _monthIncome.postValue(monthlyIncomeAmount)
                
                // Tiết kiệm = Thu nhập - Chi tiêu
                _monthSavings.postValue(monthlyIncomeAmount - monthlyExpenseAmount)
            }
        }

        // Do not override local calculations with server stats on Home.
    }

    private fun getStartOfDay(now: Long): Long {
        val bangkok = TimeZone.getTimeZone("Asia/Bangkok")
        val cal = Calendar.getInstance(bangkok).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Convert to milliseconds from epoch (accounts for timezone offset)
        return cal.timeInMillis
    }

    private fun getStartOfWeek(now: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok")).apply {
            timeInMillis = now
            firstDayOfWeek = Calendar.MONDAY
            // Get day of week (1=Sunday, 2=Monday, ..., 7=Saturday)
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            // Calculate days to subtract to get to Monday
            // If Monday (2): 0 days, If Sunday (1): 6 days back, etc.
            val daysToSubtract = (dayOfWeek + 5) % 7
            add(Calendar.DAY_OF_MONTH, -daysToSubtract)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getStartOfMonth(now: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok")).apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    // Monthly stats for any given month
    fun getMonthlyStats(monthStart: Long): Triple<Double, Double, Double> {
        val monthEnd = getEndOfMonth(monthStart)
        val expenses = allExpenses.value ?: emptyList()
        
        val income = expenses
            .filter { it.timeStamp >= monthStart && it.timeStamp <= monthEnd && it.type == "income" }
            .sumOf { it.amount }
        
        val expense = expenses
            .filter { it.timeStamp >= monthStart && it.timeStamp <= monthEnd && it.type == "expense" }
            .sumOf { it.amount }
        
        val savings = income - expense
        return Triple(income, expense, savings)
    }

    private fun getEndOfMonth(monthStart: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = monthStart
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expense)
            SyncManager.deleteExpenseIfOnline(getApplication(), expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.updateExpense(expense)
            SyncManager.syncIfOnline(getApplication())
        }
    }
}