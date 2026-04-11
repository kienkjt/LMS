package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseEntity;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.mapper.ChapterMapper;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.request.chapter.CreateChapterRequestDto;
import com.kjt.lms.model.response.ChapterResponseDto;
import com.kjt.lms.repository.ChapterRepository;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.ChapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ChapterMapper chapterMapper;
    private final MessageProvider messageProvider;

    @Override
    @Transactional
    public ChapterResponseDto createChapter(UUID courseId, CreateChapterRequestDto request) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        validateCourseOwnership(course);

        var savedChapter = chapterRepository.save(chapterMapper.toCreateEntity(request, courseId));

        log.info("Chapter created: {} for course: {}", savedChapter.getId(), courseId);

        ChapterResponseDto response = chapterMapper.toDto(savedChapter);
        response.setLessons(Collections.emptyList());
        return response;
    }

    private void validateCourseOwnership(CourseEntity course) {
        if (isAdmin()) {
            return;
        }

        UUID currentUserId = getCurrentUserId();
        if (!course.getInstructorId().equals(currentUserId)) {
            throw new BusinessException(messageProvider.getMessage("exception.course.notOwner"));
        }
    }

    private UUID getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(BaseEntity::getId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
