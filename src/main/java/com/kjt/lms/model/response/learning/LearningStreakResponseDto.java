package com.kjt.lms.model.response.learning;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class LearningStreakResponseDto {
    private long currentStreak;
    private long longestStreak;
    private long totalActiveDays;
    private LocalDate lastLearningDate;
    private boolean learnedToday;
    private boolean atRisk;
    private String warningMessage;
}

