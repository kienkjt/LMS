package com.kjt.lms.repository;

import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.response.enrollment.EnrolledCourseResponseDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, UUID> {
    boolean existsByStudentIdAndCourseIdAndDeletedFalse(UUID studentId, UUID courseId);
    Optional<EnrollmentEntity> findByStudentIdAndCourseIdAndDeletedFalse(UUID studentId, UUID courseId);
    List<EnrollmentEntity> findByStudentIdAndOrderIdAndDeletedFalse(UUID studentId, UUID orderId);
    List<EnrollmentEntity> findByStudentIdAndDeletedFalse(UUID studentId);

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
}
