package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.NotificationTypeEnum;
import com.kjt.lms.model.response.enrollment.InstructorStudentEnrollmentResponseDto;
import com.kjt.lms.model.response.learning.DailyLearningHeatmapItemDto;
import com.kjt.lms.model.response.learning.InstructorStudentEngagementDto;
import com.kjt.lms.model.response.learning.LearningStreakResponseDto;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.LessonProgressRepository;
import com.kjt.lms.repository.NoteRepository;
import com.kjt.lms.repository.NotificationRepository;
import com.kjt.lms.repository.QuizAttemptRepository;
import com.kjt.lms.service.EmailService;
import com.kjt.lms.service.LearningAnalyticsService;
import com.kjt.lms.service.NotificationService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningAnalyticsServiceImpl extends BaseService implements LearningAnalyticsService {

    private static final int DEFAULT_HEATMAP_DAYS = 90;
    private static final int AT_RISK_INACTIVE_DAYS = 7;

    private final LessonProgressRepository lessonProgressRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final NoteRepository noteRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public LearningStreakResponseDto getMyLearningStreak() {
        UUID studentId = securityUtils.getCurrentUserId();
        StreakAggregate streak = calculateStreak(studentId);

        String warning = null;
        if (!streak.learnedToday && streak.currentStreak > 0) {
            warning = "Bạn sắp mất chuỗi học " + streak.currentStreak + " ngày liên tiếp!";
        }

        return LearningStreakResponseDto.builder()
                .currentStreak(streak.currentStreak)
                .longestStreak(streak.longestStreak)
                .totalActiveDays(streak.totalActiveDays)
                .lastLearningDate(streak.lastLearningDate)
                .learnedToday(streak.learnedToday)
                .atRisk(streak.atRisk)
                .warningMessage(warning)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyLearningHeatmapItemDto> getMyLearningHeatmap(LocalDate fromDate, LocalDate toDate) {
        UUID studentId = securityUtils.getCurrentUserId();
        LocalDate end = toDate == null ? LocalDate.now() : toDate;
        LocalDate start = fromDate == null ? end.minusDays(DEFAULT_HEATMAP_DAYS - 1L) : fromDate;
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        Map<LocalDate, DailyMetric> metricByDate = buildDailyMetrics(studentId);
        List<DailyLearningHeatmapItemDto> response = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            DailyMetric metric = metricByDate.getOrDefault(date, DailyMetric.empty(date));
            response.add(DailyLearningHeatmapItemDto.builder()
                    .date(date)
                    .activityCount(metric.activityCount)
                    .estimatedMinutes(metric.estimatedMinutes)
                    .active(metric.activityCount > 0)
                    .build());
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorStudentEngagementDto> getInstructorCourseStudentEngagement(UUID courseId, int page, int pageSize) {
        validateCourseOwnership(courseId);

        int pageIndex = Math.max(page, 1) - 1;
        int size = Math.max(pageSize, 1);
        PageRequest pageable = PageRequest.of(pageIndex, size);
        Page<InstructorStudentEnrollmentResponseDto> students = enrollmentRepository.findStudentsByCourseId(courseId, pageable);

        List<InstructorStudentEngagementDto> content = students.getContent().stream().map(student -> {
            UUID studentId = student.getStudentId();
            StreakAggregate streak = calculateStreak(studentId);
            long inactiveDays = streak.lastLearningDate == null
                    ? Long.MAX_VALUE
                    : ChronoUnit.DAYS.between(streak.lastLearningDate, LocalDate.now());

            return InstructorStudentEngagementDto.builder()
                    .enrollmentId(student.getEnrollmentId())
                    .studentId(studentId)
                    .studentName(student.getStudentName())
                    .studentEmail(student.getStudentEmail())
                    .studentAvatar(student.getStudentAvatar())
                    .studentPhoneNumber(student.getStudentPhoneNumber())
                    .progressPercent(student.getProgressPercent() == null ? 0 : student.getProgressPercent().doubleValue())
                    .currentStreak(streak.currentStreak)
                    .longestStreak(streak.longestStreak)
                    .lastLearningDate(streak.lastLearningDate)
                    .inactiveDays(inactiveDays == Long.MAX_VALUE ? 9999 : inactiveDays)
                    .atRisk(inactiveDays >= AT_RISK_INACTIVE_DAYS)
                    .build();
        }).toList();

        return new PageImpl<>(content, pageable, students.getTotalElements());
    }

    @Override
    @Scheduled(cron = "0 0 20 * * *")
    @Transactional
    public void sendStreakRiskWarnings() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        String referenceType = "LEARNING_STREAK_WARNING_" + today;

        for (UUID studentId : enrollmentRepository.findDistinctActiveStudentIds()) {
            StreakAggregate streak = calculateStreak(studentId);
            if (streak.currentStreak <= 0 || streak.learnedToday || streak.lastLearningDate == null) {
                continue;
            }
            if (!streak.lastLearningDate.equals(today.minusDays(1))) {
                continue;
            }

            boolean sentToday = notificationRepository.existsByUserIdAndTypeAndReferenceTypeAndCreatedAtBetween(
                    studentId,
                    NotificationTypeEnum.SYSTEM,
                    referenceType,
                    dayStart,
                    dayEnd
            );
            if (sentToday) {
                continue;
            }

            String title = messageProvider.getMessage("notification.streak.warning.title");
            String message = messageProvider.getMessage("notification.streak.warning.message", streak.currentStreak);
            notificationService.notifyUser(
                    studentId,
                    NotificationTypeEnum.SYSTEM,
                    title,
                    message,
                    null,
                    referenceType
            );

            userRepository.findById(studentId).ifPresent(user ->
                    emailService.sendSystemNotificationEmail(
                            user.getEmail(),
                            user.getFullName(),
                            title,
                            message
                    )
            );
        }
    }

    private StreakAggregate calculateStreak(UUID studentId) {
        List<LocalDate> activeDates = buildDailyMetrics(studentId)
                .values()
                .stream()
                .filter(item -> item.activityCount > 0)
                .map(item -> item.date)
                .sorted(Comparator.naturalOrder())
                .toList();

        if (activeDates.isEmpty()) {
            return StreakAggregate.builder()
                    .currentStreak(0)
                    .longestStreak(0)
                    .totalActiveDays(0)
                    .lastLearningDate(null)
                    .learnedToday(false)
                    .atRisk(false)
                    .build();
        }

        LocalDate lastDate = activeDates.get(activeDates.size() - 1);
        long longest = 1;
        long currentRun = 1;
        for (int i = 1; i < activeDates.size(); i++) {
            if (activeDates.get(i - 1).plusDays(1).equals(activeDates.get(i))) {
                currentRun++;
            } else {
                longest = Math.max(longest, currentRun);
                currentRun = 1;
            }
        }
        longest = Math.max(longest, currentRun);

        long currentStreak = 1;
        for (int i = activeDates.size() - 2; i >= 0; i--) {
            if (activeDates.get(i).plusDays(1).equals(activeDates.get(i + 1))) {
                currentStreak++;
            } else {
                break;
            }
        }

        boolean learnedToday = lastDate.equals(LocalDate.now());
        boolean atRisk = !learnedToday && lastDate.equals(LocalDate.now().minusDays(1)) && currentStreak > 0;

        return StreakAggregate.builder()
                .currentStreak(currentStreak)
                .longestStreak(longest)
                .totalActiveDays(activeDates.size())
                .lastLearningDate(lastDate)
                .learnedToday(learnedToday)
                .atRisk(atRisk)
                .build();
    }

    private Map<LocalDate, DailyMetric> buildDailyMetrics(UUID studentId) {
        Map<LocalDate, DailyMetric> metricByDate = new HashMap<>();
        accumulate(metricByDate, lessonProgressRepository.summarizeDailyLearningByStudent(studentId));
        accumulate(metricByDate, quizAttemptRepository.summarizeDailyLearningByStudent(studentId));
        accumulate(metricByDate, noteRepository.summarizeDailyLearningByStudent(studentId));
        return metricByDate;
    }

    private void accumulate(Map<LocalDate, DailyMetric> metricByDate, List<Object[]> rows) {
        for (Object[] row : rows) {
            LocalDate date = toLocalDate(row[0]);
            long count = toLong(row[1]);
            long minutes = toLong(row[2]);
            DailyMetric current = metricByDate.getOrDefault(date, DailyMetric.empty(date));
            metricByDate.put(date, current.plus(count, minutes));
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private long toLong(Object value) {
        if (value == null) return 0;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(value));
    }

    @Value
    @Builder
    private static class StreakAggregate {
        long currentStreak;
        long longestStreak;
        long totalActiveDays;
        LocalDate lastLearningDate;
        boolean learnedToday;
        boolean atRisk;
    }

    @Value
    private static class DailyMetric {
        LocalDate date;
        long activityCount;
        long estimatedMinutes;

        static DailyMetric empty(LocalDate date) {
            return new DailyMetric(date, 0, 0);
        }

        DailyMetric plus(long count, long minutes) {
            return new DailyMetric(date, activityCount + count, estimatedMinutes + minutes);
        }
    }
}

