package com.smartexpense.server.repository;

import com.smartexpense.server.model.AiPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiPredictionRepository extends JpaRepository<AiPrediction, Long> {
    Optional<AiPrediction> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);
}
