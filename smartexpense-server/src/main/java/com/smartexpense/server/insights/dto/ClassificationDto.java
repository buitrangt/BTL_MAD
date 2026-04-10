package com.smartexpense.server.insights.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ClassificationDto {
    private Integer month;
    private Integer year;
    private String level;     // SAVING / NORMAL / WASTEFUL / OVERSPENT / NO_INCOME
    private String label;     // Tiết kiệm tốt / Bình thường / ...
    private String note;
}
