package com.smartexpense.server.repository;

import com.smartexpense.server.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByTimeStampDesc(Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.timeStamp >= :startTime AND t.timeStamp <= :endTime")
    List<Transaction> findByUserIdAndTimeStampBetween(
        @Param("userId") Long userId,
        @Param("startTime") Long startTime,
        @Param("endTime") Long endTime
    );

    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.timeStamp >= :startTime AND t.timeStamp <= :endTime AND t.category IS NOT NULL GROUP BY t.category.name")
    List<Object[]> sumByCategory(
        @Param("userId") Long userId,
        @Param("startTime") Long startTime,
        @Param("endTime") Long endTime
    );

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.note) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.type) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY t.timeStamp DESC")
    List<Transaction> searchByKeyword(
        @Param("userId") Long userId,
        @Param("keyword") String keyword
    );
}
