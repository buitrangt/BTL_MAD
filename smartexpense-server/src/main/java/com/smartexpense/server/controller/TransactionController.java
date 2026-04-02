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

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(Authentication auth) {
        return ResponseEntity.ok(transactionService.getAllTransactions(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            Authentication auth, @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.createTransaction(auth.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            Authentication auth, @PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.updateTransaction(auth.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(Authentication auth, @PathVariable Long id) {
        transactionService.deleteTransaction(auth.getName(), id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<TransactionResponse>> searchTransactions(
            Authentication auth, @RequestParam String keyword) {
        return ResponseEntity.ok(transactionService.searchTransactions(auth.getName(), keyword));
    }

    @PostMapping("/sync")
    public ResponseEntity<List<TransactionResponse>> syncTransactions(
            Authentication auth, @Valid @RequestBody List<TransactionRequest> requests) {
        return ResponseEntity.ok(transactionService.syncTransactions(auth.getName(), requests));
    }
}
