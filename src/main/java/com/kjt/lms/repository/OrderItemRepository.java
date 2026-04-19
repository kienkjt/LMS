package com.kjt.lms.repository;

import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.model.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, UUID> {
    List<OrderItemEntity> findByOrderId(UUID orderId);

    @Query("""
            SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END
            FROM OrderItemEntity oi, OrderEntity o
            WHERE oi.orderId = o.id
              AND oi.courseId = :courseId
              AND o.studentId = :studentId
              AND o.status = :status
              AND oi.deleted = false
              AND o.deleted = false
            """)
    boolean existsPaidCourseForStudent(
            @Param("studentId") UUID studentId,
            @Param("courseId") UUID courseId,
            @Param("status") OrderStatusEnum status);

    default boolean existsPaidCourseForStudent(UUID studentId, UUID courseId) {
        return existsPaidCourseForStudent(studentId, courseId, OrderStatusEnum.COMPLETED);
    }
}
