package com.kjt.lms.service;

import com.kjt.lms.model.request.review.CreateReviewRequestDto;
import com.kjt.lms.model.request.review.ReplyReviewRequestDto;
import com.kjt.lms.model.request.review.UpdateReviewRequestDto;
import com.kjt.lms.model.response.review.ReviewResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    Page<ReviewResponseDto> getCourseReviews(UUID courseId, Pageable pageable);

    ReviewResponseDto getMyReview(UUID courseId);

    ReviewResponseDto createReview(UUID courseId, CreateReviewRequestDto request);

    ReviewResponseDto updateMyReview(UUID courseId, UpdateReviewRequestDto request);

    void deleteMyReview(UUID courseId);

    ReviewResponseDto replyReview(UUID courseId, UUID reviewId, ReplyReviewRequestDto request);
}
