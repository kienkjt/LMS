package com.kjt.lms.model.request.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
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

    private String learningGoal;

    private String targetRole;

    @Min(1)
    @Max(80)
    private Integer weeklyHours;

    private BigDecimal budgetMax;

    private String preferredCategory;

    @Builder.Default
    private Boolean includeSystemContext = Boolean.TRUE;

    private List<ChatMessageDto> chatHistory;

    @NotBlank
    private String userQuestion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageDto {
        private String role;
        private String content;
    }
}
