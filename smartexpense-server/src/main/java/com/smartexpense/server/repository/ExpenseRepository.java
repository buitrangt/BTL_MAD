package com.smartexpense.server.repository;

import com.smartexpense.server.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserIdOrderByTimeStampDesc(Long userId);

    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId AND e.timeStamp >= :startTime AND e.timeStamp <= :endTime")
    List<Expense> findByUserIdAndTimeStampBetween(
        @Param("userId") Long userId,
        @Param("startTime") Long startTime,
        @Param("endTime") Long endTime
    );

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND e.timeStamp >= :startTime AND e.timeStamp <= :endTime GROUP BY e.category")
    List<Object[]> sumByCategory(
        @Param("userId") Long userId,
        @Param("startTime") Long startTime,
        @Param("endTime") Long endTime
    );
}
