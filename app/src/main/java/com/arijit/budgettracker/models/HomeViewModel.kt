package com.arijit.budgettracker.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arijit.budgettracker.api.HomeOverviewResponse
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.api.TransactionResponse
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.utils.AppRefreshBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _todayAmount = MutableLiveData(0.0)
    val todayAmount: LiveData<Double> = _todayAmount

    private val _weekAmount = MutableLiveData(0.0)
    val weekAmount: LiveData<Double> = _weekAmount

    private val _monthAmount = MutableLiveData(0.0)
    val monthAmount: LiveData<Double> = _monthAmount

    private val _monthIncome = MutableLiveData(0.0)
    val monthIncome: LiveData<Double> = _monthIncome

    private val _monthExpense = MutableLiveData(0.0)
    val monthExpense: LiveData<Double> = _monthExpense

    private val _monthSavings = MutableLiveData(0.0)
    val monthSavings: LiveData<Double> = _monthSavings

    private val _recentExpenses = MutableLiveData<List<Expense>>(emptyList())
    val recentExpenses: LiveData<List<Expense>> = _recentExpenses

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadHomeOverview() {
        // Use postValue so this can be called from any dispatcher.
        _loading.postValue(true)
        _error.postValue(null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(getApplication())
                val response = api.getHomeOverview()
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    applyData(data)
                } else {
                    _error.postValue("Không thể tải dữ liệu (${response.code()})")
                }
            } catch (e: Exception) {
                _error.postValue("Lỗi kết nối: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    private fun applyData(data: HomeOverviewResponse) {
        _todayAmount.postValue(data.todayAmount)
        _weekAmount.postValue(data.weekAmount)
        _monthAmount.postValue(data.monthAmount)
        _monthIncome.postValue(data.monthIncome)
        _monthExpense.postValue(data.monthExpense)
        _monthSavings.postValue(data.monthSavings)

        // Map TransactionResponse → Expense, limit to 5 most recent
        val expenses = data.recentTransactions.take(5).map { it.toExpense() }
        _recentExpenses.postValue(expenses)
    }

    private fun TransactionResponse.toExpense(): Expense {
        return Expense(
            id = id.toInt(),
            remoteId = id,
            amount = amount,
            name = name,
            category = categoryName ?: name,
            note = note ?: "",
            type = type.lowercase(),
            timeStamp = timeStamp,
            localCreatedAt = timeStamp,
            synced = true
        )
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(getApplication())
                val targetId = expense.remoteId ?: return@launch
                // Optimistic UI update: remove immediately from "recently"
                _recentExpenses.postValue(_recentExpenses.value.orEmpty().filterNot { it.remoteId == targetId })

                val resp = api.deleteExpense(targetId)
                if (resp.isSuccessful) {
                    // Notify all tabs to refresh immediately (Home/History/Stats).
                    AppRefreshBus.notifyChanged()
                    loadHomeOverview()
                } else {
                    _error.postValue("Không thể xóa giao dịch (${resp.code()})")
                    // Try reload anyway to keep UI consistent with server
                    loadHomeOverview()
                }
            } catch (_: Exception) {
                _error.postValue("Lỗi kết nối khi xóa giao dịch")
            }
        }
    }
}
