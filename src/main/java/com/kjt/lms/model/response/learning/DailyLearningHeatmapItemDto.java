package com.kjt.lms.model.response.learning;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class DailyLearningHeatmapItemDto {
    private LocalDate date;
    private long activityCount;
    private long estimatedMinutes;
    private boolean active;
}

