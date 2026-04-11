package com.kjt.lms.model.response;

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
public class CategoryResponseDto {

    private UUID id;
    private String name;
    private String description;
    private String thumbnail;
    private UUID courseId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdById;
    private UUID updatedById;
}

