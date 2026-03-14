package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "wishlists")
public class WishlistEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID studentId;

    @Column(nullable = false)
    private UUID courseId;
}
