package com.smartexpense.server.repository;

import com.smartexpense.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findRecent();

    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :from AND :to ORDER BY u.createdAt ASC")
    List<User> findByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
