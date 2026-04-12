package com.kjt.lms.model.response.course;

import com.kjt.lms.common.constants.CourseLevelEnum;
import com.kjt.lms.common.constants.CourseStatusEnum;
import com.kjt.lms.model.response.chapter.ChapterResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO cho instructor xem khóa học của mình
 * Dùng khi instructor view course với các stats bổ sung
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorCourseResponseDto {

    private UUID id;
    private UUID categoryId;
    private String title;
    private String shortDescription;
    private String fullDescription;
    private String thumbnail;
    private String previewVideoUrl;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private CourseLevelEnum level;
    private CourseStatusEnum status;
    private Integer totalDuration;
    private Integer totalLessons;
    private Integer totalStudents;
    private Double avgRating;
    private Integer totalReviews;
    private String language;
    private String certificate;
    private String requirements;
    private String whatYouWillLearn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChapterResponseDto> chapters;

    // Instructor-specific stats
    private Double totalEarnings;
    private Integer enrollmentStatus;
    private Long totalLessonViews;
}

