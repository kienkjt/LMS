package com.kjt.lms.repository;

import com.kjt.lms.model.entity.LessonEntity;
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
public interface LessonRepository extends JpaRepository<LessonEntity, UUID> {

    Optional<LessonEntity> findByIdAndDeletedFalse(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT l
    FROM LessonEntity l
    WHERE l.id = :id
      AND l.deleted = false
    """)
    Optional<LessonEntity> findByIdAndDeletedFalseForUpdate(@Param("id") UUID id);

    List<LessonEntity> findByCourseIdAndDeletedFalseOrderByCreatedAtAsc(UUID courseId);

    List<LessonEntity> findByCourseIdAndChapterIdAndDeletedFalseOrderByCreatedAtAsc(UUID courseId, UUID chapterId);

    boolean existsByChapterIdAndTitleIgnoreCaseAndDeletedFalse(UUID chapterId, String title);

    boolean existsByChapterIdAndTitleIgnoreCaseAndDeletedFalseAndIdNot(UUID chapterId, String title, UUID id);

    long countByCourseIdAndDeletedFalse(UUID courseId);

    long countByChapterIdAndDeletedFalse(UUID chapterId);

    @Query("""
    SELECT COALESCE(SUM(l.duration), 0)
    FROM LessonEntity l
    WHERE l.courseId = :courseId
      AND l.deleted = false
    """)
    Integer sumDurationByCourseId(@Param("courseId") UUID courseId);

    @Query("""
    SELECT COALESCE(SUM(l.duration), 0)
    FROM LessonEntity l
    WHERE l.chapterId = :chapterId
      AND l.deleted = false
    """)
    Integer sumDurationByChapterId(@Param("chapterId") UUID chapterId);
}
