package com.arijit.budgettracker.utils

import android.content.Context

/**
 * Helper Object quản lý lưu trữ dữ liệu xác thực cục bộ (Authentication SharedPreferences).
 * Chịu trách nhiệm:
 * 1. Lưu trữ an toàn JWT Token phục vụ cho Authorization Interceptor.
 * 2. Lưu trữ thông tin người dùng cơ bản (Email, Họ tên, Số điện thoại) để hiển thị nhanh trên UI mà không cần gọi lại API.
 * 3. Lưu trữ và phân tích Role (Vai trò) để phân quyền điều hướng màn hình.
 * 4. Xử lý chức năng Đăng xuất (Clear toàn bộ dữ liệu lưu trữ).
 */
object TokenManager {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_NAME = "user_name"
    private const val KEY_PHONE = "user_phone"
    private const val KEY_ROLE = "user_role"

    /**
     * Lưu trữ JWT Token vào SharedPreferences sau khi đăng nhập thành công.
     */
    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * Lấy JWT Token đã lưu để đính kèm vào Header HTTP Request.
     */
    fun getToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
    }

    /**
     * Lưu trữ thông tin cá nhân cơ bản của người dùng (email, họ tên, số điện thoại).
     */
    fun saveUser(context: Context, email: String, name: String?, phone: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, name)
            .putString(KEY_PHONE, phone) // Lưu số điện thoại vào máy cục bộ
            .apply()
    }

    /**
     * Lưu trữ Role người dùng (ADMIN hoặc USER).
     */
    fun saveRole(context: Context, role: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ROLE, role)
            .apply()
    }

    /**
     * Lấy Role người dùng đã lưu.
     */
    fun getRole(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ROLE, null)
    }

    /**
     * Kiểm tra xem người dùng hiện tại có vai trò quản trị viên (ADMIN) hay không.
     */
    fun isAdmin(context: Context): Boolean {
        return "ADMIN".equals(getRole(context), ignoreCase = true)
    }

    /**
     * Lấy họ tên người dùng để hiển thị trên Sidebar/Profile.
     */
    fun getName(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NAME, null)
    }

    /**
     * Lấy email người dùng đã đăng nhập.
     */
    fun getEmail(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, null)
    }

    /**
     * Lấy số điện thoại người dùng đã lưu.
     */
    fun getPhone(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PHONE, null)
    }

    /**
     * Kiểm tra xem người dùng đã thực hiện đăng nhập hay chưa (bằng cách kiểm tra sự tồn tại của JWT Token).
     */
    fun isLoggedIn(context: Context): Boolean {
        return getToken(context) != null
    }

    /**
     * Thực hiện đăng xuất tài khoản: Xóa sạch toàn bộ cấu hình SharedPreferences liên quan đến Auth.
     */
    fun logout(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}
