package com.kjt.lms.service;

import com.kjt.lms.model.request.order.PayOrderRequestDto;
import com.kjt.lms.model.response.order.OrderResponseDto;

import java.util.UUID;

public interface PaymentService {

    OrderResponseDto payOrder(UUID orderId, PayOrderRequestDto request);

    OrderResponseDto cancelOrder(UUID orderId);

    OrderResponseDto refundOrder(UUID orderId);
}

