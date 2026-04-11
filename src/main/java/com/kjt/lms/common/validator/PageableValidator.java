package com.kjt.lms.common.validator;

import com.kjt.lms.exception.BusinessException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PageableValidator {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_PAGE_SIZE = 1;

    public static Pageable validate(Pageable pageable) {
        if (pageable == null) {
            throw new BusinessException("Pageable cannot be null");
        }

        int pageSize = pageable.getPageSize();

        if (pageSize < MIN_PAGE_SIZE) {
            throw new BusinessException(
                String.format("Page size must be at least %d", MIN_PAGE_SIZE));
        }

        if (pageSize > MAX_PAGE_SIZE) {
            // Create new pageable with max page size
            return PageRequest.of(
                pageable.getPageNumber(),
                MAX_PAGE_SIZE,
                pageable.getSort() != null ? pageable.getSort() : Sort.unsorted()
            );
        }

        return pageable;
    }
}


