/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartexpense.server.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author admin
 */
@Service
public class FinChatService {

    // 1. URL và Key chuẩn
    private final String API_URL = "https://apifreellm.com/api/v1/chat";
    private final String API_KEY = "apf_miqaq9tc4pm82v52lvf5wqsz";

    public String getChatResponse(String userMessage, String financialContext) {
        RestTemplate restTemplate = new RestTemplate();

        // 2. Dùng Map và put dữ liệu vào (Dùng HashMap của java.util)
        java.util.Map<String, String> body = new java.util.HashMap<>();
        String fullPrompt = "Dữ liệu tài chính của tôi: " + financialContext + ". Câu hỏi: " + userMessage;
        body.put("message", fullPrompt);
        body.put("model", "apifreellm");

        // 3. Cài đặt Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);

        HttpEntity<java.util.Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<java.util.Map> response = restTemplate.postForEntity(API_URL, entity, java.util.Map.class);
            System.out.println("Full AI Response: " + response.getBody());

            if (response.getBody() != null) {
                if (response.getBody().containsKey("response")) {
                    return response.getBody().get("response").toString();
                }
            }
            return "Không tìm thấy key 'response' trong JSON: " + response.getBody();
        } catch (Exception e) {
            return "Lỗi kết nối AI: " + e.getMessage();
        }
    }
}
