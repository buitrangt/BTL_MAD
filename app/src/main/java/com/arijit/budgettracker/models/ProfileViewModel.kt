package com.arijit.budgettracker.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arijit.budgettracker.db.ExpenseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = ExpenseDatabase.getDatabase(application).expenseDao()

    private val _balance = MutableLiveData(0.0)
    val balance: LiveData<Double> = _balance

    private val _totalExpense = MutableLiveData(0.0)
    val totalExpense: LiveData<Double> = _totalExpense

    init {
        viewModelScope.launch(Dispatchers.Default) {
            dao.getAllExpensesFlow().collect { expenses ->
                val income = expenses.filter { it.type == "income" }.sumOf { it.amount }
                val expense = expenses.filter { it.type == "expense" }.sumOf { it.amount }
                _balance.postValue(income - expense)
                _totalExpense.postValue(expense)
            }
        }
    }
}

