package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.model.projection.dashboard.TimeSeriesProjection;
import com.kjt.lms.model.response.dashboard.AdminReportResponseDto;
import com.kjt.lms.model.response.dashboard.InstructorReportResponseDto;
import com.kjt.lms.model.response.dashboard.TimeSeriesPointDto;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.OrderItemRepository;
import com.kjt.lms.repository.OrderRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl extends BaseService implements ReportService {
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminReportResponseDto getAdminReport(Integer year, Integer month, Integer days) {
        DateRange range = resolveRange(year, month, days);
        int periodDays = range.periodDays();
        LocalDateTime fromDate = range.fromDateTime();
        LocalDateTime toDate = range.toDateTime();

        long completedOrders = orderRepository.countByStatusAndPaidAtBetween(
                OrderStatusEnum.COMPLETED, fromDate, toDate);
        BigDecimal revenue = defaultAmount(orderRepository.sumFinalAmountByStatusAndPaidAtBetween(
                OrderStatusEnum.COMPLETED, fromDate, toDate));

        return AdminReportResponseDto.builder()
                .periodDays(periodDays)
                .fromDate(fromDate.toLocalDate())
                .toDate(toDate.toLocalDate())
                .newStudents(userRepository.countByRoleCodeAndCreatedAtBetween("STUDENT", fromDate, toDate))
                .newInstructors(userRepository.countByRoleCodeAndCreatedAtBetween("INSTRUCTOR", fromDate, toDate))
                .newCourses(courseRepository.countByDeletedFalseAndCreatedAtBetween(fromDate, toDate))
                .newEnrollments(enrollmentRepository.countByDeletedFalseAndCreatedAtBetween(fromDate, toDate))
                .completedOrders(completedOrders)
                .revenue(revenue)
                .averageOrderValue(average(revenue, completedOrders))
                .dailyRevenue(toTimeSeries(orderRepository.findDailyRevenueTrend(
                        OrderStatusEnum.COMPLETED.getValue(),
                        fromDate
                )))
                .dailyEnrollments(toTimeSeries(enrollmentRepository.findDailyEnrollmentTrend(fromDate)))
                .topSellingCourses(orderItemRepository.findTopSellingCoursesByPaidAtBetween(
                        OrderStatusEnum.COMPLETED,
                        fromDate,
                        toDate,
                        PageRequest.of(0, 10)
                ))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorReportResponseDto getInstructorReport(Integer year, Integer month, Integer days) {
        DateRange range = resolveRange(year, month, days);
        int periodDays = range.periodDays();
        LocalDateTime fromDate = range.fromDateTime();
        LocalDateTime toDate = range.toDateTime();
        UUID instructorId = securityUtils.getCurrentUserId();

        long soldItems = orderItemRepository.countSoldItemsByInstructorAndStatusAndPaidAtBetween(
                instructorId,
                OrderStatusEnum.COMPLETED,
                fromDate,
                toDate
        );
        BigDecimal revenue = defaultAmount(orderItemRepository.sumInstructorRevenueByStatusAndPaidAtBetween(
                instructorId,
                OrderStatusEnum.COMPLETED,
                fromDate,
                toDate
        ));

        return InstructorReportResponseDto.builder()
                .periodDays(periodDays)
                .fromDate(fromDate.toLocalDate())
                .toDate(toDate.toLocalDate())
                .newCourses(courseRepository.countByInstructorIdAndDeletedFalseAndCreatedAtBetween(
                        instructorId,
                        fromDate,
                        toDate
                ))
                .newEnrollments(enrollmentRepository.countByInstructorIdAndCreatedAtBetween(
                        instructorId,
                        fromDate,
                        toDate
                ))
                .soldItems(soldItems)
                .revenue(revenue)
                .averageRevenuePerSale(average(revenue, soldItems))
                .dailyRevenue(toTimeSeries(orderItemRepository.findDailyInstructorRevenueTrend(
                        instructorId.toString(),
                        OrderStatusEnum.COMPLETED.getValue(),
                        fromDate
                )))
                .dailyEnrollments(toTimeSeries(enrollmentRepository.findDailyEnrollmentTrendByInstructor(
                        instructorId.toString(),
                        fromDate
                )))
                .topSellingCourses(orderItemRepository.findTopSellingCoursesByInstructorAndPaidAtBetween(
                        instructorId,
                        OrderStatusEnum.COMPLETED,
                        fromDate,
                        toDate,
                        PageRequest.of(0, 10)
                ))
                .build();
    }

    private int normalizeDays(Integer days) {
        if (days == null || days <= 0) {
            return 30;
        }
        return Math.min(days, 365);
    }

    private LocalDateTime startDateTime(int days) {
        return LocalDate.now().minusDays(days - 1L).atStartOfDay();
    }

    private DateRange resolveRange(Integer year, Integer month, Integer days) {
        if (year != null && month != null && month >= 1 && month <= 12) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate from = ym.atDay(1);
            LocalDate to = ym.atEndOfMonth();
            return new DateRange(from.atStartOfDay(), to.atTime(23, 59, 59), ym.lengthOfMonth());
        }
        if (year != null) {
            LocalDate from = LocalDate.of(year, 1, 1);
            LocalDate to = LocalDate.of(year, 12, 31);
            return new DateRange(from.atStartOfDay(), to.atTime(23, 59, 59), to.lengthOfYear());
        }
        int periodDays = normalizeDays(days);
        return new DateRange(startDateTime(periodDays), LocalDateTime.now(), periodDays);
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

    private record DateRange(LocalDateTime fromDateTime, LocalDateTime toDateTime, int periodDays) {
    }
}
