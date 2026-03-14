package com.smartexpense.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExpenseRequest {
    @NotNull
    private Double amount;

    @NotBlank
    private String category;

    @NotNull
    private Long timeStamp;
}
