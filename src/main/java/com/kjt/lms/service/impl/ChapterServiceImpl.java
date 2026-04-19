package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.YesNoEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.mapper.ChapterMapper;
import com.kjt.lms.mapper.LessonMapper;
import com.kjt.lms.model.entity.ChapterEntity;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.LessonEntity;
import com.kjt.lms.model.request.chapter.CreateChapterRequestDto;
import com.kjt.lms.model.request.chapter.UpdateChapterRequestDto;
import com.kjt.lms.model.response.chapter.ChapterResponseDto;
import com.kjt.lms.model.response.lesson.LessonResponseDto;
import com.kjt.lms.repository.ChapterRepository;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.LessonRepository;
import com.kjt.lms.service.ChapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterServiceImpl extends BaseService implements ChapterService {

    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ChapterMapper chapterMapper;
    private final LessonMapper lessonMapper;
    private final MessageProvider messageProvider;

    @Override
    @Transactional
    public ChapterResponseDto createChapter(UUID courseId, CreateChapterRequestDto request) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);

        if (chapterRepository.existsByCourseIdAndTitleIgnoreCaseAndDeletedFalse(courseId, request.getTitle())) {
            throw new BusinessException(messageProvider.getMessage("exception.chapter.duplicateTitle"));
        }

        ChapterEntity savedChapter = chapterRepository.save(chapterMapper.toCreateEntity(request, courseId));
        log.info("Chapter created: {} for course: {}", savedChapter.getId(), courseId);

        ChapterResponseDto response = chapterMapper.toDto(savedChapter);
        response.setLessons(Collections.emptyList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ChapterResponseDto getChapterById(UUID courseId, UUID chapterId) {
        CourseEntity course = findActiveCourseById(courseId);
        ChapterEntity chapter = findActiveChapterById(chapterId);
        validateChapterBelongsToCourse(courseId, chapter);

        ChapterResponseDto response = chapterMapper.toDto(chapter);
        response.setLessons(getLessonDtos(course, chapterId));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChapterResponseDto> getChaptersByCourse(UUID courseId) {
        CourseEntity course = findActiveCourseById(courseId);
        boolean canViewFullContent = canUserViewFullContent(course);

        List<ChapterEntity> chapters = chapterRepository.findByCourseIdAndDeletedFalseOrderByCreatedAtAsc(courseId);
        Map<UUID, List<LessonResponseDto>> lessonsByChapter = lessonRepository
                .findByCourseIdAndDeletedFalseOrderByCreatedAtAsc(courseId)
                .stream()
                .collect(Collectors.groupingBy(
                        LessonEntity::getChapterId,
                        Collectors.mapping(lesson -> {
                            LessonResponseDto dto = lessonMapper.toDto(lesson);
                            maskPaidContentIfNeeded(lesson, dto, canViewFullContent);
                            return dto;
                        }, Collectors.toList())
                ));

        return chapters.stream()
                .map(chapter -> {
                    ChapterResponseDto response = chapterMapper.toDto(chapter);
                    response.setLessons(lessonsByChapter.getOrDefault(chapter.getId(), Collections.emptyList()));
                    return response;
                })
                .toList();
    }

    @Override
    @Transactional
    public ChapterResponseDto updateChapter(UUID courseId, UUID chapterId, UpdateChapterRequestDto request) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);

        ChapterEntity chapter = findActiveChapterById(chapterId);
        validateChapterBelongsToCourse(courseId, chapter);

        if (chapterRepository.existsByCourseIdAndTitleIgnoreCaseAndDeletedFalseAndIdNot(courseId, request.getTitle(), chapterId)) {
            throw new BusinessException(messageProvider.getMessage("exception.chapter.duplicateTitle"));
        }

        chapterMapper.updateEntity(request, chapter);
        ChapterEntity updatedChapter = chapterRepository.save(chapter);

        log.info("Chapter updated: {}", chapterId);

        ChapterResponseDto response = chapterMapper.toDto(updatedChapter);
        response.setLessons(getLessonDtos(course, chapterId));
        return response;
    }

    @Override
    @Transactional
    public void deleteChapter(UUID courseId, UUID chapterId) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);

        ChapterEntity chapter = findActiveChapterById(chapterId);
        validateChapterBelongsToCourse(courseId, chapter);

        chapter.setDeleted(true);
        chapterRepository.save(chapter);

        List<LessonEntity> lessons = lessonRepository.findByCourseIdAndChapterIdAndDeletedFalseOrderByCreatedAtAsc(courseId, chapterId);
        if (!lessons.isEmpty()) {
            lessons.forEach(lesson -> lesson.setDeleted(true));
            lessonRepository.saveAll(lessons);
        }

        updateCourseAggregateFields(course);

        log.info("Chapter deleted: {} in course: {}", chapterId, courseId);
    }

    private List<LessonResponseDto> getLessonDtos(CourseEntity course, UUID chapterId) {
        boolean canViewFullContent = canUserViewFullContent(course);

        return lessonRepository.findByCourseIdAndChapterIdAndDeletedFalseOrderByCreatedAtAsc(course.getId(), chapterId)
                .stream()
                .map(lesson -> {
                    LessonResponseDto dto = lessonMapper.toDto(lesson);
                    maskPaidContentIfNeeded(lesson, dto, canViewFullContent);
                    return dto;
                })
                .toList();
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

    private ChapterEntity findActiveChapterById(UUID chapterId) {
        return chapterRepository.findByIdAndDeletedFalse(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.chapter.notFound")));
    }

    private void updateCourseAggregateFields(CourseEntity course) {
        course.setTotalLessons(Math.toIntExact(lessonRepository.countByCourseIdAndDeletedFalse(course.getId())));
        course.setTotalDuration(lessonRepository.sumDurationByCourseId(course.getId()));
        courseRepository.save(course);
    }

    private void validateChapterBelongsToCourse(UUID courseId, ChapterEntity chapter) {
        if (!courseId.equals(chapter.getCourseId())) {
            throw new BusinessException(messageProvider.getMessage("exception.chapter.notBelongToCourse"));
        }
    }
}
