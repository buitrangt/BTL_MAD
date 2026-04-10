package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ChatResponse {
    private String sessionId;
    private String reply;
}
