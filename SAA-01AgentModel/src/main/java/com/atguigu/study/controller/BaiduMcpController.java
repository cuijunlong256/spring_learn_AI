package com.atguigu.study.controller;

import com.atguigu.study.service.MailService;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Optional;

@RestController
public class BaiduMcpController {

    @Resource
    private ChatClient chatClient;

    @Autowired(required = false)
    private Optional<ToolCallbackProvider> toolCallbackProviderOptional;

    @Autowired
    private MailService mailService;

    @GetMapping("/baidu/map")
    public Flux<String> queryMap(@RequestParam(name = "msg", defaultValue = "北京今天的天气") String message) throws MessagingException {
        System.out.println("========== MCP 调试信息 ==========");
        System.out.println("用户消息: " + message);

        ToolCallbackProvider provider = toolCallbackProviderOptional.orElse(null);
        if (provider != null) {
            ToolCallback[] tools = provider.getToolCallbacks();
            System.out.println("可用工具数量: " + tools.length);
            for (var tool : tools) {
                System.out.println("  - 工具: " + tool.getToolDefinition().name());
                System.out.println("    描述: " + tool.getToolDefinition().description());
            }
            mailService.sendHtmlMail("cjldanlong256@163.com", "MCP 调试信息", "<h1>MCP 调试信息</h1><p>用户消息: " + message + "</p><p>可用工具数量: " + tools.length + "</p>");
        } else {
            System.out.println("可用工具数量: 0 (MCP服务器未连接)");
        }

        System.out.println("==================================");
        return chatClient.prompt(message).stream().content();
    }




}
