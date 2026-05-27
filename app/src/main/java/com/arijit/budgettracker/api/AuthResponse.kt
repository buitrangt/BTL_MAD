package com.arijit.budgettracker.api

/**
 * Lớp dữ liệu (Data Class) nhận thông tin phản hồi từ Server sau khi thực hiện xác thực thành công.
 *
 * @property token Chuỗi JWT Token dùng để xác thực và ủy quyền cho các API tiếp theo.
 * @property email Địa chỉ email của tài khoản.
 * @property name Họ tên đầy đủ của người dùng.
 * @property phone Số điện thoại của người dùng.
 * @property role Vai trò của người dùng trên hệ thống (ADMIN hoặc USER).
 * @property locked Trạng thái khóa tài khoản (1: Đang bị khóa, 0 hoặc null: Đang hoạt động).
 */
data class AuthResponse(
    val token: String,
    val email: String,
    val name: String?,
    val phone: String?,
    val role: String?,
    val locked: Int?
)