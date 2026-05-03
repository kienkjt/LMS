package com.kjt.lms.common.base;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.security.SecurityUtils;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@RequiredArgsConstructor
public abstract class BaseService {

    @Autowired
    protected SecurityUtils securityUtils;

    @Autowired
    protected CourseRepository courseRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected MessageProvider messageProvider;

    protected CourseEntity findActiveCourseById(UUID courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.course.notFound")));
        if (Boolean.TRUE.equals(course.getDeleted())) {
            throw new ResourceNotFoundException(messageProvider.getMessage("exception.course.deleted"));
        }
        return course;
    }

    protected void validateCourseOwnership(CourseEntity course) {
        if (securityUtils.isAdmin()) {
            return;
        }
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (!course.getInstructorId().equals(currentUserId)) {
            throw new BusinessException(messageProvider.getMessage("exception.course.notOwner"));
        }
    }

    protected void validateCourseOwnership(UUID courseId) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);
    }
}
