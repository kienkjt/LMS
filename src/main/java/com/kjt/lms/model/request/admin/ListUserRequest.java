package com.kjt.lms.model.request.admin;

import com.kjt.lms.common.constants.CommonStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListUserRequest {
    private String keyword;
    private String roleCode;
    private CommonStatusEnum active;
    private Boolean isLocked;
    private int page;
    private int size;
}


