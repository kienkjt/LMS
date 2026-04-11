package com.kjt.lms.model.request.chapter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChapterRequestDto {

    @NotBlank(message = "{validation.chapter.title.notBlank}")
    @Size(min = 3, max = 200, message = "{validation.chapter.title.size}")
    private String title;

    @Size(max = 2000, message = "{validation.chapter.description.size}")
    private String description;
}

