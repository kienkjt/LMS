package com.kjt.lms.model.response.enrollment;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EnrolledCourseResponseDto {
    private UUID enrollmentId;
    private UUID courseId;
    private String courseTitle;
    private String courseThumbnail;
    private String instructorName;
    private BigDecimal progressPercent;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
}
