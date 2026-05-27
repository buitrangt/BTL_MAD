package com.smartexpense.server.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Thực thể JPA (JPA Entity) đại diện cho bảng "users" lưu trữ thông tin tài khoản người dùng trong cơ sở dữ liệu.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính tự động tăng

    @Column(unique = true, nullable = false)
    private String email; // Địa chỉ email duy nhất dùng để đăng nhập

    @Column(nullable = false)
    private String passwordHash; // Mật khẩu đã được mã hóa Bcrypt

    private String name; // Họ và tên đầy đủ của người dùng

    @Builder.Default
    @Column(nullable = false)
    private String role = "USER"; // Vai trò phân quyền (USER / ADMIN)

    @Builder.Default
    @Column(nullable = false)
    private Boolean locked = false; // Trạng thái khóa tài khoản (true nếu bị khóa bởi admin)

    private String phone; // Số điện thoại liên kết

    private LocalDateTime passwordChangedAt; // Thời điểm cập nhật mật khẩu lần cuối

    @Column(updatable = false)
    private LocalDateTime createdAt; // Thời điểm tạo tài khoản

    /**
     * Hàm tự động điền thời gian tạo tài khoản trước khi lưu vào DB lần đầu.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
