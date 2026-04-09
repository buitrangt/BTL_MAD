package com.smartexpense.server.service;

import com.smartexpense.server.dto.ExpenseRequest;
import com.smartexpense.server.dto.ExpenseResponse;

import java.util.List;

public interface ExpenseApiService {
    List<ExpenseResponse> getAllExpenses(String userEmail);
    ExpenseResponse createExpense(String userEmail, ExpenseRequest request);
    ExpenseResponse updateExpense(String userEmail, Long expenseId, ExpenseRequest request);
    void deleteExpense(String userEmail, Long expenseId);
    List<ExpenseResponse> syncExpenses(String userEmail, List<ExpenseRequest> requests);
}
