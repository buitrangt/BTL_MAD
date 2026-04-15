package com.arijit.budgettracker.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arijit.budgettracker.api.AdminCategoryCreateRequest
import com.arijit.budgettracker.api.AdminCategoryDto
import com.arijit.budgettracker.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminCategoriesViewModel(application: Application) : AndroidViewModel(application) {

    private val _items = MutableLiveData<List<AdminCategoryDto>>(emptyList())
    val items: LiveData<List<AdminCategoryDto>> = _items

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun load() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(getApplication())
                val res = api.getAdminCategories()
                if (res.isSuccessful && res.body() != null) {
                    _items.postValue(res.body())
                } else {
                    _error.postValue("Không tải được danh mục (${res.code()})")
                }
            } catch (e: Exception) {
                _error.postValue("Lỗi kết nối: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun create(name: String, note: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(getApplication())
                val res = api.createAdminCategory(AdminCategoryCreateRequest(name, note))
                if (res.isSuccessful && res.body() != null) {
                    _message.postValue("Đã thêm danh mục")
                    val current = _items.value.orEmpty().toMutableList()
                    current.add(0, res.body()!!)
                    _items.postValue(current)
                } else {
                    _error.postValue("Không thêm được (${res.code()})")
                }
            } catch (e: Exception) {
                _error.postValue("Lỗi: ${e.message}")
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(getApplication())
                val res = api.deleteAdminCategory(id)
                if (res.isSuccessful) {
                    _message.postValue("Đã xóa")
                    _items.postValue(_items.value.orEmpty().filter { it.id != id })
                } else {
                    _error.postValue("Không xóa được (${res.code()})")
                }
            } catch (e: Exception) {
                _error.postValue("Lỗi: ${e.message}")
            }
        }
    }

    fun clearMessage() { _message.postValue(null) }
}
