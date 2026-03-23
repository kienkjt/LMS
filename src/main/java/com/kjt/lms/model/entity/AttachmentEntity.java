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
@Table(name = "attachments")
public class AttachmentEntity extends BaseEntity {

    @Column(name = "lesson_id",nullable = false)
    private UUID lessonId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;
}
