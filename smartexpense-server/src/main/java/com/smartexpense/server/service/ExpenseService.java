/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartexpense.server.service;

/**
 *
 * @author admin
 */
import com.smartexpense.server.model.Expense;
import com.smartexpense.server.repository.ExpenseRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository expenseRepository;

    public String getSummaryByUserId(Long userId) {
        List<Expense> list = expenseRepository.findByUserId(userId);
        if (list.isEmpty()) return "Chưa có dữ liệu chi tiêu nào.";

        StringBuilder sb = new StringBuilder("Dữ liệu chi tiêu của người dùng: ");
        for (Expense e : list) {
            sb.append(e.getDescription()).append(" tiêu ").append(e.getAmount()).append(" VNĐ. ");
        }
        return sb.toString();
    }
}
