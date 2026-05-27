package com.smartexpense.server.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thực thể JPA (JPA Entity) đại diện cho bảng "otp_codes" lưu trữ tạm thời mã xác thực OTP dùng khi quên mật khẩu.
 */
@Entity
@Table(name = "otp_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính tự động tăng
    
    @Column(nullable = false)
    private String email; // Email đăng ký nhận mã xác thực OTP
    
    @Column(nullable = false)
    private String otpCode; // Chuỗi mã OTP gồm 6 chữ số sinh ngẫu nhiên
    
    @Column(nullable = false)
    private LocalDateTime expiryTime; // Thời điểm hết hạn hiệu lực của mã OTP (5 phút kể từ lúc tạo)
}
