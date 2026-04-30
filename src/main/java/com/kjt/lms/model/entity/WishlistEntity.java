package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "wishlists",
        indexes = {
                @Index(name = "idx_wishlist_student", columnList = "student_id"),
                @Index(name = "idx_wishlist_course", columnList = "course_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wishlist_student_course", columnNames = {"student_id", "course_id"})
        }
)
public class WishlistEntity extends BaseEntity {

    @Column(name = "student_id", nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID studentId;

    @Column(name = "course_id", nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID courseId;
}
