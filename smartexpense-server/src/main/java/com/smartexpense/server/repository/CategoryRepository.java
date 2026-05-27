package com.smartexpense.server.repository;

import com.smartexpense.server.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * Repository thao tác với CSDL cho bảng danh mục (Categories).
 * Thuộc luồng chức năng: Quản lý danh mục.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Truy vấn danh sách danh mục của người dùng hoặc danh mục mặc định
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId OR c.user IS NULL")
    List<Category> findByUserIdOrDefault(@Param("userId") Long userId);

    java.util.Optional<Category> findByIdAndUserId(Long id, Long userId);
    java.util.Optional<Category> findByNameAndUserId(String name, Long userId);

    @Query("SELECT c FROM Category c WHERE c.name = :name AND c.user IS NULL")
    java.util.Optional<Category> findDefaultByName(@Param("name") String name);

    @Query("SELECT c FROM Category c WHERE c.user IS NULL ORDER BY c.name ASC")
    List<Category> findAllDefault();

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = :name AND c.user IS NULL")
    boolean existsDefaultByName(@Param("name") String name);
}
