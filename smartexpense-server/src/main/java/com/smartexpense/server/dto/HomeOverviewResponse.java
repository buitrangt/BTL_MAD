package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class HomeOverviewResponse {
    private BigDecimal todayAmount;
    private BigDecimal weekAmount;
    private BigDecimal monthAmount;
    private BigDecimal monthIncome;
    private BigDecimal monthExpense;
    private BigDecimal monthSavings;
    private List<TransactionResponse> recentTransactions;
}
