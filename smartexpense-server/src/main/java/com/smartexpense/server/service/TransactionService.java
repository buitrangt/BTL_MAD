package com.smartexpense.server.service;

import com.smartexpense.server.dto.TransactionRequest;
import com.smartexpense.server.dto.TransactionResponse;
import java.util.List;

public interface TransactionService {
    List<TransactionResponse> getAllTransactions(String userEmail);
    TransactionResponse createTransaction(String userEmail, TransactionRequest request);
    TransactionResponse updateTransaction(String userEmail, Long transactionId, TransactionRequest request);
    void deleteTransaction(String userEmail, Long transactionId);
    List<TransactionResponse> syncTransactions(String userEmail, List<TransactionRequest> requests);
    List<TransactionResponse> searchTransactions(String userEmail, String keyword);
}
