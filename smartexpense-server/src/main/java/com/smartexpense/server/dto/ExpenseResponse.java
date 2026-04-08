package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder
public class ExpenseResponse {
    private Long id;
    private BigDecimal amount;
    private String category;
    private Long timeStamp;
}
