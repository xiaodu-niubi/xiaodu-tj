package com.xiaodu.promotion.strategy.scope;

import com.xiaodu.api.dto.promotion.OrderCourseDTO;
import com.xiaodu.promotion.constants.ScopeType;

import java.util.List;

public interface Scope {

    boolean canUse(OrderCourseDTO course);

    ScopeType getType();

    List<Long> getScopeIds();
}
