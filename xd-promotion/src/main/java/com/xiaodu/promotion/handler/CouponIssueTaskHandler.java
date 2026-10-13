package com.xiaodu.promotion.handler;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaodu.common.utils.CollUtils;
import com.xiaodu.promotion.domain.po.Coupon;
import com.xiaodu.promotion.enums.CouponStatus;
import com.xiaodu.promotion.service.ICouponService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CouponIssueTaskHandler {

    private final ICouponService couponService;

    @XxlJob("couponIssueJobHandler")
    public void handleCouponIssueJob(){
        // 1.获取分片信息，作为页码，每页最多查询 20条
        int index = XxlJobHelper.getShardIndex() + 1; // 分片序号
        int size = Integer.parseInt(XxlJobHelper.getJobParam()); //// 每页大小
        // 2.查询<<未开始>>的优惠券
        Page<Coupon> page = couponService.lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.UN_ISSUE)
                .le(Coupon::getTermBeginTime, LocalDateTime.now()) // 优惠券开始时间早于当前时间
                .page(new Page<>(index, size));
        // 3.发放优惠券
        List<Coupon> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return;
        }
        couponService.beginIssueBatch(records);
    }
}
