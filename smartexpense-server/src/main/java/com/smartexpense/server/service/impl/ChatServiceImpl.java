package com.smartexpense.server.service.impl;

import com.smartexpense.server.dto.ChatMessageDto;
import com.smartexpense.server.dto.ChatResponse;
import com.smartexpense.server.insights.dto.AnomalyDto;
import com.smartexpense.server.insights.dto.BudgetSuggestionDto;
import com.smartexpense.server.insights.dto.InsightsSummaryDto;
import com.smartexpense.server.insights.service.InsightsQueryService;
import com.smartexpense.server.model.ChatMessage;
import com.smartexpense.server.model.Transaction;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.ChatMessageRepository;
import com.smartexpense.server.repository.TransactionRepository;
import com.smartexpense.server.repository.UserRepository;
import com.smartexpense.server.service.ChatService;
import com.smartexpense.server.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TransactionRepository transactionRepository;
    private final GeminiService geminiService;
    private final InsightsQueryService insightsQueryService;

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final TimeZone ZONE = TimeZone.getTimeZone("Asia/Bangkok");

    @Override
    public ChatResponse sendMessage(String userEmail, String sessionId, String userMessage) {
        User user = findUser(userEmail);

        // Resolve session id
        String sid = (sessionId == null || sessionId.isBlank())
                ? UUID.randomUUID().toString()
                : sessionId;

        // Build financial context from insights
        String systemPrompt = buildSystemPrompt(userEmail);

        // Load history of this session
        List<ChatMessage> history = chatMessageRepository
                .findByUserIdAndSessionIdOrderByCreatedAtAsc(user.getId(), sid);

        // Call Gemini
        String reply = geminiService.chat(systemPrompt, history, userMessage);

        // Persist user message + assistant reply
        ChatMessage userMsg = ChatMessage.builder()
                .userId(user.getId())
                .sessionId(sid)
                .role("user")
                .content(userMessage)
                .build();
        chatMessageRepository.save(userMsg);

        ChatMessage botMsg = ChatMessage.builder()
                .userId(user.getId())
                .sessionId(sid)
                .role("assistant")
                .content(reply)
                .build();
        chatMessageRepository.save(botMsg);

        return ChatResponse.builder()
                .sessionId(sid)
                .reply(reply)
                .build();
    }

    @Override
    public List<ChatMessageDto> getSessionHistory(String userEmail, String sessionId) {
        User user = findUser(userEmail);
        return chatMessageRepository
                .findByUserIdAndSessionIdOrderByCreatedAtAsc(user.getId(), sessionId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageDto> getRecentMessages(String userEmail, int limit) {
        User user = findUser(userEmail);
        return chatMessageRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .limit(limit)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Builds the system prompt that gives Gemini full financial context for the user.
     * Includes: spending classification, prediction, anomalies, budget suggestions.
     */
    private String buildSystemPrompt(String userEmail) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là FinBot - trợ lý tài chính cá nhân thông minh của ứng dụng SmartExpense. ");
        sb.append("Hãy trả lời ngắn gọn, thân thiện bằng tiếng Việt. ");
        sb.append("Dùng emoji hợp lý. Khi đề cập số tiền, dùng định dạng có dấu chấm phân cách hàng nghìn + 'đ' (ví dụ 250.000đ). ");
        sb.append("Dựa vào DỮ LIỆU TÀI CHÍNH dưới đây để tư vấn cá nhân hóa:\n\n");

        appendTransactionSnapshot(sb, userEmail);

        try {
            InsightsSummaryDto summary = insightsQueryService.getSummary(userEmail);

            // Classification
            if (summary.getClassification() != null) {
                sb.append("📊 MỨC CHI TIÊU THÁNG NÀY: ")
                        .append(summary.getClassification().getLabel())
                        .append(" (").append(summary.getClassification().getLevel()).append(")\n");
                if (summary.getClassification().getNote() != null) {
                    sb.append("Ghi chú: ").append(summary.getClassification().getNote()).append("\n");
                }
                sb.append("\n");
            }

            // Prediction
            if (summary.getPrediction() != null) {
                sb.append("🔮 DỰ ĐOÁN CHI TIÊU THÁNG: ")
                        .append("Hiện tại đã chi ").append(formatVnd(summary.getPrediction().getCurrentAmount()))
                        .append(", dự đoán cuối tháng: ").append(formatVnd(summary.getPrediction().getPredictedAmount()))
                        .append("\n\n");
            }

            // Anomalies
            if (summary.getAnomalies() != null && !summary.getAnomalies().isEmpty()) {
                sb.append("🚨 BẤT THƯỜNG TRONG CHI TIÊU:\n");
                for (AnomalyDto a : summary.getAnomalies()) {
                    sb.append("- ").append(a.getCategoryName())
                            .append(": tăng ").append(a.getPercentIncrease()).append("% ")
                            .append("(+").append(formatVnd(a.getAmountDiff())).append(")\n");
                }
                sb.append("\n");
            } else {
                sb.append("✅ Không có khoản chi bất thường nào tháng này.\n\n");
            }

            // Budget suggestions
            if (summary.getBudgetSuggestions() != null && !summary.getBudgetSuggestions().isEmpty()) {
                sb.append("💰 NGÂN SÁCH ĐỀ XUẤT THÁNG NÀY:\n");
                for (BudgetSuggestionDto b : summary.getBudgetSuggestions()) {
                    sb.append("- ").append(b.getCategoryName()).append(": ")
                            .append(formatVnd(b.getSuggestedAmount())).append("\n");
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            log.warn("Failed to load insights for system prompt: {}", e.getMessage());
            sb.append("(Chưa có đủ dữ liệu phân tích)\n\n");
        }

        sb.append("Khi user hỏi câu chung không liên quan tài chính cá nhân, vẫn trả lời thân thiện và chuyên nghiệp.");
        return sb.toString();
    }

    private String formatVnd(java.math.BigDecimal amount) {
        if (amount == null) return "0đ";
        return VND.format(amount.longValue()) + "đ";
    }

    /**
     * Appends a per-user financial snapshot so Gemini can answer questions like
     * "hôm nay tôi nhận bao nhiêu" or "tuần này tôi chi bao nhiêu".
     */
    private void appendTransactionSnapshot(StringBuilder sb, String userEmail) {
        try {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user == null) return;

            Calendar cal = Calendar.getInstance(ZONE);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();
            long endOfDay = startOfDay + 24L * 60 * 60 * 1000 - 1;

            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            long startOfWeek = cal.getTimeInMillis();

            cal.set(Calendar.DAY_OF_MONTH, 1);
            long startOfMonth = cal.getTimeInMillis();
            cal.add(Calendar.MONTH, 1);
            long endOfMonth = cal.getTimeInMillis() - 1;

            long now = System.currentTimeMillis();

            BigDecimal todayIncome = sumByType(user.getId(), "income", startOfDay, endOfDay);
            BigDecimal todayExpense = sumByType(user.getId(), "expense", startOfDay, endOfDay);
            BigDecimal weekIncome = sumByType(user.getId(), "income", startOfWeek, now);
            BigDecimal weekExpense = sumByType(user.getId(), "expense", startOfWeek, now);
            BigDecimal monthIncome = sumByType(user.getId(), "income", startOfMonth, endOfMonth);
            BigDecimal monthExpense = sumByType(user.getId(), "expense", startOfMonth, endOfMonth);
            BigDecimal monthSavings = monthIncome.subtract(monthExpense);

            sb.append("📅 TÓM TẮT TÀI CHÍNH (theo múi giờ Asia/Bangkok):\n");
            sb.append("• Hôm nay: thu ").append(formatVnd(todayIncome))
                    .append(" / chi ").append(formatVnd(todayExpense)).append("\n");
            sb.append("• Tuần này (từ T2): thu ").append(formatVnd(weekIncome))
                    .append(" / chi ").append(formatVnd(weekExpense)).append("\n");
            sb.append("• Tháng này: thu ").append(formatVnd(monthIncome))
                    .append(" / chi ").append(formatVnd(monthExpense))
                    .append(" / tiết kiệm ").append(formatVnd(monthSavings)).append("\n\n");

            List<Transaction> recent = transactionRepository
                    .findByUserIdOrderByTimeStampDesc(user.getId())
                    .stream()
                    .limit(10)
                    .collect(Collectors.toList());

            if (!recent.isEmpty()) {
                SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", new Locale("vi", "VN"));
                fmt.setTimeZone(ZONE);
                sb.append("🧾 10 GIAO DỊCH GẦN NHẤT (mới → cũ):\n");
                for (Transaction t : recent) {
                    boolean isIncome = "income".equalsIgnoreCase(t.getType());
                    String sign = isIncome ? "+" : "-";
                    String categoryName = t.getCategory() != null ? t.getCategory().getName() : "(không có danh mục)";
                    sb.append("- ")
                            .append(fmt.format(new Date(t.getTimeStamp())))
                            .append(" ").append(sign).append(formatVnd(t.getAmount()))
                            .append(" · ").append(categoryName);
                    if (t.getName() != null && !t.getName().isBlank()) {
                        sb.append(" · ").append(t.getName());
                    }
                    if (t.getNote() != null && !t.getNote().isBlank()) {
                        sb.append(" (").append(t.getNote()).append(")");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            log.warn("Failed to build transaction snapshot for chat: {}", e.getMessage());
        }
    }

    private BigDecimal sumByType(Long userId, String type, long start, long end) {
        return transactionRepository
                .findByUserIdAndTypeAndTimeStampBetween(userId, type, start, end)
                .stream()
                .map(Transaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ChatMessageDto toDto(ChatMessage m) {
        return ChatMessageDto.builder()
                .id(m.getId())
                .sessionId(m.getSessionId())
                .role(m.getRole())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
