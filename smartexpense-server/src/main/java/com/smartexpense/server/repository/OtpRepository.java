package com.smartexpense.server.repository;

import com.smartexpense.server.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository xử lý các truy vấn cơ sở dữ liệu liên quan đến bảng otp_codes phục vụ mã OTP quên mật khẩu.
 */
@Repository
public interface OtpRepository extends JpaRepository<OtpToken, Long> {
    
    /**
     * Tìm mã xác thực OTP mới nhất được sinh ra cho địa chỉ email tương ứng.
     */
    Optional<OtpToken> findTopByEmailOrderByExpiryTimeDesc(String email);

    /**
     * Xóa toàn bộ các mã OTP cũ liên kết với địa chỉ email sau khi hoàn thành xác thực.
     */
    void deleteByEmail(String email);
}