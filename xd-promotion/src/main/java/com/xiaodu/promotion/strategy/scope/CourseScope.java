package com.xiaodu.promotion.strategy.scope;

import com.xiaodu.api.dto.promotion.OrderCourseDTO;
import com.xiaodu.promotion.constants.ScopeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CourseScope implements Scope {

    private final List<Long> scopeIds;

    @Override
    public boolean canUse(OrderCourseDTO course) {
        return scopeIds.contains(course.getId());
    }

    @Override
    public ScopeType getType() {
        return ScopeType.COURSE;
    }
}
