package com.kjt.lms.model.response.note;

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
public class NoteResponseDto {

    private UUID id;
    private UUID studentId;
    private UUID courseId;
    private UUID lessonId;
    private String lessonTitle;
    private String content;
    private Integer videoTimestamp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
