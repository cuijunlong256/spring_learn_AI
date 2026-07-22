package com.atguigu.study.controller;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;

public class SkillExample {

    private static void SkillExampleMethod() throws GraphRunnerException {
        DashScopeApi dashScopeApi = DashScopeApi
                .builder()
                .apiKey(System.getenv("aiQwen_key"))
                .build();
        ChatModel chatModel = DashScopeChatModel
                .builder()
                .dashScopeApi(dashScopeApi)
                .build();

        SkillRegistry registry = FileSystemSkillRegistry.builder()
                .projectSkillsDirectory("path/to/project/skills")
                .build();
        SkillsAgentHook hook = SkillsAgentHook.builder()
                .skillRegistry(registry)
                .build();

        ReactAgent agent = ReactAgent.builder()
                .model(chatModel)
                .hooks(hook)
                .description("description_text")
                .build();

        AssistantMessage call = agent.call("请介绍你有哪些技能");
        System.out.println(call.getText());
    }


    public static void main(String[] args) {

    }
}
