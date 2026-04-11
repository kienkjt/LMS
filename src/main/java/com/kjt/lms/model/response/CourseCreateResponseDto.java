package com.kjt.lms.model.response;

import com.kjt.lms.common.constants.CourseStatusEnum;
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
public class CourseCreateResponseDto {

    private UUID id;
    private UUID instructorId;
    private String title;
    private CourseStatusEnum status;
    private LocalDateTime createdAt;
}
