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

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
@Validated
public class SmsController {
    private final SmsService smsService;

    @GetMapping("/templates")
    public ResponseEntity<List<SmsTemplateResponse>> getTemplates() {
        return ResponseEntity.ok(smsService.getActiveTemplates());
    }

    @PostMapping("/transactions/sync")
    public ResponseEntity<List<SmsTransactionResponse>> syncSmsTransactions(
            Authentication auth,
            @Valid @RequestBody List<SmsTransactionRequest> requests) {
        return ResponseEntity.ok(smsService.syncSmsTransactions(auth.getName(), requests));
    }
}
