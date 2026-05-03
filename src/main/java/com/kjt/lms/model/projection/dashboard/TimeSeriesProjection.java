package com.kjt.lms.model.projection.dashboard;

import java.math.BigDecimal;

public interface TimeSeriesProjection {

    String getLabel();

    BigDecimal getAmount();

    Long getCount();
}
