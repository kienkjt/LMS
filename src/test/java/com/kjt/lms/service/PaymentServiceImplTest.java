package com.kjt.lms.service;

import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.security.SecurityUtils;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.entity.OrderEntity;
import com.kjt.lms.model.entity.OrderItemEntity;
import com.kjt.lms.model.request.order.PayOrderRequestDto;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.OrderItemRepository;
import com.kjt.lms.repository.OrderRepository;
import com.kjt.lms.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private MessageProvider messageProvider;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        lenient().when(messageProvider.getMessage(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void payOrder_shouldCompleteOrderAndCreateEnrollment() {
        UUID studentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        OrderEntity order = OrderEntity.builder()
                .studentId(studentId)
                .status(OrderStatusEnum.PENDING)
                .build();
        order.setId(orderId);

        OrderItemEntity orderItem = OrderItemEntity.builder()
                .orderId(orderId)
                .courseId(courseId)
                .courseTitle("Java")
                .paidPrice(new BigDecimal("100"))
                .build();

        PayOrderRequestDto request = new PayOrderRequestDto();
        request.setTransactionId("TXN-001");

        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(securityUtils.getCurrentUserId()).thenReturn(studentId);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(orderItem));
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId)).thenReturn(false);

        var response = paymentService.payOrder(orderId, request);

        assertEquals(OrderStatusEnum.COMPLETED, response.getStatus());
        assertEquals("TXN-001", response.getTransactionId());
        verify(enrollmentRepository).save(any(EnrollmentEntity.class));
    }

    @Test
    void cancelOrder_shouldCancelPendingOrder() {
        UUID studentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        OrderEntity order = OrderEntity.builder()
                .studentId(studentId)
                .status(OrderStatusEnum.PENDING)
                .build();
        order.setId(orderId);

        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(securityUtils.getCurrentUserId()).thenReturn(studentId);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());

        var response = paymentService.cancelOrder(orderId);

        assertEquals(OrderStatusEnum.CANCELLED, response.getStatus());
    }

    @Test
    void refundOrder_shouldRefundCompletedOrderAndRevokeEnrollment() {
        UUID studentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        OrderEntity order = OrderEntity.builder()
                .studentId(studentId)
                .status(OrderStatusEnum.COMPLETED)
                .build();
        order.setId(orderId);

        EnrollmentEntity enrollment = EnrollmentEntity.builder()
                .studentId(studentId)
                .courseId(UUID.randomUUID())
                .orderId(orderId)
                .build();

        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(securityUtils.getCurrentUserId()).thenReturn(studentId);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentRepository.findByStudentIdAndOrderIdAndDeletedFalse(studentId, orderId))
                .thenReturn(List.of(enrollment));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());

        var response = paymentService.refundOrder(orderId);

        assertEquals(OrderStatusEnum.REFUNDED, response.getStatus());
        verify(enrollmentRepository).saveAll(any());
    }

    @Test
    void payOrder_shouldThrowAccessDenied_whenOrderNotOwnedByUser() {
        UUID currentUserId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        OrderEntity order = OrderEntity.builder()
                .studentId(studentId)
                .status(OrderStatusEnum.PENDING)
                .build();
        order.setId(orderId);

        PayOrderRequestDto request = new PayOrderRequestDto();
        request.setTransactionId("TXN-001");

        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(securityUtils.isAdmin()).thenReturn(false);

        assertThrows(BusinessException.class, () -> paymentService.payOrder(orderId, request));
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }
}

