package com.kjt.lms.repository;

import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.model.entity.OrderItemEntity;
import com.kjt.lms.model.projection.dashboard.TimeSeriesProjection;
import com.kjt.lms.model.response.dashboard.TopCourseDashboardDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Query("""
            SELECT COALESCE(SUM(oi.instructorRevenue), 0)
            FROM OrderItemEntity oi, OrderEntity o
            WHERE oi.orderId = o.id
              AND oi.instructorId = :instructorId
              AND o.status = :status
              AND oi.deleted = false
              AND o.deleted = false
            """)
    BigDecimal sumInstructorRevenueByStatus(
            @Param("instructorId") UUID instructorId,
            @Param("status") OrderStatusEnum status);

    @Query("""
            SELECT COUNT(oi)
            FROM OrderItemEntity oi, OrderEntity o
            WHERE oi.orderId = o.id
              AND oi.instructorId = :instructorId
              AND o.status = :status
              AND oi.deleted = false
              AND o.deleted = false
            """)
    long countSoldItemsByInstructorAndStatus(
            @Param("instructorId") UUID instructorId,
            @Param("status") OrderStatusEnum status);

    @Query("""
            SELECT new com.kjt.lms.model.response.dashboard.TopCourseDashboardDto(
                c.id,
                c.title,
                COUNT(oi),
                COALESCE(SUM(oi.paidPrice), 0),
                COALESCE(c.avgRating, 0.0),
                c.totalStudents
            )
            FROM OrderItemEntity oi, OrderEntity o, CourseEntity c
            WHERE oi.orderId = o.id
              AND oi.courseId = c.id
              AND o.status = :status
              AND oi.deleted = false
              AND o.deleted = false
              AND c.deleted = false
            GROUP BY c.id, c.title, c.avgRating, c.totalStudents
            ORDER BY COALESCE(SUM(oi.paidPrice), 0) DESC, COUNT(oi) DESC
            """)
    List<TopCourseDashboardDto> findTopSellingCourses(@Param("status") OrderStatusEnum status, Pageable pageable);

    @Query("""
            SELECT new com.kjt.lms.model.response.dashboard.TopCourseDashboardDto(
                c.id,
                c.title,
                COUNT(oi),
                COALESCE(SUM(oi.instructorRevenue), 0),
                COALESCE(c.avgRating, 0.0),
                c.totalStudents
            )
            FROM OrderItemEntity oi, OrderEntity o, CourseEntity c
            WHERE oi.orderId = o.id
              AND oi.courseId = c.id
              AND oi.instructorId = :instructorId
              AND o.status = :status
              AND oi.deleted = false
              AND o.deleted = false
              AND c.deleted = false
            GROUP BY c.id, c.title, c.avgRating, c.totalStudents
            ORDER BY COALESCE(SUM(oi.instructorRevenue), 0) DESC, COUNT(oi) DESC
            """)
    List<TopCourseDashboardDto> findTopSellingCoursesByInstructor(
            @Param("instructorId") UUID instructorId,
            @Param("status") OrderStatusEnum status,
            Pageable pageable);

    @Query(value = """
            SELECT DATE_FORMAT(o.paid_at, '%Y-%m-%d') AS label,
                   COALESCE(SUM(oi.instructor_revenue), 0) AS amount,
                   COUNT(*) AS count
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.instructor_id = :instructorId
              AND o.status = :status
              AND oi.deleted = false
              AND o.deleted = false
              AND o.paid_at >= :fromDate
            GROUP BY DATE_FORMAT(o.paid_at, '%Y-%m-%d')
            ORDER BY label
            """, nativeQuery = true)
    List<TimeSeriesProjection> findDailyInstructorRevenueTrend(
            @Param("instructorId") UUID instructorId,
            @Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate);
}
