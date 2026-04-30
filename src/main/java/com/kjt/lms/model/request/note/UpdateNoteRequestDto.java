package com.kjt.lms.model.request.note;

import jakarta.validation.constraints.Min;
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
public class UpdateNoteRequestDto {

    @NotBlank(message = "{validation.note.content.notBlank}")
    @Size(max = 5000, message = "{validation.note.content.size}")
    private String content;

    @Min(value = 0, message = "{validation.note.videoTimestamp.min}")
    private Integer videoTimestamp;
}
