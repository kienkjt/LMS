package com.kjt.lms.model.response.dashboard;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class AdminReportResponseDto {
    private int periodDays;
    private LocalDate fromDate;
    private LocalDate toDate;
    private long newStudents;
    private long newInstructors;
    private long newCourses;
    private long newEnrollments;
    private long completedOrders;
    private BigDecimal revenue;
    private BigDecimal averageOrderValue;
    private List<TimeSeriesPointDto> dailyRevenue;
    private List<TimeSeriesPointDto> dailyEnrollments;
    private List<TopCourseDashboardDto> topSellingCourses;
}
