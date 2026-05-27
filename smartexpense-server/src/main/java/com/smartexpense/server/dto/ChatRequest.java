package com.smartexpense.server.dto;

import lombok.Data;

/**
 * Data Transfer Object (DTO) gửi tin nhắn trò chuyện từ Client lên FinChat (AI).
 */
@Data
public class ChatRequest {
    private String message;   // Nội dung tin nhắn người dùng gửi lên chatbot AI
    private String sessionId; // Mã phiên hội thoại (tùy chọn - nếu rỗng, server sẽ tự khởi tạo phiên mới)
}
