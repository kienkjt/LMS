package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.common.validator.Common;
import com.kjt.lms.model.request.review.CreateReviewRequestDto;
import com.kjt.lms.model.request.review.ReplyReviewRequestDto;
import com.kjt.lms.model.request.review.UpdateReviewRequestDto;
import com.kjt.lms.model.response.review.ReviewResponseDto;
import com.kjt.lms.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Course review management")
public class ReviewController {

    private final ReviewService reviewService;
    private final MessageProvider messageProvider;

    @GetMapping
    @Operation(summary = "Get public reviews of a course")
    public ResponseEntity<APIResponse<Page<ReviewResponseDto>>> getCourseReviews(
            @PathVariable UUID courseId,
            @RequestParam(value = "page", defaultValue = Common.PAGE_DEFAULT) Integer page,
            @RequestParam(value = "pageSize", defaultValue = Common.PAGE_SIZE_DEFAULT) Integer pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<ReviewResponseDto> response = reviewService.getCourseReviews(courseId, pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR')")
    @Operation(summary = "Get current student's review for a course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<ReviewResponseDto>> getMyReview(@PathVariable UUID courseId) {
        ReviewResponseDto response = reviewService.getMyReview(courseId);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR')")
    @Operation(summary = "Create a course review", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<ReviewResponseDto>> createReview(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateReviewRequestDto request) {
        ReviewResponseDto response = reviewService.createReview(courseId, request);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("review.created.success")));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR')")
    @Operation(summary = "Update current student's review", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<ReviewResponseDto>> updateMyReview(
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateReviewRequestDto request) {
        ReviewResponseDto response = reviewService.updateMyReview(courseId, request);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("review.updated.success")));
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR')")
    @Operation(summary = "Delete current student's review", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Void>> deleteMyReview(@PathVariable UUID courseId) {
        reviewService.deleteMyReview(courseId);
        return ResponseEntity.ok(APIResponse.success(null, messageProvider.getMessage("review.deleted.success")));
    }

    @PostMapping("/{reviewId}/reply")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Reply to a course review", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<ReviewResponseDto>> replyReview(
            @PathVariable UUID courseId,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReplyReviewRequestDto request) {
        ReviewResponseDto response = reviewService.replyReview(courseId, reviewId, request);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("review.reply.success")));
    }
}
