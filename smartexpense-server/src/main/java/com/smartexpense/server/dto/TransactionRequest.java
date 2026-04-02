package com.smartexpense.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {
    private String name;

    @NotNull
    private BigDecimal amount;

    private Long categoryId;

    @NotBlank
    private String type;

    private String note;

    @NotNull
    private Long timeStamp;
}
