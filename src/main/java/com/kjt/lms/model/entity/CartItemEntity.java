package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
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
@Table(name = "cart_items")
public class CartItemEntity extends BaseEntity {

    @Column(name = "cart_id", nullable = false)
    private UUID cartId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
