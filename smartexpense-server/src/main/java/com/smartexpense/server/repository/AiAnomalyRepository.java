package com.smartexpense.server.repository;

import com.smartexpense.server.model.AiAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiAnomalyRepository extends JpaRepository<AiAnomaly, Long> {
    List<AiAnomaly> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);

    @Modifying
    @Query("DELETE FROM AiAnomaly a WHERE a.userId = :userId AND a.month = :month AND a.year = :year")
    void deleteByUserIdAndMonthAndYear(@Param("userId") Long userId,
                                       @Param("month") Integer month,
                                       @Param("year") Integer year);
}
