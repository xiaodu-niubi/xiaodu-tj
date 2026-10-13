package com.xiaodu.learning.service;

import com.xiaodu.common.domain.dto.PageDTO;
import com.xiaodu.learning.domain.dto.ReplyDTO;
import com.xiaodu.learning.domain.po.InteractionReply;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaodu.learning.domain.query.ReplyPageQuery;
import com.xiaodu.learning.domain.vo.ReplyVO;

/**
 * <p>
 * 互动问题的回答或评论 服务类
 * </p>
 *
 * @author 虎哥
 */
public interface IInteractionReplyService extends IService<InteractionReply> {

    void saveReply(ReplyDTO replyDTO);

    PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery pageQuery, boolean isStudent);

    void hiddenReply(Long id, Boolean hidden);

    ReplyVO queryReplyById(Long id);
}
