package com.kjt.lms.model.response.ai;

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
public class LearningAssistantPromptResponseDto {
    private String intent; // mục đích của người dùng
    private String prompt;
    private String answer; // câu trả lời từ AI
    private List<String> followUpQuestions; // các câu hỏi tiếp theo để đào sâu hơn
    private List<RecommendedCourseDto> recommendedCourses; // các khóa học được đề xuất dựa trên mục đích của người dùng
    private List<RoadmapStepDto> roadmap; // lộ trình học tập được đề xuất để đạt được mục tiêu của người dùng

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedCourseDto {
        private UUID courseId;
        private String title;
        private String level;
        private String category;
        private BigDecimal price;
        private BigDecimal discountPrice;
        private Double rating;
        private Integer totalStudents;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoadmapStepDto {
        private Integer stepNo;
        private String title;
        private String objective;
        private String estimatedDuration;
        private UUID courseId;
        private String courseTitle;
    }
}
