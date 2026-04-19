package com.kjt.lms.service;

import com.kjt.lms.model.request.cart.AddToCartRequestDto;
import com.kjt.lms.model.response.cart.CartResponseDto;

import java.util.UUID;

public interface CartService {
    CartResponseDto getMyCart();
    CartResponseDto addToCart(AddToCartRequestDto request);
    CartResponseDto removeFromCart(UUID cartItemId);
    void clearCart();
}
