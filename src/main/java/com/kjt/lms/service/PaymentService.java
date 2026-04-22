package com.kjt.lms.service;

import com.kjt.lms.model.request.order.InitPaymentRequestDto;
import com.kjt.lms.model.request.order.PayOrderRequestDto;
import com.kjt.lms.model.response.order.OrderResponseDto;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.UUID;

public interface PaymentService {

    OrderResponseDto initPayment(UUID orderId, InitPaymentRequestDto request, HttpServletRequest httpRequest);

    OrderResponseDto payOrder(UUID orderId, PayOrderRequestDto request);

    OrderResponseDto handleVnPayReturn(Map<String, String> queryParams);

    Map<String, String> handleVnPayIpn(Map<String, String> queryParams);

    OrderResponseDto cancelOrder(UUID orderId);

    OrderResponseDto refundOrder(UUID orderId);
}
