package com.smartexpense.server.controller;

import com.smartexpense.server.dto.AuthRequest;
import com.smartexpense.server.dto.AuthResponse;
import com.smartexpense.server.dto.ChangePasswordRequest;
import com.smartexpense.server.dto.ResetPasswordRequest;
import com.smartexpense.server.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các yêu cầu liên quan đến xác thực người dùng (Authentication)
 * Bao gồm: Đăng ký, Đăng nhập, Quên mật khẩu (gửi OTP, kiểm tra OTP, đặt lại mật khẩu) và Đổi mật khẩu.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * API Đăng ký tài khoản mới
     * POST /api/auth/register
     * 
     * @param request Chứa email, mật khẩu và họ tên đăng ký
     * @return ResponseEntity chứa JWT Token và thông tin cá nhân sau đăng ký
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * API Đăng nhập hệ thống
     * POST /api/auth/login
     * 
     * @param request Chứa email và mật khẩu đăng nhập
     * @return ResponseEntity chứa JWT Token và thông tin tài khoản nếu đăng nhập thành công
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
    
    /**
     * API Yêu cầu gửi mã OTP phục vụ quên mật khẩu
     * POST /api/auth/forgot-password
     * 
     * @param email Địa chỉ email nhận mã OTP
     * @return Thông báo trạng thái gửi mã thành công hoặc lỗi
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        try {
            authService.sendOtp(email);
            return ResponseEntity.ok("Mã OTP đã được gửi đến email của bạn.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * API Xác thực mã OTP người dùng nhập vào
     * POST /api/auth/verify-otp
     * 
     * @param email Địa chỉ email nhận OTP
     * @param otp Mã xác thực OTP cần đối chiếu
     * @return Xác nhận mã hợp lệ hoặc thông báo lỗi quá hạn/sai mã
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        boolean isValid = authService.verifyOtp(email, otp);
        if (isValid) {
            return ResponseEntity.ok("Mã OTP chính xác.");
        } else {
            return ResponseEntity.badRequest().body("Mã OTP không đúng hoặc đã hết hạn.");
        }
    }

    /**
     * API Đặt lại mật khẩu mới sau khi xác thực OTP thành công
     * POST /api/auth/reset-password
     * 
     * @param request Chứa thông tin email và mật khẩu mới
     * @return Thông báo đặt lại mật khẩu thành công hoặc lỗi
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getEmail(), request.getNewPassword());
            return ResponseEntity.ok("Mật khẩu đã được thay đổi thành công.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * API Đổi mật khẩu dành cho người dùng đã đăng nhập vào hệ thống
     * POST /api/auth/change-password
     * 
     * @param auth Đối tượng chứa thông tin đăng nhập hiện tại
     * @param request Chứa mật khẩu cũ và mật khẩu mới
     * @return Thông báo đổi mật khẩu thành công hoặc lỗi
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        try {
            authService.changePassword(auth.getName(), request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok("Đổi mật khẩu thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

