package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private String name;
    private BigDecimal amount;
    private Long categoryId;
    private String categoryName;
    private String type;
    private String note;
    private Long timeStamp;
}
