package com.atguigu.study.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class McpClientConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel, Optional<ToolCallbackProvider> toolCallbackProvider) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(toolCallbackProvider.map(ToolCallbackProvider::getToolCallbacks).orElse(new org.springframework.ai.tool.ToolCallback[0]))
                .build();
    }
}
