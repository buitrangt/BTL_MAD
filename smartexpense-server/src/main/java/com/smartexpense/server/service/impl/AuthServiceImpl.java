package com.smartexpense.server.service.impl;

import com.smartexpense.server.dto.AuthRequest;
import com.smartexpense.server.dto.AuthResponse;
import com.smartexpense.server.model.OtpToken;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.OtpRepository;
import com.smartexpense.server.repository.UserRepository;
import com.smartexpense.server.security.JwtUtil;
import com.smartexpense.server.service.AuthService;
import com.smartexpense.server.service.MailService;
import java.time.LocalDateTime;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Lớp triển khai nghiệp vụ xác thực người dùng (AuthService)
 * Xử lý: Đăng ký tài khoản, đăng nhập, cấp và kiểm tra OTP, đặt lại mật khẩu và đổi mật khẩu.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpRepository otpRepository; 
    private final MailService mailService;
    private final JwtUtil jwtUtil;

    /**
     * Nghiệp vụ đăng ký tài khoản mới:
     * 1. Kiểm tra sự tồn tại của Email trong DB
     * 2. Mã hóa mật khẩu người dùng gửi lên
     * 3. Lưu thông tin tài khoản mới và sinh JWT Token tự động đăng nhập
     */
    @Override
    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getName(), user.getRole(), user.getPhone());
    }

    /**
     * Nghiệp vụ đăng nhập hệ thống:
     * 1. Kiểm tra Email có tồn tại không
     * 2. So khớp mật khẩu đã hash dưới DB
     * 3. Kiểm tra tài khoản có bị khóa không
     * 4. Sinh JWT Token mới để xác thực phiên làm việc
     */
    @Override
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        if (Boolean.TRUE.equals(user.getLocked())) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getName(), user.getRole(), user.getPhone());
    }

    /**
     * Nghiệp vụ gửi mã OTP phục vụ quên mật khẩu:
     * 1. Kiểm tra email của người dùng có tồn tại trong hệ thống
     * 2. Sinh ngẫu nhiên mã số OTP gồm 6 chữ số
     * 3. Lưu OTP kèm thời gian hết hạn (5 phút) vào bảng otp_codes
     * 4. Gọi dịch vụ MailService để gửi email chứa mã OTP đến người dùng
     */
    @Override
    public void sendOtp(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email không tồn tại trong hệ thống!");
        }

        String otp = String.format("%06d", new Random().nextInt(1000000));

        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setOtpCode(otp);
        otpToken.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otpRepository.save(otpToken);

        mailService.sendOtpEmail(email, otp);
    }

    /**
     * Nghiệp vụ đối chiếu mã xác thực OTP:
     * 1. Tìm bản ghi OTP mới nhất của email trong DB
     * 2. So khớp mã số OTP và kiểm tra xem mã đã hết hạn chưa (5 phút)
     */
    @Override
    public boolean verifyOtp(String email, String otp) {
        return otpRepository.findTopByEmailOrderByExpiryTimeDesc(email)
                .map(token -> token.getOtpCode().equals(otp) 
                              && token.getExpiryTime().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    /**
     * Nghiệp vụ đặt lại mật khẩu mới:
     * 1. Tìm tài khoản người dùng tương ứng với email
     * 2. Mã hóa mật khẩu mới và lưu vào cơ sở dữ liệu
     */
    @Override
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

    }

    /**
     * Nghiệp vụ đổi mật khẩu (khi người dùng đã đăng nhập):
     * 1. Đối chiếu mật khẩu cũ nhập vào
     * 2. Kiểm tra mật khẩu mới phải khác mật khẩu cũ
     * 3. Tiến hành mã hóa mật khẩu mới và lưu trữ
     */
    @Override
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }

        if (oldPassword.equals(newPassword)) {
            throw new RuntimeException("Mật khẩu mới phải khác mật khẩu cũ");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
