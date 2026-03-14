package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ExpenseResponse {
    private Long id;
    private Double amount;
    private String category;
    private Long timeStamp;
}
