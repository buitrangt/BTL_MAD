/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartexpense.server.controller;

import com.smartexpense.server.service.ExpenseService;
import com.smartexpense.server.service.FinChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author admin
 */
@RestController
@RequestMapping("/api/finchat")
@RequiredArgsConstructor
public class FinChatController {
    private final FinChatService finChatService;
    private final ExpenseService expenseService; // Để lấy dữ liệu chi tiêu thật

    @PostMapping("/ask")
    public ResponseEntity<String> ask(@RequestParam String message, @RequestParam Long userId) {
        // Lấy danh sách chi tiêu của User để AI có dữ liệu phân tích
        String context = expenseService.getSummaryByUserId(userId); 
        
        String answer = finChatService.getChatResponse(message, context);
        return ResponseEntity.ok(answer);
    }
}
