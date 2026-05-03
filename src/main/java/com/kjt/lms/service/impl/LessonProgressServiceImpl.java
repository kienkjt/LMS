package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.entity.LessonEntity;
import com.kjt.lms.model.entity.LessonProgressEntity;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.model.response.progress.CourseProgressResponseDto;
import com.kjt.lms.model.response.progress.LessonProgressDetailResponseDto;
import com.kjt.lms.model.response.progress.StudentCourseProgressResponseDto;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.LessonProgressRepository;
import com.kjt.lms.repository.LessonRepository;
import com.kjt.lms.service.LessonProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Override
    @Transactional(readOnly = true)
    public Page<StudentCourseProgressResponseDto> getCourseStudentsProgress(UUID courseId, int page, int pageSize) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);

        int pageIndex = Math.max(page, 1) - 1;
        int size = Math.max(pageSize, 1);
        Pageable pageable = PageRequest.of(pageIndex, size);
        List<LessonEntity> lessons = lessonRepository.findByCourseIdAndDeletedFalseOrderByCreatedAtAsc(courseId);

        return enrollmentRepository.findByCourseIdAndDeletedFalseOrderByCreatedAtDesc(courseId, pageable)
                .map(enrollment -> {
                    UserEntity student = userRepository.findById(enrollment.getStudentId())
                            .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    messageProvider.getMessage("exception.user.notfound")));

                    return buildStudentCourseProgress(course, enrollment, student, lessons);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public StudentCourseProgressResponseDto getStudentCourseProgress(UUID courseId, UUID studentId) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);

        EnrollmentEntity enrollment = enrollmentRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId)
                .orElseThrow(() -> new BusinessException(
                        messageProvider.getMessage("exception.enrollment.required")));

        UserEntity student = userRepository.findById(studentId)
                .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));

        List<LessonEntity> lessons = lessonRepository.findByCourseIdAndDeletedFalseOrderByCreatedAtAsc(courseId);
        return buildStudentCourseProgress(course, enrollment, student, lessons);
    }

    private StudentCourseProgressResponseDto buildStudentCourseProgress(
            CourseEntity course,
            EnrollmentEntity enrollment,
            UserEntity student,
            List<LessonEntity> lessons) {
        Map<UUID, LessonProgressEntity> progressByLessonId = lessonProgressRepository
                .findByStudentIdAndCourseIdAndDeletedFalse(student.getId(), course.getId())
                .stream()
                .collect(Collectors.toMap(
                        LessonProgressEntity::getLessonId,
                        Function.identity(),
                        (first, second) -> first
                ));

        List<LessonProgressDetailResponseDto> lessonProgresses = lessons.stream()
                .map(lesson -> {
                    LessonProgressEntity progress = progressByLessonId.get(lesson.getId());
                    boolean completed = progress != null && Boolean.TRUE.equals(progress.getCompleted());

                    return LessonProgressDetailResponseDto.builder()
                            .lessonId(lesson.getId())
                            .chapterId(lesson.getChapterId())
                            .lessonTitle(lesson.getTitle())
                            .lessonType(lesson.getType())
                            .duration(lesson.getDuration())
                            .completed(completed)
                            .completedAt(progress != null ? progress.getCompletedAt() : null)
                            .build();
                })
                .toList();

        long completedLessons = lessonProgresses.stream()
                .filter(lesson -> Boolean.TRUE.equals(lesson.getCompleted()))
                .count();
        BigDecimal progressPercent = calculateProgressPercent(completedLessons, lessons.size());

        return StudentCourseProgressResponseDto.builder()
                .enrollmentId(enrollment.getId())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .studentId(student.getId())
                .studentName(student.getFullName())
                .studentEmail(student.getEmail())
                .studentAvatar(student.getAvatar())
                .totalLessons(lessons.size())
                .completedLessons(completedLessons)
                .progressPercent(progressPercent)
                .enrolledAt(enrollment.getCreatedAt())
                .completedAt(enrollment.getCompletedAt())
                .lessons(lessonProgresses)
                .build();
    }

    private CourseProgressResponseDto calculateProgress(UUID studentId, UUID courseId) {
        long totalLessons = lessonRepository.countByCourseIdAndDeletedFalse(courseId);
        long completedLessons = lessonProgressRepository
                .countByStudentIdAndCourseIdAndCompletedTrueAndDeletedFalse(studentId, courseId);

        BigDecimal progressPercent = calculateProgressPercent(completedLessons, totalLessons);

        return CourseProgressResponseDto.builder()
                .courseId(courseId)
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .progressPercent(progressPercent)
                .build();
    }

    private BigDecimal calculateProgressPercent(long completedLessons, long totalLessons) {
        if (totalLessons <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(completedLessons)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(totalLessons), 2, RoundingMode.HALF_UP);
    }
}
