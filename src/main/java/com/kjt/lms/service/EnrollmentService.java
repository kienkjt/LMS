package com.kjt.lms.service;

import com.kjt.lms.model.response.enrollment.EnrolledCourseResponseDto;
import com.kjt.lms.model.response.enrollment.EnrollmentResponseDto;
import com.kjt.lms.model.response.enrollment.InstructorStudentEnrollmentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface EnrollmentService {

    EnrollmentResponseDto enrollCourse(UUID courseId);

    List<EnrolledCourseResponseDto> getMyEnrolledCourses();

    Page<InstructorStudentEnrollmentResponseDto> getStudentsByCourseForInstructor(UUID courseId, Pageable pageable);
}

