package com.smartexpense.server.controller;

import com.smartexpense.server.dto.ExpenseRequest;
import com.smartexpense.server.dto.ExpenseResponse;
import com.smartexpense.server.service.ExpenseService;
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
    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses(Authentication auth) {
        return ResponseEntity.ok(expenseService.getAllExpenses(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            Authentication auth, @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.createExpense(auth.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(Authentication auth, @PathVariable Long id) {
        expenseService.deleteExpense(auth.getName(), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync")
    public ResponseEntity<List<ExpenseResponse>> syncExpenses(
            Authentication auth, @Valid @RequestBody List<ExpenseRequest> requests) {
        return ResponseEntity.ok(expenseService.syncExpenses(auth.getName(), requests));
    }
}
