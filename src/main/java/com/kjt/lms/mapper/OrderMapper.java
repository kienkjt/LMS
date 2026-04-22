package com.kjt.lms.mapper;

import com.kjt.lms.model.entity.OrderEntity;
import com.kjt.lms.model.entity.OrderItemEntity;
import com.kjt.lms.model.response.order.OrderResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "items", source = "items")
    @Mapping(target = "paymentUrl", ignore = true)
    OrderResponseDto toResponse(OrderEntity order, List<OrderItemEntity> items);
}
