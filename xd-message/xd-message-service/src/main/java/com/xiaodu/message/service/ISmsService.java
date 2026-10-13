package com.xiaodu.message.service;

import com.xiaodu.api.dto.sms.SmsInfoDTO;
import com.xiaodu.api.dto.user.UserDTO;
import com.xiaodu.message.domain.po.NoticeTemplate;

import java.util.List;

public interface ISmsService {
    void sendMessageByTemplate(NoticeTemplate noticeTemplate, List<UserDTO> users);

    void sendMessage(SmsInfoDTO smsInfoDTO);

    void sendMessageAsync(SmsInfoDTO smsInfoDTO);
}
