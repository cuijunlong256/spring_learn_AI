package com.atguigu.study.controller;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetectionHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIType;
import com.alibaba.cloud.ai.graph.agent.hook.pii.RedactionStrategy;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.*;
import com.alibaba.cloud.ai.graph.agent.interceptor.contextediting.ContextEditingInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HooksAndInterceptors {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HooksAndInterceptors.class);

    // ... existing code ...

    /**
     * 演示 ReactAgent 中 Hooks（钩子）和 Interceptors（拦截器）的基本配置与使用。
     * <p>
     * 该方法完成以下功能：
     * 1. 初始化 DashScope 聊天模型（基于通义千问 API）
     * 2. 创建多种 Hook（日志记录、消息修剪、对话摘要）和 Interceptor（护栏检查、工具重试）
     * 3. 构建一个 ReactAgent 并将上述 Hook 和 Interceptor 注册到 Agent 中
     * 4. 通过指定线程配置调用 Agent，并输出响应结果
     * </p>
     *
     * @throws GraphRunnerException 当 Agent 执行图运行过程中发生异常时抛出
     */
    public static void basicHooksAndInterceptors() throws GraphRunnerException {
        /*
         * 初始化 DashScope API 和聊天模型，
         * API Key 从环境变量 "aiQwen_key" 中读取
         */
        DashScopeApi dashScopeApi = DashScopeApi
                .builder()
                .apiKey(System.getenv("aiQwen_key"))
                .build();
        ChatModel chatModel = DashScopeChatModel
                .builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // 创建 Hooks 和 Interceptors
        // 创建日志模型钩子实例，用于处理模型相关的日志记录
        ModelHook loggingHook = new LoggingModelHook();
// 创建消息修剪模型钩子实例，用于处理模型消息的修剪操作
        MessagesModelHook messageTrimmingHook = new MessageTrimmingHook();
// 创建护栏拦截器实例，用于处理模型的防护和安全检查
        ModelInterceptor guardrailInterceptor = new GuardrailInterceptor();
// 创建重试工具拦截器实例，用于在工具调用失败时进行重试
        ToolInterceptor retryInterceptor = new RetryToolInterceptor();



        /*
         * 创建对话摘要 Hook，当对话消息 token 数超过 4000 时自动进行摘要压缩，
         * 摘要后保留最近 20 条消息以保持上下文连贯性
         */
        SummarizationHook summarizationHook = SummarizationHook.builder()
                .model(chatModel)
                .maxTokensBeforeSummary(4000)
                .messagesToKeep(20)
                .build();


        // 创建工具
        ToolCallback sendEmailTool = createSendEmailTool();
        ToolCallback deleteDataTool = createDeleteDataTool();
        ToolCallback searchTool = createSearchTool();
        ToolCallback databaseTool = createDatabaseTool();
        ToolCallback myTool = createSampleTool();
        ToolCallback[] tools = new ToolCallback[]{sendEmailTool, deleteDataTool};

        // 创建 Human-in-the-Loop Hook
        HumanInTheLoopHook humanReviewHook = HumanInTheLoopHook.builder()
                .approvalOn("sendEmailTool", ToolConfig.builder().description("Please confirm sending the email.").build())
                .approvalOn("deleteDataTool", ToolConfig.builder().description("Please confirm deleting the data.").build())
                .build();

        PIIDetectionHook piiDetectionHook = PIIDetectionHook.builder()
                .piiType(PIIType.EMAIL)
                .strategy(RedactionStrategy.REDACT)
                .applyToInput(true)
                .build();


        ChatModel selectorModel = chatModel; // 用于选择的另一个ChatModel

        ToolCallback tool1 = createSampleTool();
        ToolCallback tool2 = createSampleTool();


        /*
         * 构建 ReactAgent 实例，注册日志钩子、消息修剪钩子、摘要钩子，
         * 以及护栏拦截器和工具重试拦截器
         */
// 创建一个ReactAgent实例，使用建造者模式进行配置
        ReactAgent agent = ReactAgent.builder()
        // 设置代理名称为"hook_interceptor_demo"
                .name("hook_interceptor_demo")
        // 设置使用的聊天模型
                .model(chatModel)
        // 配置可用的工具集
                .tools(tools)
                .tools(searchTool,databaseTool,myTool,tool1,tool2)
        // 添加多个钩子函数，包括日志记录、消息修剪和摘要功能
                .hooks(loggingHook, messageTrimmingHook, summarizationHook,
                        humanReviewHook,piiDetectionHook)
                .hooks(ModelCallLimitHook.builder().runLimit(5).build())
        // 添加拦截器，包括护栏拦截器和重试拦截器
                .interceptors(guardrailInterceptor,retryInterceptor)
                .interceptors(ToolRetryInterceptor.builder().maxRetries(2).onFailure(ToolRetryInterceptor.OnFailureBehavior.RETURN_MESSAGE)
                        .build())
                .interceptors(ToolSelectionInterceptor.builder().selectionModel(selectorModel).build())
                .interceptors(ContextEditingInterceptor.builder().trigger(200000).clearAtLeast(6000).build())
                .saver(new MemorySaver())
        // 完成构建并返回配置好的agent实例
                .build();

        /*
         * 配置运行参数并调用 Agent，
         * 使用线程 ID "cjl" 标识本次会话，最终打印 Agent 的回复内容
         */
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

//        RunnableConfig config2= RunnableConfig.builder()
//                .threadId("1")
//                .addMetadata("user_id", "user_123")
//                .build();

//        AssistantMessage response = agent.call("获取用户信息", config2);
//        System.out.println(response.getText());

        // 测试 PII 检测 - 输入中的邮箱会被遮蔽
        AssistantMessage testPii = agent.call("我的邮箱是 test@example.com，请联系我", config);
        log.info("PII 处理后: {}", testPii.getText());

        AssistantMessage response = agent.call("查询数据库：SELECT * FROM users", config);
        System.out.println(response.getText());


    }


    public static void main(String[] args) throws GraphRunnerException {
        basicHooksAndInterceptors();
    }




    private static ToolCallback createSampleTool(){
        return FunctionToolCallback.builder("sampleTool",(CreateSampleRequest input)->"Sample result")
                .description("A sample tool")
                .inputType(CreateSampleRequest.class)
                .build();
    }

    public record CreateSampleRequest(@ToolParam(description = "Search query string") String query) {}

    private static ToolCallback createDatabaseTool(){
        return FunctionToolCallback.builder("databaseTool",(DatabaseRequest input) -> "Database query results")
                .description("Query database")
                .inputType(DatabaseRequest.class)
                .build();
    }

    private static ToolCallback createSearchTool(){
        return FunctionToolCallback.builder("searchTool",(SearchRequest input) -> "Search results")
                .description("Search the web")
                .inputType(SearchRequest.class)
                .build();
    }

    /**
     * 数据库查询请求参数
     */
    public record DatabaseRequest(@ToolParam(description = "Database query string") String query) {}

    /**
     * 搜索请求参数
     */
    public record SearchRequest(@ToolParam(description = "Search query string") String query) {}


    private static ToolCallback createDeleteDataTool(){
        return FunctionToolCallback.builder("delete_data", (DeleteDataRequest request) -> "Data deleted for: " + request.target())
                .description("Delete data from the system")
                .inputType(DeleteDataRequest.class)
                .build();
    }

    private static ToolCallback createSendEmailTool() {
        return FunctionToolCallback.builder("send_email", (SendEmailRequest request) -> "Email sent to: " + request.to())
                .description("Send an email to the specified recipient")
                .inputType(SendEmailRequest.class)
                .build();
    }

    /**
     * 删除数据请求参数
     */
    public record DeleteDataRequest(@ToolParam(description = "Target data to delete") String target) {}

    /**
     * 发送邮件请求参数
     */
    public record SendEmailRequest(
        @ToolParam(description = "Email recipient address") String to,
        @ToolParam(description = "Email subject") String subject,
        @ToolParam(description = "Email body content") String body
    ) {}


    /**
     * 重试工具拦截器
     */
    private static class RetryToolInterceptor extends ToolInterceptor {
        @Override
        public String getName() {
            return "RetryToolInterceptor";
        }

        @Override
        public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
            // 简化的实现
            return handler.call(request);
        }
    }

    /**
     * 护栏拦截器
 * 该拦截器继承自ModelInterceptor，用于对模型请求进行拦截和检查
     */
    private static class GuardrailInterceptor extends ModelInterceptor  {
    /**
     * 获取拦截器的名称
     * @return 返回拦截器的唯一标识名称
     */
        @Override
        public String getName() {
            return "guardrail_interceptor";
        }


    /**
     * 拦截器的主要处理方法
     * 该方法会在模型请求执行前后被调用，用于实现护栏逻辑
     * @param request 包含模型请求的参数和信息
     * @param handler 用于继续处理请求的调用处理器
     * @return 返回模型调用的结果，此处直接调用处理器进行处理并返回结果
     */
        @Override
        public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
            // 简化的实现
            return handler.call(request);
        }
    }

    /**
     * 消息修剪 Hook
     * 使用 MessagesModelHook 实现，在模型调用前修剪消息列表，只保留最后 10 条消息
     */
    public static class MessageTrimmingHook extends MessagesModelHook {

        private static final int MAX_MESSAGES = 10;
        @Override
        public String getName() {
            return "message_trimming";
        }

        @Override
        public HookPosition[] getHookPositions() {
            return new HookPosition[]{HookPosition.BEFORE_MODEL};
        }

        @Override
        public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
            if (previousMessages.size() > MAX_MESSAGES) {
                previousMessages = previousMessages.subList(previousMessages.size() - MAX_MESSAGES, previousMessages.size());
                return new AgentCommand(previousMessages, UpdatePolicy.REPLACE);
            }
            // 如果消息数量未超过限制，返回原始消息（不进行修改）
            return new AgentCommand(previousMessages);
        }
    }



    public static class LoggingModelHook extends ModelHook {
        @Override
        public String getName() {
            return "logging_model_hook";
        }

        @Override
        public HookPosition[] getHookPositions() {
            return new HookPosition[] {HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL};
        }

        @Override
        public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
            log.info("Before model call");
            return CompletableFuture.completedFuture(Map.of());
        }

        @Override
        public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
            log.info("After model call");
            return CompletableFuture.completedFuture(Map.of());
        }
    }
}
