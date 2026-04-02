package com.smartexpense.server.service;


import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã OTP Quên mật khẩu - SmartExpense");
        message.setText("Mã xác thực của bạn là: " + otp + ". Mã có hiệu lực trong 5 phút.");
        mailSender.send(message);
    }
}