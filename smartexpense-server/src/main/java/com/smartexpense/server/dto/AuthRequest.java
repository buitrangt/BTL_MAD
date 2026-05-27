package com.smartexpense.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object (DTO) chứa thông tin đăng nhập hoặc đăng ký tài khoản từ Client gửi lên.
 */
@Data
public class AuthRequest {
    
    @Email(message = "Email không đúng định dạng")
    @NotBlank(message = "Email không được để trống")
    private String email; // Địa chỉ email tài khoản

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password; // Mật khẩu người dùng dạng thô

    private String name; // Họ tên đầy đủ người dùng (chỉ gửi lên khi đăng ký)
}
