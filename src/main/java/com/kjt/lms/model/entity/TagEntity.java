package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tags")
public class TagEntity extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 80)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "course_count", nullable = false)
    private Integer courseCount;
}
