package com.smartexpense.server.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_classifications", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "month", "year"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiClassification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, length = 20)
    private String level;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "PROCESSING";
    }
}
