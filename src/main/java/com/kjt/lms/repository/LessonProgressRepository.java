package com.kjt.lms.repository;

import com.kjt.lms.model.entity.LessonProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgressEntity, UUID> {

    Optional<LessonProgressEntity> findByStudentIdAndLessonIdAndDeletedFalse(UUID studentId, UUID lessonId);

    List<LessonProgressEntity> findByStudentIdAndCourseIdAndDeletedFalse(UUID studentId, UUID courseId);

    long countByStudentIdAndCourseIdAndCompletedTrueAndDeletedFalse(UUID studentId, UUID courseId);

    @Query("""
            SELECT lp.lessonId
            FROM LessonProgressEntity lp
            WHERE lp.deleted = false
              AND lp.studentId = :studentId
              AND lp.courseId = :courseId
              AND lp.completed = true
            """)
    List<UUID> findCompletedLessonIdsByStudentIdAndCourseId(
            @Param("studentId") UUID studentId,
            @Param("courseId") UUID courseId
    );

    @Query("""
            SELECT FUNCTION('DATE', lp.completedAt) AS learningDate,
                   COUNT(lp) AS activityCount,
                   COALESCE(SUM(COALESCE(l.duration, 0)), 0) AS estimatedMinutes
            FROM LessonProgressEntity lp
            JOIN LessonEntity l ON l.id = lp.lessonId
            WHERE lp.deleted = false
              AND lp.studentId = :studentId
              AND lp.completed = true
              AND lp.completedAt IS NOT NULL
            GROUP BY FUNCTION('DATE', lp.completedAt)
            """)
    List<Object[]> summarizeDailyLearningByStudent(@Param("studentId") UUID studentId);
}

