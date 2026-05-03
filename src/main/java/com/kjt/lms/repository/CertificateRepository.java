package com.kjt.lms.repository;

import com.kjt.lms.model.entity.CertificateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<CertificateEntity, UUID> {

    Optional<CertificateEntity> findByEnrollmentIdAndDeletedFalse(UUID enrollmentId);

    Optional<CertificateEntity> findByUserIdAndCourseIdAndDeletedFalse(UUID userId, UUID courseId);

    Optional<CertificateEntity> findByIdAndDeletedFalse(UUID id);

    List<CertificateEntity> findByUserIdAndDeletedFalseOrderByIssuedAtDesc(UUID userId);
}
