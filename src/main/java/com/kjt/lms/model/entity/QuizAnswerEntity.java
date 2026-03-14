package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "quiz_answers")
public class QuizAnswerEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID attemptId;

    @Column(nullable = false)
    private UUID questionId;

    @Column(columnDefinition = "TEXT")
    private String selectedAnswer;

    @Column(nullable = false)
    private Boolean isCorrect = false;

    @Column(nullable = false)
    private Integer earnedPoints;
}
