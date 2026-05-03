package com.kjt.lms.repository;

import com.kjt.lms.model.entity.OrderEntity;
import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.model.projection.dashboard.TimeSeriesProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByIdAndDeletedFalse(UUID orderId);

    Optional<OrderEntity> findByTransactionIdAndDeletedFalse(String transactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderEntity> findWithLockByTransactionIdAndDeletedFalse(String transactionId);

    Page<OrderEntity> findByStudentIdOrderByCreatedAtDesc(UUID studentId, Pageable pageable);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(OrderStatusEnum status);

    @Query("""
            SELECT COALESCE(SUM(o.finalAmount), 0)
            FROM OrderEntity o
            WHERE o.status = :status
              AND o.deleted = false
            """)
    BigDecimal sumFinalAmountByStatus(@Param("status") OrderStatusEnum status);

    @Query(value = """
            SELECT DATE_FORMAT(o.paid_at, '%Y-%m-%d') AS label,
                   COALESCE(SUM(o.final_amount), 0) AS amount,
                   COUNT(*) AS count
            FROM orders o
            WHERE o.status = :status
              AND o.deleted = false
              AND o.paid_at >= :fromDate
            GROUP BY DATE_FORMAT(o.paid_at, '%Y-%m-%d')
            ORDER BY label
            """, nativeQuery = true)
    List<TimeSeriesProjection> findDailyRevenueTrend(
            @Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate);

    @Query(value = """
            SELECT DATE_FORMAT(o.paid_at, '%Y-%m') AS label,
                   COALESCE(SUM(o.final_amount), 0) AS amount,
                   COUNT(*) AS count
            FROM orders o
            WHERE o.status = :status
              AND o.deleted = false
              AND o.paid_at >= :fromDate
            GROUP BY DATE_FORMAT(o.paid_at, '%Y-%m')
            ORDER BY label
            """, nativeQuery = true)
    List<TimeSeriesProjection> findMonthlyRevenueTrend(
            @Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate);
}
