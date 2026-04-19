package com.kjt.lms.controller;

import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.request.cart.AddToCartRequestDto;
import com.kjt.lms.model.response.cart.CartResponseDto;
import com.kjt.lms.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management")
@PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current user's cart", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CartResponseDto>> getMyCart() {
        CartResponseDto response = cartService.getMyCart();
        return ResponseEntity.ok(APIResponse.success(response, "Cart retrieved successfully"));
    }

    @PostMapping("/add")
    @Operation(summary = "Add a course to cart", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CartResponseDto>> addToCart(@Valid @RequestBody AddToCartRequestDto request) {
        CartResponseDto response = cartService.addToCart(request);
        return ResponseEntity.ok(APIResponse.success(response, "Added to cart successfully"));
    }

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Remove an item from cart", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CartResponseDto>> removeFromCart(@PathVariable UUID cartItemId) {
        CartResponseDto response = cartService.removeFromCart(cartItemId);
        return ResponseEntity.ok(APIResponse.success(response, "Removed from cart successfully"));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear all items from cart", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Void>> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok(APIResponse.success(null, "Cart cleared successfully"));
    }
}
