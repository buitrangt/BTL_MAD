package com.arijit.budgettracker.models

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arijit.budgettracker.utils.TokenManager

class UserProfileViewModel : ViewModel() {

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> get() = _user
    private val _isLoggedOut = MutableLiveData<Boolean>()
    val isLoggedOut: LiveData<Boolean> get() = _isLoggedOut

//    fun loadUserData(context: Context) {
//        val name = TokenManager.getName(context) ?: "Người dùng"
//        val email = TokenManager.getEmail(context) ?: ""
//        // Đóng gói vào object User
//        _user.value = User(
//            name = name,
//            email = email
//        )
//    }
    fun logout(context: Context) {
        // Thực hiện xóa token/session
        TokenManager.logout(context)

        // Thông báo cho Activity biết đã đăng xuất thành công
        _isLoggedOut.value = true
    }
}