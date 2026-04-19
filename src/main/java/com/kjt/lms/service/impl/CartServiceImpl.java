package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseEntity;
import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.CartEntity;
import com.kjt.lms.model.entity.CartItemEntity;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.model.request.cart.AddToCartRequestDto;
import com.kjt.lms.model.response.cart.CartItemResponseDto;
import com.kjt.lms.model.response.cart.CartResponseDto;
import com.kjt.lms.repository.CartItemRepository;
import com.kjt.lms.repository.CartRepository;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final MessageProvider messageProvider;

    private UUID getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(BaseEntity::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private CartEntity getOrCreateCart(UUID userId) {
        return cartRepository.findByUserIdAndDeletedFalse(userId)
                .orElseGet(() -> {
                    CartEntity newCart = CartEntity.builder()
                            .userId(userId)
                            .totalAmount(BigDecimal.ZERO)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponseDto getMyCart() {
        UUID userId = getCurrentUserId();
        CartEntity cart = getOrCreateCart(userId);
        return mapToDto(cart);
    }

    @Override
    @Transactional
    public CartResponseDto addToCart(AddToCartRequestDto request) {
        UUID userId = getCurrentUserId();
        CartEntity cart = getOrCreateCart(userId);

        CourseEntity course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if(Boolean.TRUE.equals(course.getDeleted()) || course.getActive() != CommonStatusEnum.ACTIVE) {
            throw new BusinessException("Course is not available for purchase");
        }
        
        // Prevent adding own course
        if (course.getInstructorId().equals(userId)) {
            throw new BusinessException("You cannot buy your own course");
        }

        // Check if already in cart
        Optional<CartItemEntity> existingItem = cartItemRepository.findByCartIdAndCourseIdAndDeletedFalse(cart.getId(), course.getId());
        if (existingItem.isPresent()) {
            throw new BusinessException("Course is already in your cart");
        }

        // Calculate price (discount price if available, else regular price)
        BigDecimal actualPrice = (course.getDiscountPrice() != null && course.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0)
                ? course.getDiscountPrice() : course.getPrice();

        CartItemEntity item = CartItemEntity.builder()
                .cartId(cart.getId())
                .courseId(course.getId())
                .price(actualPrice)
                .build();
        
        cartItemRepository.save(item);

        updateCartTotal(cart);
        
        log.info("User {} added course {} to cart", userId, course.getId());
        
        return mapToDto(cart);
    }

    @Override
    @Transactional
    public CartResponseDto removeFromCart(UUID cartItemId) {
        UUID userId = getCurrentUserId();
        CartEntity cart = getOrCreateCart(userId);

        CartItemEntity item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCartId().equals(cart.getId())) {
            throw new BusinessException("Item does not belong to your cart");
        }

        item.setDeleted(true);
        cartItemRepository.save(item);

        updateCartTotal(cart);
        return mapToDto(cart);
    }

    @Override
    @Transactional
    public void clearCart() {
        UUID userId = getCurrentUserId();
        CartEntity cart = getOrCreateCart(userId);
        
        List<CartItemEntity> items = cartItemRepository.findByCartIdAndDeletedFalse(cart.getId());
        for(CartItemEntity item : items) {
             item.setDeleted(true);
        }
        cartItemRepository.saveAll(items);
        
        cart.setTotalAmount(BigDecimal.ZERO);
        cartRepository.save(cart);
    }

    private void updateCartTotal(CartEntity cart) {
        List<CartItemEntity> items = cartItemRepository.findByCartIdAndDeletedFalse(cart.getId());
        BigDecimal total = items.stream()
                .map(CartItemEntity::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        cart.setTotalAmount(total);
        cartRepository.save(cart);
    }

    private CartResponseDto mapToDto(CartEntity cart) {
        List<CartItemEntity> items = cartItemRepository.findByCartIdAndDeletedFalse(cart.getId());
        
        List<CartItemResponseDto> itemDtos = items.stream().map(item -> {
            CourseEntity course = courseRepository.findById(item.getCourseId()).orElse(null);
            if (course == null) return null;
            
            String instructorName = userRepository.findById(course.getInstructorId())
                                        .map(UserEntity::getFullName).orElse("Unknown");

            return CartItemResponseDto.builder()
                    .id(item.getId())
                    .courseId(course.getId())
                    .courseTitle(course.getTitle())
                    .courseThumbnail(course.getThumbnail())
                    .instructorName(instructorName)
                    .price(item.getPrice())
                    .originalPrice(course.getPrice())
                    .build();
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return CartResponseDto.builder()
                .id(cart.getId())
                .totalAmount(cart.getTotalAmount())
                .items(itemDtos)
                .build();
    }
}
