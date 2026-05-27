package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) đại diện cho một tin nhắn đơn lẻ trong lịch sử hội thoại của Chatbot AI gửi ra Client.
 */
@Data
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private Long id;                 // Khóa chính định danh tin nhắn
    private String sessionId;        // Mã phiên chat chứa tin nhắn này
    private String role;             // Vai trò của người gửi ("user" hoặc "assistant")
    private String content;          // Nội dung văn bản của tin nhắn
    private LocalDateTime createdAt; // Thời điểm tin nhắn được lưu vào cơ sở dữ liệu
}
