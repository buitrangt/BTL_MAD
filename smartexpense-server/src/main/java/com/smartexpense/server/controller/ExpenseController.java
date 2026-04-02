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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final TransactionService transactionService;

    // DTOs for /api/expenses endpoint
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ExpenseRequest {
        private Double amount;
        private String category;
        private Long timeStamp;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ExpenseResponse {
        private Long id;
        private Double amount;
        private String category;
        private Long timeStamp;

        public static ExpenseResponse from(TransactionResponse transaction) {
            return ExpenseResponse.builder()
                    .id(transaction.getId())
                    .amount(transaction.getAmount().doubleValue())
                    .category(transaction.getCategoryName() != null ? transaction.getCategoryName() : transaction.getName())
                    .timeStamp(transaction.getTimeStamp())
                    .build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses(Authentication auth) {
        List<TransactionResponse> transactions = transactionService.getAllTransactions(auth.getName());
        List<ExpenseResponse> expenses = transactions.stream()
                .map(ExpenseResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(expenses);
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            Authentication auth, @Valid @RequestBody ExpenseRequest request) {
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setName(request.getCategory());
        transactionRequest.setAmount(new java.math.BigDecimal(request.getAmount()));
        transactionRequest.setType("EXPENSE");
        transactionRequest.setTimeStamp(request.getTimeStamp());
        
        TransactionResponse transaction = transactionService.createTransaction(auth.getName(), transactionRequest);
        return ResponseEntity.ok(ExpenseResponse.from(transaction));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            Authentication auth, @PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setName(request.getCategory());
        transactionRequest.setAmount(new java.math.BigDecimal(request.getAmount()));
        transactionRequest.setType("EXPENSE");
        transactionRequest.setTimeStamp(request.getTimeStamp());
        
        TransactionResponse transaction = transactionService.updateTransaction(auth.getName(), id, transactionRequest);
        return ResponseEntity.ok(ExpenseResponse.from(transaction));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(Authentication auth, @PathVariable Long id) {
        transactionService.deleteTransaction(auth.getName(), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync")
    public ResponseEntity<List<ExpenseResponse>> syncExpenses(
            Authentication auth, @Valid @RequestBody List<ExpenseRequest> requests) {
        List<TransactionRequest> transactionRequests = requests.stream()
                .map(req -> {
                    TransactionRequest tReq = new TransactionRequest();
                    tReq.setName(req.getCategory());
                    tReq.setAmount(new java.math.BigDecimal(req.getAmount()));
                    tReq.setType("EXPENSE");
                    tReq.setTimeStamp(req.getTimeStamp());
                    return tReq;
                })
                .collect(Collectors.toList());
        
        List<TransactionResponse> transactions = transactionService.syncTransactions(auth.getName(), transactionRequests);
        List<ExpenseResponse> expenses = transactions.stream()
                .map(ExpenseResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(expenses);
    }
}
