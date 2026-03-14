package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import com.kjt.lms.common.constants.LessonTypeEnum;
import com.kjt.lms.common.constants.LessonTypeEnumConverter;
import com.kjt.lms.common.constants.YesNoEnum;
import com.kjt.lms.common.constants.YesNoEnumConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "lessons")
public class LessonEntity extends BaseEntity {

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "type", nullable = false)
    @Convert(converter = LessonTypeEnumConverter.class)
    private LessonTypeEnum type;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "video_public_id", length = 500)
    private String videoPublicId;

    @Column(name = "duration")
    private Integer duration = 0;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "free_preview", nullable = false)
    @Convert(converter = YesNoEnumConverter.class)
    private YesNoEnum freePreview;

    @Column(name = "quiz_id")
    private UUID quizId;
}