package com.smartexpense.server.repository;

import com.smartexpense.server.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * Repository thao tác với CSDL cho bảng giao dịch (Transactions).
 * Thuộc luồng chức năng: Quản lý giao dịch, Xem chi tiêu, Xem thống kê.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByTimeStampDesc(Long userId);
    List<Transaction> findByUserIdAndTypeIgnoreCaseOrderByTimeStampDesc(Long userId, String type);
    java.util.Optional<Transaction> findByIdAndUserId(Long id, Long userId);
    long countByUserIdAndCategoryId(Long userId, Long categoryId);

    // Truy vấn cơ sở dữ liệu để tìm kiếm giao dịch theo từ khóa (tên, ghi chú, danh mục)
    @Query("""
            SELECT t
            FROM Transaction t
            WHERE t.user.id = :userId
              AND (
                    LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(COALESCE(t.note, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR (t.category IS NOT NULL AND LOWER(t.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              )
            ORDER BY t.timeStamp DESC
            """)
    List<Transaction> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    // Truy vấn danh sách giao dịch trong khoảng thời gian để tính toán chi tiêu/thống kê
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.timeStamp >= :startTime AND t.timeStamp <= :endTime")
    List<Transaction> findByUserIdAndTimeStampBetween(
        @Param("userId") Long userId,
        @Param("startTime") Long startTime,
        @Param("endTime") Long endTime
    );

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND LOWER(t.type) = LOWER(:type) AND t.timeStamp >= :startTime AND t.timeStamp <= :endTime")
    List<Transaction> findByUserIdAndTypeAndTimeStampBetween(
        @Param("userId") Long userId,
        @Param("type") String type,
        @Param("startTime") Long startTime,
        @Param("endTime") Long endTime
    );

    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.timeStamp >= :startTime AND t.timeStamp <= :endTime AND t.category IS NOT NULL GROUP BY t.category.name")
    List<Object[]> sumByCategory(
        @Param("userId") Long userId,
        @Param("startTime") Long startTime,
        @Param("endTime") Long endTime
    );

    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND LOWER(t.type) = LOWER(:type) AND t.timeStamp >= :startTime AND t.timeStamp <= :endTime AND t.category IS NOT NULL GROUP BY t.category.name")
    List<Object[]> sumByCategoryAndType(
        @Param("userId") Long userId,
        @Param("type") String type,
        @Param("startTime") Long startTime,
        @Param("endTime") Long endTime
    );
}
