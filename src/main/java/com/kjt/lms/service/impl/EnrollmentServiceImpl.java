package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.CourseStatusEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.model.response.enrollment.EnrolledCourseResponseDto;
import com.kjt.lms.model.response.enrollment.EnrollmentResponseDto;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.OrderItemRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentServiceImpl extends BaseService implements EnrollmentService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final OrderItemRepository orderItemRepository;
    private final MessageProvider messageProvider;

    @Override
    @Transactional
    public EnrollmentResponseDto enrollCourse(UUID courseId) {
        UUID studentId = securityUtils.getCurrentUserId();

        CourseEntity course = findActiveCourseById(courseId);

        if (course.getStatus() != CourseStatusEnum.APPROVED) {
            throw new BusinessException(messageProvider.getMessage("exception.enrollment.course.notAvailable"));
        }

        return enrollmentRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId)
                .map(this::toResponse)
                .orElseGet(() -> createEnrollment(studentId, course));
    }

    private EnrollmentResponseDto createEnrollment(UUID studentId, CourseEntity course) {
        boolean freeCourse = isFreeCourse(course);
        boolean paidCourse = orderItemRepository.existsPaidCourseForStudent(studentId, course.getId());

        if (!freeCourse && !paidCourse) {
            throw new BusinessException(messageProvider.getMessage("exception.enrollment.paymentRequired"));
        }

        EnrollmentEntity enrollment = EnrollmentEntity.builder()
                .studentId(studentId)
                .courseId(course.getId())
                .progressPercent(BigDecimal.ZERO)
                .build();

        EnrollmentEntity savedEnrollment = enrollmentRepository.save(enrollment);
        log.info("Student {} enrolled course {}", studentId, course.getId());
        return toResponse(savedEnrollment);
    }

    private boolean isFreeCourse(CourseEntity course) {
        BigDecimal effectivePrice = course.getDiscountPrice() != null
                ? course.getDiscountPrice()
                : course.getPrice();
        return effectivePrice == null || effectivePrice.compareTo(BigDecimal.ZERO) <= 0;
    }

    private EnrollmentResponseDto toResponse(EnrollmentEntity enrollment) {
        return EnrollmentResponseDto.builder()
                .enrollmentId(enrollment.getId())
                .courseId(enrollment.getCourseId())
                .studentId(enrollment.getStudentId())
                .orderId(enrollment.getOrderId())
                .progressPercent(enrollment.getProgressPercent())
                .enrolledAt(enrollment.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrolledCourseResponseDto> getMyEnrolledCourses() {
        UUID studentId = securityUtils.getCurrentUserId();
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentIdAndDeletedFalse(studentId);

        return enrollments.stream()
                .map(enrollment -> {
                    CourseEntity course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
                    if (course == null) return null;

                    String instructorName = userRepository.findById(course.getInstructorId())
                            .map(UserEntity::getFullName)
                            .orElse("Unknown");

                    return EnrolledCourseResponseDto.builder()
                            .enrollmentId(enrollment.getId())
                            .courseId(enrollment.getCourseId())
                            .courseTitle(course.getTitle())
                            .courseThumbnail(course.getThumbnail())
                            .instructorName(instructorName)
                            .progressPercent(enrollment.getProgressPercent())
                            .enrolledAt(enrollment.getCreatedAt())
                            .completedAt(enrollment.getCompletedAt())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}