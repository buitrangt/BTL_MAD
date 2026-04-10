package com.smartexpense.server.service;

import com.smartexpense.server.dto.ChatMessageDto;
import com.smartexpense.server.dto.ChatResponse;

import java.util.List;

public interface ChatService {
    /**
     * Sends a message in a chat session and gets the AI reply.
     * If sessionId is null/blank, creates a new session UUID.
     */
    ChatResponse sendMessage(String userEmail, String sessionId, String userMessage);

    /**
     * Returns full message history of a session for the user.
     */
    List<ChatMessageDto> getSessionHistory(String userEmail, String sessionId);

    /**
     * Returns latest N sessions for the user (1 entry per session, with last message preview).
     */
    List<ChatMessageDto> getRecentMessages(String userEmail, int limit);
}
