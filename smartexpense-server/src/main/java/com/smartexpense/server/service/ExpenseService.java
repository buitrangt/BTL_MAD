package com.smartexpense.server.service;

import com.smartexpense.server.dto.ExpenseRequest;
import com.smartexpense.server.dto.ExpenseResponse;
import java.util.List;

public interface ExpenseService {
    List<ExpenseResponse> getAllExpenses(String userEmail);
    ExpenseResponse createExpense(String userEmail, ExpenseRequest request);
    void deleteExpense(String userEmail, Long expenseId);
    List<ExpenseResponse> syncExpenses(String userEmail, List<ExpenseRequest> requests);
}
