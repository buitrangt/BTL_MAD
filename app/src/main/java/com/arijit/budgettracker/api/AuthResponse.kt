package com.arijit.budgettracker.api

data class AuthResponse(
    val token: String,
    val email: String,
    val name: String?,
    val phone: String?,
    val role: String?,
    val locked: Int?
)