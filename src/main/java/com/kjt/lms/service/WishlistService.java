package com.kjt.lms.service;

import com.kjt.lms.model.response.wishlist.WishlistResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface WishlistService {

    Page<WishlistResponseDto> getMyWishlist(Pageable pageable);

    WishlistResponseDto addToWishlist(UUID courseId);

    void removeFromWishlist(UUID wishlistId);

    void removeCourseFromWishlist(UUID courseId);

    boolean isCourseInWishlist(UUID courseId);
}
