package com.xiaodu.trade.service;

import com.xiaodu.trade.domain.dto.OrderDelayQueryDTO;
import com.xiaodu.trade.domain.dto.PayApplyFormDTO;
import com.xiaodu.trade.domain.vo.PayChannelVO;

import java.util.List;

public interface IPayService {
    List<PayChannelVO> queryPayChannels();

    String applyPayOrder(PayApplyFormDTO payApply);

    void queryPayResult(OrderDelayQueryDTO message);
}
