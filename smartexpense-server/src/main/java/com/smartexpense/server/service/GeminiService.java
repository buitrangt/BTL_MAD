package com.smartexpense.server.service;

import com.smartexpense.server.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Service kết nối và giao tiếp với Google Gemini API (External AI Service)
 * Đóng vai trò là đầu mối gửi ngữ cảnh tài chính, lịch sử chat và câu hỏi mới của người dùng
 * sang mô hình AI để nhận về phản hồi tư vấn tài chính cá nhân hóa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    @Value("${gemini.api.url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Gửi yêu cầu sinh nội dung trò chuyện (generateContent) sang Google Gemini API.
     *
     * @param systemPrompt   Ngữ cảnh dữ liệu tài chính (tổng chi tiêu hôm nay, tuần này, giao dịch gần nhất...) làm Prompt chỉ dẫn hệ thống.
     * @param history        Lịch sử trò chuyện cũ của phiên chat hiện tại (bỏ qua tin nhắn mới của người dùng).
     * @param userMessage    Tin nhắn/câu hỏi mới nhất của người dùng.
     * @return Văn bản phản hồi (câu trả lời) từ Gemini API, hoặc chuỗi thông báo lỗi nếu có sự cố xảy ra.
     */
    public String chat(String systemPrompt, List<ChatMessage> history, String userMessage) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("YOUR_")) {
            return "[Lỗi cấu hình] Chưa thiết lập GEMINI_API_KEY trong application.properties.";
        }

        try {
            // Khởi dựng URL gọi API của Google Gemini
            String url = baseUrl + "/" + model + ":generateContent?key=" + apiKey;

            JSONArray contents = new JSONArray();

            // Đưa system prompt làm lượt hội thoại đầu tiên để "mồi" dữ liệu tài chính cho AI
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                contents.put(makeContent("user", systemPrompt));
                contents.put(makeContent("model", "Đã hiểu. Tôi sẽ tư vấn dựa trên dữ liệu tài chính trên."));
            }

            // Đưa lịch sử các tin nhắn cũ của phiên chat vào request để AI nắm bắt được luồng hội thoại
            if (history != null) {
                for (ChatMessage msg : history) {
                    String role = "user".equals(msg.getRole()) ? "user" : "model";
                    contents.put(makeContent(role, msg.getContent()));
                }
            }

            // Đưa tin nhắn mới nhất của người dùng vào cuối danh sách hội thoại
            contents.put(makeContent("user", userMessage));

            // Đóng gói cấu trúc Payload JSON gửi đi
            JSONObject body = new JSONObject();
            body.put("contents", contents);

            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.7);
            genConfig.put("maxOutputTokens", 1024);
            body.put("generationConfig", genConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            
            // Thực hiện cuộc gọi HTTP POST gửi Payload sang Google Gemini API
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getBody() == null) {
                return "Không nhận được phản hồi từ AI.";
            }

            // Phân tách kết quả JSON trả về để trích xuất văn bản trả lời của AI
            JSONObject json = new JSONObject(response.getBody());
            if (!json.has("candidates")) {
                log.warn("Gemini response missing candidates: {}", response.getBody());
                return "AI không trả về nội dung. Vui lòng thử lại.";
            }
            JSONArray candidates = json.getJSONArray("candidates");
            if (candidates.isEmpty()) {
                return "AI không trả về nội dung.";
            }
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length(); i++) {
                if (parts.getJSONObject(i).has("text")) {
                    sb.append(parts.getJSONObject(i).getString("text"));
                }
            }
            return sb.toString().trim();

        } catch (Exception e) {
            log.error("Gemini call failed: {}", e.getMessage(), e);
            return "Lỗi gọi AI: " + e.getMessage();
        }
    }

    /**
     * Hàm phụ trợ đóng gói nội dung tin nhắn theo đúng định dạng cấu trúc JSON của Gemini API.
     */
    private JSONObject makeContent(String role, String text) {
        JSONObject obj = new JSONObject();
        obj.put("role", role);
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", text);
        parts.put(part);
        obj.put("parts", parts);
        return obj;
    }
}
