package com.smartexpense.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object (DTO) gửi yêu cầu thay đổi mật khẩu từ người dùng đã đăng nhập.
 */
@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Mật khẩu cũ không được để trống")
    private String oldPassword; // Mật khẩu hiện tại để xác thực chủ tài khoản

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String newPassword; // Mật khẩu mới cần thiết lập
}
