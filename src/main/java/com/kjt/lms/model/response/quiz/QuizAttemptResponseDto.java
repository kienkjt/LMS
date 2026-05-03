package com.kjt.lms.model.response.quiz;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class QuizAttemptResponseDto {

    private UUID id;
    private UUID studentId;
    private UUID quizId;
    private Integer attemptNumber;
    private BigDecimal score;
    private Integer totalPoints;
    private Integer earnedPoints;
    private Boolean passed;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer timeSpent;
    private List<QuizAnswerResultResponseDto> answers;
}
