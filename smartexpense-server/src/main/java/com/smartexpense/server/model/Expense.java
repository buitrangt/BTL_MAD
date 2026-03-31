package com.smartexpense.server.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description; // Nội dung: "Ăn sáng", "Mua sắm"
    private Double amount;      // Số tiền: 50000
    private String category;    // Phân loại: "Food", "Transport"
    private LocalDateTime date; // Ngày tiêu

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Gắn với User nào
}