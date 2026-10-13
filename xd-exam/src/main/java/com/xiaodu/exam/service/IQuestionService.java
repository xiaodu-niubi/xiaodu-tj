package com.xiaodu.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaodu.api.dto.exam.QuestionDTO;
import com.xiaodu.common.domain.dto.PageDTO;
import com.xiaodu.exam.domain.dto.QuestionFormDTO;
import com.xiaodu.exam.domain.po.Question;
import com.xiaodu.exam.domain.query.QuestionPageQuery;
import com.xiaodu.exam.domain.vo.QuestionDetailVO;
import com.xiaodu.exam.domain.vo.QuestionPageVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 题目 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2022-09-02
 */
public interface IQuestionService extends IService<Question> {

    void addQuestion(QuestionFormDTO questionFormDTO);

    void updateQuestion(QuestionFormDTO questionDTO);

    void deleteQuestionById(Long id);

    PageDTO<QuestionPageVO> queryQuestionByPage(QuestionPageQuery query);

    QuestionDetailVO queryQuestionDetailById(Long id);

    List<QuestionDTO> queryQuestionByIds(List<Long> ids);

    Map<Long, Integer> countQuestionNumOfCreater(List<Long> createrIds);

    List<QuestionDTO> queryQuestionByBizId(Long bizId);

    Boolean checkNameValid(String name);

    Map<Long, Integer> queryQuestionScores(List<Long> ids);
}
