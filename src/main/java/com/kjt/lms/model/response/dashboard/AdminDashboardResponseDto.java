package com.kjt.lms.model.response.dashboard;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class AdminDashboardResponseDto {

    private long totalUsers;
    private long totalStudents;
    private long totalInstructors;
    private long totalCourses;
    private long publishedCourses;
    private long pendingReviewCourses;
    private long totalEnrollments;
    private long totalOrders;
    private long completedOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private List<TimeSeriesPointDto> dailyRevenue;
    private List<TimeSeriesPointDto> monthlyRevenue;
    private List<TimeSeriesPointDto> dailyEnrollments;
    private List<StatusCountDto> courseStatusDistribution;
    private List<StatusCountDto> orderStatusDistribution;
    private List<TopCourseDashboardDto> topSellingCourses;
}
