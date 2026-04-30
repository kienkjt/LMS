package com.kjt.lms.repository;

import com.kjt.lms.model.entity.WishlistEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistEntity, UUID> {

    Page<WishlistEntity> findByStudentIdAndDeletedFalseOrderByCreatedAtDesc(UUID studentId, Pageable pageable);

    Optional<WishlistEntity> findByStudentIdAndCourseId(UUID studentId, UUID courseId);

    Optional<WishlistEntity> findByStudentIdAndCourseIdAndDeletedFalse(UUID studentId, UUID courseId);

    Optional<WishlistEntity> findByIdAndStudentIdAndDeletedFalse(UUID id, UUID studentId);

    boolean existsByStudentIdAndCourseIdAndDeletedFalse(UUID studentId, UUID courseId);
}
