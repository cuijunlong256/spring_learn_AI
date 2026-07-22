package com.atguigu.study.service;

import com.atguigu.study.component.AmapWeatherTools;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.nio.file.Files;

@Service
public class WeatherAgentService {

    private final ChatClient chatClient;

    public WeatherAgentService(ChatModel chatModel, AmapWeatherTools amapWeatherTools) throws Exception {
        // 1. 读取 Markdown 技能文件内容作为系统提示词
        ClassPathResource resource = new ClassPathResource("search_skills/search_weather.md");
        String skillPrompt = new String(Files.readAllBytes(resource.getFile().toPath()));

        // 2. 构建 ChatClient 并绑定工具和系统提示词
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(skillPrompt)
                .defaultTools(amapWeatherTools)
                .build();
    }

    /**
     * 处理用户对话
     */
    public String chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }
}