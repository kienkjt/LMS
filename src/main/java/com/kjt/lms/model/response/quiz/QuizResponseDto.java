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
public class QuizResponseDto {

    private UUID id;
    private UUID courseId;
    private UUID lessonId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private BigDecimal passScore;
    private Integer maxAttempts;
    private Boolean shuffleQuestions;
    private Long totalQuestions;
    private LocalDateTime createdAt;
    private List<QuestionResponseDto> questions;
}
