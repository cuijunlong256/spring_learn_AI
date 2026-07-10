package com.atguigu.study.controller;




import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.renderer.SaaStTemplateRenderer;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ToolsExaplem {


    /**
     * 示例1：编程方式规范 - FunctionToolCallback
     */
   public static void programmaticToolSpecification(){
       ToolCallback toolCallback = FunctionToolCallback.builder("currentWeather", new WeatherService()).
               description("Get the current weather in a given location")
               .inputType(WeatherRequest.class)
               .build();
   }

   public static void toolCall() throws GraphRunnerException {
       DashScopeApi dashScopeApi = DashScopeApi.builder()
               .apiKey(System.getenv("aiQwen_key"))
               .build();

       ChatModel chatModel = DashScopeChatModel.builder()
               .dashScopeApi(dashScopeApi)
               .build();
       ToolCallback toolCallback = FunctionToolCallback
               .builder("currentWeather", new WeatherService())
               .description("Get the weather in location")
               .inputType(WeatherRequest.class)
               .build();

       String systemPrompt = """
				你是一个专业的{{role}}助手。
				你的专业领域是{{domain}}。
				请用{{language}}语言回答用户的问题。
				""";

       RunnableConfig config = RunnableConfig.builder()
               .threadId("user_123")
               .addMetadata("user_id", "张三")
               .build();

       // 使用自定义分隔符的 instruction
       String instruction = """
				用户询问的主题是：{{topic}}
				请根据以下要求回答：
				1. 保持专业性
				2. 提供具体示例
				3. 语言要{{style}}
				""";
       TemplateRenderer customRenderer = SaaStTemplateRenderer.builder()
               .startDelimiter("{{")
               .endDelimiter("}}")
               .build();


       ToolCallback accountTool = FunctionToolCallback
               .builder("get_account_info", new AccountInfoTool())
               .description("Get the current user's account information")
               .inputType(String.class)
               .build();



// 创建一个ReactAgent实例，使用建造者模式进行配置
       ReactAgent agent = ReactAgent.builder()
    // 设置代理名称为"custom_template_agent"
               .name("custom_template_agent")
    // 配置使用的聊天模型
               .model(chatModel)
    // 设置系统提示词，用于指导AI的行为
               .systemPrompt(systemPrompt)
    // 设置指令，告诉AI需要执行的具体任务
               .instruction(instruction)
    // 自定义模板渲染器，用于处理输入输出的格式化
               .templateRenderer(customRenderer)
    // 配置保存器，使用MemorySaver来存储会话状态
               .saver(new MemorySaver())
               .hooks(new SpringAATest.LoggingHook(), ModelCallLimitHook.builder().runLimit(3).build(),new SpringAATest.CustomStopConditionHook())
               .interceptors(new SpringAATest.GuardrailInterceptor(),new SpringAATest.ToolMonitoringInterceptor())
               .tools(toolCallback,accountTool)
               .build();
//       AssistantMessage call = agent.call("我叫张三", config);
//       System.out.println(call.getText());
//       AssistantMessage call1 = agent.call("我叫什么", config);
//       System.out.println(call1.getText());
//       AssistantMessage call2 = agent.call("我的账户信息", config);
//       System.out.println(call2.getText());
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
//       agent.call("我叫什么", config);
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

    public static void main(String[] args) throws GraphRunnerException {
        // TODO Auto-generated method stub
        toolCall();

    }
    /**
     * 账户信息工具
 * 这是一个实现了BiFunction接口的工具类，用于根据用户ID查询账户信息
     */
    public static class AccountInfoTool implements BiFunction<String, ToolContext, String> {

    /**
     * 用户数据库，存储了用户ID与账户信息的映射关系
     * 使用Map.of创建不可变Map，包含两个示例用户：user123和user456
     * 每个用户信息包含姓名、账户类型、余额和邮箱等字段
     */
        private static final Map<String, Map<String, Object>> USER_DATABASE = Map.of(
                "user123", Map.of(
                        "name", "Alice Johnson",
                        "account_type", "Premium",
                        "balance", 5000,
                        "email", "alice@example.com"
                ),
                "user456", Map.of(
                        "name", "Bob Smith",
                        "account_type", "Standard",
                        "balance", 1200,
                        "email", "bob@example.com"
                ),
            "张三", Map.of(
                    "name", "张三",
                    "account_type", "Standard",
                    "balance", 100000,
                    "email", "aaa@example.com"
            )
        );

    /**
     * 实现BiFunction的apply方法，处理查询请求
     * @param query 查询参数，此实现中未使用
     * @param toolContext 工具上下文，包含配置信息
     * @return 返回格式化的账户信息字符串，或错误信息
     */
        @Override
        public String apply(String query, ToolContext toolContext) {
        // 从工具上下文中获取配置信息
            RunnableConfig config = (RunnableConfig) toolContext.getContext().get("config");
        // 从配置中获取用户ID

            // 增加非空判断，防止空指针异常
            if (config == null) {
                return "Config not provided in tool context";
            }

            String userId = (String) config.metadata("user_id").orElse(null);

        // 检查用户ID是否存在
            if (userId == null) {
                return "User ID not provided";
            }

        // 从用户数据库中获取用户信息
            Map<String, Object> user = USER_DATABASE.get(userId);
            if (user != null) {
            // 如果用户存在，返回格式化的账户信息
                return String.format(
                        "Account holder: %s\nType: %s\nBalance: $%d",
                        user.get("name"),
                        user.get("account_type"),
                        user.get("balance")
                );
            }

        // 如果用户不存在，返回错误信息
            return "User not found";
        }
    }

    public enum Unit {C, F}

    // ==================== 访问上下文 ====================

    public enum UnitType {CELSIUS, FAHRENHEIT}

    public static class WeatherService implements Function<WeatherRequest, WeatherResponse> {
        @Override
        public WeatherResponse apply(WeatherRequest request) {
            return new WeatherResponse(30.0, Unit.C);
        }
    }

    public record WeatherRequest(@ToolParam(description = "城市或坐标") String location,
                                 Unit unit) { }

    public record WeatherResponse(double temp, Unit unit){
    }

}
