package com.smartexpense.server.repository;

import com.smartexpense.server.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpToken, Long> {
    
    Optional<OtpToken> findTopByEmailOrderByExpiryTimeDesc(String email);

    void deleteByEmail(String email);
}