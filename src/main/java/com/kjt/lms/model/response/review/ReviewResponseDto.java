package com.kjt.lms.model.response.review;

import com.kjt.lms.common.constants.CommonStatusEnum;
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
public class ReviewResponseDto {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentAvatar;
    private UUID courseId;
    private Integer rating;
    private String comment;
    private String instructorReply;
    private LocalDateTime repliedAt;
    private CommonStatusEnum active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
