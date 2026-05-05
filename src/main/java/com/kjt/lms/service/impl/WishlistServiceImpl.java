package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.CourseStatusEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.WishlistEntity;
import com.kjt.lms.model.response.course.CourseListItemResponseDto;
import com.kjt.lms.model.response.wishlist.WishlistResponseDto;
import com.kjt.lms.repository.WishlistRepository;
import com.kjt.lms.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl extends BaseService implements WishlistService {

    private static final Set<CourseStatusEnum> WISHLIST_VISIBLE_STATUSES =
            Set.of(CourseStatusEnum.PUBLISHED);

    private final WishlistRepository wishlistRepository;
    private final MessageProvider messageProvider;

    @Override
    @Transactional(readOnly = true)
    public Page<WishlistResponseDto> getMyWishlist(Pageable pageable) {
        UUID studentId = securityUtils.getCurrentUserId();
        return wishlistRepository.findByStudentIdAndDeletedFalseOrderByCreatedAtDesc(studentId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public WishlistResponseDto addToWishlist(UUID courseId) {
        UUID studentId = securityUtils.getCurrentUserId();
        CourseEntity course = getWishlistableCourse(courseId, studentId);

        WishlistEntity wishlist = wishlistRepository.findByStudentIdAndCourseId(studentId, course.getId())
                .map(existing -> {
                    if (!Boolean.TRUE.equals(existing.getDeleted())) {
                        throw new BusinessException(messageProvider.getMessage("exception.wishlist.alreadyExists"));
                    }
                    existing.setDeleted(false);
                    return existing;
                })
                .orElseGet(() -> WishlistEntity.builder()
                        .studentId(studentId)
                        .courseId(course.getId())
                        .build());

        WishlistEntity savedWishlist = wishlistRepository.save(wishlist);
        log.info("User {} added course {} to wishlist", studentId, course.getId());
        return toResponse(savedWishlist, course);
    }

    @Override
    @Transactional
    public void removeFromWishlist(UUID wishlistId) {
        UUID studentId = securityUtils.getCurrentUserId();
        WishlistEntity wishlist = wishlistRepository.findByIdAndStudentIdAndDeletedFalse(wishlistId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.wishlist.notFound")));

        wishlist.setDeleted(true);
        wishlistRepository.save(wishlist);
    }

    @Override
    @Transactional
    public void removeCourseFromWishlist(UUID courseId) {
        UUID studentId = securityUtils.getCurrentUserId();
        WishlistEntity wishlist = wishlistRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.wishlist.notFound")));

        wishlist.setDeleted(true);
        wishlistRepository.save(wishlist);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCourseInWishlist(UUID courseId) {
        UUID studentId = securityUtils.getCurrentUserId();
        return wishlistRepository.existsByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId);
    }

    private CourseEntity getWishlistableCourse(UUID courseId, UUID studentId) {
        CourseEntity course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.course.notFound")));

        if (course.getInstructorId().equals(studentId)) {
            throw new BusinessException(messageProvider.getMessage("exception.wishlist.ownCourse"));
        }

        if (!CommonStatusEnum.ACTIVE.equals(course.getActive())
                || !WISHLIST_VISIBLE_STATUSES.contains(course.getStatus())) {
            throw new BusinessException(messageProvider.getMessage("exception.enrollment.course.notAvailable"));
        }

        return course;
    }

    private WishlistResponseDto toResponse(WishlistEntity wishlist) {
        CourseEntity course = courseRepository.findByIdAndDeletedFalse(wishlist.getCourseId())
                .orElse(null);
        return toResponse(wishlist, course);
    }

    private WishlistResponseDto toResponse(WishlistEntity wishlist, CourseEntity course) {
        return WishlistResponseDto.builder()
                .id(wishlist.getId())
                .courseId(wishlist.getCourseId())
                .createdAt(wishlist.getCreatedAt())
                .course(course == null ? null : toCourseListItem(course))
                .build();
    }

    private CourseListItemResponseDto toCourseListItem(CourseEntity course) {
        String instructorName = userRepository.findById(course.getInstructorId())
                .map(user -> user.getFullName())
                .orElse("Unknown");

        return CourseListItemResponseDto.builder()
                .id(course.getId())
                .title(course.getTitle())
                .shortDescription(course.getShortDescription())
                .thumbnail(course.getThumbnail())
                .price(course.getPrice())
                .discountPrice(course.getDiscountPrice())
                .level(course.getLevel())
                .status(course.getStatus())
                .totalDuration(course.getTotalDuration())
                .totalLessons(course.getTotalLessons())
                .totalStudents(course.getTotalStudents())
                .avgRating(course.getAvgRating())
                .totalReviews(course.getTotalReviews())
                .language(course.getLanguage())
                .createdAt(course.getCreatedAt())
                .instructorName(instructorName)
                .build();
    }
}
