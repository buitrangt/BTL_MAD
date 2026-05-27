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

/**
 * Controller xử lý các yêu cầu liên quan đến Trợ lý ảo FinChat (Chatbot AI)
 * Nhận tin nhắn trò chuyện, truy vấn lịch sử hội thoại của người dùng.
 */
@RestController
@RequestMapping("/api/finchat")
@RequiredArgsConstructor
public class FinChatController {

    private final ChatService chatService;

    /**
     * API gửi tin nhắn chat đến Trợ lý ảo FinChat
     * POST /api/finchat/message
     *
     * @param auth    Thông tin người dùng đã đăng nhập (lấy email từ JWT Token)
     * @param request Payload chứa tin nhắn người dùng và sessionId của cuộc hội thoại
     * @return Câu trả lời từ AI kèm sessionId cập nhật
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
     * API lấy toàn bộ lịch sử tin nhắn của một phiên chat cụ thể
     * GET /api/finchat/history/{sessionId}
     *
     * @param auth      Thông tin người dùng đăng nhập
     * @param sessionId Mã định danh phiên hội thoại cần lấy lịch sử
     * @return Danh sách các tin nhắn đã trò chuyện trong phiên chat
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessageDto>> getHistory(
            Authentication auth,
            @PathVariable String sessionId
    ) {
        return ResponseEntity.ok(chatService.getSessionHistory(auth.getName(), sessionId));
    }

    /**
     * API lấy danh sách các tin nhắn gần đây nhất của người dùng trên toàn bộ các phiên
     * GET /api/finchat/recent
     *
     * @param auth  Thông tin người dùng đăng nhập
     * @param limit Giới hạn số lượng tin nhắn trả về (mặc định là 20)
     * @return Danh sách các tin nhắn gần đây nhất
     */
    @GetMapping("/recent")
    public ResponseEntity<List<ChatMessageDto>> getRecent(
            Authentication auth,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(chatService.getRecentMessages(auth.getName(), limit));
    }
}

