package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseEntity;
import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.CourseStatusEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.DuplicateResourceException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.mapper.CourseMapper;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.request.course.CreateCourseRequestDto;
import com.kjt.lms.model.request.course.UpdateCourseRequestDto;
import com.kjt.lms.model.response.CourseResponseDto;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseMapper courseMapper;
    private final MessageProvider messageProvider;

    @Override
    @Transactional
    public CourseResponseDto createCourse(CreateCourseRequestDto request) {
        // Get current instructor ID
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID instructorId = userRepository.findByEmail(email)
                .map(BaseEntity::getId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));

        if (courseRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException(
                    messageProvider.getMessage("exception.course.slugExists"));
        }

        // Create course entity
        CourseEntity course = CourseEntity.builder()
                .instructorId(instructorId)
                .categoryId(request.getCategoryId())
                .title(request.getTitle())
                .slug(request.getSlug())
                .shortDescription(request.getShortDescription())
                .fullDescription(request.getFullDescription())
                .thumbnail(request.getThumbnail())
                .previewVideoUrl(request.getPreviewVideoUrl())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .level(request.getLevel())
                .status(CourseStatusEnum.DRAFT)  // Default to DRAFT
                .totalDuration(request.getTotalDuration())
                .totalLessons(0)
                .totalStudents(0)
                .avgRating(0.0)
                .totalReviews(0)
                .language(request.getLanguage())
                .certificate(request.getCertificate())
                .requirements(request.getRequirements())
                .whatYouWillLearn(request.getWhatYouWillLearn())
                .active(CommonStatusEnum.ACTIVE)
                .build();

        CourseEntity savedCourse = courseRepository.save(course);

        log.info("Course created: {} by instructor: {}", savedCourse.getId(), instructorId);

        return courseMapper.toDto(savedCourse);
    }

    @Override
    @Transactional
    public CourseResponseDto updateCourse(UUID courseId, UpdateCourseRequestDto request) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        // Verify ownership
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID instructorId = userRepository.findByEmail(email)
                .map(BaseEntity::getId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new BusinessException(
                    messageProvider.getMessage("exception.course.notOwner"));
        }

        // Check if slug is changed and already exists
        if (!course.getSlug().equals(request.getSlug()) &&
            courseRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException(
                    messageProvider.getMessage("exception.course.slugExists"));
        }

        // Update fields
        course.setTitle(request.getTitle());
        course.setSlug(request.getSlug());
        course.setShortDescription(request.getShortDescription());
        course.setFullDescription(request.getFullDescription());
        course.setThumbnail(request.getThumbnail());
        course.setPreviewVideoUrl(request.getPreviewVideoUrl());
        course.setPrice(request.getPrice());
        course.setDiscountPrice(request.getDiscountPrice());
        course.setLevel(request.getLevel());
        course.setTotalDuration(request.getTotalDuration());
        course.setLanguage(request.getLanguage());
        course.setCertificate(request.getCertificate());
        course.setRequirements(request.getRequirements());
        course.setWhatYouWillLearn(request.getWhatYouWillLearn());
        course.setCategoryId(request.getCategoryId());

        CourseEntity updatedCourse = courseRepository.save(course);

        log.info("Course updated: {} by instructor: {}", courseId, instructorId);

        return courseMapper.toDto(updatedCourse);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponseDto getCourseById(UUID courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        return courseMapper.toDto(course);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponseDto getCourseBySlug(String slug) {
        CourseEntity course = courseRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        return courseMapper.toDto(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> getInstructorCourses(Pageable pageable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID instructorId = userRepository.findByEmail(email)
                .map(u -> u.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));

        return courseRepository.findByInstructorId(instructorId, pageable)
                .map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> getPublishedCourses(Pageable pageable) {
        return courseRepository.findByStatusAndActive(
                CourseStatusEnum.PUBLISHED,
                CommonStatusEnum.ACTIVE,
                pageable
        ).map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> searchCourses(String keyword, Pageable pageable) {
        return courseRepository.searchByTitle(
                keyword,
                CourseStatusEnum.PUBLISHED,
                CommonStatusEnum.ACTIVE,
                pageable
        ).map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> getCoursesByCategory(UUID categoryId, Pageable pageable) {
        return courseRepository.findByCategoryIdAndStatusAndActive(
                categoryId,
                CourseStatusEnum.PUBLISHED,
                CommonStatusEnum.ACTIVE,
                pageable
        ).map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> getTopRatedCourses(Pageable pageable) {
        return courseRepository.findTopRatedCourses(
                CourseStatusEnum.PUBLISHED,
                CommonStatusEnum.ACTIVE,
                pageable
        ).map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> getTrendingCourses(Pageable pageable) {
        return courseRepository.findTrendingCourses(
                CourseStatusEnum.PUBLISHED,
                CommonStatusEnum.ACTIVE,
                pageable
        ).map(courseMapper::toDto);
    }

    @Override
    @Transactional
    public CourseResponseDto publishCourse(UUID courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.course.notFound")));

        // Verify ownership
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID instructorId = userRepository.findByEmail(email)
                .map(u -> u.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new BusinessException(
                    messageProvider.getMessage("exception.course.notOwner"));
        }

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

        // Verify ownership
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID instructorId = userRepository.findByEmail(email)
                .map(BaseEntity::getId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new BusinessException(
                    messageProvider.getMessage("exception.course.notOwner"));
        }

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

        // Verify ownership
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID instructorId = userRepository.findByEmail(email)
                .map(u -> u.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new BusinessException(
                    messageProvider.getMessage("exception.course.notOwner"));
        }

        course.setActive(CommonStatusEnum.DELETED);
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
}

