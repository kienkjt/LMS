package com.kjt.lms.repository;

import com.kjt.lms.model.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {
    List<CartItemEntity> findByCartIdAndDeletedFalse(UUID cartId);
    Optional<CartItemEntity> findByCartIdAndCourseIdAndDeletedFalse(UUID cartId, UUID courseId);
}
