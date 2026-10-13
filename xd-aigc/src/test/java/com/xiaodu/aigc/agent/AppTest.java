package com.xiaodu.aigc.agent;

import cn.hutool.core.map.MapUtil;
import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class AppTest {

    @Test
    public void testAppCall() throws Exception {
        // 构造业务参数
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ1c2VyIjp7InVzZXJJZCI6Miwicm9sZUlkIjoyLCJyZW1lbWJlck1lIjpmYWxzZX0sImV4cCI6MTc1ODEwNjk3NH0.QVqKSEE3gjo8mlN1tuWaNaFOxMWhLVXNzn2jFKeT-hw3O2xGhlPfWtFM2yLXHGM1Kg7tujKp93W7cmLjOwMDA-CkzSAt4lG_vZfvQ9_IOgrKAMFZAj6EdCjjdZrib0sX9tUtpUbLTMBM9ItR5755tL29qFcXDouJPMCPSXxOzygBonTks1awCN0CgP0HGQfrY3WAZTCm9jVa7dTWGdXC_Y5_fiCCnJikJBP-q7S8B0yk5amKxpbE8VM4uSV18J8Zvv7TI2HQF0NzFi85d9WdammwUvFxzoFjEMEAoZpcYEOueqCJ8NXWw2301iNHiTbH97MGIoGCscadSxK1ZpBfbA";
        Map<String, Object> bizParams = MapUtil.<String, Object>builder()
                .put("user_defined_tokens", MapUtil.of("tool_e8f18cf5-5a6a-400c-905f-8bcbbbef4899", // 工具id
                        MapUtil.of("user_token", token)))
                .build();

        // bizParams.add("user_defined_tokens", JsonObject);
        ApplicationParam param = ApplicationParam.builder()
                // 若没有配置环境变量，可用百炼API Key将下行替换为：.apiKey("sk-xxx")。但不建议在生产环境中直接将API Key硬编码到代码中，以减少API Key泄露风险。
                .apiKey(System.getenv("ALIYUN_API_KEY"))
                .appId("6955019c253a479582181fe4811102be") // 智能体id
                .prompt("课程推荐，20岁，本科，没有编程基础，对java感兴趣")
                .incrementalOutput(true) // 开启增量输出
                .bizParams(JsonUtils.toJsonObject(bizParams))
                .build();

        Application application = new Application();
        Flowable<ApplicationResult> result = application.streamCall(param);

        // 阻塞式的打印内容
        result.blockingForEach(data -> {
            System.out.printf("%s\n",data.getOutput().getText());
        });

    }

}
