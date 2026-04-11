package com.kjt.lms.repository;

import com.kjt.lms.model.entity.ChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<ChapterEntity, UUID> {

    Optional<ChapterEntity> findByIdAndDeletedFalse(UUID id);

    List<ChapterEntity> findByCourseIdAndDeletedFalseOrderByCreatedAtAsc(UUID courseId);

    boolean existsByCourseIdAndTitleIgnoreCaseAndDeletedFalse(UUID courseId, String title);

    boolean existsByCourseIdAndTitleIgnoreCaseAndDeletedFalseAndIdNot(UUID courseId, String title, UUID id);
}
