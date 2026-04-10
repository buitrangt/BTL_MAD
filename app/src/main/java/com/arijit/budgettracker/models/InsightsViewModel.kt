package com.arijit.budgettracker.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arijit.budgettracker.api.InsightsSummaryDto
import com.arijit.budgettracker.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val _summary = MutableLiveData<InsightsSummaryDto?>()
    val summary: LiveData<InsightsSummaryDto?> = _summary

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun load() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(getApplication())
                val response = api.getInsightsSummary()
                if (response.isSuccessful && response.body() != null) {
                    _summary.postValue(response.body())
                } else {
                    _error.postValue("Không tải được phân tích (${response.code()})")
                }
            } catch (e: Exception) {
                _error.postValue("Lỗi kết nối: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun refresh() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(getApplication())
                val response = api.refreshInsights()
                if (response.isSuccessful && response.body() != null) {
                    _summary.postValue(response.body())
                } else {
                    _error.postValue("Không cập nhật được (${response.code()})")
                }
            } catch (e: Exception) {
                _error.postValue("Lỗi kết nối: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
        }
    }
}
