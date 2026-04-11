package com.kjt.lms.repository;

import com.kjt.lms.model.entity.LessonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<LessonEntity, UUID> {

    List<LessonEntity> findByCourseIdOrderByCreatedAtAsc(UUID courseId);

    List<LessonEntity> findByCourseIdAndChapterIdOrderByCreatedAtAsc(UUID courseId, UUID chapterId);

    long countByCourseId(UUID courseId);

    long countByChapterId(UUID chapterId);

    @Query("SELECT COALESCE(SUM(l.duration), 0) FROM LessonEntity l WHERE l.courseId = :courseId")
    Integer sumDurationByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT COALESCE(SUM(l.duration), 0) FROM LessonEntity l WHERE l.chapterId = :chapterId")
    Integer sumDurationByChapterId(@Param("chapterId") UUID chapterId);
}

