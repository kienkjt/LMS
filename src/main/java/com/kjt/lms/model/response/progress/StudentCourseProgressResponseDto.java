package com.kjt.lms.model.response.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseProgressResponseDto {

    private UUID enrollmentId;
    private UUID courseId;
    private String courseTitle;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private String studentAvatar;
    private long totalLessons;
    private long completedLessons;
    private BigDecimal progressPercent;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
    private List<LessonProgressDetailResponseDto> lessons;
}
