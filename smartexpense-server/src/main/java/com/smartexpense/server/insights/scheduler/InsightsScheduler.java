package com.smartexpense.server.insights.scheduler;

import com.smartexpense.server.insights.service.InsightsComputeService;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Daily background job that recomputes AI insights for every active user.
 * Runs at 02:00 Asia/Bangkok every day.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsightsScheduler {

    private final UserRepository userRepository;
    private final InsightsComputeService computeService;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Bangkok")
    public void nightlyComputeAll() {
        log.info("=== InsightsScheduler: nightly compute started ===");
        long start = System.currentTimeMillis();

        List<User> users = userRepository.findAll();
        int success = 0;
        int failed = 0;

        for (User user : users) {
            try {
                computeService.computeCurrentMonthForUser(user.getId());
                success++;
            } catch (Exception e) {
                failed++;
                log.error("Insights compute failed for user {}: {}", user.getId(), e.getMessage());
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("=== InsightsScheduler: done. success={}, failed={}, elapsed={}ms ===",
                success, failed, elapsed);
    }
}
