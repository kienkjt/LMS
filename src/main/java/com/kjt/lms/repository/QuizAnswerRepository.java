package com.kjt.lms.repository;

import com.kjt.lms.model.entity.QuizAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswerEntity, UUID> {

    List<QuizAnswerEntity> findByAttemptIdAndDeletedFalse(UUID attemptId);

    List<QuizAnswerEntity> findByAttemptIdInAndDeletedFalse(List<UUID> attemptIds);
}
