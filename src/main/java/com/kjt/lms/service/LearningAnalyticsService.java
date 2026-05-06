package com.kjt.lms.service;

import com.kjt.lms.model.response.learning.DailyLearningHeatmapItemDto;
import com.kjt.lms.model.response.learning.InstructorStudentEngagementDto;
import com.kjt.lms.model.response.learning.LearningStreakResponseDto;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LearningAnalyticsService {
    LearningStreakResponseDto getMyLearningStreak();
    List<DailyLearningHeatmapItemDto> getMyLearningHeatmap(LocalDate fromDate, LocalDate toDate);
    Page<InstructorStudentEngagementDto> getInstructorCourseStudentEngagement(UUID courseId, int page, int pageSize);
    void sendStreakRiskWarnings();
}

