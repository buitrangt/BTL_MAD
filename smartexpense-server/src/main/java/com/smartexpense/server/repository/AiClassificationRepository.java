package com.smartexpense.server.repository;

import com.smartexpense.server.model.AiClassification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiClassificationRepository extends JpaRepository<AiClassification, Long> {
    Optional<AiClassification> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);
}
