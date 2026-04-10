package com.smartexpense.server.repository;

import com.smartexpense.server.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUserIdAndSessionIdOrderByCreatedAtAsc(Long userId, String sessionId);
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId);
}
