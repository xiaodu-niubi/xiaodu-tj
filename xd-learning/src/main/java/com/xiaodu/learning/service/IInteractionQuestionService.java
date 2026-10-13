package com.xiaodu.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaodu.common.domain.dto.PageDTO;
import com.xiaodu.learning.domain.dto.QuestionFormDTO;
import com.xiaodu.learning.domain.po.InteractionQuestion;
import com.xiaodu.learning.domain.query.QuestionAdminPageQuery;
import com.xiaodu.learning.domain.query.QuestionPageQuery;
import com.xiaodu.learning.domain.vo.QuestionAdminVO;
import com.xiaodu.learning.domain.vo.QuestionVO;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author 虎哥
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {

    InteractionQuestion saveQuestion(QuestionFormDTO questionDTO);

    PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query);

    QuestionVO queryQuestionById(Long id);

    PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query);

    QuestionAdminVO queryQuestionByIdAdmin(Long id);

    void hiddenQuestion(Long id, Boolean hidden);

    void updateQuestion(Long id, QuestionFormDTO questionDTO);

    void deleteById(Long id);
}
