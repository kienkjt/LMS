package com.kjt.lms.controller;

import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.response.learning.DailyLearningHeatmapItemDto;
import com.kjt.lms.model.response.learning.InstructorStudentEngagementDto;
import com.kjt.lms.model.response.learning.LearningStreakResponseDto;
import com.kjt.lms.service.LearningAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/learning-analytics")
@RequiredArgsConstructor
@Tag(name = "Learning Analytics", description = "Learning streak and engagement analytics")
public class LearningAnalyticsController {

    private final LearningAnalyticsService learningAnalyticsService;

    @GetMapping("/me/streak")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get current user's daily learning streak", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<LearningStreakResponseDto>> getMyStreak() {
        return ResponseEntity.ok(APIResponse.success(learningAnalyticsService.getMyLearningStreak(), null));
    }

    @GetMapping("/me/heatmap")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get current user's learning heatmap", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<List<DailyLearningHeatmapItemDto>>> getMyHeatmap(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(APIResponse.success(learningAnalyticsService.getMyLearningHeatmap(fromDate, toDate), null));
    }

    @GetMapping("/instructor/courses/{courseId}/students")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get student engagement analytics for instructor course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Page<InstructorStudentEngagementDto>>> getInstructorCourseStudentEngagement(
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(APIResponse.success(
                learningAnalyticsService.getInstructorCourseStudentEngagement(courseId, page, pageSize),
                null
        ));
    }
}

