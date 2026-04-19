package com.kjt.lms.service;

import com.kjt.lms.model.response.enrollment.EnrolledCourseResponseDto;
import com.kjt.lms.model.response.enrollment.EnrollmentResponseDto;

import java.util.List;
import java.util.UUID;

public interface EnrollmentService {

    EnrollmentResponseDto enrollCourse(UUID courseId);

    List<EnrolledCourseResponseDto> getMyEnrolledCourses();
}

