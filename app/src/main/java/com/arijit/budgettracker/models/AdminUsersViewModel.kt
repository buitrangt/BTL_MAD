package com.arijit.budgettracker.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arijit.budgettracker.api.AdminLockRequest
import com.arijit.budgettracker.api.AdminUserDto
import com.arijit.budgettracker.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminUsersViewModel(application: Application) : AndroidViewModel(application) {

    private val _users = MutableLiveData<List<AdminUserDto>>(emptyList())
    val users: LiveData<List<AdminUserDto>> = _users

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _lockResult = MutableLiveData<Pair<Long, Boolean>?>()
    val lockResult: LiveData<Pair<Long, Boolean>?> = _lockResult

    fun load(search: String = "") {
        _loading.value = true
        _error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(getApplication())
                val res = api.getAdminUsers(search = search, page = 0, size = 50)
                if (res.isSuccessful && res.body() != null) {
                    _users.postValue(res.body()!!.items)
                } else {
                    _error.postValue("Không tải được danh sách (${res.code()})")
                }
            } catch (e: Exception) {
                _error.postValue("Lỗi kết nối: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun setLocked(userId: Long, locked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(getApplication())
                val res = api.setAdminUserLock(userId, AdminLockRequest(locked))
                if (res.isSuccessful) {
                    _lockResult.postValue(userId to locked)
                    val current = _users.value.orEmpty()
                    _users.postValue(current.map {
                        if (it.id == userId) it.copy(locked = locked) else it
                    })
                } else {
                    _error.postValue("Không cập nhật được (${res.code()})")
                }
            } catch (e: Exception) {
                _error.postValue("Lỗi kết nối: ${e.message}")
            }
        }
    }
}
