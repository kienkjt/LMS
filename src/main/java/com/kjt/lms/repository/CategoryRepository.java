package com.kjt.lms.repository;

import com.kjt.lms.model.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    Optional<CategoryEntity> findByIdAndDeletedFalse(UUID id);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, UUID id);

    Page<CategoryEntity> findByDeletedFalse(Pageable pageable);

    @Query("""
    SELECT c
    FROM CategoryEntity c
    WHERE c.deleted = false
      AND (
            LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
    ORDER BY COALESCE(c.updatedAt, c.createdAt) DESC, c.createdAt DESC
    """)
    Page<CategoryEntity> search(@Param("keyword") String keyword, Pageable pageable);
}

