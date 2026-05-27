package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data Transfer Object (DTO) chứa phản hồi xác thực thành công gửi về Client.
 * Chứa token JWT dùng để gán Authorization Header cho các request sau.
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token; // Chuỗi token JWT được sinh ra để định danh phiên làm việc
    private String email; // Email của tài khoản đăng nhập
    private String name;  // Họ tên của tài khoản
    private String role;  // Vai trò của tài khoản (USER/ADMIN)
    private String phone; // Số điện thoại liên kết của tài khoản
}
