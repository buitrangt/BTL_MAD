package com.smartexpense.server.controller;

import com.smartexpense.server.dto.SmsTemplateResponse;
import com.smartexpense.server.dto.SmsTransactionRequest;
import com.smartexpense.server.dto.SmsTransactionResponse;
import com.smartexpense.server.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API cho chức năng SMS: cung cấp mẫu (template) ngân hàng và đồng bộ giao dịch từ SMS.
 */
@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
@Validated
public class SmsController {
    private final SmsService smsService;

    // GET /api/sms/templates - lấy danh sách template ngân hàng đang bật
    @GetMapping("/templates")
    public ResponseEntity<List<SmsTemplateResponse>> getTemplates() {
        return ResponseEntity.ok(smsService.getActiveTemplates());
    }

    // POST /api/sms/transactions/sync - nhận danh sách giao dịch SMS từ app và lưu vào DB
    @PostMapping("/transactions/sync")
    public ResponseEntity<List<SmsTransactionResponse>> syncSmsTransactions(
            Authentication auth,
            @Valid @RequestBody List<SmsTransactionRequest> requests) {
        return ResponseEntity.ok(smsService.syncSmsTransactions(auth.getName(), requests));
    }
}
