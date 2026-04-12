package com.kjt.lms.model.response.lesson;

import com.kjt.lms.common.constants.LessonTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponseDto {

    private UUID id;
    private UUID chapterId;
    private UUID courseId;
    private String title;
    private String description;
    private LessonTypeEnum type;
    private String videoUrl;
    private String videoPublicId;
    private Integer duration;
    private String content;
    private Boolean freePreview;
    private UUID quizId;
}

