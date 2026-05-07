package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.YesNoEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.mapper.LessonMapper;
import com.kjt.lms.model.entity.ChapterEntity;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.LessonEntity;
import com.kjt.lms.model.request.lesson.CreateLessonRequestDto;
import com.kjt.lms.model.request.lesson.UpdateLessonRequestDto;
import com.kjt.lms.model.response.lesson.LessonResponseDto;
import com.kjt.lms.model.response.media.MediaUploadResponse;
import com.kjt.lms.repository.ChapterRepository;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.LessonRepository;
import com.kjt.lms.service.LessonService;
import com.kjt.lms.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonServiceImpl extends BaseService implements LessonService {

    private final LessonRepository lessonRepository;
    private final ChapterRepository chapterRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonMapper lessonMapper;
    private final MessageProvider messageProvider;
    private final MediaStorageService mediaStorageService;

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
        CourseEntity course = findActiveCourseById(courseId);
        ChapterEntity chapter = findActiveChapterById(chapterId);
        validateChapterBelongsToCourse(courseId, chapter);

        LessonEntity lesson = findActiveLessonById(lessonId);
        validateLessonBelongsToChapterAndCourse(courseId, chapterId, lesson);

        boolean canViewFullContent = canUserViewFullContent(course);
        LessonResponseDto dto = lessonMapper.toDto(lesson);
        maskPaidContentIfNeeded(lesson, dto, canViewFullContent);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponseDto> getLessonsByChapter(UUID courseId, UUID chapterId) {
        CourseEntity course = findActiveCourseById(courseId);
        ChapterEntity chapter = findActiveChapterById(chapterId);
        validateChapterBelongsToCourse(courseId, chapter);

        boolean canViewFullContent = canUserViewFullContent(course);

        return lessonRepository.findByCourseIdAndChapterIdAndDeletedFalseOrderByCreatedAtAsc(courseId, chapterId)
                .stream()
                .map(lesson -> {
                    LessonResponseDto dto = lessonMapper.toDto(lesson);
                    maskPaidContentIfNeeded(lesson, dto, canViewFullContent);
                    return dto;
                })
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

    @Override
    @Transactional
    public LessonResponseDto uploadLessonVideo(UUID courseId, UUID chapterId, UUID lessonId, MultipartFile file) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);

        ChapterEntity chapter = findActiveChapterById(chapterId);
        validateChapterBelongsToCourse(courseId, chapter);

        LessonEntity lesson = findActiveLessonById(lessonId);
        validateLessonBelongsToChapterAndCourse(courseId, chapterId, lesson);

        try {
            MediaUploadResponse uploadResponse = mediaStorageService.uploadCourseVideo(file);

            if (lesson.getVideoPublicId() != null && !lesson.getVideoPublicId().isEmpty()) {
                mediaStorageService.deleteMedia(lesson.getVideoPublicId(), "video");
            }

            lesson.setVideoUrl(uploadResponse.getSecureUrl());
            lesson.setVideoPublicId(uploadResponse.getPublicId());

            if (uploadResponse.getDuration() != null) {
                lesson.setDuration(uploadResponse.getDuration().intValue());
            }

            LessonEntity updatedLesson = lessonRepository.save(lesson);
            updateAggregateFields(course, chapter);

            log.info("Lesson video uploaded: {} in chapter: {} of course: {}", lessonId, chapterId, courseId);
            return lessonMapper.toDto(updatedLesson);
        } catch (Exception ex) {
            log.error("Lesson video upload failed: {} - {}", lessonId, ex.getMessage());
            throw new BusinessException(messageProvider.getMessage("media.lesson.video.upload.failed"));
        }
    }

    @Override
    @Transactional
    public LessonResponseDto uploadLessonDocument(UUID courseId, UUID chapterId, UUID lessonId, MultipartFile file) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);

        ChapterEntity chapter = findActiveChapterById(chapterId);
        validateChapterBelongsToCourse(courseId, chapter);

        LessonEntity lesson = findActiveLessonById(lessonId);
        validateLessonBelongsToChapterAndCourse(courseId, chapterId, lesson);

        try {
            MediaUploadResponse uploadResponse = mediaStorageService.uploadCourseDocument(file);

            lesson.setContent(uploadResponse.getSecureUrl());
            LessonEntity updatedLesson = lessonRepository.save(lesson);

            log.info("Lesson document uploaded: {} in chapter: {} of course: {}", lessonId, chapterId, courseId);
            return lessonMapper.toDto(updatedLesson);
        } catch (Exception ex) {
            log.error("Lesson document upload failed: {} - {}", lessonId, ex.getMessage());
            throw new BusinessException(messageProvider.getMessage("media.lesson.document.upload.failed"));
        }
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

    private void validateChapterBelongsToCourse(UUID courseId, ChapterEntity chapter) {
        if (!courseId.equals(chapter.getCourseId())) {
            throw new BusinessException(messageProvider.getMessage("exception.chapter.notBelongToCourse"));
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


    private boolean canUserViewFullContent(CourseEntity course) {
        if (isFreeCourse(course)) {
            return true;
        }

        if (securityUtils.isAdmin()) {
            return true;
        }

        try {
            UUID currentUserId = securityUtils.getCurrentUserId();
            if (course.getInstructorId().equals(currentUserId)) {
                return true;
            }
            return enrollmentRepository.existsByStudentIdAndCourseIdAndDeletedFalse(currentUserId, course.getId());
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    private boolean isFreeCourse(CourseEntity course) {
        BigDecimal effectivePrice = course.getDiscountPrice() != null
                ? course.getDiscountPrice()
                : course.getPrice();
        return effectivePrice == null || effectivePrice.compareTo(BigDecimal.ZERO) <= 0;
    }

    private void maskPaidContentIfNeeded(LessonEntity lesson, LessonResponseDto dto, boolean canViewFullContent) {
        if (YesNoEnum.ACTIVE.equals(lesson.getFreePreview())) {
            return;
        }
        if (!canViewFullContent) {
            dto.setVideoUrl(null);
            dto.setVideoPublicId(null);
            dto.setContent(null);
            dto.setQuizId(null);
        }
    }
}

