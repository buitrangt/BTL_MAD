package com.smartexpense.server.repository;

import com.smartexpense.server.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository xử lý các truy vấn cơ sở dữ liệu liên quan đến bảng chat_messages (tin nhắn chatbot AI).
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Lấy toàn bộ lịch sử tin nhắn của một cuộc hội thoại cụ thể dựa trên userId và sessionId,
     * sắp xếp theo thứ tự thời gian tăng dần (cũ đến mới).
     */
    List<ChatMessage> findByUserIdAndSessionIdOrderByCreatedAtAsc(Long userId, String sessionId);

    /**
     * Lấy tất cả tin nhắn của một người dùng, sắp xếp theo thời gian mới nhất trước.
     */
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId);
}
