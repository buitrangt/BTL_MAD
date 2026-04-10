package com.smartexpense.server.repository;

import com.smartexpense.server.model.SmsTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SmsTemplateRepository extends JpaRepository<SmsTemplate, Long> {
    List<SmsTemplate> findByIsActiveTrue();
}
