package com.kjt.lms.repository;

import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.model.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {

    Page<ReviewEntity> findByCourseIdAndActiveAndDeletedFalseOrderByCreatedAtDesc(
            UUID courseId,
            CommonStatusEnum active,
            Pageable pageable);

    Optional<ReviewEntity> findByStudentIdAndCourseId(UUID studentId, UUID courseId);

    Optional<ReviewEntity> findByStudentIdAndCourseIdAndDeletedFalse(UUID studentId, UUID courseId);

    Optional<ReviewEntity> findByIdAndCourseIdAndDeletedFalse(UUID id, UUID courseId);

    Optional<ReviewEntity> findByIdAndDeletedFalse(UUID id);

    Page<ReviewEntity> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0)
            FROM ReviewEntity r
            WHERE r.courseId = :courseId
              AND r.active = :active
              AND r.deleted = false
            """)
    Double calculateAverageRating(@Param("courseId") UUID courseId, @Param("active") CommonStatusEnum active);

    long countByCourseIdAndActiveAndDeletedFalse(UUID courseId, CommonStatusEnum active);
}
