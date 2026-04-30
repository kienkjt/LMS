package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.CourseStatusEnum;
import com.kjt.lms.common.constants.YesNoEnum;
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
import com.kjt.lms.model.response.chapter.ChapterResponseDto;
import com.kjt.lms.model.response.course.CourseCreateResponseDto;
import com.kjt.lms.model.response.course.CourseDetailResponseDto;
import com.kjt.lms.model.response.course.CourseListItemResponseDto;
import com.kjt.lms.model.response.course.CourseUpdateResponseDto;
import com.kjt.lms.model.response.lesson.LessonResponseDto;
import com.kjt.lms.model.response.media.MediaUploadResponse;
import com.kjt.lms.repository.CategoryRepository;
import com.kjt.lms.repository.ChapterRepository;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.LessonRepository;
import com.kjt.lms.service.CourseService;
import com.kjt.lms.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl extends BaseService implements CourseService {

    private static final Set<CourseStatusEnum> PUBLIC_VISIBLE_STATUSES = Set.of(CourseStatusEnum.PUBLISHED, CourseStatusEnum.APPROVED);

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final CourseMapper courseMapper;
    private final MessageProvider messageProvider;
    private final MediaStorageService mediaStorageService;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public CourseCreateResponseDto createCourse(CreateCourseRequestDto request) {
        UUID instructorId = securityUtils.getCurrentUserId();
        validateCategoryId(request.getCategoryId());

        CourseEntity savedCourse = courseRepository.save(courseMapper.toCreateEntity(request, instructorId));
        log.info("Course created: {} by instructor: {}", savedCourse.getId(), instructorId);
        return courseRepository.findCreateResponseById(savedCourse.getId())
                .orElseGet(() -> courseMapper.toCreateResponse(savedCourse));
    }

    @Override
    @Transactional
    public CourseUpdateResponseDto updateCourse(UUID courseId, UpdateCourseRequestDto request) {
        CourseEntity course = getOwnedCourse(courseId);
        validateCategoryId(request.getCategoryId());

        courseMapper.updateCourseFromRequest(request, course);
        CourseEntity updatedCourse = courseRepository.save(course);

        log.info("Course updated: {}", courseId);
        return courseMapper.toDto(updatedCourse);
    }

    @Override
    public CourseDetailResponseDto getCourseById(UUID courseId) {
        CourseEntity course = getAccessibleCourse(courseId);

        CourseDetailResponseDto detailResponse = buildCourseDetailResponse(course);
        courseRepository.findInstructorNameByCourseId(courseId).ifPresent(detailResponse::setInstructorName);
        return detailResponse;
    }

    @Override
    public Page<CourseListItemResponseDto> getInstructorCourses(Pageable pageable) {
        UUID instructorId = securityUtils.getCurrentUserId();
        return courseRepository.findInstructorCoursesWithInstructorName(instructorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseListItemResponseDto> searchCourses(SearchCourseRequest request, Pageable pageable) {
        SearchCourseRequest normalizedRequest = request == null ? SearchCourseRequest.builder().build() : request;

        Page<CourseListItemResponseDto> results = courseRepository.searchPublicWithInstructorName(
                normalizedRequest.getKeyword(),
                PUBLIC_VISIBLE_STATUSES,
                CommonStatusEnum.ACTIVE,
                pageable
        );

        if (results.hasContent()) {
            log.info("Search completed - found {} courses", results.getTotalElements());
        } else {
            log.info("Search completed - no courses found");
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseListItemResponseDto> searchManagedCourses(SearchCourseRequest request, Pageable pageable) {
        SearchCourseRequest normalizedRequest = request == null ? SearchCourseRequest.builder().build() : request;
        UUID instructorId = securityUtils.isAdmin() ? null : securityUtils.getCurrentUserId();

        return courseRepository.searchManagedWithInstructorName(
                normalizedRequest.getKeyword(),
                normalizedRequest.getCourseStatus(),
                normalizedRequest.getCourseLevel(),
                normalizedRequest.getActive(),
                instructorId,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseListItemResponseDto> getCoursesByCategory(UUID categoryId, Pageable pageable) {
        // Validate category exists AND not deleted
        categoryRepository.findByIdAndDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageProvider.getMessage("exception.category.notFound")));

        return courseRepository.findCoursesByCategoryWithInstructorName(
                categoryId,
                PUBLIC_VISIBLE_STATUSES,
                CommonStatusEnum.ACTIVE,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseListItemResponseDto> getTopRatedCourses(Pageable pageable) {
        return courseRepository.findTopRatedPublicCourses(PUBLIC_VISIBLE_STATUSES, CommonStatusEnum.ACTIVE, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseListItemResponseDto> getTrendingCourses(Pageable pageable) {
        return courseRepository.findTrendingPublicCourses(PUBLIC_VISIBLE_STATUSES, CommonStatusEnum.ACTIVE, pageable);
    }

    @Override
    @Transactional
    public CourseUpdateResponseDto publishCourse(UUID courseId) {
        CourseEntity course = getOwnedCourse(courseId);

        validateCoursePublishableState(course);

        log.info("Publishing course: {} (from {} to PUBLISHED)", courseId, course.getStatus());
        course.setStatus(CourseStatusEnum.PUBLISHED);
        CourseEntity updatedCourse = courseRepository.save(course);

        log.info("Course published: {} by instructor: {}", courseId, securityUtils.getCurrentUserId());
        return courseMapper.toDto(updatedCourse);
    }

    @Override
    @Transactional
    public CourseUpdateResponseDto unpublishCourse(UUID courseId) {
        CourseEntity course = getOwnedCourse(courseId);

        if (course.getStatus() != CourseStatusEnum.PUBLISHED) {
            throw new BusinessException(
                    String.format("Cannot unpublish course in %s status", course.getStatus()));
        }

        log.info("Unpublishing course: {} (from PUBLISHED to DRAFT)", courseId);
        course.setStatus(CourseStatusEnum.DRAFT);
        CourseEntity updatedCourse = courseRepository.save(course);

        log.info("Course unpublished: {} by instructor: {}", courseId, securityUtils.getCurrentUserId());
        return courseMapper.toDto(updatedCourse);
    }

    @Override
    @Transactional
    public void deleteCourse(UUID courseId) {
        CourseEntity course = getOwnedCourse(courseId);

        course.setDeleted(true);
        courseRepository.save(course);

        log.info("Course deleted: {} by instructor: {}", courseId, securityUtils.getCurrentUserId());
    }

    @Override
    @Transactional
    public CourseUpdateResponseDto approveCourse(UUID courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        // State machine: only PUBLISHED can be approved
        if (course.getStatus() != CourseStatusEnum.PUBLISHED) {
            throw new BusinessException(
                    String.format("Cannot approve course in %s status. Only PUBLISHED courses can be approved.",
                            course.getStatus()));
        }

        course.setStatus(CourseStatusEnum.APPROVED);
        CourseEntity updatedCourse = courseRepository.save(course);

        log.info("Course approved: {}", courseId);

        return courseMapper.toDto(updatedCourse);
    }

    @Override
    @Transactional
    public CourseUpdateResponseDto rejectCourse(UUID courseId, String reason) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        if (course.getStatus() != CourseStatusEnum.PUBLISHED) {
            throw new BusinessException(
                    String.format("Cannot reject course in %s status. Only PUBLISHED courses can be rejected.",
                            course.getStatus()));
        }

        course.setStatus(CourseStatusEnum.REJECTED);
        course.setRejectReason(reason);
        CourseEntity updatedCourse = courseRepository.save(course);

        log.info("Course rejected: {} with reason: {}", courseId, reason);

        return courseMapper.toDto(updatedCourse);
    }

    @Override
    @Transactional
    public CourseUpdateResponseDto uploadCourseImage(UUID courseId, MultipartFile file) {
        CourseEntity course = getOwnedCourse(courseId);

        try {
            MediaUploadResponse uploadResponse = mediaStorageService.uploadCourseImage(file);

            if (course.getThumbnail() != null && !course.getThumbnail().isEmpty()) {
                deleteOldMedia(course.getThumbnail(), "image");
            }

            course.setThumbnail(uploadResponse.getSecureUrl());
            course.setThumbnailPublicId(uploadResponse.getPublicId());
            CourseEntity updatedCourse = courseRepository.save(course);

            log.info("Course image uploaded: {} by instructor: {}", courseId, securityUtils.getCurrentUserId());
            return courseMapper.toDto(updatedCourse);

        } catch (Exception ex) {
            log.error("Course image upload failed: {} - {}", courseId, ex.getMessage());
            throw new BusinessException(messageProvider.getMessage("media.course.image.upload.failed"));
        }
    }

    @Override
    @Transactional
    public CourseUpdateResponseDto uploadCoursePreviewVideo(UUID courseId, MultipartFile file) {
        CourseEntity course = getOwnedCourse(courseId);

        try {
            MediaUploadResponse uploadResponse = mediaStorageService.uploadCourseVideo(file);

            if (course.getPreviewVideoUrl() != null && !course.getPreviewVideoUrl().isEmpty()) {
                deleteOldMedia(course.getPreviewVideoUrl(), "video");
            }

            course.setPreviewVideoUrl(uploadResponse.getSecureUrl());
            course.setPreviewVideoPublicId(uploadResponse.getPublicId());
            CourseEntity updatedCourse = courseRepository.save(course);

            log.info("Course preview video uploaded: {} by instructor: {}", courseId, securityUtils.getCurrentUserId());
            return courseMapper.toDto(updatedCourse);

        } catch (Exception ex) {
            log.error("Course preview video upload failed: {} - {}", courseId, ex.getMessage());
            throw new BusinessException(messageProvider.getMessage("media.course.video.upload.failed"));
        }
    }

    private CourseDetailResponseDto buildCourseDetailResponse(CourseEntity course) {
        boolean canViewFullContent = canUserViewFullContent(course);

        List<ChapterEntity> chapters = chapterRepository.findByCourseIdAndDeletedFalseOrderByCreatedAtAsc(course.getId());
        List<LessonEntity> allLessons = lessonRepository.findByCourseIdAndDeletedFalseOrderByCreatedAtAsc(course.getId());

        Map<UUID, List<LessonEntity>> lessonsByChapter = allLessons.stream()
                .collect(Collectors.groupingBy(LessonEntity::getChapterId));

        List<ChapterResponseDto> chapterResponses = chapters.stream()
                .map(chapter -> buildChapterResponse(chapter, lessonsByChapter, canViewFullContent))
                .toList();

        CourseDetailResponseDto detail = courseMapper.toDetailDto(course);
        detail.setChapters(chapterResponses);
        return detail;
    }

    private ChapterResponseDto buildChapterResponse(
            ChapterEntity chapter,
            Map<UUID, List<LessonEntity>> lessonsByChapter,
            boolean canViewFullContent) {
        List<LessonResponseDto> lessons = lessonsByChapter
                .getOrDefault(chapter.getId(), Collections.emptyList())
                .stream()
                .map(lesson -> {
                    LessonResponseDto dto = courseMapper.toLessonDto(lesson);
                    maskPaidContentIfNeeded(lesson, dto, canViewFullContent);
                    return dto;
                })
                .toList();

        ChapterResponseDto chapterResponse = courseMapper.toChapterDto(chapter);
        chapterResponse.setLessons(lessons);
        return chapterResponse;
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

    private void validateCategoryId(UUID categoryId) {
        if (categoryId == null) {
            return;
        }

        categoryRepository.findByIdAndDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.category.notFound")));
    }


    private CourseEntity getOwnedCourse(UUID courseId) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);
        return course;
    }

    private CourseEntity getAccessibleCourse(UUID courseId) {
        CourseEntity course = findActiveCourseById(courseId);
        if (securityUtils.isAdmin()) {
            return course;
        }

        UUID currentUserId = getCurrentUserIdIfAuthenticated();
        if (course.getInstructorId().equals(currentUserId)) {
            return course;
        }

        if (isPubliclyVisible(course)) {
            return course;
        }

        throw new ResourceNotFoundException(messageProvider.getMessage("exception.course.notFound"));
    }


    private void validateCoursePublishableState(CourseEntity course) {
        if (!isPublishableStatus(course.getStatus())) {
            throw new BusinessException(
                    String.format("Cannot publish course in %s status. Only DRAFT or REJECTED can be published.",
                            course.getStatus()));
        }
    }

    private boolean isPublishableStatus(CourseStatusEnum status) {
        return status == CourseStatusEnum.DRAFT || status == CourseStatusEnum.REJECTED;
    }

    private boolean isPubliclyVisible(CourseEntity course) {
        return CommonStatusEnum.ACTIVE.equals(course.getActive())
                && PUBLIC_VISIBLE_STATUSES.contains(course.getStatus());
    }

    private UUID getCurrentUserIdIfAuthenticated() {
        try {
            return securityUtils.getCurrentUserId();
        } catch (ResourceNotFoundException ex) {
            return null;
        }
    }


    private void deleteOldMedia(String mediaUrl, String resourceType) {
        try {
            String publicId = extractPublicIdFromUrl(mediaUrl);
            if (publicId != null && !publicId.isEmpty()) {
                mediaStorageService.deleteMedia(publicId, resourceType);
            }
        } catch (Exception e) {
            log.warn("Failed to delete old {}: {} - {}", resourceType, mediaUrl, e.getMessage());
        }
    }

    private String extractPublicIdFromUrl(String cloudinaryUrl) {
        if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
            return null;
        }
        try {
            String[] parts = cloudinaryUrl.split("/");
            if (parts.length > 0) {
                String lastPart = parts[parts.length - 1];
                int dotIndex = lastPart.lastIndexOf(".");
                if (dotIndex > 0) {
                    lastPart = lastPart.substring(0, dotIndex);
                }
                if (cloudinaryUrl.contains("lms/courses/")) {
                    int folderIndex = cloudinaryUrl.indexOf("lms/courses/");
                    String folderPath = cloudinaryUrl.substring(folderIndex);
                    return folderPath.substring(0, folderPath.lastIndexOf("."));
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract public ID from URL: {}", cloudinaryUrl);
        }
        return null;
    }
}
