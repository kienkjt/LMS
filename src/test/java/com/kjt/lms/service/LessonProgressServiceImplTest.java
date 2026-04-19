package com.kjt.lms.service;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.security.SecurityUtils;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.entity.LessonEntity;
import com.kjt.lms.model.entity.LessonProgressEntity;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.LessonProgressRepository;
import com.kjt.lms.repository.LessonRepository;
import com.kjt.lms.service.impl.LessonProgressServiceImpl;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonProgressServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private MessageProvider messageProvider;

    @InjectMocks
    private LessonProgressServiceImpl lessonProgressService;

    @BeforeEach
    void setUp() {
        lenient().when(messageProvider.getMessage(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void completeLesson_shouldUpdateProgressAndReturnPercent() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        EnrollmentEntity enrollment = EnrollmentEntity.builder()
                .studentId(studentId)
                .courseId(courseId)
                .progressPercent(BigDecimal.ZERO)
                .build();

        LessonEntity lesson = new LessonEntity();
        lesson.setId(lessonId);
        lesson.setCourseId(courseId);
        lesson.setDeleted(false);

        when(securityUtils.getCurrentUserId()).thenReturn(studentId);
        when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId))
                .thenReturn(Optional.of(enrollment));
        when(lessonRepository.findByIdAndDeletedFalse(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonProgressRepository.findByStudentIdAndLessonIdAndDeletedFalse(studentId, lessonId))
                .thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgressEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonRepository.countByCourseIdAndDeletedFalse(courseId)).thenReturn(4L);
        when(lessonProgressRepository.countByStudentIdAndCourseIdAndCompletedTrueAndDeletedFalse(studentId, courseId))
                .thenReturn(1L);

        var response = lessonProgressService.completeLesson(courseId, lessonId);

        assertEquals(4L, response.getTotalLessons());
        assertEquals(1L, response.getCompletedLessons());
        assertEquals(new BigDecimal("25.00"), response.getProgressPercent());
    }

    @Test
    void getCourseProgress_shouldThrowBusinessException_whenNotEnrolled() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(securityUtils.getCurrentUserId()).thenReturn(studentId);
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId)).thenReturn(false);

        assertThrows(BusinessException.class, () -> lessonProgressService.getCourseProgress(courseId));
    }
}
