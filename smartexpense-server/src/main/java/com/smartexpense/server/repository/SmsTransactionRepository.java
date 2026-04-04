package com.smartexpense.server.repository;

import com.smartexpense.server.model.SmsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SmsTransactionRepository extends JpaRepository<SmsTransaction, Long> {
    List<SmsTransaction> findByUserIdOrderByTransactionTimeDesc(Long userId);
    boolean existsByUserIdAndRawContentAndTransactionTime(Long userId, String rawContent, Long transactionTime);
}
