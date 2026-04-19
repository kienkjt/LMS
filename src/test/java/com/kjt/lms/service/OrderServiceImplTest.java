package com.kjt.lms.service;

import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.model.entity.OrderEntity;
import com.kjt.lms.model.entity.OrderItemEntity;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.repository.CartItemRepository;
import com.kjt.lms.repository.CartRepository;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.OrderItemRepository;
import com.kjt.lms.repository.OrderRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void getOrderById_shouldReturnOrder_whenOwner() {
        UUID studentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        mockAuthenticatedUser(studentId);

        OrderEntity order = OrderEntity.builder()
                .studentId(studentId)
                .orderCode("ORD-001")
                .totalAmount(new BigDecimal("100"))
                .build();
        order.setId(orderId);

        OrderItemEntity item = OrderItemEntity.builder()
                .orderId(orderId)
                .courseId(UUID.randomUUID())
                .courseTitle("Java")
                .paidPrice(new BigDecimal("100"))
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));

        var response = orderService.getOrderById(orderId);

        assertEquals(orderId, response.getId());
        assertEquals("ORD-001", response.getOrderCode());
        assertEquals(1, response.getItems().size());
    }

    @Test
    void getOrderById_shouldThrowBusinessException_whenNotOwner() {
        UUID studentId = UUID.randomUUID();
        UUID anotherStudentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        mockAuthenticatedUser(studentId);

        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setStudentId(anotherStudentId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> orderService.getOrderById(orderId));
    }

    private void mockAuthenticatedUser(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@example.com", null, List.of()));

        UserEntity user = new UserEntity();
        user.setId(userId);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
    }
}
