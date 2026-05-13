package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.model.request.admin.ListOrderRequest;
import com.kjt.lms.model.request.admin.UpdateOrderStatusRequest;
import com.kjt.lms.model.response.admin.AdminOrderListResponse;
import com.kjt.lms.model.response.admin.AdminOrderDetailResponse;
import com.kjt.lms.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin Order Management", description = "Quản lý đơn hàng cho admin")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;
    private final MessageProvider messageProvider;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách đơn hàng",
            description = "Lấy danh sách đơn hàng với tìm kiếm, lọc và phân trang",
            security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Page<AdminOrderListResponse>>> listOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatusEnum status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        ListOrderRequest request = ListOrderRequest.builder()
                .keyword(keyword)
                .status(status)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .fromDate(fromDate)
                .toDate(toDate)
                .page(Math.max(page, 0))
                .size(Math.max(size, 1))
                .build();

        Page<AdminOrderListResponse> response = adminOrderService.listOrders(request);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("admin.order.list.success")));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chi tiết đơn hàng",
            description = "Lấy chi tiết thông tin một đơn hàng",
            security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<AdminOrderDetailResponse>> getOrderDetail(@PathVariable UUID orderId) {
        AdminOrderDetailResponse response = adminOrderService.getOrderDetail(orderId);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("admin.order.detail.success")));
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật trạng thái đơn hàng",
            description = "Cập nhật trạng thái đơn hàng (PENDING, COMPLETED, CANCELLED, REFUNDED)",
            security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<AdminOrderDetailResponse>> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        AdminOrderDetailResponse response = adminOrderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("admin.order.status.updated")));
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hủy đơn hàng",
            description = "Hủy một đơn hàng chưa thanh toán",
            security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<AdminOrderDetailResponse>> cancelOrder(
            @PathVariable UUID orderId,
            @RequestParam String reason) {
        AdminOrderDetailResponse response = adminOrderService.cancelOrder(orderId, reason);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("admin.order.cancelled")));
    }

    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hoàn tiền đơn hàng",
            description = "Hoàn tiền cho một đơn hàng đã thanh toán",
            security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<AdminOrderDetailResponse>> refundOrder(
            @PathVariable UUID orderId,
            @RequestParam String reason) {
        AdminOrderDetailResponse response = adminOrderService.refundOrder(orderId, reason);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("admin.order.refunded")));
    }
}






