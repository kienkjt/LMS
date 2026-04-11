package com.kjt.lms.model.request.lesson;

import com.kjt.lms.common.constants.LessonTypeEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLessonRequestDto {

    @NotBlank(message = "{validation.lesson.title.notBlank}")
    @Size(min = 3, max = 200, message = "{validation.lesson.title.size}")
    private String title;

    @Size(max = 2000, message = "{validation.lesson.description.size}")
    private String description;

    @NotNull(message = "{validation.lesson.type.notNull}")
    private LessonTypeEnum type;

    @Size(max = 500, message = "{validation.lesson.videoUrl.size}")
    private String videoUrl;

    @Size(max = 500, message = "{validation.lesson.videoPublicId.size}")
    private String videoPublicId;

    @NotNull(message = "{validation.lesson.duration.notNull}")
    @Min(value = 0, message = "{validation.lesson.duration.min}")
    private Integer duration;

    private String content;

    @NotNull(message = "{validation.lesson.freePreview.notNull}")
    private Boolean freePreview;

    private UUID quizId;
}

