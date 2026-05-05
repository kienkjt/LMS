package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.NotificationTypeEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.ReviewEntity;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.model.request.review.CreateReviewRequestDto;
import com.kjt.lms.model.request.review.ReplyReviewRequestDto;
import com.kjt.lms.model.request.review.UpdateReviewRequestDto;
import com.kjt.lms.model.response.review.ReviewResponseDto;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.ReviewRepository;
import com.kjt.lms.service.NotificationService;
import com.kjt.lms.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl extends BaseService implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MessageProvider messageProvider;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getCourseReviews(UUID courseId, Pageable pageable) {
        findActiveCourseById(courseId);
        return reviewRepository.findByCourseIdAndActiveAndDeletedFalseOrderByCreatedAtDesc(
                courseId,
                CommonStatusEnum.ACTIVE,
                pageable
        ).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDto getMyReview(UUID courseId) {
        UUID studentId = securityUtils.getCurrentUserId();
        ReviewEntity review = reviewRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.review.notFound")));
        return toResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponseDto createReview(UUID courseId, CreateReviewRequestDto request) {
        UUID studentId = securityUtils.getCurrentUserId();
        CourseEntity course = findActiveCourseById(courseId);
        validateStudentCanReview(studentId, course);

        ReviewEntity review = reviewRepository.findByStudentIdAndCourseId(studentId, courseId)
                .map(existing -> {
                    if (!Boolean.TRUE.equals(existing.getDeleted())) {
                        throw new BusinessException(messageProvider.getMessage("exception.review.alreadyExists"));
                    }
                    existing.setDeleted(false);
                    existing.setActive(CommonStatusEnum.ACTIVE);
                    existing.setInstructorReply(null);
                    existing.setRepliedAt(null);
                    applyReviewFields(existing, request.getRating(), request.getComment());
                    return existing;
                })
                .orElseGet(() -> ReviewEntity.builder()
                        .studentId(studentId)
                        .courseId(courseId)
                        .rating(request.getRating())
                        .comment(request.getComment())
                        .active(CommonStatusEnum.ACTIVE)
                        .build());

        ReviewEntity savedReview = reviewRepository.save(review);
        refreshCourseReviewStats(course);
        notificationService.notifyUser(
                course.getInstructorId(),
                NotificationTypeEnum.NEW_REVIEW,
                messageProvider.getMessage("notification.review.received.title"),
                messageProvider.getMessage("notification.review.received.message", course.getTitle()),
                savedReview.getId(),
                "REVIEW"
        );
        log.info("Student {} reviewed course {}", studentId, courseId);
        return toResponse(savedReview);
    }

    @Override
    @Transactional
    public ReviewResponseDto updateMyReview(UUID courseId, UpdateReviewRequestDto request) {
        UUID studentId = securityUtils.getCurrentUserId();
        CourseEntity course = findActiveCourseById(courseId);
        ReviewEntity review = reviewRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.review.notFound")));

        applyReviewFields(review, request.getRating(), request.getComment());
        ReviewEntity savedReview = reviewRepository.save(review);
        refreshCourseReviewStats(course);
        return toResponse(savedReview);
    }

    @Override
    @Transactional
    public void deleteMyReview(UUID courseId) {
        UUID studentId = securityUtils.getCurrentUserId();
        CourseEntity course = findActiveCourseById(courseId);
        ReviewEntity review = reviewRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.review.notFound")));

        review.setDeleted(true);
        reviewRepository.save(review);
        refreshCourseReviewStats(course);
    }

    @Override
    @Transactional
    public ReviewResponseDto replyReview(UUID courseId, UUID reviewId, ReplyReviewRequestDto request) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);

        ReviewEntity review = reviewRepository.findByIdAndCourseIdAndDeletedFalse(reviewId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.review.notFound")));

        review.setInstructorReply(request.getReply());
        review.setRepliedAt(LocalDateTime.now());
        return toResponse(reviewRepository.save(review));
    }

    private void validateStudentCanReview(UUID studentId, CourseEntity course) {
        if (course.getInstructorId().equals(studentId)) {
            throw new BusinessException(messageProvider.getMessage("exception.review.ownCourse"));
        }

        if (!enrollmentRepository.existsByStudentIdAndCourseIdAndDeletedFalse(studentId, course.getId())) {
            throw new BusinessException(messageProvider.getMessage("exception.enrollment.required"));
        }
    }

    private void applyReviewFields(ReviewEntity review, Integer rating, String comment) {
        review.setRating(rating);
        review.setComment(comment);
    }

    private void refreshCourseReviewStats(CourseEntity course) {
        Double average = reviewRepository.calculateAverageRating(course.getId(), CommonStatusEnum.ACTIVE);
        long total = reviewRepository.countByCourseIdAndActiveAndDeletedFalse(course.getId(), CommonStatusEnum.ACTIVE);

        course.setAvgRating(average == null ? 0.0 : Math.round(average * 10.0) / 10.0);
        course.setTotalReviews(Math.toIntExact(total));
        courseRepository.save(course);
    }

    private ReviewResponseDto toResponse(ReviewEntity review) {
        UserEntity student = userRepository.findById(review.getStudentId()).orElse(null);
        return ReviewResponseDto.builder()
                .id(review.getId())
                .studentId(review.getStudentId())
                .studentName(student == null ? "Unknown" : student.getFullName())
                .studentAvatar(student == null ? null : student.getAvatar())
                .courseId(review.getCourseId())
                .rating(review.getRating())
                .comment(review.getComment())
                .instructorReply(review.getInstructorReply())
                .repliedAt(review.getRepliedAt())
                .active(review.getActive())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
