package com.atguigu.study;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.function.BiFunction;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ToolsTest {


    public record SearchQuery(String query) {
    }

    @Test
    public void testSearchAgent(@Autowired ChatModel chatModel) throws GraphRunnerException {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("aiQwen_key"))
                .build();

        DashScopeChatModel chatModel1 = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

        // 创建工具回调
        ToolCallback toolCallback = FunctionToolCallback
                .builder("search", (SearchQuery input) -> {
                    return "搜索结果: " + input.query();
                })
                .description("搜索工具，用于搜索内容")
                .inputType(SearchQuery.class)
                .build();


        // 注册工具回调  // answer 中已经包含了基于搜索结果生成的完整回复

        //使用多个工具
        ReactAgent searchAgent = ReactAgent.builder()
                .name("search_agent")
                .model(chatModel1)
                .tools(toolCallback)
                .systemPrompt("你是一个助手，你需要根据用户输入，进行多轮问答，并使用工具进行搜索。")
                .build();

        AssistantMessage response = searchAgent.call("杭州的天气怎么样？");
        System.out.println(response.getText());

// UserMessage 输入
        UserMessage userMessage = new UserMessage("帮我分析这个问题");
        AssistantMessage response1 = searchAgent.call(userMessage);
        System.out.println(response1.getText());


        List<Message> messages = List.of(
                new UserMessage("我想了解 Java 多线程"),
                new UserMessage("特别是线程池的使用")
        );
        AssistantMessage response2 = searchAgent.call(messages);
        System.out.println(response2.getText());


    }


    @Test
    public  void instructionUsageTest01() {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("aiQwen_key"))
                .build();

        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        String instruction = """
				你是一个经验丰富的软件架构师。
				
				在回答问题时，请：
				1. 首先理解用户的核心需求
				2. 分析可能的技术方案
				3. 提供清晰的建议和理由
				4. 如果需要更多信息，主动询问
				
				保持专业、友好的语气。
				""";

        ReactAgent agent = ReactAgent.builder()
                .name("architect_agent")
                .model(chatModel)
                .instruction(instruction)
                .systemPrompt("你一个架构师，你需要根据用户输入，进行多轮问答，并给出建议。")
                .build();

        AssistantMessage response = null;
        try {
            response = agent.call("你好");
        } catch (GraphRunnerException e) {

        }
        System.out.println(response.getText());

    }





}
