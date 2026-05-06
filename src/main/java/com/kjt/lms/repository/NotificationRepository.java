package com.kjt.lms.repository;

import com.kjt.lms.model.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<NotificationEntity> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    long countByUserIdAndReadFalseAndDeletedFalse(UUID userId);

    @Modifying
    @Query("""
            UPDATE NotificationEntity n
            SET n.read = true,
                n.readAt = :readAt
            WHERE n.userId = :userId
              AND n.read = false
              AND n.deleted = false
            """)
    int markAllAsRead(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);

    boolean existsByUserIdAndTypeAndReferenceTypeAndCreatedAtBetween(
            UUID userId,
            com.kjt.lms.common.constants.NotificationTypeEnum type,
            String referenceType,
            LocalDateTime start,
            LocalDateTime end
    );
}
