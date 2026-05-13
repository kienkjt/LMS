package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.request.order.CheckoutRequestDto;
import com.kjt.lms.model.request.order.InitPaymentRequestDto;
import com.kjt.lms.model.request.order.PayOrderRequestDto;
import com.kjt.lms.model.response.order.OrderResponseDto;
import com.kjt.lms.service.OrderService;
import com.kjt.lms.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders and Payment", description = "Endpoints for handling checkout")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final MessageProvider messageProvider;

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Checkout current cart", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<OrderResponseDto>> checkout(
            @Valid @RequestBody CheckoutRequestDto request,
            HttpServletRequest httpRequest) {
        OrderResponseDto response = orderService.checkoutCart(request);
        response = initGatewayPaymentIfNeeded(response, request, httpRequest);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("order.checkout.success")));
    }

    @PostMapping("/checkout/courses/{courseId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Checkout a course directly without using cart", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<OrderResponseDto>> checkoutCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody CheckoutRequestDto request,
            HttpServletRequest httpRequest) {
        OrderResponseDto response = orderService.checkoutCourse(courseId, request);
        response = initGatewayPaymentIfNeeded(response, request, httpRequest);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("order.checkout.success")));
    }

    @PostMapping("/{orderId}/pay/init")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Init payment URL for an existing order", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<OrderResponseDto>> initPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody InitPaymentRequestDto request,
            HttpServletRequest httpRequest) {
        OrderResponseDto response = paymentService.initPayment(orderId, request, httpRequest);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("payment.init.success")));
    }

    private OrderResponseDto initGatewayPaymentIfNeeded(OrderResponseDto response, CheckoutRequestDto request, HttpServletRequest httpRequest) {
        if (response == null || response.getId() == null || request == null || request.getPaymentMethod() == null) {
            return response;
        }

        InitPaymentRequestDto initRequest = new InitPaymentRequestDto();
        initRequest.setPaymentMethod(request.getPaymentMethod());

        try {
            return paymentService.initPayment(response.getId(), initRequest, httpRequest);
        } catch (Exception ex) {
            return response;
        }
    }

    @PostMapping("/{orderId}/pay")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Pay an existing order", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<OrderResponseDto>> payOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody PayOrderRequestDto request) {
        OrderResponseDto response = paymentService.payOrder(orderId, request);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("payment.completed.success")));
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Cancel an unpaid order", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<OrderResponseDto>> cancelOrder(@PathVariable UUID orderId) {
        OrderResponseDto response = paymentService.cancelOrder(orderId);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("payment.cancelled.success")));
    }

    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Request refund for a paid order", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<OrderResponseDto>> refundOrder(
            @PathVariable UUID orderId,
            @RequestParam String reason) {
        OrderResponseDto response = paymentService.refundOrder(orderId, reason);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("payment.refunded.success")));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get user's order history", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<List<OrderResponseDto>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<OrderResponseDto> response = orderService.getMyOrders(page, size);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("order.list.success")));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get order details", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<OrderResponseDto>> getOrderById(@PathVariable UUID orderId) {
        OrderResponseDto response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("order.detail.success")));
    }
}
