package com.smartexpense.server.service.impl;

import com.smartexpense.server.dto.SmsTemplateResponse;
import com.smartexpense.server.dto.SmsTransactionRequest;
import com.smartexpense.server.dto.SmsTransactionResponse;
import com.smartexpense.server.exception.ResourceNotFoundException;
import com.smartexpense.server.model.*;
import com.smartexpense.server.repository.*;
import com.smartexpense.server.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {
    private final SmsTemplateRepository smsTemplateRepository;
    private final SmsTransactionRepository smsTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public List<SmsTemplateResponse> getActiveTemplates() {
        return smsTemplateRepository.findByIsActiveTrue().stream()
                .map(t -> SmsTemplateResponse.builder()
                        .id(t.getId())
                        .senderPattern(t.getSenderPattern())
                        .amountRegex(t.getAmountRegex())
                        .type(t.getType())
                        .bankName(t.getBankName())
                        .version(t.getVersion())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<SmsTransactionResponse> syncSmsTransactions(String userEmail, List<SmsTransactionRequest> requests) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<SmsTransactionResponse> responses = new ArrayList<>();
        for (SmsTransactionRequest req : requests) {
            // Skip duplicates
            if (smsTransactionRepository.existsByUserIdAndRawContentAndTransactionTime(
                    user.getId(), req.getRawContent(), req.getTransactionTime())) {
                continue;
            }

            Category category = resolveCategory(req.getParsedCategoryName(), user.getId());

            String transactionType = "CREDIT".equals(req.getType()) ? "INCOME" : "EXPENSE";
            Transaction transaction = Transaction.builder()
                    .name("SMS - " + req.getSender())
                    .amount(req.getParsedAmount())
                    .category(category)
                    .type(transactionType)
                    .note("Auto-parsed from SMS: " + req.getSender())
                    .timeStamp(req.getTransactionTime())
                    .user(user)
                    .build();
            transaction = transactionRepository.save(transaction);

            SmsTransaction smsTransaction = SmsTransaction.builder()
                    .user(user)
                    .transaction(transaction)
                    .sender(req.getSender())
                    .rawContent(req.getRawContent())
                    .parsedAmount(req.getParsedAmount())
                    .parsedCategory(category)
                    .status("CONFIRMED")
                    .transactionTime(req.getTransactionTime())
                    .build();
            smsTransaction = smsTransactionRepository.save(smsTransaction);

            responses.add(SmsTransactionResponse.builder()
                    .id(smsTransaction.getId())
                    .transactionId(transaction.getId())
                    .sender(smsTransaction.getSender())
                    .parsedAmount(smsTransaction.getParsedAmount())
                    .status(smsTransaction.getStatus())
                    .transactionTime(smsTransaction.getTransactionTime())
                    .build());
        }
        return responses;
    }

    private Category resolveCategory(String categoryName, Long userId) {
        if (categoryName == null || categoryName.isEmpty()) return null;
        return categoryRepository.findByNameAndUserId(categoryName, userId)
                .or(() -> categoryRepository.findDefaultByName(categoryName))
                .orElse(null);
    }
}
