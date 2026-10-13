package com.xiaodu.promotion.service;

import com.xiaodu.api.dto.promotion.CouponDiscountDTO;
import com.xiaodu.api.dto.promotion.OrderCouponDTO;
import com.xiaodu.api.dto.promotion.OrderCourseDTO;

import java.util.List;

public interface IDiscountService {
    List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses);

    CouponDiscountDTO queryDiscountDetailByOrder(OrderCouponDTO orderCouponDTO);
}
