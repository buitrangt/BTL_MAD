package com.arijit.budgettracker.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.db.ExpenseDatabase
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.exp

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val expenseDao = ExpenseDatabase.Companion.getDatabase(application).expenseDao()
    val latestExpenses: LiveData<List<Expense>> = expenseDao.getLatest8Expenses()
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
        viewModelScope.launch {
            expenseDao.getAllExpensesFlow().collect { expenses ->
                val now = System.currentTimeMillis()
                val startOfDay = getStartOfDay(now)
                val startOfWeek = getStartOfWeek(now)
                val startOfMonth = getStartOfMonth(now)

                // Note: timeStamp in DB is in SECONDS, need to convert to milliseconds
                _todayAmount.value = expenses.filter { (it.timeStamp * 1000) >= startOfDay }.sumOf { it.amount }
                _weekAmount.value = expenses.filter { (it.timeStamp * 1000) >= startOfWeek }.sumOf { it.amount }
                _monthAmount.value = expenses.filter { (it.timeStamp * 1000) >= startOfMonth }.sumOf { it.amount }
                
                // Tính chi tiêu tháng (chỉ EXPENSE)
                val monthlyExpenseAmount = expenses
                    .filter { (it.timeStamp * 1000) >= startOfMonth && it.type == "EXPENSE" }
                    .sumOf { it.amount }
                _monthExpense.value = monthlyExpenseAmount
                
                // Tính thu nhập tháng (chỉ INCOME)
                val monthlyIncomeAmount = expenses
                    .filter { (it.timeStamp * 1000) >= startOfMonth && it.type == "INCOME" }
                    .sumOf { it.amount }
                _monthIncome.value = monthlyIncomeAmount
                
                // Tiết kiệm = Thu nhập - Chi tiêu
                _monthSavings.value = monthlyIncomeAmount - monthlyExpenseAmount
            }
        }
    }

    private fun getStartOfDay(now: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getStartOfWeek(now: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getStartOfMonth(now: Long): Long {
        val cal = Calendar.getInstance().apply {
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
            .filter { (it.timeStamp * 1000) >= monthStart && (it.timeStamp * 1000) <= monthEnd && it.type == "INCOME" }
            .sumOf { it.amount }
        
        val expense = expenses
            .filter { (it.timeStamp * 1000) >= monthStart && (it.timeStamp * 1000) <= monthEnd && it.type == "EXPENSE" }
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
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.updateExpense(expense)
        }
    }
}