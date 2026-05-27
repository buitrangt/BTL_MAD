package com.smartexpense.server.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể JPA (JPA Entity) đại diện cho bảng "chat_messages" lưu trữ toàn bộ lịch sử tin nhắn trò chuyện với Chatbot AI.
 */
@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính tự động tăng

    @Column(name = "user_id", nullable = false)
    private Long userId; // Khóa ngoại liên kết tới bảng users (id của người dùng sở hữu tin nhắn)

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId; // Mã phiên hội thoại UUID kết nối các tin nhắn cùng cuộc trò chuyện

    @Column(nullable = false, length = 20)
    private String role;  // Vai trò của người gửi: nhận giá trị "user" hoặc "assistant"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // Nội dung tin nhắn

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // Thời điểm gửi tin nhắn

    /**
     * Tự động gán thời gian hiện tại vào trường createdAt trước khi lưu vào DB.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
