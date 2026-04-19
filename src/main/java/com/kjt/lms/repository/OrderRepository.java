package com.kjt.lms.repository;

import com.kjt.lms.model.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByIdAndDeletedFalse(UUID orderId);

    Page<OrderEntity> findByStudentIdOrderByCreatedAtDesc(UUID studentId, Pageable pageable);
}
