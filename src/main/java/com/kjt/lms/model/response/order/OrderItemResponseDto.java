package com.kjt.lms.model.response.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OrderItemResponseDto {
    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private String courseThumbnail;
    private BigDecimal paidPrice;
}
