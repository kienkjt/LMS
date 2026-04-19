package com.kjt.lms.repository;

import com.kjt.lms.model.entity.EnrollmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, UUID> {
    boolean existsByStudentIdAndCourseIdAndDeletedFalse(UUID studentId, UUID courseId);
    Page<EnrollmentEntity> findByStudentIdAndDeletedFalseOrderByCreatedAtDesc(UUID studentId, Pageable pageable);
    Optional<EnrollmentEntity> findByStudentIdAndCourseIdAndDeletedFalse(UUID studentId, UUID courseId);
    List<EnrollmentEntity> findByStudentIdAndOrderIdAndDeletedFalse(UUID studentId, UUID orderId);
    List<EnrollmentEntity> findByStudentIdAndDeletedFalse(UUID studentId);

}
