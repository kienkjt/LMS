package com.kjt.lms.repository;

import com.kjt.lms.common.constants.WithdrawalStatusEnum;
import com.kjt.lms.model.entity.WithdrawalRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequestEntity, UUID> {
    Page<WithdrawalRequestEntity> findByInstructorIdAndDeletedFalse(UUID instructorId, Pageable pageable);

    Page<WithdrawalRequestEntity> findByStatusAndDeletedFalse(WithdrawalStatusEnum status, Pageable pageable);

    Optional<WithdrawalRequestEntity> findByIdAndDeletedFalse(UUID id);

    Page<WithdrawalRequestEntity> findByDeletedFalse(Pageable pageable);
}

