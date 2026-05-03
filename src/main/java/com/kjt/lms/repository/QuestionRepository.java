package com.kjt.lms.repository;

import com.kjt.lms.model.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<QuestionEntity, UUID> {

    Optional<QuestionEntity> findByIdAndDeletedFalse(UUID id);

    List<QuestionEntity> findByQuizIdAndDeletedFalseOrderByCreatedAtAsc(UUID quizId);

    long countByQuizIdAndDeletedFalse(UUID quizId);
}
