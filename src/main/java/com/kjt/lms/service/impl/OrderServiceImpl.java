package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.CourseStatusEnum;
import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.CartEntity;
import com.kjt.lms.model.entity.CartItemEntity;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.OrderEntity;
import com.kjt.lms.model.entity.OrderItemEntity;
import com.kjt.lms.model.request.order.CheckoutRequestDto;
import com.kjt.lms.model.response.order.OrderResponseDto;
import com.kjt.lms.repository.CartItemRepository;
import com.kjt.lms.repository.CartRepository;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.OrderItemRepository;
import com.kjt.lms.repository.OrderRepository;
import com.kjt.lms.service.CartService;
import com.kjt.lms.service.OrderService;
import com.kjt.lms.service.PlatformFeeService;
import com.kjt.lms.mapper.OrderMapper;
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

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CartService cartService;
    private final PlatformFeeService platformFeeService;
    private final OrderMapper orderMapper;
    private final MessageProvider messageProvider;

    private String generateOrderCode() {
        return "ORD-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
    }

    @Override
    @Transactional
    public OrderResponseDto checkoutCart(CheckoutRequestDto request) {
        UUID userId = securityUtils.getCurrentUserId();
        CartEntity cart = cartRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(messageProvider.getMessage("exception.cart.empty")));

        List<CartItemEntity> cartItems = cartItemRepository.findByCartIdAndDeletedFalse(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BusinessException(messageProvider.getMessage("exception.cart.empty"));
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
                    .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.course.notFound")));

            validatePurchasableCourse(course, userId);
            createOrderItem(order.getId(), course, cartItem.getPrice());
        }

        cartService.clearCart();
        return mapToDto(order);
    }

    @Override
    @Transactional
    public OrderResponseDto checkoutCourse(UUID courseId, CheckoutRequestDto request) {
        UUID userId = securityUtils.getCurrentUserId();
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.course.notFound")));

        validatePurchasableCourse(course, userId);

        BigDecimal paidPrice = getActualCoursePrice(course);
        OrderEntity order = OrderEntity.builder()
                .studentId(userId)
                .orderCode(generateOrderCode())
                .totalAmount(paidPrice)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(paidPrice)
                .status(OrderStatusEnum.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .note(request.getNote())
                .build();

        order = orderRepository.save(order);
        createOrderItem(order.getId(), course, paidPrice);
        return mapToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(UUID orderId) {
        UUID userId = securityUtils.getCurrentUserId();
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.order.notFound")));

        if (!order.getStudentId().equals(userId) && !securityUtils.isAdmin()) {
            throw new BusinessException(messageProvider.getMessage("exception.order.accessDenied"));
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
        return orderMapper.toResponse(order, items);
    }

    private void validatePurchasableCourse(CourseEntity course, UUID userId) {
        if (Boolean.TRUE.equals(course.getDeleted())
                || course.getActive() != CommonStatusEnum.ACTIVE
                || course.getStatus() != CourseStatusEnum.PUBLISHED) {
            throw new BusinessException(messageProvider.getMessage("exception.enrollment.course.notAvailable"));
        }

        if (course.getInstructorId().equals(userId)) {
            throw new BusinessException(messageProvider.getMessage("exception.cart.ownCourse"));
        }

        if (enrollmentRepository.existsByStudentIdAndCourseIdAndDeletedFalse(userId, course.getId())) {
            throw new BusinessException(messageProvider.getMessage("exception.enrollment.alreadyEnrolled", course.getTitle()));
        }
    }

    private BigDecimal getActualCoursePrice(CourseEntity course) {
        return course.getDiscountPrice() != null && course.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0
                ? course.getDiscountPrice()
                : course.getPrice();
    }

    private void createOrderItem(UUID orderId, CourseEntity course, BigDecimal paidPrice) {
        OrderItemEntity orderItem = OrderItemEntity.builder()
                .orderId(orderId)
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .courseThumbnail(course.getThumbnail())
                .originalPrice(course.getPrice())
                .paidPrice(paidPrice)
                .instructorId(course.getInstructorId())
                .instructorRevenue(paidPrice.multiply(platformFeeService.getInstructorRevenueRate()))
                .build();
        orderItemRepository.save(orderItem);
    }
}
