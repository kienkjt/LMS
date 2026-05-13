package com.kjt.lms.service;

import com.kjt.lms.model.request.admin.ListOrderRequest;
import com.kjt.lms.model.request.admin.UpdateOrderStatusRequest;
import com.kjt.lms.model.response.admin.AdminOrderListResponse;
import com.kjt.lms.model.response.admin.AdminOrderDetailResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface AdminOrderService {
    /**
     * Lấy danh sách đơn hàng với tìm kiếm và lọc
     */
    Page<AdminOrderListResponse> listOrders(ListOrderRequest request);

    /**
     * Lấy chi tiết một đơn hàng
     */
    AdminOrderDetailResponse getOrderDetail(UUID orderId);

    /**
     * Cập nhật trạng thái đơn hàng
     */
    AdminOrderDetailResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);

    /**
     * Hủy đơn hàng
     */
    AdminOrderDetailResponse cancelOrder(UUID orderId, String reason);

    /**
     * Hoàn tiền đơn hàng
     */
    AdminOrderDetailResponse refundOrder(UUID orderId, String reason);
}

