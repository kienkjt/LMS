package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseEntity;
import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.CourseStatusEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.mapper.CourseMapper;
import com.kjt.lms.model.entity.ChapterEntity;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.LessonEntity;
import com.kjt.lms.model.request.course.CreateCourseRequestDto;
import com.kjt.lms.model.request.course.SearchCourseRequest;
import com.kjt.lms.model.request.course.UpdateCourseRequestDto;
import com.kjt.lms.model.response.ChapterResponseDto;
import com.kjt.lms.model.response.CourseCreateResponseDto;
import com.kjt.lms.model.response.CourseDetailResponseDto;
import com.kjt.lms.model.response.CourseListItemResponseDto;
import com.kjt.lms.model.response.CourseResponseDto;
import com.kjt.lms.model.response.LessonResponseDto;
import com.kjt.lms.repository.CategoryRepository;
import com.kjt.lms.repository.ChapterRepository;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.LessonRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final CourseMapper courseMapper;
    private final MessageProvider messageProvider;

    @Override
    @Transactional
    public CourseCreateResponseDto createCourse(CreateCourseRequestDto request) {
        UUID instructorId = getCurrentUserId();
        validateCategoryId(request.getCategoryId());

        CourseEntity savedCourse = courseRepository.save(courseMapper.toCreateEntity(request, instructorId));

        log.info("Course created: {} by instructor: {}", savedCourse.getId(), instructorId);
        return courseMapper.toCreateResponse(savedCourse);
    }

    @Override
    @Transactional
    public CourseResponseDto updateCourse(UUID courseId, UpdateCourseRequestDto request) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        UUID instructorId = getCurrentUserId();
        validateCourseOwnership(course, instructorId);
        validateCategoryId(request.getCategoryId());

        courseMapper.updateCourseFromRequest(request, course);
        CourseEntity updatedCourse = courseRepository.save(course);

        log.info("Course updated: {} by instructor: {}", courseId, instructorId);
        return courseMapper.toDto(updatedCourse);
    }

    @Override
    public CourseDetailResponseDto getCourseById(UUID courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        return buildCourseDetailResponse(course);
    }

    @Override
    public Page<CourseListItemResponseDto> getInstructorCourses(Pageable pageable) {
        UUID instructorId = getCurrentUserId();
        return courseRepository.findByInstructorId(instructorId, pageable)
                .map(courseMapper::toListItemDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseListItemResponseDto> searchCourses(SearchCourseRequest request, Pageable pageable) {

        Page<CourseListItemResponseDto> results = courseRepository.search(
                request.getKeyword(),
                request.getCourseStatus(),
                request.getCourseLevel(),
                request.getActive(),
                pageable
        ).map(courseMapper::toListItemDto);

        if (results.hasContent()) {
            log.info("Search completed - found {} courses", results.getTotalElements());
        } else {
            log.info("Search completed - no courses found");
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseListItemResponseDto> getCoursesByCategory(UUID categoryId, Pageable pageable) {
        // Validate category exists AND not deleted
        categoryRepository.findByIdAndDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageProvider.getMessage("exception.category.notFound")));

        // Always filter PUBLISHED + ACTIVE (security enforcement)
        return courseRepository.findCoursesByCategory(
                categoryId,
                CourseStatusEnum.PUBLISHED,
                CommonStatusEnum.ACTIVE,
                pageable
        ).map(courseMapper::toListItemDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseListItemResponseDto> getTopRatedCourses(Pageable pageable) {
        return courseRepository.search(
                null,
                CourseStatusEnum.PUBLISHED,
                null,
                CommonStatusEnum.ACTIVE,
                pageable
        ).map(courseMapper::toListItemDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseListItemResponseDto> getTrendingCourses(Pageable pageable) {
        return courseRepository.search(
                null,
                CourseStatusEnum.PUBLISHED,
                null,
                CommonStatusEnum.ACTIVE,
                pageable
        ).map(courseMapper::toListItemDto);
    }

    @Override
    @Transactional
    public CourseResponseDto publishCourse(UUID courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        UUID instructorId = getCurrentUserId();
        validateCourseOwnership(course, instructorId);

        // Validate status workflow: check if already published
        if (course.getStatus() == CourseStatusEnum.PUBLISHED) {
            throw new BusinessException(
                    messageProvider.getMessage("exception.course.alreadyPublished"));
        }

        // Validate only DRAFT or REJECTED can be published
        if (!isPublishableStatus(course.getStatus())) {
            throw new BusinessException(
                    String.format("Cannot publish course in %s status", course.getStatus()));
        }

        log.info("Publishing course: {} (from {} to PUBLISHED)", courseId, course.getStatus());

        course.setStatus(CourseStatusEnum.PUBLISHED);
        CourseEntity updatedCourse = courseRepository.save(course);

        log.info("Course published: {} by instructor: {}", courseId, instructorId);
        return courseMapper.toDto(updatedCourse);
    }

    @Override
    @Transactional
    public CourseResponseDto unpublishCourse(UUID courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        UUID instructorId = getCurrentUserId();
        validateCourseOwnership(course, instructorId);

        // Validate status workflow: only PUBLISHED can be unpublished
        if (course.getStatus() != CourseStatusEnum.PUBLISHED) {
            throw new BusinessException(
                    String.format("Cannot unpublish course in %s status", course.getStatus()));
        }

        log.info("Unpublishing course: {} (from PUBLISHED to DRAFT)", courseId);

        course.setStatus(CourseStatusEnum.DRAFT);
        CourseEntity updatedCourse = courseRepository.save(course);

        log.info("Course unpublished: {} by instructor: {}", courseId, instructorId);
        return courseMapper.toDto(updatedCourse);
    }

    @Override
    @Transactional
    public void deleteCourse(UUID courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        UUID instructorId = getCurrentUserId();
        validateCourseOwnership(course, instructorId);

        course.setDeleted(true);
        courseRepository.save(course);

        log.info("Course deleted: {} by instructor: {}", courseId, instructorId);
    }

    @Override
    @Transactional
    public CourseResponseDto approveCourse(UUID courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        if (course.getStatus() == CourseStatusEnum.APPROVED) {
            throw new BusinessException(
                    messageProvider.getMessage("exception.course.alreadyApproved"));
        }

        course.setStatus(CourseStatusEnum.APPROVED);
        CourseEntity updatedCourse = courseRepository.save(course);

        log.info("Course approved: {}", courseId);

        return courseMapper.toDto(updatedCourse);
    }

    @Override
    @Transactional
    public CourseResponseDto rejectCourse(UUID courseId, String reason) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        course.setStatus(CourseStatusEnum.REJECTED);
        course.setRejectReason(reason);
        CourseEntity updatedCourse = courseRepository.save(course);

        log.info("Course rejected: {} with reason: {}", courseId, reason);

        return courseMapper.toDto(updatedCourse);
    }

    private CourseDetailResponseDto buildCourseDetailResponse(CourseEntity course) {
        List<ChapterEntity> chapters = chapterRepository.findByCourseIdAndDeletedFalseOrderByCreatedAtAsc(course.getId());
        Map<UUID, List<LessonEntity>> lessonsByChapter = lessonRepository.findByCourseIdAndDeletedFalseOrderByCreatedAtAsc(course.getId())
                .stream()
                .collect(Collectors.groupingBy(LessonEntity::getChapterId));

        List<ChapterResponseDto> chapterResponses = chapters.stream()
                .map(chapter -> {
                    List<LessonResponseDto> lessons = lessonsByChapter
                            .getOrDefault(chapter.getId(), Collections.emptyList())
                            .stream()
                            .map(courseMapper::toLessonDto)
                            .toList();

                    ChapterResponseDto chapterResponse = courseMapper.toChapterDto(chapter);
                    chapterResponse.setLessons(lessons);
                    return chapterResponse;
                })
                .toList();

        CourseDetailResponseDto detail = courseMapper.toDetailDto(course);
        detail.setChapters(chapterResponses);
        return detail;
    }

    private UUID getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(BaseEntity::getId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));
    }

    private void validateCategoryId(UUID categoryId) {
        if (categoryId == null) {
            return;
        }

        categoryRepository.findByIdAndDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.category.notFound")));
    }

    private void validateCourseOwnership(CourseEntity course, UUID currentUserId) {
        if (!course.getInstructorId().equals(currentUserId)) {
            throw new BusinessException(messageProvider.getMessage("exception.course.notOwner"));
        }
    }

    private boolean isPublishableStatus(CourseStatusEnum status) {
        return status == CourseStatusEnum.DRAFT || status == CourseStatusEnum.REJECTED;
    }
}
