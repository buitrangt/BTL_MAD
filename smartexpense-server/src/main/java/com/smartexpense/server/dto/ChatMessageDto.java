package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private Long id;
    private String sessionId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
