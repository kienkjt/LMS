package com.kjt.lms.model.request.quiz;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateQuizRequestDto {

    @NotBlank
    @Size(max = 200)
    private String title;

    private String description;

    private Integer timeLimitMinutes;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal passScore;

    private Integer maxAttempts;

    private Boolean shuffleQuestions;
}
