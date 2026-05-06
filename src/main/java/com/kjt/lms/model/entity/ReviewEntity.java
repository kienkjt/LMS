package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.CommonStatusEnumConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "reviews")
public class ReviewEntity extends BaseEntity {

    @Column(name = "student_id", nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID studentId;

    @Column(name = "course_id", nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID courseId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "instructor_reply", columnDefinition = "TEXT")
    private String instructorReply;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "active", nullable = false)
    @Convert(converter = CommonStatusEnumConverter.class)
    @Builder.Default
    private CommonStatusEnum active = CommonStatusEnum.ACTIVE;
}
