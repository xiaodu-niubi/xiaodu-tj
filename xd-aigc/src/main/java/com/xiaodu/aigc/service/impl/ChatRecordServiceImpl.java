package com.xiaodu.aigc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaodu.aigc.entity.ChatRecord;
import com.xiaodu.aigc.mapper.ChatRecordMapper;
import com.xiaodu.aigc.service.ChatRecordService;
import org.springframework.stereotype.Service;

@Service
public class ChatRecordServiceImpl extends ServiceImpl<ChatRecordMapper, ChatRecord> implements ChatRecordService {
}
