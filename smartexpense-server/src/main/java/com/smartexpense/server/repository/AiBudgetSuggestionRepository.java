package com.smartexpense.server.repository;

import com.smartexpense.server.model.AiBudgetSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiBudgetSuggestionRepository extends JpaRepository<AiBudgetSuggestion, Long> {
    List<AiBudgetSuggestion> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);

    @Modifying
    @Query("DELETE FROM AiBudgetSuggestion s WHERE s.userId = :userId AND s.month = :month AND s.year = :year")
    void deleteByUserIdAndMonthAndYear(@Param("userId") Long userId,
                                       @Param("month") Integer month,
                                       @Param("year") Integer year);
}
