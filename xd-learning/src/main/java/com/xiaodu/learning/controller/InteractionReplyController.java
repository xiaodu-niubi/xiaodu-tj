package com.xiaodu.learning.controller;

import com.xiaodu.common.domain.dto.PageDTO;
import com.xiaodu.learning.domain.dto.ReplyDTO;
import com.xiaodu.learning.domain.query.ReplyPageQuery;
import com.xiaodu.learning.domain.vo.ReplyVO;
import com.xiaodu.learning.service.IInteractionReplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 互动问题的回答或评论 前端控制器
 */
@RestController
@RequestMapping("/replies")
@Tag(name = "互动问答相关接口")
@RequiredArgsConstructor
public class InteractionReplyController {

    @Resource
    private IInteractionReplyService replyService;

    @Operation(summary = "新增回答或评论")
    @PostMapping
    public void saveReply(@RequestBody ReplyDTO replyDTO) {
        replyService.saveReply(replyDTO);
    }

    @Operation(summary = "分页查询回答或评论")
    @GetMapping("page")
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery pageQuery) {
        return replyService.queryReplyPage(pageQuery, false);
    }
}
