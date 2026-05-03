package com.kjt.lms.repository;

import com.kjt.lms.model.entity.QuizEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<QuizEntity, UUID> {

    Optional<QuizEntity> findByIdAndDeletedFalse(UUID id);

    List<QuizEntity> findByCourseIdAndDeletedFalseOrderByCreatedAtAsc(UUID courseId);

    List<QuizEntity> findByLessonIdAndDeletedFalseOrderByCreatedAtAsc(UUID lessonId);

    long countByCourseIdAndDeletedFalse(UUID courseId);
}
