package com.smartexpense.server.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(nullable = false)
    private String sender;

    @Column(name = "raw_content", nullable = false, columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "parsed_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal parsedAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parsed_category_id")
    private Category parsedCategory;

    @Column(nullable = false)
    private String status;

    @Column(name = "transaction_time", nullable = false)
    private Long transactionTime;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "CONFIRMED";
    }
}
