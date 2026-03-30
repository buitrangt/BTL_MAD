package com.arijit.budgettracker.models

data class User(
    val id: Long?,
    val name: String?,
    val email: String,
    val phone: String?,
    val role: String?,
    val locked: Int?, // 0: Active, 1: Locked
    val createdAt: String? = null
)