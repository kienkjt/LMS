package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "order_items")
public class OrderItemEntity extends BaseEntity {

    @Column(name = "order_id", nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID orderId;

    @Column(name = "course_id", nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID courseId;

    @Column(name = "course_title", nullable = false, length = 200)
    private String courseTitle;

    @Column(name = "course_thumbnail", length = 500)
    private String courseThumbnail;

    @Column(name = "original_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "paid_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidPrice;

    @Column(name = "instructor_revenue", precision = 12, scale = 2)
    private BigDecimal instructorRevenue;

    @Column(name = "instructor_id", length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID instructorId;
}