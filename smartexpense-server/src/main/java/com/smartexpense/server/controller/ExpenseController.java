package com.smartexpense.server.controller;

import com.smartexpense.server.dto.ExpenseRequest;
import com.smartexpense.server.dto.ExpenseResponse;
import com.smartexpense.server.service.ExpenseApiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseApiService expenseApiService;

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses(Authentication auth) {
        return ResponseEntity.ok(expenseApiService.getAllExpenses(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            Authentication auth,
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseApiService.createExpense(auth.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseApiService.updateExpense(auth.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(Authentication auth, @PathVariable Long id) {
        expenseApiService.deleteExpense(auth.getName(), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync")
    public ResponseEntity<List<ExpenseResponse>> syncExpenses(
            Authentication auth,
            @Valid @RequestBody List<ExpenseRequest> requests) {
        return ResponseEntity.ok(expenseApiService.syncExpenses(auth.getName(), requests));
    }
}
