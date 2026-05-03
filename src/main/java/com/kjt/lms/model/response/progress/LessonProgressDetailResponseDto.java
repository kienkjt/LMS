package com.kjt.lms.model.response.progress;

import com.kjt.lms.common.constants.LessonTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgressDetailResponseDto {

    private UUID lessonId;
    private UUID chapterId;
    private String lessonTitle;
    private LessonTypeEnum lessonType;
    private Integer duration;
    private Boolean completed;
    private LocalDateTime completedAt;
}
