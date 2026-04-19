package com.kjt.lms.service;

import com.kjt.lms.common.constants.CourseStatusEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.security.SecurityUtils;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.OrderItemRepository;
import com.kjt.lms.service.impl.EnrollmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private MessageProvider messageProvider;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    @BeforeEach
    void setUp() {
        lenient().when(messageProvider.getMessage(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void enrollCourse_shouldCreateEnrollment_whenCourseIsFree() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        CourseEntity freeCourse = new CourseEntity();
        freeCourse.setId(courseId);
        freeCourse.setDeleted(false);
        freeCourse.setStatus(CourseStatusEnum.APPROVED);
        freeCourse.setPrice(BigDecimal.ZERO);

        EnrollmentEntity savedEnrollment = EnrollmentEntity.builder()
                .studentId(studentId)
                .courseId(courseId)
                .progressPercent(BigDecimal.ZERO)
                .build();
        savedEnrollment.setId(UUID.randomUUID());

        when(securityUtils.getCurrentUserId()).thenReturn(studentId);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(freeCourse));
        when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId))
                .thenReturn(Optional.empty());
        when(orderItemRepository.existsPaidCourseForStudent(studentId, courseId)).thenReturn(false);
        when(enrollmentRepository.save(any(EnrollmentEntity.class))).thenReturn(savedEnrollment);

        var response = enrollmentService.enrollCourse(courseId);

        assertEquals(courseId, response.getCourseId());
        assertEquals(studentId, response.getStudentId());
        assertEquals(BigDecimal.ZERO, response.getProgressPercent());
    }

    @Test
    void enrollCourse_shouldThrowBusinessException_whenPaidCourseNotPurchased() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        CourseEntity paidCourse = new CourseEntity();
        paidCourse.setId(courseId);
        paidCourse.setDeleted(false);
        paidCourse.setStatus(CourseStatusEnum.APPROVED);
        paidCourse.setPrice(new BigDecimal("100"));

        when(securityUtils.getCurrentUserId()).thenReturn(studentId);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(paidCourse));
        when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId))
                .thenReturn(Optional.empty());
        when(orderItemRepository.existsPaidCourseForStudent(studentId, courseId)).thenReturn(false);

        assertThrows(BusinessException.class, () -> enrollmentService.enrollCourse(courseId));
        verify(enrollmentRepository, never()).save(any(EnrollmentEntity.class));
    }

    @Test
    void enrollCourse_shouldReturnExistingEnrollment_whenAlreadyEnrolled() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        CourseEntity course = new CourseEntity();
        course.setId(courseId);
        course.setDeleted(false);
        course.setStatus(CourseStatusEnum.APPROVED);

        EnrollmentEntity existing = EnrollmentEntity.builder()
                .studentId(studentId)
                .courseId(courseId)
                .progressPercent(new BigDecimal("25.00"))
                .build();
        existing.setId(UUID.randomUUID());

        when(securityUtils.getCurrentUserId()).thenReturn(studentId);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId))
                .thenReturn(Optional.of(existing));

        var response = enrollmentService.enrollCourse(courseId);

        assertEquals(existing.getId(), response.getEnrollmentId());
        assertEquals(new BigDecimal("25.00"), response.getProgressPercent());
        verify(enrollmentRepository, never()).save(any(EnrollmentEntity.class));
    }
}
