package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseEntity;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.mapper.LessonMapper;
import com.kjt.lms.model.entity.ChapterEntity;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.request.lesson.CreateLessonRequestDto;
import com.kjt.lms.model.response.LessonResponseDto;
import com.kjt.lms.repository.ChapterRepository;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.LessonRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.LessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LessonMapper lessonMapper;
    private final MessageProvider messageProvider;

    @Override
    @Transactional
    public LessonResponseDto createLesson(UUID courseId, UUID chapterId, CreateLessonRequestDto request) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        validateCourseOwnership(course);

        ChapterEntity chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.chapter.notFound")));

        if (!courseId.equals(chapter.getCourseId())) {
            throw new BusinessException(messageProvider.getMessage("exception.chapter.notBelongToCourse"));
        }

        var savedLesson = lessonRepository.save(lessonMapper.toCreateEntity(request, courseId, chapterId));
        updateAggregateFields(course, chapter);

        log.info("Lesson created: {} for chapter: {} in course: {}", savedLesson.getId(), chapterId, courseId);

        return lessonMapper.toDto(savedLesson);
    }

    private void updateAggregateFields(CourseEntity course, ChapterEntity chapter) {
        chapter.setTotalLessons(Math.toIntExact(lessonRepository.countByChapterId(chapter.getId())));
        chapter.setTotalDuration(lessonRepository.sumDurationByChapterId(chapter.getId()));
        chapterRepository.save(chapter);

        course.setTotalLessons(Math.toIntExact(lessonRepository.countByCourseId(course.getId())));
        course.setTotalDuration(lessonRepository.sumDurationByCourseId(course.getId()));
        courseRepository.save(course);
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

