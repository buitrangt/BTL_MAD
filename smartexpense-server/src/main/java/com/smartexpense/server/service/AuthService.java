package com.smartexpense.server.service;

import com.smartexpense.server.dto.AuthRequest;
import com.smartexpense.server.dto.AuthResponse;

/**
 * Interface định nghĩa các nghiệp vụ xác thực người dùng (Authentication Service).
 * Bao gồm đăng ký, đăng nhập, quản lý OTP phục vụ quên mật khẩu và thay đổi mật khẩu.
 */
public interface AuthService {
    
    /**
     * Nghiệp vụ đăng ký tài khoản người dùng mới.
     *
     * @param request Chứa email, mật khẩu thô và họ tên người dùng.
     * @return AuthResponse Chứa JWT Token sinh ra tự động đăng nhập và thông tin tài khoản vừa tạo.
     */
    AuthResponse register(AuthRequest request);

    /**
     * Nghiệp vụ đăng nhập hệ thống.
     *
     * @param request Chứa email đăng nhập và mật khẩu thô để đối chiếu.
     * @return AuthResponse Chứa JWT Token xác thực phiên và thông tin tài khoản nếu đăng nhập thành công.
     */
    AuthResponse login(AuthRequest request);
    
    /**
     * Nghiệp vụ sinh và gửi mã xác thực OTP qua email phục vụ chức năng Quên mật khẩu.
     *
     * @param email Địa chỉ email nhận mã OTP.
     */
    void sendOtp(String email);

    /**
     * Nghiệp vụ đối soát mã OTP do người dùng nhập vào.
     *
     * @param email Địa chỉ email gắn với yêu cầu OTP.
     * @param otp Mã xác thực OTP cần kiểm tra.
     * @return true nếu mã chính xác và chưa quá hạn (5 phút); ngược lại là false.
     */
    boolean verifyOtp(String email, String otp);

    /**
     * Nghiệp vụ đặt lại mật khẩu mới cho người dùng sau khi đã xác thực mã OTP thành công.
     *
     * @param email Địa chỉ email của tài khoản cần đổi mật khẩu.
     * @param newPassword Mật khẩu mới cần thiết lập.
     */
    void resetPassword(String email, String newPassword);

    /**
     * Nghiệp vụ thay đổi mật khẩu dành cho người dùng đang trong phiên đăng nhập.
     *
     * @param email Email của tài khoản thực hiện đổi mật khẩu.
     * @param oldPassword Mật khẩu cũ hiện tại để xác minh danh tính.
     * @param newPassword Mật khẩu mới cần cập nhật.
     */
    void changePassword(String email, String oldPassword, String newPassword);
}
