package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import com.kjt.lms.common.constants.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "courses")
public class CourseEntity extends BaseEntity {

    @Column(name = "instructor_id", nullable = false)
    private UUID instructorId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(name = "full_description", columnDefinition = "LONGTEXT")
    private String fullDescription;

    @Column(name = "thumbnail", length = 500)
    private String thumbnail;

    @Column(name = "preview_video_url", length = 500)
    private String previewVideoUrl;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "discount_price", precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @Column(name = "level", nullable = false)
    @Convert(converter = CourseLevelEnumConverter.class)
    private CourseLevelEnum level;

    @Column(name = "status", nullable = false)
    @Convert(converter = CourseStatusEnumConverter.class)
    private CourseStatusEnum status;

    @Column(name = "total_duration")
    private Integer totalDuration;

    @Column(name = "total_lessons")
    private Integer totalLessons;

    @Column(name = "total_students")
    private Integer totalStudents;

    @Column(name = "avg_rating")
    private Double avgRating;

    @Column(name = "total_reviews")
    private Integer totalReviews;

    @Column(name = "language", length = 50)
    private String language;

    @Column(name = "certificate", length = 100)
    private String certificate;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "what_you_will_learn", columnDefinition = "TEXT")
    private String whatYouWillLearn;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "active", nullable = false)
    @Convert(converter = CommonStatusEnumConverter.class)
    @Builder.Default
    private CommonStatusEnum active = CommonStatusEnum.ACTIVE;
}