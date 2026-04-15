package com.arijit.budgettracker.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arijit.budgettracker.api.AdminOverviewResponse
import com.arijit.budgettracker.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val _overview = MutableLiveData<AdminOverviewResponse?>()
    val overview: LiveData<AdminOverviewResponse?> = _overview

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
                val response = api.getAdminOverview()
                if (response.isSuccessful && response.body() != null) {
                    _overview.postValue(response.body())
                } else {
                    _error.postValue("Không tải được dữ liệu (${response.code()})")
                }
            } catch (e: Exception) {
                _error.postValue("Lỗi kết nối: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
        }
    }
}
