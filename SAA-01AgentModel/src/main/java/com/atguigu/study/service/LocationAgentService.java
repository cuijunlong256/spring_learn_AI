package com.atguigu.study.service;

import com.atguigu.study.component.AmapLocationTools;
import com.atguigu.study.component.AmapWeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;


@Service
public class LocationAgentService {



    private final ChatClient chatClient;

    public LocationAgentService(ChatModel chatModel, AmapLocationTools amapLocationTools) throws Exception {
        // 1. 读取 Markdown 技能文件内容作为系统提示词
        ClassPathResource resource = new ClassPathResource("search_skills/localtion_search.md");
        String skillPrompt = new String(Files.readAllBytes(resource.getFile().toPath()));

        // 2. 构建 ChatClient 并绑定工具和系统提示词
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(skillPrompt)
                .defaultTools(amapLocationTools)
                .build();
    }

    /**
     * 处理用户消息，自动触发工具调用并返回自然语言结果。
     */
    public String chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }
}