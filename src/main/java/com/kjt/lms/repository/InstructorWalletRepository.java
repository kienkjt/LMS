package com.kjt.lms.repository;

import com.kjt.lms.model.entity.InstructorWalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstructorWalletRepository extends JpaRepository<InstructorWalletEntity, UUID> {
    Optional<InstructorWalletEntity> findByInstructorIdAndDeletedFalse(UUID instructorId);
}

