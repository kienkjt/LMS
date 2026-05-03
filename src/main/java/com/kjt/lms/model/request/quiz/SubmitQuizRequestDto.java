package com.kjt.lms.model.request.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SubmitQuizRequestDto {

    private Integer timeSpent;

    @Valid
    @NotEmpty
    private List<AnswerSubmissionDto> answers;

    @Getter
    @Setter
    public static class AnswerSubmissionDto {

        @NotNull
        private UUID questionId;

        private String selectedAnswer;
    }
}
