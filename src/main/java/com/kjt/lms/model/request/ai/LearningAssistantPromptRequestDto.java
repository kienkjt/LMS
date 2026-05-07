package com.kjt.lms.model.request.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningAssistantPromptRequestDto {

    private String studentName;

    private String courseName;

    @Min(0)
    @Max(100)
    private Integer progressPercent;

    private String currentLesson;

    private String retrievedChunks;

    private UUID courseId;

    @Builder.Default
    private Boolean includeSystemContext = Boolean.TRUE;

    @NotBlank
    private String userQuestion;
}
