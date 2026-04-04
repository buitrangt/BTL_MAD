package com.smartexpense.server.repository;

import com.smartexpense.server.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.user.id = :userId OR c.user IS NULL")
    List<Category> findByUserIdOrDefault(@Param("userId") Long userId);

    Optional<Category> findByNameAndUserIsNull(String name);

    Optional<Category> findByNameAndUserId(String name, Long userId);
}
