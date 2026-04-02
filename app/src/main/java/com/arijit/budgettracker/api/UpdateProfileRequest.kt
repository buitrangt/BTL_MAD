package com.arijit.budgettracker.api

data class UpdateProfileRequest(
    val email: String,    // Bắt buộc phải có để định danh user ở Backend
    val name: String?,
    val phone: String?,
    val password: String? = null // Mặc định là null nếu không dùng đến
)