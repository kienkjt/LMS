package com.kjt.lms.model.response.enrollment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponseDto {

    private UUID enrollmentId;
    private UUID courseId;
    private UUID studentId;
    private UUID orderId;
    private BigDecimal progressPercent;
    private LocalDateTime enrolledAt;
}

