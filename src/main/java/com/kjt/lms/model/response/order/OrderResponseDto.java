package com.kjt.lms.model.response.order;

import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.common.constants.PaymentMethodEnum;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderResponseDto {
    private UUID id;
    private String orderCode;
    private BigDecimal totalAmount;
    private OrderStatusEnum status;
    private PaymentMethodEnum paymentMethod;
    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private List<OrderItemResponseDto> items;
    private String paymentUrl; // used if external gateway provides a redirect url
}
