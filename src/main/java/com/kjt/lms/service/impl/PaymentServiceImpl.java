package com.kjt.lms.service.impl;

import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.security.SecurityUtils;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.entity.OrderEntity;
import com.kjt.lms.model.entity.OrderItemEntity;
import com.kjt.lms.model.request.order.PayOrderRequestDto;
import com.kjt.lms.model.response.order.OrderItemResponseDto;
import com.kjt.lms.model.response.order.OrderResponseDto;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.OrderItemRepository;
import com.kjt.lms.repository.OrderRepository;
import com.kjt.lms.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SecurityUtils securityUtils;
    private final MessageProvider messageProvider;

    @Override
    @Transactional
    public OrderResponseDto payOrder(UUID orderId, PayOrderRequestDto request) {
        OrderEntity order = getAccessibleOrder(orderId);

        if (order.getStatus() == OrderStatusEnum.COMPLETED) {
            if (order.getTransactionId() != null
                    && !order.getTransactionId().equals(request.getTransactionId())) {
                throw new BusinessException(messageProvider.getMessage("exception.payment.transaction.mismatch"));
            }
            return mapToDto(order);
        }

        if (order.getStatus() != OrderStatusEnum.PENDING) {
            throw new BusinessException(messageProvider.getMessage("exception.order.notPayable"));
        }

        order.setStatus(OrderStatusEnum.COMPLETED);
        order.setTransactionId(request.getTransactionId());
        order.setPaidAt(LocalDateTime.now());

        OrderEntity savedOrder = orderRepository.save(order);
        createEnrollmentsForOrder(savedOrder);

        log.info("Order {} paid successfully", orderId);
        return mapToDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDto cancelOrder(UUID orderId) {
        OrderEntity order = getAccessibleOrder(orderId);

        if (order.getStatus() != OrderStatusEnum.PENDING) {
            throw new BusinessException(messageProvider.getMessage("exception.order.notCancellable"));
        }

        order.setStatus(OrderStatusEnum.CANCELLED);
        OrderEntity savedOrder = orderRepository.save(order);

        log.info("Order {} cancelled", orderId);
        return mapToDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDto refundOrder(UUID orderId) {
        OrderEntity order = getAccessibleOrder(orderId);

        if (order.getStatus() != OrderStatusEnum.COMPLETED) {
            throw new BusinessException(messageProvider.getMessage("exception.order.notRefundable"));
        }

        order.setStatus(OrderStatusEnum.REFUNDED);
        OrderEntity savedOrder = orderRepository.save(order);
        revokeEnrollmentsByOrder(savedOrder);

        log.info("Order {} refunded", orderId);
        return mapToDto(savedOrder);
    }

    private OrderEntity getAccessibleOrder(UUID orderId) {
        OrderEntity order = orderRepository.findByIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.order.notFound")));

        UUID currentUserId = securityUtils.getCurrentUserId();
        if (!securityUtils.isAdmin() && !order.getStudentId().equals(currentUserId)) {
            throw new BusinessException(messageProvider.getMessage("exception.order.accessDenied"));
        }

        return order;
    }

    private void createEnrollmentsForOrder(OrderEntity order) {
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(order.getId());

        for (OrderItemEntity item : orderItems) {
            boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseIdAndDeletedFalse(
                    order.getStudentId(), item.getCourseId());
            if (enrolled) {
                continue;
            }

            EnrollmentEntity enrollment = EnrollmentEntity.builder()
                    .studentId(order.getStudentId())
                    .courseId(item.getCourseId())
                    .orderId(order.getId())
                    .progressPercent(BigDecimal.ZERO)
                    .certificateIssued(false)
                    .build();
            enrollmentRepository.save(enrollment);
        }
    }

    private void revokeEnrollmentsByOrder(OrderEntity order) {
        List<EnrollmentEntity> enrollments = enrollmentRepository
                .findByStudentIdAndOrderIdAndDeletedFalse(order.getStudentId(), order.getId());

        if (enrollments.isEmpty()) {
            return;
        }

        enrollments.forEach(enrollment -> enrollment.setDeleted(true));
        enrollmentRepository.saveAll(enrollments);
    }

    private OrderResponseDto mapToDto(OrderEntity order) {
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemResponseDto> itemDtos = items.stream().map(item ->
                OrderItemResponseDto.builder()
                        .id(item.getId())
                        .courseId(item.getCourseId())
                        .courseTitle(item.getCourseTitle())
                        .courseThumbnail(item.getCourseThumbnail())
                        .paidPrice(item.getPaidPrice())
                        .build()
        ).collect(Collectors.toList());

        return OrderResponseDto.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .transactionId(order.getTransactionId())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .items(itemDtos)
                .build();
    }
}

