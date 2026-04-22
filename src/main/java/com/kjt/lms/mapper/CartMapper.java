package com.kjt.lms.mapper;

import com.kjt.lms.model.entity.CartEntity;
import com.kjt.lms.model.entity.CartItemEntity;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.response.cart.CartItemResponseDto;
import com.kjt.lms.model.response.cart.CartResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "id", source = "item.id")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "courseThumbnail", source = "course.thumbnail")
    @Mapping(target = "instructorName", source = "instructorName")
    @Mapping(target = "price", source = "item.price")
    @Mapping(target = "originalPrice", source = "course.price")
    CartItemResponseDto toItemResponse(CartItemEntity item, CourseEntity course, String instructorName);

    @Mapping(target = "id", source = "cart.id")
    @Mapping(target = "totalAmount", source = "cart.totalAmount")
    @Mapping(target = "items", source = "items")
    CartResponseDto toResponse(CartEntity cart, List<CartItemResponseDto> items);
}

