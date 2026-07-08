package com.atguigu.study.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LllmConfig {


    //TODO: 1.配置LLLM的参数
    @Bean
    public DashScopeApi dashScopeApi(){
        return DashScopeApi.builder()
                .apiKey(System.getenv("aiQwen_key"))
                .build();
    }


    /**
     * 创建并配置一个ChatClient BeanD
     * 该方法使用Spring Framework的@Bean注解，将返回的ChatClient对象注册为Spring应用上下文中的一个Bean
     *
     * @param chatModel ChatModel类型的参数，用于构建ChatClient，提供与聊天模型交互的能力
     * @return 返回一个配置好的ChatClient实例，可以用于与聊天模型进行交互
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        // 使用ChatClient的构建器模式，传入chatModel参数来创建ChatClient实例
        return ChatClient.builder(chatModel).build();
    }

}

