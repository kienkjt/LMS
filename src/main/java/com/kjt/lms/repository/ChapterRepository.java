package com.kjt.lms.repository;

import com.kjt.lms.model.entity.ChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<ChapterEntity, UUID> {

    List<ChapterEntity> findByCourseIdOrderByCreatedAtAsc(UUID courseId);
}
