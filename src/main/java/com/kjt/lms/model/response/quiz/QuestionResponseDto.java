package com.kjt.lms.model.response.quiz;

import com.kjt.lms.common.constants.QuizTypeEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class QuestionResponseDto {

    private UUID id;
    private UUID quizId;
    private String questionText;
    private QuizTypeEnum type;
    private String options;
    private String correctAnswer;
    private String explanation;
    private Integer points;
}
