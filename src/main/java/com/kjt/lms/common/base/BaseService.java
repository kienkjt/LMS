package com.kjt.lms.common.base;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.security.SecurityUtils;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class BaseService {

    protected SecurityUtils securityUtils;
    protected CourseRepository courseRepository;
    protected MessageProvider messageProvider;

    protected CourseEntity findActiveCourseById(UUID courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));
        if (Boolean.TRUE.equals(course.getDeleted())) {
            throw new ResourceNotFoundException(
                    messageProvider.getMessage("exception.course.notFound"));
        }
        return course;
    }

    protected void validateCourseOwnership(CourseEntity course) {
        if (securityUtils.isAdmin()) return;
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (!course.getInstructorId().equals(currentUserId)) {
            throw new BusinessException(
                    messageProvider.getMessage("exception.course.notOwner"));
        }
    }
}
