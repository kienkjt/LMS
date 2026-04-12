package com.kjt.lms.model.response.course;

import com.kjt.lms.common.constants.CourseLevelEnum;
import com.kjt.lms.common.constants.CourseStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO cho Course Update/Admin Operations
 * Dùng khi cập nhật course hoặc admin xem thông tin course đầy đủ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseUpdateResponseDto {

    private UUID id;
    private UUID instructorId;
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
    private UUID reviewedBy;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdById;
    private UUID updatedById;
}

