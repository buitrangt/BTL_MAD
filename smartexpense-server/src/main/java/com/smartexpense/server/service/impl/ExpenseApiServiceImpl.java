package com.smartexpense.server.service.impl;

import com.smartexpense.server.dto.ExpenseRequest;
import com.smartexpense.server.dto.ExpenseResponse;
import com.smartexpense.server.model.Category;
import com.smartexpense.server.model.Transaction;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.CategoryRepository;
import com.smartexpense.server.repository.TransactionRepository;
import com.smartexpense.server.repository.UserRepository;
import com.smartexpense.server.service.ExpenseApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseApiServiceImpl implements ExpenseApiService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public List<ExpenseResponse> getAllExpenses(String userEmail) {
        User user = findUser(userEmail);
        return transactionRepository.findByUserIdOrderByTimeStampDesc(user.getId())
                .stream()
                .map(this::toExpenseResponse)
                .toList();
    }

    @Override
    public ExpenseResponse createExpense(String userEmail, ExpenseRequest request) {
        User user = findUser(userEmail);
        Category category = resolveCategoryByName(user, request.getCategory());

        Transaction transaction = Transaction.builder()
                .name(request.getCategory())
                .amount(request.getAmount())
                .category(category)
            .type(normalizeType(request.getType()))
            .note(request.getNote())
                .timeStamp(request.getTimeStamp())
                .user(user)
                .build();

        return toExpenseResponse(transactionRepository.save(transaction));
    }

    @Override
    public ExpenseResponse updateExpense(String userEmail, Long expenseId, ExpenseRequest request) {
        User user = findUser(userEmail);
        Transaction transaction = transactionRepository.findByIdAndUserId(expenseId, user.getId())
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        Category category = resolveCategoryByName(user, request.getCategory());
        transaction.setAmount(request.getAmount());
        transaction.setCategory(category);
        transaction.setTimeStamp(request.getTimeStamp());
        transaction.setName(request.getCategory());
        transaction.setType(normalizeType(request.getType()));
        transaction.setNote(request.getNote());

        return toExpenseResponse(transactionRepository.save(transaction));
    }

    @Override
    public void deleteExpense(String userEmail, Long expenseId) {
        User user = findUser(userEmail);
        Transaction transaction = transactionRepository.findByIdAndUserId(expenseId, user.getId())
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        transactionRepository.delete(transaction);
    }

    @Override
    @Transactional
    public List<ExpenseResponse> syncExpenses(String userEmail, List<ExpenseRequest> requests) {
        User user = findUser(userEmail);
        List<ExpenseResponse> results = new ArrayList<>();

        for (ExpenseRequest request : requests) {
            Category category = resolveCategoryByName(user, request.getCategory());
            Transaction transaction = Transaction.builder()
                    .name(request.getCategory())
                    .amount(request.getAmount())
                    .category(category)
                    .type(normalizeType(request.getType()))
                    .note(request.getNote())
                    .timeStamp(request.getTimeStamp())
                    .user(user)
                    .build();
            results.add(toExpenseResponse(transactionRepository.save(transaction)));
        }

        return results;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Category resolveCategoryByName(User user, String categoryName) {
        String normalized = categoryName == null ? "Khac" : categoryName.trim();
        if (normalized.isEmpty()) {
            normalized = "Khac";
        }

        final String normalizedCategory = normalized;

        return categoryRepository.findByNameAndUserId(normalizedCategory, user.getId())
            .or(() -> categoryRepository.findDefaultByName(normalizedCategory))
                .orElseGet(() -> categoryRepository.save(Category.builder()
                .name(normalizedCategory)
                        .note(null)
                        .isDefault(false)
                        .user(user)
                        .build()));
    }

    private ExpenseResponse toExpenseResponse(Transaction transaction) {
        return ExpenseResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .category(transaction.getCategory() != null ? transaction.getCategory().getName() : null)
                .timeStamp(transaction.getTimeStamp())
                .build();
    }

    private String normalizeType(String type) {
        if (type == null) {
            return "expense";
        }
        String normalized = type.trim().toLowerCase();
        return normalized.isEmpty() ? "expense" : normalized;
    }
}
