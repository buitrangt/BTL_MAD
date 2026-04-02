package com.arijit.budgettracker.api

data class AuthRequest(
    val email: String,
    val password: String,
    val name: String? = null,
    val phone: String? = null,
    val locked: Int = 0,      // Gửi kèm 0 lên cho chắc
    val role: String = "USER"  // Gửi kèm USER lên luôn
)