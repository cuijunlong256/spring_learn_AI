package com.atguigu.study;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.StaticToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {
    org.springframework.ai.mcp.client.common.autoconfigure.McpClientAutoConfiguration.class,
    org.springframework.ai.mcp.client.common.autoconfigure.McpToolCallbackAutoConfiguration.class
})
public class SAABaiduMcpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SAABaiduMcpClientApplication.class, args);
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolCallbackProvider toolCallbackProvider() {
        return new StaticToolCallbackProvider(new ToolCallback[0]);
    }
}


