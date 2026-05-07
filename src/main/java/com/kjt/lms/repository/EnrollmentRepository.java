package com.kjt.lms.repository;

import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.projection.dashboard.TimeSeriesProjection;
import com.kjt.lms.model.response.enrollment.EnrolledCourseResponseDto;
import com.kjt.lms.model.response.enrollment.InstructorStudentEnrollmentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, UUID> {
    boolean existsByStudentIdAndCourseIdAndDeletedFalse(UUID studentId, UUID courseId);
    Optional<EnrollmentEntity> findByStudentIdAndCourseIdAndDeletedFalse(UUID studentId, UUID courseId);
    Page<EnrollmentEntity> findByCourseIdAndDeletedFalseOrderByCreatedAtDesc(UUID courseId, Pageable pageable);
    List<EnrollmentEntity> findByStudentIdAndOrderIdAndDeletedFalse(UUID studentId, UUID orderId);
    List<EnrollmentEntity> findByStudentIdAndDeletedFalse(UUID studentId);

    long countByDeletedFalse();

    long countByCourseIdAndDeletedFalse(UUID courseId);

    @Query("""
            SELECT COUNT(e)
            FROM EnrollmentEntity e, CourseEntity c
            WHERE e.courseId = c.id
              AND c.instructorId = :instructorId
              AND e.deleted = false
              AND c.deleted = false
            """)
    long countByInstructorId(@Param("instructorId") UUID instructorId);

    @Query(value = """
            SELECT DATE_FORMAT(e.created_at, '%Y-%m-%d') AS label,
                   0 AS amount,
                   COUNT(*) AS count
            FROM enrollments e
            WHERE e.deleted = false
              AND e.created_at >= :fromDate
            GROUP BY DATE_FORMAT(e.created_at, '%Y-%m-%d')
            ORDER BY label
            """, nativeQuery = true)
    List<TimeSeriesProjection> findDailyEnrollmentTrend(@Param("fromDate") LocalDateTime fromDate);

    @Query(value = """
            SELECT DATE_FORMAT(e.created_at, '%Y-%m-%d') AS label,
                   0 AS amount,
                   COUNT(*) AS count
            FROM enrollments e
            JOIN courses c ON c.id = e.course_id
            WHERE e.deleted = false
              AND c.deleted = false
              AND c.instructor_id = CAST(:instructorId AS CHAR(36))
              AND e.created_at >= :fromDate
            GROUP BY DATE_FORMAT(e.created_at, '%Y-%m-%d')
            ORDER BY label
            """, nativeQuery = true)
    List<TimeSeriesProjection> findDailyEnrollmentTrendByInstructor(
            @Param("instructorId") String instructorId,
            @Param("fromDate") LocalDateTime fromDate);

    @Query("""
    SELECT new com.kjt.lms.model.response.enrollment.EnrolledCourseResponseDto(
        e.id,
        c.id,
        c.title,
        c.thumbnail,
        u.fullName,
        e.progressPercent,
        e.createdAt,
        e.completedAt
    )
    FROM EnrollmentEntity e
    JOIN CourseEntity c ON c.id = e.courseId
    JOIN UserEntity u ON u.id = c.instructorId
    WHERE e.studentId = :studentId
      AND e.deleted = false
    ORDER BY e.createdAt DESC
    """)
    List<EnrolledCourseResponseDto> findEnrolledCoursesByStudentId(@Param("studentId") UUID studentId);

    @Query("""
    SELECT new com.kjt.lms.model.response.enrollment.InstructorStudentEnrollmentResponseDto(
        e.id,
        e.courseId,
        s.id,
        s.fullName,
        s.email,
        s.avatar,
        s.phoneNumber,
        e.progressPercent,
        e.createdAt,
        e.completedAt
    )
    FROM EnrollmentEntity e
    JOIN CourseEntity c ON c.id = e.courseId
    JOIN UserEntity s ON s.id = e.studentId
    WHERE e.courseId = :courseId
      AND e.deleted = false
      AND c.deleted = false
    ORDER BY e.createdAt DESC
    """)
    Page<InstructorStudentEnrollmentResponseDto> findStudentsByCourseId(
            @Param("courseId") UUID courseId,
            Pageable pageable
    );

    @Query("""
    SELECT DISTINCT e.studentId
    FROM EnrollmentEntity e
    WHERE e.deleted = false
    """)
    List<UUID> findDistinctActiveStudentIds();
}
