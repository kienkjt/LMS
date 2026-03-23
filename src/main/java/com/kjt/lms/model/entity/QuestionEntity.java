package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import com.kjt.lms.common.constants.QuizTypeEnum;
import com.kjt.lms.common.constants.QuizTypeEnumConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "questions")
public class QuestionEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID quizId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(nullable = false)
    @Convert(converter = QuizTypeEnumConverter.class)
    private QuizTypeEnum type;

    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false)
    private Integer points;
}
