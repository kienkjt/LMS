package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.entity.LessonEntity;
import com.kjt.lms.model.entity.LessonProgressEntity;
import com.kjt.lms.model.response.progress.CourseProgressResponseDto;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.LessonProgressRepository;
import com.kjt.lms.repository.LessonRepository;
import com.kjt.lms.service.LessonProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonProgressServiceImpl extends BaseService implements LessonProgressService {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final MessageProvider messageProvider;

    @Override
    @Transactional
    public CourseProgressResponseDto completeLesson(UUID courseId, UUID lessonId) {
        UUID studentId = securityUtils.getCurrentUserId();

        EnrollmentEntity enrollment = enrollmentRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId)
                .orElseThrow(() -> new BusinessException(
                        messageProvider.getMessage("exception.enrollment.required")));

        LessonEntity lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.lesson.notFound")));

        if (!courseId.equals(lesson.getCourseId())) {
            throw new BusinessException(messageProvider.getMessage("exception.lesson.notBelongToCourse"));
        }

        LessonProgressEntity lessonProgress = lessonProgressRepository
                .findByStudentIdAndLessonIdAndDeletedFalse(studentId, lessonId)
                .orElseGet(() -> LessonProgressEntity.builder()
                        .studentId(studentId)
                        .courseId(courseId)
                        .lessonId(lessonId)
                        .build());

        if (!Boolean.TRUE.equals(lessonProgress.getCompleted())) {
            lessonProgress.setCompleted(true);
            lessonProgress.setCompletedAt(LocalDateTime.now());
            lessonProgressRepository.save(lessonProgress);
            log.info("Student {} completed lesson {} in course {}", studentId, lessonId, courseId);
        }

        CourseProgressResponseDto progress = calculateProgress(studentId, courseId);
        enrollment.setProgressPercent(progress.getProgressPercent());

        if (progress.getProgressPercent().compareTo(new BigDecimal("100.00")) >= 0) {
            enrollment.setCompletedAt(LocalDateTime.now());
        }

        enrollmentRepository.save(enrollment);
        return progress;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseProgressResponseDto getCourseProgress(UUID courseId) {
        UUID studentId = securityUtils.getCurrentUserId();

        if (!enrollmentRepository.existsByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId)) {
            throw new BusinessException(messageProvider.getMessage("exception.enrollment.required"));
        }

        return calculateProgress(studentId, courseId);
    }

    private CourseProgressResponseDto calculateProgress(UUID studentId, UUID courseId) {
        long totalLessons = lessonRepository.countByCourseIdAndDeletedFalse(courseId);
        long completedLessons = lessonProgressRepository
                .countByStudentIdAndCourseIdAndCompletedTrueAndDeletedFalse(studentId, courseId);

        BigDecimal progressPercent = BigDecimal.ZERO;
        if (totalLessons > 0) {
            progressPercent = BigDecimal.valueOf(completedLessons)
                    .multiply(new BigDecimal("100"))
                    .divide(BigDecimal.valueOf(totalLessons), 2, RoundingMode.HALF_UP);
        }

        return CourseProgressResponseDto.builder()
                .courseId(courseId)
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .progressPercent(progressPercent)
                .build();
    }
}