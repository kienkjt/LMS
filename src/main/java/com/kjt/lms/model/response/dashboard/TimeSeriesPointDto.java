package com.kjt.lms.model.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class TimeSeriesPointDto {

    private String label;
    private BigDecimal amount;
    private long count;
}
