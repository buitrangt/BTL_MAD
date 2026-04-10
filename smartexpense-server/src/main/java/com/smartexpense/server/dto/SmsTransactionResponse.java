package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder
public class SmsTransactionResponse {
    private Long id;
    private Long transactionId;
    private String sender;
    private BigDecimal parsedAmount;
    private String status;
    private Long transactionTime;
}
