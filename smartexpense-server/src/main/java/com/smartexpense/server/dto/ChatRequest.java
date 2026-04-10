package com.smartexpense.server.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String sessionId; // optional - if null, server creates a new session
}
