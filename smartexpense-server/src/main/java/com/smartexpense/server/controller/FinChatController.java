package com.smartexpense.server.controller;

import com.smartexpense.server.dto.ChatMessageDto;
import com.smartexpense.server.dto.ChatRequest;
import com.smartexpense.server.dto.ChatResponse;
import com.smartexpense.server.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finchat")
@RequiredArgsConstructor
public class FinChatController {

    private final ChatService chatService;

    /**
     * Send a message in a chat session. If sessionId is null, server creates a new one.
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(
            Authentication auth,
            @RequestBody ChatRequest request
    ) {
        ChatResponse response = chatService.sendMessage(
                auth.getName(),
                request.getSessionId(),
                request.getMessage()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get full message history for a session.
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessageDto>> getHistory(
            Authentication auth,
            @PathVariable String sessionId
    ) {
        return ResponseEntity.ok(chatService.getSessionHistory(auth.getName(), sessionId));
    }

    /**
     * Get most recent messages across all sessions (for sidebar/list).
     */
    @GetMapping("/recent")
    public ResponseEntity<List<ChatMessageDto>> getRecent(
            Authentication auth,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(chatService.getRecentMessages(auth.getName(), limit));
    }
}
