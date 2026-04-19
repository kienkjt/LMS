package com.kjt.lms.model.response.cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CartItemResponseDto {
    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private String courseThumbnail;
    private String instructorName;
    private BigDecimal price;
    private BigDecimal originalPrice;
}
