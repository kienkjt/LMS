package com.kjt.lms.repository;

import com.kjt.lms.common.constants.WithdrawalStatusEnum;
import com.kjt.lms.common.constants.WithdrawalTypeEnum;
import com.kjt.lms.model.entity.WithdrawalRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequestEntity, UUID> {
    Page<WithdrawalRequestEntity> findByInstructorIdAndDeletedFalse(UUID instructorId, Pageable pageable);

    Page<WithdrawalRequestEntity> findByStatusAndDeletedFalse(WithdrawalStatusEnum status, Pageable pageable);

    Optional<WithdrawalRequestEntity> findByIdAndDeletedFalse(UUID id);

    Page<WithdrawalRequestEntity> findByDeletedFalse(Pageable pageable);

    boolean existsByOrderIdAndInstructorIdAndTypeAndDeletedFalse(UUID orderId, UUID instructorId, WithdrawalTypeEnum type);

    @Query("""
        SELECT COALESCE(SUM(w.requestedAmount), 0)
        FROM WithdrawalRequestEntity w
        WHERE w.instructorId = :instructorId
            AND w.type = :type
            AND w.status IN :statuses
            AND w.deleted = false
    """)
    BigDecimal sumRequestedAmountByInstructorIdAndTypeAndStatusInAndDeletedFalse(
            @Param("instructorId") UUID instructorId,
            @Param("type") WithdrawalTypeEnum type,
            @Param("statuses") Collection<WithdrawalStatusEnum> statuses
    );

    List<WithdrawalRequestEntity> findByTypeAndStatusAndAvailableAtLessThanEqualAndDeletedFalse(
            WithdrawalTypeEnum type,
            WithdrawalStatusEnum status,
            LocalDateTime availableAt
    );

    List<WithdrawalRequestEntity> findByInstructorIdAndOrderIdAndTypeAndStatusAndDeletedFalse(
            UUID instructorId,
            UUID orderId,
            WithdrawalTypeEnum type,
            WithdrawalStatusEnum status
    );
}

