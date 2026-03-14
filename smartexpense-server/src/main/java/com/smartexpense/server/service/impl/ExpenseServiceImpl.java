package com.smartexpense.server.service.impl;

import com.smartexpense.server.dto.ExpenseRequest;
import com.smartexpense.server.dto.ExpenseResponse;
import com.smartexpense.server.model.Expense;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.ExpenseRepository;
import com.smartexpense.server.repository.UserRepository;
import com.smartexpense.server.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @Override
    public List<ExpenseResponse> getAllExpenses(String userEmail) {
        User user = findUser(userEmail);
        return expenseRepository.findByUserIdOrderByTimeStampDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ExpenseResponse createExpense(String userEmail, ExpenseRequest request) {
        User user = findUser(userEmail);
        Expense expense = Expense.builder()
                .amount(request.getAmount())
                .category(request.getCategory())
                .timeStamp(request.getTimeStamp())
                .user(user)
                .build();
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    public void deleteExpense(String userEmail, Long expenseId) {
        User user = findUser(userEmail);
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        expenseRepository.delete(expense);
    }

    @Override
    @Transactional
    public List<ExpenseResponse> syncExpenses(String userEmail, List<ExpenseRequest> requests) {
        User user = findUser(userEmail);
        List<ExpenseResponse> responses = new ArrayList<>();
        for (ExpenseRequest request : requests) {
            Expense expense = Expense.builder()
                    .amount(request.getAmount())
                    .category(request.getCategory())
                    .timeStamp(request.getTimeStamp())
                    .user(user)
                    .build();
            responses.add(toResponse(expenseRepository.save(expense)));
        }
        return responses;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private ExpenseResponse toResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .timeStamp(expense.getTimeStamp())
                .build();
    }
}
