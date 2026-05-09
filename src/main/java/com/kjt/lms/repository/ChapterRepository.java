package com.kjt.lms.repository;

import com.kjt.lms.model.entity.ChapterEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<ChapterEntity, UUID> {

    Optional<ChapterEntity> findByIdAndDeletedFalse(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM ChapterEntity c
            WHERE c.id = :id
              AND c.deleted = false
            """)
    Optional<ChapterEntity> findByIdAndDeletedFalseForUpdate(@Param("id") UUID id);

    List<ChapterEntity> findByCourseIdAndDeletedFalseOrderByCreatedAtAsc(UUID courseId);

    boolean existsByCourseIdAndTitleIgnoreCaseAndDeletedFalse(UUID courseId, String title);

    boolean existsByCourseIdAndTitleIgnoreCaseAndDeletedFalseAndIdNot(UUID courseId, String title, UUID id);
}
