package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.CourseStatusEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.mapper.EnrollmentMapper;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.response.enrollment.EnrolledCourseResponseDto;
import com.kjt.lms.model.response.enrollment.EnrollmentResponseDto;
import com.kjt.lms.model.response.enrollment.InstructorStudentEnrollmentResponseDto;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.OrderItemRepository;
import com.kjt.lms.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentServiceImpl extends BaseService implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final OrderItemRepository orderItemRepository;
    private final MessageProvider messageProvider;
    private final EnrollmentMapper enrollmentMapper;

    @Override
    @Transactional
    public EnrollmentResponseDto enrollCourse(UUID courseId) {
        UUID studentId = securityUtils.getCurrentUserId();

        CourseEntity course = findActiveCourseById(courseId);

        if (course.getStatus() != CourseStatusEnum.APPROVED) {
            throw new BusinessException(messageProvider.getMessage("exception.enrollment.course.notAvailable"));
        }

        return enrollmentRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId)
                .map(enrollmentMapper::toResponse)
                .orElseGet(() -> createEnrollment(studentId, course));
    }

    private EnrollmentResponseDto createEnrollment(UUID studentId, CourseEntity course) {
        boolean freeCourse = isFreeCourse(course);
        boolean paidCourse = orderItemRepository.existsPaidCourseForStudent(studentId, course.getId());

        if (!freeCourse && !paidCourse) {
            throw new BusinessException(messageProvider.getMessage("exception.enrollment.paymentRequired"));
        }

        EnrollmentEntity enrollment = enrollmentMapper.toCreateEntity(studentId, course.getId());

        EnrollmentEntity savedEnrollment = enrollmentRepository.save(enrollment);
        log.info("Student {} enrolled course {}", studentId, course.getId());
        return enrollmentMapper.toResponse(savedEnrollment);
    }

    private boolean isFreeCourse(CourseEntity course) {
        BigDecimal effectivePrice = course.getDiscountPrice() != null
                ? course.getDiscountPrice()
                : course.getPrice();
        return effectivePrice == null || effectivePrice.compareTo(BigDecimal.ZERO) <= 0;
    }

    @Override
    public List<EnrolledCourseResponseDto> getMyEnrolledCourses() {
        UUID studentId = securityUtils.getCurrentUserId();
        return enrollmentRepository.findEnrolledCoursesByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorStudentEnrollmentResponseDto> getStudentsByCourseForInstructor(UUID courseId, Pageable pageable) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);
        return enrollmentRepository.findStudentsByCourseId(courseId, pageable);
    }
}
