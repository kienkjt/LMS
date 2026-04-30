package com.kjt.lms.repository;

import com.kjt.lms.model.entity.NoteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<NoteEntity, UUID> {

    Page<NoteEntity> findByStudentIdAndCourseIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID studentId,
            UUID courseId,
            Pageable pageable);

    Page<NoteEntity> findByStudentIdAndCourseIdAndLessonIdAndDeletedFalseOrderByVideoTimestampAscCreatedAtAsc(
            UUID studentId,
            UUID courseId,
            UUID lessonId,
            Pageable pageable);

    Optional<NoteEntity> findByIdAndStudentIdAndDeletedFalse(UUID id, UUID studentId);
}
