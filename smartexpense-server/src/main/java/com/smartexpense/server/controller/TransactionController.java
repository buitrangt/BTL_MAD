package com.smartexpense.server.controller;

import com.smartexpense.server.dto.TransactionRequest;
import com.smartexpense.server.dto.TransactionResponse;
import com.smartexpense.server.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các request liên quan đến giao dịch.
 * Thuộc luồng chức năng: Quản lý giao dịch (Thêm/Sửa/Xóa, Xem lịch sử, Tìm kiếm).
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    // 1. Lấy danh sách toàn bộ giao dịch của người dùng (Xem lịch sử giao dịch)
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(Authentication auth) {
        return ResponseEntity.ok(transactionService.getAllTransactions(auth.getName()));
    }

    // 2. Thêm mới một giao dịch
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            Authentication auth, @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.createTransaction(auth.getName(), request));
    }

    // 3. Cập nhật thông tin giao dịch (Sửa giao dịch)
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.updateTransaction(auth.getName(), id, request));
    }

    // 4. Xóa một giao dịch
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(Authentication auth, @PathVariable Long id) {
        transactionService.deleteTransaction(auth.getName(), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync")
    public ResponseEntity<List<TransactionResponse>> syncTransactions(
            Authentication auth, @Valid @RequestBody List<TransactionRequest> requests) {
        return ResponseEntity.ok(transactionService.syncTransactions(auth.getName(), requests));
    }

    // 5. Tìm kiếm giao dịch theo từ khóa
    @GetMapping("/search")
    public ResponseEntity<List<TransactionResponse>> searchTransactions(
            Authentication auth, @RequestParam String keyword) {
        return ResponseEntity.ok(transactionService.searchTransactions(auth.getName(), keyword));
    }
}
