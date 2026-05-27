package com.smartexpense.server.dto;

import lombok.Data;

/**
 * Data Transfer Object (DTO) chứa yêu cầu đặt lại mật khẩu mới cho tài khoản (dùng sau xác thực OTP).
 */
@Data
public class ResetPasswordRequest {
    private String email;       // Email của tài khoản cần đặt lại mật khẩu
    private String newPassword;  // Mật khẩu mới cần lưu trữ
}