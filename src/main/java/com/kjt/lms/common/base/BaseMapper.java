package com.kjt.lms.common.base;

import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

public interface BaseMapper<E, D> {
    D toDto(E entity);

    E toEntity(D dto);

    List<D> toDtoList(List<E> entities);

    List<E> toEntityList(List<D> dtos);

    @Named("partialUpdate")
    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    void partialUpdate(@MappingTarget E entity, D dto);
}
