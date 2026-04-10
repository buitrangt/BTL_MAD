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
 * Gemini API client. Uses the public REST endpoint:
 *   POST {url}/{model}:generateContent?key={key}
 *
 * Maintains conversation history by sending all prior messages in the request.
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
     * Sends a chat completion request with system prompt + conversation history + new user message.
     *
     * @param systemPrompt   The financial context / instructions to seed the model.
     * @param history        Prior messages of this session (oldest first), excluding the new user message.
     * @param userMessage    The new user message.
     * @return Plain text answer from Gemini, or an error string.
     */
    public String chat(String systemPrompt, List<ChatMessage> history, String userMessage) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("YOUR_")) {
            return "[Lỗi cấu hình] Chưa thiết lập GEMINI_API_KEY trong application.properties.";
        }

        try {
            String url = baseUrl + "/" + model + ":generateContent?key=" + apiKey;

            JSONArray contents = new JSONArray();

            // Inject system prompt as the first "user" turn (Gemini does not accept system role
            // in v1beta gemini-2.0-flash; we prepend instructions to the conversation).
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                contents.put(makeContent("user", systemPrompt));
                contents.put(makeContent("model", "Đã hiểu. Tôi sẽ tư vấn dựa trên dữ liệu tài chính trên."));
            }

            // History
            if (history != null) {
                for (ChatMessage msg : history) {
                    String role = "user".equals(msg.getRole()) ? "user" : "model";
                    contents.put(makeContent(role, msg.getContent()));
                }
            }

            // New user message
            contents.put(makeContent("user", userMessage));

            JSONObject body = new JSONObject();
            body.put("contents", contents);

            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.7);
            genConfig.put("maxOutputTokens", 1024);
            body.put("generationConfig", genConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getBody() == null) {
                return "Không nhận được phản hồi từ AI.";
            }

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
