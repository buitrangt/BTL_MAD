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

/**
 * Service triển khai logic xử lý giao dịch.
 * Thuộc luồng chức năng: Quản lý giao dịch (Thêm/Sửa/Xóa, Xem lịch sử, Tìm kiếm).
 * Chịu trách nhiệm tương tác với Repository để lấy và lưu dữ liệu.
 */
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // 1. Lấy danh sách giao dịch từ CSDL (Xem lịch sử giao dịch)
    @Override
    public List<TransactionResponse> getAllTransactions(String userEmail) {
        User user = findUser(userEmail);
        return transactionRepository.findByUserIdOrderByTimeStampDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 2. Logic tạo mới giao dịch và lưu vào CSDL
    @Override
    public TransactionResponse createTransaction(String userEmail, TransactionRequest request) {
        User user = findUser(userEmail);
        Category category = resolveCategory(user, request.getCategoryId(), request.getCategoryName());
        Transaction transaction = Transaction.builder()
                .name(request.getName())
                .amount(request.getAmount())
                .category(category)
                .type(normalizeType(request.getType()))
                .note(request.getNote())
                .timeStamp(request.getTimeStamp())
                .user(user)
                .build();
        return toResponse(transactionRepository.save(transaction));
    }

    // 3. Logic tìm và cập nhật giao dịch (Sửa giao dịch)
    @Override
    public TransactionResponse updateTransaction(String userEmail, Long transactionId, TransactionRequest request) {
        User user = findUser(userEmail);
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, user.getId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        Category category = resolveCategory(user, request.getCategoryId(), request.getCategoryName());
        transaction.setName(request.getName());
        transaction.setAmount(request.getAmount());
        transaction.setCategory(category);
        transaction.setType(normalizeType(request.getType()));
        transaction.setNote(request.getNote());
        transaction.setTimeStamp(request.getTimeStamp());

        return toResponse(transactionRepository.save(transaction));
    }

    // 4. Logic xóa giao dịch khỏi CSDL
    @Override
    public void deleteTransaction(String userEmail, Long transactionId) {
        User user = findUser(userEmail);
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, user.getId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        transactionRepository.delete(transaction);
    }

    @Override
    @Transactional
    public List<TransactionResponse> syncTransactions(String userEmail, List<TransactionRequest> requests) {
        User user = findUser(userEmail);
        List<TransactionResponse> responses = new ArrayList<>();
        for (TransactionRequest request : requests) {
            Category category = resolveCategory(user, request.getCategoryId(), request.getCategoryName());
            Transaction transaction = Transaction.builder()
                    .name(request.getName())
                    .amount(request.getAmount())
                    .category(category)
                    .type(normalizeType(request.getType()))
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

    private Category resolveCategory(User user, Long categoryId, String categoryName) {
        if (categoryId != null) {
            return categoryRepository.findById(categoryId).orElse(null);
        }
        if (categoryName != null) {
            String normalized = categoryName.trim();
            if (normalized.isEmpty()) normalized = "Khac";
            final String finalName = normalized;
            return categoryRepository.findByNameAndUserId(finalName, user.getId())
                    .or(() -> categoryRepository.findDefaultByName(finalName))
                    .orElseGet(() -> categoryRepository.save(Category.builder()
                            .name(finalName)
                            .note(null)
                            .isDefault(false)
                            .user(user)
                            .build()));
        }
        return null;
    }

    private String normalizeType(String type) {
        if (type == null) {
            return "expense";
        }
        String normalized = type.trim().toLowerCase();
        return normalized.isEmpty() ? "expense" : normalized;
    }

    // 5. Logic tìm kiếm giao dịch theo từ khóa
    @Override
    public List<TransactionResponse> searchTransactions(String userEmail, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllTransactions(userEmail);
        }
        User user = findUser(userEmail);
        return transactionRepository.searchByKeyword(user.getId(), keyword.trim())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
