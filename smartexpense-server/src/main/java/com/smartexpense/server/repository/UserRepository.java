package com.smartexpense.server.repository;

import com.smartexpense.server.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository xử lý các truy vấn cơ sở dữ liệu liên quan đến bảng users.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Tìm kiếm người dùng dựa vào địa chỉ email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Kiểm tra xem địa chỉ email đã được đăng ký trong hệ thống chưa.
     */
    boolean existsByEmail(String email);

    /**
     * Đếm số lượng tài khoản đăng ký mới trong một khoảng thời gian nhất định.
     */
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Truy vấn danh sách các tài khoản đăng ký gần đây nhất.
     */
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findRecent();

    /**
     * Tìm danh sách tài khoản được đăng ký trong khoảng thời gian chỉ định.
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :from AND :to ORDER BY u.createdAt ASC")
    List<User> findByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Tìm kiếm phân trang tài khoản người dùng theo từ khóa tìm kiếm (email hoặc tên).
     */
    @Query("SELECT u FROM User u WHERE :q IS NULL OR :q = '' " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(COALESCE(u.name, '')) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<User> searchUsers(@Param("q") String q, Pageable pageable);
}
