package com.arijit.budgettracker.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arijit.budgettracker.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val _balance = MutableLiveData(0.0)
    val balance: LiveData<Double> = _balance

    private val _totalExpense = MutableLiveData(0.0)
    val totalExpense: LiveData<Double> = _totalExpense

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(getApplication())
                val response = api.getHomeOverview()
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    // Balance = thu nhập tháng - chi tiêu tháng
                    _balance.postValue(data.monthSavings)
                    _totalExpense.postValue(data.monthExpense)
                }
            } catch (_: Exception) {
                // ignore
            }
        }
    }
}
