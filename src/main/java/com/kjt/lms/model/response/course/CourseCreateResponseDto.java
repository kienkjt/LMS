package com.kjt.lms.model.response.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
import com.kjt.lms.common.constants.CourseStatusEnum;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreateResponseDto {

    private UUID id;
    private UUID instructorId;
    private String instructorName;
    private String title;
    private CourseStatusEnum status;
    private LocalDateTime createdAt;
}
