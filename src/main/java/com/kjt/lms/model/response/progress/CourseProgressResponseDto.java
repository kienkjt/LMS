package com.kjt.lms.model.response.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressResponseDto {

    private UUID courseId;
    private long totalLessons;
    private long completedLessons;
    private BigDecimal progressPercent;
}

