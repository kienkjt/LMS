package com.kjt.lms.model.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class StatusCountDto {

    private String status;
    private String description;
    private long count;
}
