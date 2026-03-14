package com.kjt.lms.repository;

import com.kjt.lms.model.entity.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<OtpEntity, UUID> {
    Optional<OtpEntity> findByEmailAndPurposeAndIsUsedFalse(String email, String purpose);

    @Modifying
    @Query("DELETE FROM OtpEntity o WHERE o.expiredAt < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);

    long countByEmailAndPurposeAndCreatedAtAfter(String email, String purpose, LocalDateTime createdAt);
}

