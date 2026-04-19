package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.CartEntity;
import com.kjt.lms.model.entity.CartItemEntity;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.OrderEntity;
import com.kjt.lms.model.entity.OrderItemEntity;
import com.kjt.lms.model.request.order.CheckoutRequestDto;
import com.kjt.lms.model.response.order.OrderItemResponseDto;
import com.kjt.lms.model.response.order.OrderResponseDto;
import com.kjt.lms.repository.CartItemRepository;
import com.kjt.lms.repository.CartRepository;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.OrderItemRepository;
import com.kjt.lms.repository.OrderRepository;
import com.kjt.lms.service.CartService;
import com.kjt.lms.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl extends BaseService implements OrderService {

    private static final BigDecimal INSTRUCTOR_REVENUE_RATE = new BigDecimal("0.8");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CartService cartService;

    private String generateOrderCode() {
        return "ORD-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
    }

    @Override
    @Transactional
    public OrderResponseDto checkoutCart(CheckoutRequestDto request) {
        UUID userId = securityUtils.getCurrentUserId();
        CartEntity cart = cartRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException("Cart is empty"));

        List<CartItemEntity> cartItems = cartItemRepository.findByCartIdAndDeletedFalse(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BusinessException("Cart is empty");
        }

        OrderEntity order = OrderEntity.builder()
                .studentId(userId)
                .orderCode(generateOrderCode())
                .totalAmount(cart.getTotalAmount())
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(cart.getTotalAmount())
                .status(OrderStatusEnum.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .note(request.getNote())
                .build();

        order = orderRepository.save(order);

        for (CartItemEntity cartItem : cartItems) {
            CourseEntity course = courseRepository.findById(cartItem.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

            if (enrollmentRepository.existsByStudentIdAndCourseIdAndDeletedFalse(userId, course.getId())) {
                throw new BusinessException("You are already enrolled in: " + course.getTitle());
            }

            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .orderId(order.getId())
                    .courseId(course.getId())
                    .courseTitle(course.getTitle())
                    .courseThumbnail(course.getThumbnail())
                    .originalPrice(course.getPrice())
                    .paidPrice(cartItem.getPrice())
                    .instructorId(course.getInstructorId())
                    .instructorRevenue(cartItem.getPrice().multiply(INSTRUCTOR_REVENUE_RATE))
                    .build();
            orderItemRepository.save(orderItem);
        }

        cartService.clearCart();
        return mapToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(UUID orderId) {
        UUID userId = securityUtils.getCurrentUserId();
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getStudentId().equals(userId) && !securityUtils.isAdmin()) {
            throw new BusinessException("Access denied");
        }

        return mapToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getMyOrders(int page, int size) {
        UUID userId = securityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderEntity> orders = orderRepository.findByStudentIdOrderByCreatedAtDesc(userId, pageable);

        return orders.stream().map(this::mapToDto).collect(Collectors.toList());
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