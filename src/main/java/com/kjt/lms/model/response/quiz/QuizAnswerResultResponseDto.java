package com.kjt.lms.model.response.quiz;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class QuizAnswerResultResponseDto {

    private UUID questionId;
    private String selectedAnswer;
    private Boolean correct;
    private Integer earnedPoints;
    private Integer points;
    private String correctAnswer;
    private String explanation;
}
