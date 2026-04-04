package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class SmsTemplateResponse {
    private Long id;
    private String senderPattern;
    private String amountRegex;
    private String type;
    private String bankName;
    private Integer version;
}
