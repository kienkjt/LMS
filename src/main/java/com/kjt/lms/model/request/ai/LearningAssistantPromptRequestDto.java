package com.kjt.lms.model.request.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningAssistantPromptRequestDto {

    @NotBlank
    private String studentName;

    @NotBlank
    private String courseName;

    @Min(0)
    @Max(100)
    private Integer progressPercent;

    @NotBlank
    private String currentLesson;

    @NotBlank
    private String retrievedChunks;

    @NotBlank
    private String userQuestion;
}
