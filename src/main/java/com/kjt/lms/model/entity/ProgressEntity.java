package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "lesson_progress")
public class ProgressEntity extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "last_position")
    private Integer lastPosition;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_watched_at")
    private LocalDateTime lastWatchedAt;
}