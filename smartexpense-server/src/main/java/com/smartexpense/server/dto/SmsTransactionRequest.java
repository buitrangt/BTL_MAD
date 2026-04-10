package com.smartexpense.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SmsTransactionRequest {
    @NotBlank
    private String sender;

    @NotBlank
    private String rawContent;

    @NotNull
    private BigDecimal parsedAmount;

    private String parsedCategoryName;

    @NotBlank
    private String type;

    @NotNull
    private Long transactionTime;
}
