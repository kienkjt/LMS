package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.CourseStatusEnum;
import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.model.projection.dashboard.TimeSeriesProjection;
import com.kjt.lms.model.response.dashboard.AdminDashboardResponseDto;
import com.kjt.lms.model.response.dashboard.InstructorDashboardResponseDto;
import com.kjt.lms.model.response.dashboard.StatusCountDto;
import com.kjt.lms.model.response.dashboard.TimeSeriesPointDto;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.OrderItemRepository;
import com.kjt.lms.repository.OrderRepository;
import com.kjt.lms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl extends BaseService implements DashboardService {

    private final EnrollmentRepository enrollmentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponseDto getAdminDashboard() {
        long completedOrders = orderRepository.countByStatusAndDeletedFalse(OrderStatusEnum.COMPLETED);
        BigDecimal totalRevenue = defaultAmount(orderRepository.sumFinalAmountByStatus(OrderStatusEnum.COMPLETED));
        LocalDateTime last30Days = LocalDateTime.now().minusDays(29).toLocalDate().atStartOfDay();
        LocalDateTime last12Months = LocalDateTime.now().minusMonths(11).withDayOfMonth(1).toLocalDate().atStartOfDay();

        return AdminDashboardResponseDto.builder()
                .totalUsers(userRepository.countByDeletedFalse())
                .totalStudents(userRepository.countByRoleCode("STUDENT"))
                .totalInstructors(userRepository.countByRoleCode("INSTRUCTOR"))
                .totalCourses(courseRepository.countByDeletedFalse())
                .publishedCourses(courseRepository.countByStatusAndDeletedFalse(CourseStatusEnum.PUBLISHED))
                .pendingReviewCourses(courseRepository.countByStatusAndDeletedFalse(CourseStatusEnum.PENDING_REVIEW))
                .totalEnrollments(enrollmentRepository.countByDeletedFalse())
                .totalOrders(orderRepository.countByDeletedFalse())
                .completedOrders(completedOrders)
                .totalRevenue(totalRevenue)
                .averageOrderValue(average(totalRevenue, completedOrders))
                .dailyRevenue(toTimeSeries(orderRepository.findDailyRevenueTrend(OrderStatusEnum.COMPLETED.getValue(), last30Days)))
                .monthlyRevenue(toTimeSeries(orderRepository.findMonthlyRevenueTrend(OrderStatusEnum.COMPLETED.getValue(), last12Months)))
                .dailyEnrollments(toTimeSeries(enrollmentRepository.findDailyEnrollmentTrend(last30Days)))
                .courseStatusDistribution(adminCourseStatusDistribution())
                .orderStatusDistribution(orderStatusDistribution())
                .topSellingCourses(orderItemRepository.findTopSellingCourses(OrderStatusEnum.COMPLETED, PageRequest.of(0, 10)))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorDashboardResponseDto getInstructorDashboard() {
        UUID instructorId = securityUtils.getCurrentUserId();
        long soldItems = orderItemRepository.countSoldItemsByInstructorAndStatus(instructorId, OrderStatusEnum.COMPLETED);
        BigDecimal totalRevenue = defaultAmount(orderItemRepository.sumInstructorRevenueByStatus(instructorId, OrderStatusEnum.COMPLETED));
        LocalDateTime last30Days = LocalDateTime.now().minusDays(29).toLocalDate().atStartOfDay();

        return InstructorDashboardResponseDto.builder()
                .totalCourses(courseRepository.countByInstructorIdAndDeletedFalse(instructorId))
                .publishedCourses(courseRepository.countByInstructorIdAndStatusAndDeletedFalse(instructorId, CourseStatusEnum.PUBLISHED))
                .pendingReviewCourses(courseRepository.countByInstructorIdAndStatusAndDeletedFalse(instructorId, CourseStatusEnum.PENDING_REVIEW))
                .totalStudents(enrollmentRepository.countByInstructorId(instructorId))
                .soldItems(soldItems)
                .totalRevenue(totalRevenue)
                .averageRevenuePerSale(average(totalRevenue, soldItems))
                .dailyRevenue(toTimeSeries(orderItemRepository.findDailyInstructorRevenueTrend(
                        instructorId,
                        OrderStatusEnum.COMPLETED.getValue(),
                        last30Days
                )))
                .dailyEnrollments(toTimeSeries(enrollmentRepository.findDailyEnrollmentTrendByInstructor(instructorId, last30Days)))
                .courseStatusDistribution(instructorCourseStatusDistribution(instructorId))
                .topSellingCourses(orderItemRepository.findTopSellingCoursesByInstructor(
                        instructorId,
                        OrderStatusEnum.COMPLETED,
                        PageRequest.of(0, 10)
                ))
                .build();
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal average(BigDecimal amount, long divisor) {
        if (divisor <= 0) {
            return BigDecimal.ZERO;
        }
        return defaultAmount(amount).divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    }

    private List<TimeSeriesPointDto> toTimeSeries(List<TimeSeriesProjection> projections) {
        return projections.stream()
                .map(item -> TimeSeriesPointDto.builder()
                        .label(item.getLabel())
                        .amount(defaultAmount(item.getAmount()))
                        .count(item.getCount() == null ? 0L : item.getCount())
                        .build())
                .toList();
    }

    private List<StatusCountDto> adminCourseStatusDistribution() {
        return Arrays.stream(CourseStatusEnum.values())
                .map(status -> StatusCountDto.builder()
                        .status(status.name())
                        .description(status.getDescription())
                        .count(courseRepository.countByStatusAndDeletedFalse(status))
                        .build())
                .toList();
    }

    private List<StatusCountDto> instructorCourseStatusDistribution(UUID instructorId) {
        return Arrays.stream(CourseStatusEnum.values())
                .map(status -> StatusCountDto.builder()
                        .status(status.name())
                        .description(status.getDescription())
                        .count(courseRepository.countByInstructorIdAndStatusAndDeletedFalse(instructorId, status))
                        .build())
                .toList();
    }

    private List<StatusCountDto> orderStatusDistribution() {
        return Arrays.stream(OrderStatusEnum.values())
                .map(status -> StatusCountDto.builder()
                        .status(status.name())
                        .description(status.getDescription())
                        .count(orderRepository.countByStatusAndDeletedFalse(status))
                        .build())
                .toList();
    }
}
