package com.kjt.lms.model.request.quiz;

import com.kjt.lms.common.constants.QuizTypeEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateQuestionRequestDto {

    @NotBlank
    private String questionText;

    @NotNull
    private QuizTypeEnum type;

    private String options;

    @NotBlank
    private String correctAnswer;

    private String explanation;

    @NotNull
    @Min(1)
    private Integer points;
}
