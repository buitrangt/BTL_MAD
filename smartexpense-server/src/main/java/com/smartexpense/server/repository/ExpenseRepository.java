/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartexpense.server.repository;

import com.smartexpense.server.model.Expense;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author admin
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    // Lấy tất cả chi tiêu của User
    List<Expense> findByUserId(Long userId);
}
