package com.atguigu.study.controller;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.*;
import com.alibaba.cloud.ai.graph.agent.renderer.SaaStTemplateRenderer;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SpringAATest {
    public static void configureMemory() throws GraphRunnerException {

        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("aiQwen_key"))
                .build();

        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // 配置内存存储
//        ReactAgent agent = ReactAgent.builder()
//                .name("chat_agent")
//                .model(chatModel)
//                .saver(new MemorySaver())
//                .build();

        // 使用 thread_id 维护对话上下文
        RunnableConfig config = RunnableConfig.builder()
                .threadId("user_123")
                .build();

//        agent.call("我叫张三", config);
//        AssistantMessage call = agent.call("我叫什么名字？", config);// 输出: "你叫张三"
//        System.out.println(call.getText());

        ToolCallback searchTool = FunctionToolCallback
                .builder("search_tool", (SearchQuery input) -> {
                    System.out.println("[搜索工具] 搜索内容: " + input.query());
                    return "搜索结果: 关于'" + input.query() + "'的信息显示，这是一个重要的技术主题。";
                })
                .description("搜索工具，用于搜索技术信息")
                .inputType(SearchQuery.class)
                .build();

        ToolCallback analysisTool = FunctionToolCallback
                .builder("analysis_tool", (AnalysisQuery input) -> {
                    System.out.println("[分析工具] 分析内容: " + input.topic() + ", 维度: " + input.dimension());
                    return "分析结果: '" + input.topic() + "'在'" + input.dimension() + "'维度的分析显示存在多个关键点需要深入探讨。";
                })
                .description("分析工具，用于深度分析某个主题")
                .inputType(AnalysisQuery.class)
                .build();

        ToolCallback summaryTool = FunctionToolCallback
                .builder("summary_tool", (SummaryQuery input) -> {
                    System.out.println("[总结工具] 总结内容: " + input.content());
                    return "总结完成: 已对'" + input.content() + "'进行了全面总结。";
                })
                .description("总结工具，用于总结内容")
                .inputType(SummaryQuery.class)
                .build();

        TemplateRenderer customRenderer = SaaStTemplateRenderer.builder()
                .startDelimiter("{{")
                .endDelimiter("}}")
                .build();

        // 使用自定义分隔符的 systemPrompt
        String systemPrompt = """
				你是一个专业的{{role}}助手。
				你的专业领域是{{domain}}。
				请用{{language}}语言回答用户的问题。
				""";

        // 使用自定义分隔符的 instruction
        String instruction = """
				用户询问的主题是：{{topic}}
				请根据以下要求回答：
				1. 保持专业性
				2. 提供具体示例
				3. 语言要{{style}}
				""";
// 创建一个ReactAgent实例，使用构建器模式进行配置
        ReactAgent agent = ReactAgent.builder()
        // 设置代理名称为"custom_template_agent"
                .name("custom_template_agent")
        // 配置使用的聊天模型
                .model(chatModel)
        // 设置系统提示词，用于指导AI的行为
                .systemPrompt(systemPrompt)
        // 设置指令，告诉AI如何执行任务
                .instruction(instruction)
        // 自定义模板渲染器，用于处理输入和输出的格式
                .templateRenderer(customRenderer)
        // 配置保存器，使用内存保存器来存储会话状态
                .saver(new MemorySaver())
                .hooks(new LoggingHook(), ModelCallLimitHook.builder().runLimit(3).build(),new CustomStopConditionHook())
                .interceptors(new GuardrailInterceptor(),new ToolMonitoringInterceptor())
                .tools(searchTool, analysisTool, summaryTool)
                .build();
        AssistantMessage call = agent.call("我叫张三", config);
//        AssistantMessage call1 = agent.call("我叫李1", config);
//        AssistantMessage call2 = agent.call("我叫李2", config);
//        AssistantMessage call3 = agent.call("我叫李3", config);
//        AssistantMessage call4 = agent.call("我叫李4", config);
//        AssistantMessage call5 = agent.call("我叫李5", config);
        System.out.println(call.getText());
//        System.out.println(call1.getText());
//        System.out.println(call2.getText());
//        System.out.println(call3.getText());
//        System.out.println(call4.getText());
//        System.out.println(call5.getText());
        // 使用时，状态中的变量会自动替换 {{ }} 包裹的占位符
        Map<String, Object> inputs = Map.of(
                "input", """ 
                        请执行以下任务，每一步都需要单独思考和回答：
                        第一步：介绍现在的主流框架特点
                        第二步：介绍一下SOA架构
                        第三步：介绍Spring框架的核心理念
                        第四步：介绍分布式架构
                        """,
                "role", "技术专家",
                "domain", "Java企业级开发",
                "language", "中文",
                "topic", "Spring框架",
                "style", "简洁易懂"
        );
        agent.call("我叫什么", config);
        Optional<OverAllState> result = agent.invoke(inputs);
        if (result.isPresent()) {
            List<Message> messages = (List<Message>) result.get().value("messages").orElse(List.of());
            for (Message message : messages) {
                if (message instanceof AssistantMessage) {
                    System.out.println("Agent回复: " + ((AssistantMessage) message).getText());
                }
            }
        }

    }
    public record SearchQuery(String query) {}

    public record AnalysisQuery(String topic, String dimension) {}

    public record SummaryQuery(String content) {}

    public static void main(String[] args) throws GraphRunnerException {
        configureMemory();
    }


    /**
     * 示例14：AgentHook - 在 Agent 开始/结束时执行
     */
    /**
     * 注意：Hook 类定义在这里，但必须在构建 ReactAgent 时通过 .hooks(new LoggingHook()) 注册才能生效。
     * 当前 configureMemory 方法中的 agent 构建过程未注册此 Hook，因此不会执行。
 * 这是一个日志记录的 Hook 类，用于在 Agent 执行前后打印日志信息。
     */
    public static class LoggingHook extends AgentHook {
    /**
     * 获取 Hook 的名称
     * @return 返回 "logging" 作为 Hook 的名称
     */
        @Override
        public String getName() {
            return "logging";
        }

    /**
     * 定义 Hook 的执行位置
     * @return 返回一个包含 BEFORE_AGENT 和 AFTER_AGENT 的数组，表示在 Agent 执行前后执行此 Hook
     */
        @Override
        public HookPosition[] getHookPositions() {
            return new HookPosition[] {
                    HookPosition.BEFORE_AGENT,
                    HookPosition.AFTER_AGENT
            };
        }

    /**
     * 在 Agent 执行前调用的方法
     * @param state 系统的整体状态
     * @param
     * @return 返回一个已完成的 CompletableFuture，包含一个空 Map
     */
        @Override
        public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
            System.out.println("Agent 开始执行");
            return CompletableFuture.completedFuture(Map.of());
        }

    /**
     * 在 Agent 执行后调用的方法
     * @param state 系统的整体状态
     * @param config 配置信息
     * @return 返回一个已完成的 CompletableFuture，包含一个空 Map
     */
        @Override
        public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
            System.out.println("Agent 执行完成");
            return CompletableFuture.completedFuture(Map.of());
        }
    }

/**
 * GuardrailInterceptor 类是一个模型拦截器，用于拦截和处理模型请求与响应，
 * 实现内容安全检查和过滤功能。
 */
    public static class GuardrailInterceptor extends ModelInterceptor {
    /**
     * 拦截模型请求的核心方法，在模型调用前后进行检查和处理
     * @param request 模型请求对象，包含输入消息等信息
     * @param handler 模型调用处理器，用于执行实际的模型调用
     * @return 返回处理后的模型响应
     */
        @Override
        public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
            // 前置：检查输入
            if (containsSensitiveContent(request.getMessages())) {
                return ModelResponse.of(new AssistantMessage("检测到不适当的内容"));
            }
            System.out.println("======开始拦截=====");
            // 执行调用
            ModelResponse response = handler.call(request);

            System.out.println("======拦截结束=====");
            // 后置：检查输出
            return sanitizeIfNeeded(response);
        }

    /**
     * 检查消息列表中是否包含敏感内容
     * @param messages 要检查的消息列表
     * @return 如果包含敏感内容返回true，否则返回false
     */
        private boolean containsSensitiveContent(List<Message> messages) {
            // 实现敏感内容检测逻辑
            return false;
        }

    /**
     * 根据需要对响应内容进行清理处理
     * @param response 原始模型响应
     * @return 处理后的模型响应，如果不需要处理则返回原始响应
     */
        private ModelResponse sanitizeIfNeeded(ModelResponse response) {
            // 实现响应清理逻辑
            return response;
        }

    /**
     * 获取拦截器的名称
     * @return 返回拦截器名称 "GuardrailInterceptor"
     */
        @Override
        public String getName() {
            return "GuardrailInterceptor";
        }
    }

    /**
     * 示例17：ToolInterceptor - 监控和错误处理
 * 这是一个用于监控工具调用并处理错误的拦截器类
     */
    public static class ToolMonitoringInterceptor extends ToolInterceptor {
    /**
     * 拦截工具调用的方法，用于执行监控和错误处理
     * @param request 工具调用请求对象，包含工具调用的相关信息
     * @param handler 工具调用处理器，用于实际执行工具调用
     * @return ToolCallResponse 工具调用的响应结果
     */
        @Override
        public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
            long startTime = System.currentTimeMillis(); // 记录开始时间，用于计算执行耗时
            try {
                System.out.println("======开始拦截ToolMonitoringInterceptor=====");
                ToolCallResponse response = handler.call(request); // 执行实际的工具调用
                logSuccess(request, System.currentTimeMillis() - startTime); // 记录成功日志
                System.out.println("======拦截结束ToolMonitoringInterceptor=====");
                return response;
            }
            catch (Exception e) {
                logError(request, e, System.currentTimeMillis() - startTime); // 记录错误日志
            // 返回一个错误响应，提示用户工具执行遇到问题
                return ToolCallResponse.of(request.getToolCallId(), request.getToolName(),
                        "工具执行遇到问题，请稍后重试");
            }
        }

    /**
     * 记录工具调用成功的方法
     * @param request 工具调用请求对象
     * @param duration 工具执行耗时（毫秒）
     */
        private void logSuccess(ToolCallRequest request, long duration) {
            System.out.println("Tool " + request.getToolName() + " succeeded in " + duration + "ms");
        }

    /**
     * 记录工具调用错误的方法
     * @param request 工具调用请求对象
     * @param e 工具调用时抛出的异常
     * @param duration 工具执行耗时（毫秒）
     */
        private void logError(ToolCallRequest request, Exception e, long duration) {
            System.err.println("Tool " + request.getToolName() + " failed in " + duration + "ms: " + e.getMessage());
        }

    /**
     * 获取拦截器名称
     * @return 拦截器名称 "ToolMonitoringInterceptor"
     */
        @Override
        public String getName() {
            return "ToolMonitoringInterceptor";
        }
    }

/**
 * 自定义停止条件钩子类，继承自ModelHook
 * 用于在特定条件下控制模型的执行流程
 */
    public static class CustomStopConditionHook extends ModelHook {

        @Override
        public String getName() {
            return "custom_stop_condition";
        }

        @Override
        public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
// 检查是否找到答案，展示使用 OverAllState
            boolean answerFound = (Boolean) state.value("answer_found").orElse(false);
// 检查错误次数，展示使用 RunnableConfig
            int errorCount = (Integer) Optional.ofNullable(config.context().get("error_count")).orElse(0);

// 找到答案或错误过多时停止
            if (answerFound || errorCount > 3) {
                List<Message> messages = new ArrayList<>();
                messages.add(new AssistantMessage(
                        answerFound ? "已找到答案，Agent 执行完成。"
                                : "错误次数过多 (" + errorCount + ")，Agent 执行终止。"
                ));
// the messages will be appended to the original message list context.
                return CompletableFuture.completedFuture(Map.of("messages", messages));
            }

            return CompletableFuture.completedFuture(Map.of());
        }

    }
}
