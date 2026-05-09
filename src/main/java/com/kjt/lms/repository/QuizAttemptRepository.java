package com.kjt.lms.repository;

import com.kjt.lms.model.entity.QuizAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttemptEntity, UUID> {

    Optional<QuizAttemptEntity> findByIdAndDeletedFalse(UUID id);

    List<QuizAttemptEntity> findByStudentIdAndQuizIdAndDeletedFalseOrderByAttemptNumberDesc(UUID studentId, UUID quizId);

    List<QuizAttemptEntity> findByQuizIdAndDeletedFalse(UUID quizId);

    long countByStudentIdAndQuizIdAndDeletedFalse(UUID studentId, UUID quizId);

    boolean existsByStudentIdAndQuizIdAndPassedTrueAndDeletedFalse(UUID studentId, UUID quizId);

    @Query("""
            SELECT COUNT(q)
            FROM QuizEntity q
            WHERE q.courseId = :courseId
              AND q.deleted = false
              AND NOT EXISTS (
                  SELECT a.id
                  FROM QuizAttemptEntity a
                  WHERE a.quizId = q.id
                    AND a.studentId = :studentId
                    AND a.passed = true
                    AND a.deleted = false
              )
            """)
    long countUnpassedCourseQuizzes(@Param("studentId") UUID studentId, @Param("courseId") UUID courseId);

    @Query("""
            SELECT FUNCTION('DATE', qa.submittedAt) AS learningDate,
                   COUNT(qa) AS activityCount,
                   COALESCE(SUM(COALESCE(qa.timeSpent, 0)), 0) AS estimatedMinutes
            FROM QuizAttemptEntity qa
            WHERE qa.deleted = false
              AND qa.studentId = :studentId
              AND qa.submittedAt IS NOT NULL
            GROUP BY FUNCTION('DATE', qa.submittedAt)
            """)
    List<Object[]> summarizeDailyLearningByStudent(@Param("studentId") UUID studentId);
}
