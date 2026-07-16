package com.atguigu.study.controller;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MemoryExample {


    private static final Logger log = LoggerFactory.getLogger(MemoryExample.class);

    public static void basicMemoryConfiguration() throws GraphRunnerException {
        DashScopeApi dashScopeApi = DashScopeApi
                .builder()
                .apiKey(System.getenv("aiQwen_key"))
                .build();
        ChatModel chatModel = DashScopeChatModel
                .builder()
                .dashScopeApi(dashScopeApi)
                .build();
        ToolCallback toolCallback = createGetUserInfoTool();
        ToolCallback[] tools = new ToolCallback[0];


        // 用于总结的模型（可以是更便宜的模型）
        ChatModel summaryModel = chatModel;

        MessageSummarizationHook summarizationHook = new MessageSummarizationHook(
                summaryModel,
                4000,  // 在 4000 tokens 时触发总结
                20     // 总结后保留最后 20 条消息
        );

        // 创建工具
        ToolCallback getUserInfoTool = FunctionToolCallback
                .builder("get_user_name", new UserInfoTool())
                .description("查找用户信息")
                .inputType(String.class)
                .build();




// 创建一个ReactAgent实例，使用建造者模式进行配置
        ReactAgent agent =ReactAgent.builder()
    // 设置工具回调接口，用于处理工具调用
                .tools(toolCallback,getUserInfoTool)
    // 设置代理名称为"chat_agent"
                .name("chat_agent")
    // 设置保存器为MemorySaver，用于保存会话状态
                .saver(new MemorySaver())
                .systemPrompt("请简洁明了。")
                .hooks(new MessageDeletionHook(),summarizationHook)
                .model(chatModel)
                .build();

        RunnableConfig config = RunnableConfig
                .builder()
                .threadId("cjl")
                .build();
        agent.call("你好，我叫 bob", config);
        agent.call("我喜欢玩魔兽世界", config);
        agent.call("现在对狗做同样的事情", config);
        agent.call("现在对兔子做同样的事情", config);
        AssistantMessage call = agent.call("我喜欢玩什么", config);
        log.info("Assistant response: {}", call.getText());

        RunnableConfig config2= RunnableConfig.builder()
                .threadId("1")
                .addMetadata("user_id", "user_123")
                .build();

        AssistantMessage response = agent.call("获取用户信息", config2);
        System.out.println(response.getText());

    }
    public static void main(String[] args) {
        try {
            basicMemoryConfiguration();
        } catch (Exception e) {
           log.error("Error occurred: ", e);
        }
    }

    /**
     * 示例11：在工具中读取短期记忆
     */
    public static class UserInfoTool implements BiFunction<String, ToolContext, String> {

        @Override
        public String apply(String query, ToolContext toolContext) {
            RunnableConfig config = (RunnableConfig) toolContext.getContext().get("config");
            String userId = (String) config.metadata("user_id").orElse("");
            if ("user_123".equals(userId)) {
                return "用户是 John Smith";
            }
            else {
                return "未知用户";
            }
        }


    }

    @HookPositions({HookPosition.BEFORE_MODEL})
    public static class MessageSummarizationHook extends MessagesModelHook {
        private final ChatModel summaryModel;
        private final int maxTokensBeforeSummary;
        private final int messagesToKeep;

        public MessageSummarizationHook(
                ChatModel summaryModel,
                int maxTokensBeforeSummary,
                int messagesToKeep
        ) {
            this.summaryModel = summaryModel;
            this.maxTokensBeforeSummary = maxTokensBeforeSummary;
            this.messagesToKeep = messagesToKeep;
        }

        @Override
        public String getName() {
            return "message_summarization";
        }

        @Override
        public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
            int tokenLength = previousMessages.stream()
                    .mapToInt(message -> message.getText().length() / 4)
                    .sum();
            if (tokenLength < maxTokensBeforeSummary) {
                return new AgentCommand(previousMessages);
            }

            int messagesToSummarize = previousMessages.size() - messagesToKeep;
            if (messagesToSummarize <= 0) {
                // 如果消息数量不足以总结，无需更改
                return new AgentCommand(previousMessages);
            }

            List<Message> messagesToSummarizeList = previousMessages.subList(0, messagesToSummarize);
            List<Message> currentMessages = previousMessages.subList(messagesToSummarize, previousMessages.size());
            // 生成摘要
            String summary = generateSummary(messagesToSummarizeList);
            SystemMessage systemMessage = new SystemMessage("以下是之前的对话摘要: " + summary);
            List<Message> newMessages = List.of(systemMessage);
            newMessages.addAll(currentMessages);


            return new AgentCommand(newMessages, UpdatePolicy.REPLACE);
        }

        private String generateSummary(List<Message> messages) {
            StringBuilder conversation = new StringBuilder();
            for (Message message : messages) {
                conversation.append(message.getMessageType()).append(": ").append(message.getText()).append("\n");
            }
            String summaryPrompt = "请简要总结以下对话:\n\n" + conversation;
            ChatResponse call = summaryModel.call(new Prompt(new UserMessage(summaryPrompt)));
            return call.getResult().getOutput().getText();

        }
    }
    /**
     * 自定义Hook，删除前两条消息
     */
    @HookPositions({HookPosition.AFTER_MODEL})
    public static class MessageDeletionHook extends MessagesModelHook{
        @Override
        public String getName() {
            return "custom_memory";
        }

        @Override
        public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
            if (previousMessages.size() <= 2) {
                // 如果消息数量不超过2条，无需删除
                return new AgentCommand(previousMessages);
            }
            List<Message> messages = previousMessages.subList(2, previousMessages.size());
            return new AgentCommand(messages, UpdatePolicy.REPLACE);
        }
    }

    private static ToolCallback createGetUserInfoTool() {
        return FunctionToolCallback.builder("get_user_info", (GetUserInfoRequest request) -> {
            return "User info for " + request.userId();
        })
                .description("Get user information")
                .inputType(GetUserInfoRequest.class)
                .build();
    }

    public record GetUserInfoRequest(String userId) {}

}
