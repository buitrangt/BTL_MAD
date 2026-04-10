package com.arijit.budgettracker.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.api.WeeklyOverviewResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val _weeklyOverview = MutableLiveData<WeeklyOverviewResponse?>()
    val weeklyOverview: LiveData<WeeklyOverviewResponse?> = _weeklyOverview

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadWeeklyOverview() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(getApplication())
                val response = api.getWeeklyOverview()
                if (response.isSuccessful && response.body() != null) {
                    _weeklyOverview.postValue(response.body())
                } else {
                    _error.postValue("Không thể tải dữ liệu")
                }
            } catch (e: Exception) {
                _error.postValue("Lỗi kết nối: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
        }
    }
}
