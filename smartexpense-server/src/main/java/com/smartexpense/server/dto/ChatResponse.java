package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object (DTO) phản hồi kết quả hội thoại từ chatbot AI về cho Client.
 */
@Data
@AllArgsConstructor
@Builder
public class ChatResponse {
    private String sessionId; // Mã phiên hội thoại (đảm bảo đồng bộ luồng trò chuyện giữa Client và Server)
    private String reply;     // Nội dung phản hồi từ mô hình AI
}
