package com.xiaodu.message.controller;

import com.xiaodu.api.dto.sms.SmsInfoDTO;
import com.xiaodu.message.service.ISmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信发送控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("sms")
@Tag(name = "短信发送控制器")
public class SmsController {

    private final ISmsService smsService;

    @Operation(summary = "同步发送短信")
    @PostMapping("message")
    public void sendMessage(@RequestBody SmsInfoDTO smsInfoDTO) {
        smsService.sendMessageAsync(smsInfoDTO);
    }
}
