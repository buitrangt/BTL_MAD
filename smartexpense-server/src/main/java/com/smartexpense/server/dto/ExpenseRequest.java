package com.smartexpense.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseRequest {
    @NotNull
    private BigDecimal amount;

    @NotBlank
    private String category;

    @NotNull
    private Long timeStamp;

    private String note;

    private String type;
}
