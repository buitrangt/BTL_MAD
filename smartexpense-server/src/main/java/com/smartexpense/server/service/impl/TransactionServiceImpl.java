package com.smartexpense.server.service.impl;

import com.smartexpense.server.dto.TransactionRequest;
import com.smartexpense.server.dto.TransactionResponse;
import com.smartexpense.server.model.Category;
import com.smartexpense.server.model.Transaction;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.CategoryRepository;
import com.smartexpense.server.repository.TransactionRepository;
import com.smartexpense.server.repository.UserRepository;
import com.smartexpense.server.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public List<TransactionResponse> getAllTransactions(String userEmail) {
        User user = findUser(userEmail);
        return transactionRepository.findByUserIdOrderByTimeStampDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TransactionResponse createTransaction(String userEmail, TransactionRequest request) {
        User user = findUser(userEmail);
        Category category = findCategory(request.getCategoryId());
        Transaction transaction = Transaction.builder()
                .name(request.getName())
                .amount(request.getAmount())
                .category(category)
                .type(request.getType())
                .note(request.getNote())
                .timeStamp(request.getTimeStamp())
                .user(user)
                .build();
        return toResponse(transactionRepository.save(transaction));
    }

    @Override
    public void deleteTransaction(String userEmail, Long transactionId) {
        User user = findUser(userEmail);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        transactionRepository.delete(transaction);
    }

    @Override
    @Transactional
    public List<TransactionResponse> syncTransactions(String userEmail, List<TransactionRequest> requests) {
        User user = findUser(userEmail);
        List<TransactionResponse> responses = new ArrayList<>();
        for (TransactionRequest request : requests) {
            Category category = findCategory(request.getCategoryId());
            Transaction transaction = Transaction.builder()
                    .name(request.getName())
                    .amount(request.getAmount())
                    .category(category)
                    .type(request.getType())
                    .note(request.getNote())
                    .timeStamp(request.getTimeStamp())
                    .user(user)
                    .build();
            responses.add(toResponse(transactionRepository.save(transaction)));
        }
        return responses;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Category findCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId).orElse(null);
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .name(transaction.getName())
                .amount(transaction.getAmount())
                .categoryId(transaction.getCategory() != null ? transaction.getCategory().getId() : null)
                .categoryName(transaction.getCategory() != null ? transaction.getCategory().getName() : null)
                .type(transaction.getType())
                .note(transaction.getNote())
                .timeStamp(transaction.getTimeStamp())
                .build();
    }
}
