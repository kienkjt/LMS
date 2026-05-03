package com.kjt.lms.model.response.dashboard;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class InstructorDashboardResponseDto {

    private long totalCourses;
    private long publishedCourses;
    private long pendingReviewCourses;
    private long totalStudents;
    private long soldItems;
    private BigDecimal totalRevenue;
    private BigDecimal averageRevenuePerSale;
    private List<TimeSeriesPointDto> dailyRevenue;
    private List<TimeSeriesPointDto> dailyEnrollments;
    private List<StatusCountDto> courseStatusDistribution;
    private List<TopCourseDashboardDto> topSellingCourses;
}
