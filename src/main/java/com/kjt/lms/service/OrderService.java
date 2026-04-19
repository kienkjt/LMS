package com.kjt.lms.service;

import com.kjt.lms.model.request.order.CheckoutRequestDto;
import com.kjt.lms.model.response.order.OrderResponseDto;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponseDto checkoutCart(CheckoutRequestDto request);
    OrderResponseDto getOrderById(UUID orderId);
    List<OrderResponseDto> getMyOrders(int page, int size);
}
