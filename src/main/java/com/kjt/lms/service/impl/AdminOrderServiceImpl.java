package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.OrderEntity;
import com.kjt.lms.model.entity.OrderItemEntity;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.model.request.admin.ListOrderRequest;
import com.kjt.lms.model.request.admin.UpdateOrderStatusRequest;
import com.kjt.lms.model.response.admin.AdminOrderDetailResponse;
import com.kjt.lms.model.response.admin.AdminOrderListResponse;
import com.kjt.lms.model.response.order.OrderItemResponseDto;
import com.kjt.lms.repository.OrderItemRepository;
import com.kjt.lms.repository.OrderRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.AdminOrderService;
import com.kjt.lms.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOrderServiceImpl extends BaseService implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final MessageProvider messageProvider;
    private final WithdrawalService withdrawalService;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminOrderListResponse> listOrders(ListOrderRequest request) {
        Pageable pageable = PageRequest.of(
                Math.max(request.getPage(), 0),
                Math.max(request.getSize(), 1)
        );

        String keyword = request.getKeyword() != null ? request.getKeyword() : "";
        String statusValue = request.getStatus() != null ? request.getStatus().getValue() : null;

        Page<OrderEntity> orders = orderRepository.findOrdersWithFilter(
                keyword,
                statusValue,
                request.getFromDate(),
                request.getToDate(),
                request.getMinAmount(),
                request.getMaxAmount(),
                pageable
        );

        return orders.map(this::mapToListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrderDetail(UUID orderId) {
        OrderEntity order = orderRepository.findByIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.order.notFound")));

        return mapToDetailResponse(order);
    }

    @Override
    @Transactional
    public AdminOrderDetailResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        OrderEntity order = orderRepository.findByIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.order.notFound")));

        validateStatusTransition(order.getStatus(), request.getStatus());

        if (request.getStatus() == OrderStatusEnum.REFUNDED) {
            validateRequiredReason(request.getReason());
            withdrawalService.processRefundAdjustment(orderId);
            String noteMessage = messageProvider.getMessage("admin.order.refunded.by.admin", request.getReason().trim());
            appendAdminNote(order, noteMessage);
        } else if (request.getStatus() == OrderStatusEnum.CANCELLED) {
            validateRequiredReason(request.getReason());
            String noteMessage = messageProvider.getMessage("admin.order.cancelled.by.admin", request.getReason().trim());
            appendAdminNote(order, noteMessage);
        }

        log.info("Admin updating order {} status from {} to {}",
                orderId, order.getStatus(), request.getStatus());

        order.setStatus(request.getStatus());
        order = orderRepository.save(order);

        return mapToDetailResponse(order);
    }

    @Override
    @Transactional
    public AdminOrderDetailResponse cancelOrder(UUID orderId, String reason) {
        OrderEntity order = orderRepository.findByIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.order.notFound")));

        if (!order.getStatus().equals(OrderStatusEnum.PENDING)) {
            throw new BusinessException(messageProvider.getMessage("admin.order.cancel.only.pending"));
        }

        validateRequiredReason(reason);
        order.setStatus(OrderStatusEnum.CANCELLED);
        String noteMessage = messageProvider.getMessage("admin.order.cancelled.by.admin", reason.trim());
        appendAdminNote(order, noteMessage);
        order = orderRepository.save(order);

        log.info("Admin cancelled order {}: {}", orderId, reason);

        return mapToDetailResponse(order);
    }

    @Override
    @Transactional
    public AdminOrderDetailResponse refundOrder(UUID orderId, String reason) {
        OrderEntity order = orderRepository.findByIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.order.notFound")));

        if (!order.getStatus().equals(OrderStatusEnum.COMPLETED)) {
            throw new BusinessException(messageProvider.getMessage("admin.order.refund.only.completed"));
        }

        validateRequiredReason(reason);
        withdrawalService.processRefundAdjustment(orderId);

        order.setStatus(OrderStatusEnum.REFUNDED);
        String noteMessage = messageProvider.getMessage("admin.order.refunded.by.admin", reason.trim());
        appendAdminNote(order, noteMessage);
        order = orderRepository.save(order);

        log.info("Admin refunded order {}: {}", orderId, reason);

        return mapToDetailResponse(order);
    }

    private void validateStatusTransition(OrderStatusEnum currentStatus, OrderStatusEnum newStatus) {
        if (currentStatus.equals(newStatus)) {
            throw new BusinessException(messageProvider.getMessage("admin.order.status.same"));
        }

        switch (currentStatus) {
            case CANCELLED, REFUNDED ->
                    throw new BusinessException(messageProvider.getMessage("admin.order.status.locked"));
            case PENDING -> {
                if (!newStatus.equals(OrderStatusEnum.COMPLETED) && !newStatus.equals(OrderStatusEnum.CANCELLED)) {
                    throw new BusinessException(messageProvider.getMessage("admin.order.pending.invalid"));
                }
            }
            case COMPLETED -> {
                if (!newStatus.equals(OrderStatusEnum.REFUNDED)) {
                    throw new BusinessException(messageProvider.getMessage("admin.order.completed.invalid"));
                }
            }
        }
    }

    private AdminOrderListResponse mapToListResponse(OrderEntity order) {
        int itemCount = orderItemRepository.countByOrderIdAndDeletedFalse(order.getId());

        UserEntity student = userRepository.findById(order.getStudentId()).orElse(null);
        String studentName = student != null ? student.getFullName() : "N/A";
        String studentEmail = student != null ? student.getEmail() : "N/A";

        return AdminOrderListResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .studentId(order.getStudentId())
                .studentName(studentName)
                .studentEmail(studentEmail)
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().toString() : "N/A")
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .itemCount(itemCount)
                .build();
    }

    private AdminOrderDetailResponse mapToDetailResponse(OrderEntity order) {
        UserEntity student = userRepository.findById(order.getStudentId()).orElse(null);

        List<OrderItemEntity> items = orderItemRepository.findByOrderIdAndDeletedFalse(order.getId());
        List<OrderItemResponseDto> itemDtos = items.stream()
                .map(item -> OrderItemResponseDto.builder()
                        .id(item.getId())
                        .courseId(item.getCourseId())
                        .courseTitle(item.getCourseTitle())
                        .courseThumbnail(item.getCourseThumbnail())
                        .paidPrice(item.getPaidPrice())
                        .build())
                .collect(Collectors.toList());

        return AdminOrderDetailResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .studentId(order.getStudentId())
                .studentName(student != null ? student.getFullName() : "N/A")
                .studentEmail(student != null ? student.getEmail() : "N/A")
                .studentPhone(student != null ? student.getPhoneNumber() : "N/A")
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().toString() : "N/A")
                .transactionId(order.getTransactionId())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .note(order.getNote())
                .items(itemDtos)
                .build();
    }

    private void validateRequiredReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(messageProvider.getMessage("admin.order.reason.required"));
        }
    }

    private void appendAdminNote(OrderEntity order, String note) {
        order.setNote((order.getNote() != null ? order.getNote() + " | " : "") + note);
    }
}






