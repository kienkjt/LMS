package com.kjt.lms.repository;

import com.kjt.lms.model.entity.LessonProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgressEntity, UUID> {

    Optional<LessonProgressEntity> findByStudentIdAndLessonIdAndDeletedFalse(UUID studentId, UUID lessonId);

    long countByStudentIdAndCourseIdAndCompletedTrueAndDeletedFalse(UUID studentId, UUID courseId);
}

