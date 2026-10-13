package com.xiaodu.aigc.agent;

import com.xiaodu.aigc.config.SystemPromptConfig;
import com.xiaodu.aigc.enums.AgentTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 路由智能体
 */
@Component
@RequiredArgsConstructor
public class RouteAgent extends AbstractAgent {

    private final SystemPromptConfig systemPromptConfig; // 注入系统提示语

    /**
     * 获取智能体类型
     *
     * @return
     */
    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.ROUTE;
    }

    /**
     * 获取系统提示语
     * @return
     */
    @Override
    public String systemMessage() {
        return this.systemPromptConfig.getRouteAgentSystemMessage().get();
    }
}
