package com.kjt.lms.model.response.cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CartResponseDto {
    private UUID id;
    private BigDecimal totalAmount;
    private List<CartItemResponseDto> items;
}
