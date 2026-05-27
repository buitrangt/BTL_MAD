package com.arijit.budgettracker.api

/**
 * Lớp dữ liệu (Data Class) chứa thông tin yêu cầu xác thực gửi từ Client lên Server.
 * Dùng chung cho cả hai nghiệp vụ Đăng ký tài khoản mới và Đăng nhập.
 *
 * @property email Địa chỉ email của người dùng (dùng làm tên đăng nhập chính).
 * @property password Mật khẩu đăng nhập dạng thô.
 * @property name Họ và tên đầy đủ của người dùng (chỉ bắt buộc khi Đăng ký).
 * @property phone Số điện thoại của người dùng (tùy chọn).
 * @property locked Trạng thái khóa tài khoản (mặc định là 0 - không khóa).
 * @property role Vai trò phân quyền trong hệ thống (mặc định gửi "USER").
 */
data class AuthRequest(
    val email: String,
    val password: String,
    val name: String? = null,
    val phone: String? = null,
    val locked: Int = 0,
    val role: String = "USER"
)