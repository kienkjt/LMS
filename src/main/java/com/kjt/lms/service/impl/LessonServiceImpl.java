package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseEntity;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.mapper.LessonMapper;
import com.kjt.lms.model.entity.ChapterEntity;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.LessonEntity;
import com.kjt.lms.model.request.lesson.CreateLessonRequestDto;
import com.kjt.lms.model.request.lesson.UpdateLessonRequestDto;
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

import java.util.List;
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
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);

        ChapterEntity chapter = findActiveChapterById(chapterId);
        validateChapterBelongsToCourse(courseId, chapter);

        if (lessonRepository.existsByChapterIdAndTitleIgnoreCaseAndDeletedFalse(chapterId, request.getTitle())) {
            throw new BusinessException(messageProvider.getMessage("exception.lesson.duplicateTitle"));
        }

        LessonEntity savedLesson = lessonRepository.save(lessonMapper.toCreateEntity(request, courseId, chapterId));
        updateAggregateFields(course, chapter);

        log.info("Lesson created: {} for chapter: {} in course: {}", savedLesson.getId(), chapterId, courseId);

        return lessonMapper.toDto(savedLesson);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonResponseDto getLessonById(UUID courseId, UUID chapterId, UUID lessonId) {
        findActiveCourseById(courseId);
        ChapterEntity chapter = findActiveChapterById(chapterId);
        validateChapterBelongsToCourse(courseId, chapter);

        LessonEntity lesson = findActiveLessonById(lessonId);
        validateLessonBelongsToChapterAndCourse(courseId, chapterId, lesson);

        return lessonMapper.toDto(lesson);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponseDto> getLessonsByChapter(UUID courseId, UUID chapterId) {
        findActiveCourseById(courseId);
        ChapterEntity chapter = findActiveChapterById(chapterId);
        validateChapterBelongsToCourse(courseId, chapter);

        return lessonRepository.findByCourseIdAndChapterIdAndDeletedFalseOrderByCreatedAtAsc(courseId, chapterId)
                .stream()
                .map(lessonMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public LessonResponseDto updateLesson(UUID courseId, UUID chapterId, UUID lessonId, UpdateLessonRequestDto request) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);

        ChapterEntity chapter = findActiveChapterById(chapterId);
        validateChapterBelongsToCourse(courseId, chapter);

        LessonEntity lesson = findActiveLessonById(lessonId);
        validateLessonBelongsToChapterAndCourse(courseId, chapterId, lesson);

        if (lessonRepository.existsByChapterIdAndTitleIgnoreCaseAndDeletedFalseAndIdNot(chapterId, request.getTitle(), lessonId)) {
            throw new BusinessException(messageProvider.getMessage("exception.lesson.duplicateTitle"));
        }

        lessonMapper.updateEntity(request, lesson);
        LessonEntity updatedLesson = lessonRepository.save(lesson);
        updateAggregateFields(course, chapter);

        log.info("Lesson updated: {}", lessonId);

        return lessonMapper.toDto(updatedLesson);
    }

    @Override
    @Transactional
    public void deleteLesson(UUID courseId, UUID chapterId, UUID lessonId) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);

        ChapterEntity chapter = findActiveChapterById(chapterId);
        validateChapterBelongsToCourse(courseId, chapter);

        LessonEntity lesson = findActiveLessonById(lessonId);
        validateLessonBelongsToChapterAndCourse(courseId, chapterId, lesson);

        lesson.setDeleted(true);
        lessonRepository.save(lesson);

        updateAggregateFields(course, chapter);

        log.info("Lesson deleted: {} from chapter: {} in course: {}", lessonId, chapterId, courseId);
    }

    private CourseEntity findActiveCourseById(UUID courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        if (Boolean.TRUE.equals(course.getDeleted())) {
            throw new ResourceNotFoundException(messageProvider.getMessage("exception.course.notFound"));
        }

        return course;
    }

    private ChapterEntity findActiveChapterById(UUID chapterId) {
        return chapterRepository.findByIdAndDeletedFalse(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.chapter.notFound")));
    }

    private LessonEntity findActiveLessonById(UUID lessonId) {
        return lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.lesson.notFound")));
    }

    private void validateLessonBelongsToChapterAndCourse(UUID courseId, UUID chapterId, LessonEntity lesson) {
        if (!courseId.equals(lesson.getCourseId()) || !chapterId.equals(lesson.getChapterId())) {
            throw new BusinessException(messageProvider.getMessage("exception.lesson.notBelongToChapter"));
        }
    }

    private void updateAggregateFields(CourseEntity course, ChapterEntity chapter) {
        chapter.setTotalLessons(Math.toIntExact(lessonRepository.countByChapterIdAndDeletedFalse(chapter.getId())));
        chapter.setTotalDuration(lessonRepository.sumDurationByChapterId(chapter.getId()));
        chapterRepository.save(chapter);

        course.setTotalLessons(Math.toIntExact(lessonRepository.countByCourseIdAndDeletedFalse(course.getId())));
        course.setTotalDuration(lessonRepository.sumDurationByCourseId(course.getId()));
        courseRepository.save(course);
    }

    private void validateChapterBelongsToCourse(UUID courseId, ChapterEntity chapter) {
        if (!courseId.equals(chapter.getCourseId())) {
            throw new BusinessException(messageProvider.getMessage("exception.chapter.notBelongToCourse"));
        }
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

