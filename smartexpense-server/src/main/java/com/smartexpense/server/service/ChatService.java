package com.smartexpense.server.service;

import com.smartexpense.server.dto.ChatMessageDto;
import com.smartexpense.server.dto.ChatResponse;

import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ giao tiếp với FinChat (Chatbot AI).
 * Đóng vai trò làm cổng kết nối xử lý và lưu trữ lịch sử trò chuyện.
 */
public interface ChatService {
    
    /**
     * Nghiệp vụ gửi tin nhắn và nhận phản hồi từ trợ lý ảo FinChat AI.
     * Tự động khởi tạo UUID cho sessionId nếu đây là tin nhắn đầu tiên của phiên chat mới.
     *
     * @param userEmail Email của người dùng gửi tin nhắn (dùng để xác định danh tính và lấy dữ liệu chi tiêu làm ngữ cảnh).
     * @param sessionId Mã định danh phiên chat hiện tại (tùy chọn).
     * @param userMessage Nội dung câu hỏi/tin nhắn của người dùng gửi đi.
     * @return ChatResponse Chứa câu trả lời của AI và sessionId cập nhật.
     */
    ChatResponse sendMessage(String userEmail, String sessionId, String userMessage);

    /**
     * Nghiệp vụ lấy toàn bộ danh sách lịch sử tin nhắn của một phiên chat cụ thể.
     *
     * @param userEmail Email của người dùng sở hữu phiên chat.
     * @param sessionId Mã phiên chat cần truy xuất lịch sử.
     * @return Danh sách các tin nhắn đã gửi và nhận trong phiên.
     */
    List<ChatMessageDto> getSessionHistory(String userEmail, String sessionId);

    /**
     * Nghiệp vụ lấy danh sách các tin nhắn trò chuyện gần đây nhất của người dùng trên hệ thống.
     *
     * @param userEmail Email người dùng đăng nhập.
     * @param limit Giới hạn tối đa số lượng tin nhắn trả về.
     * @return Danh sách tin nhắn gần đây.
     */
    List<ChatMessageDto> getRecentMessages(String userEmail, int limit);
}
